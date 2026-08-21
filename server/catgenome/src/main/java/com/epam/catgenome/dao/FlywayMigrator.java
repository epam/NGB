/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.epam.catgenome.dao;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * Runs the Flyway migration of the NGB schema at context startup.
 *
 * <p>Replaces the {@code <bean class="org.flywaydb.core.Flyway" init-method="migrate"/>} that
 * {@code applicationContext-flyway.xml} used until Phase 5 of the Java 21 migration. Flyway 10
 * removed the JavaBean setter API that bean relied on, so the configuration now goes through
 * {@link Flyway#configure()} and this class exists to hold it. Everything the XML used to set is
 * still a property here, and the file is still copied in per database flavour from
 * {@code profiles/<flavour>/}, so the build-time flavour switch is unchanged.
 *
 * <p>It also carries the one-time upgrade of the schema-history table, which is the reason this is
 * a class and not three lines of XML. Databases created by NGB up to and including 2.7.x have a
 * {@code schema_version} table written by Flyway 3, and Flyway 10+ cannot write to it:
 *
 * <ul>
 *   <li>the {@code version_rank} column Flyway 3 maintained was dropped in Flyway 4, so an insert
 *       from a modern Flyway leaves it null and the {@code NOT NULL} constraint rejects the row -
 *       every future migration fails, and on H2 it fails <em>after</em> the migration's own DDL has
 *       been committed;</li>
 *   <li>the primary key moved from {@code version} to {@code installed_rank} and {@code version}
 *       became nullable, which repeatable migrations need;</li>
 *   <li>the checksum algorithm changed between Flyway 3 and 10, so all ~59 recorded checksums
 *       mismatch and {@code validate} fails even though not one script was edited.</li>
 * </ul>
 *
 * <p>{@link #migrate()} therefore looks for {@code version_rank}, and only if it is there converts
 * the table and calls {@link Flyway#repair()} before migrating. The condition matters: repairing
 * unconditionally on every startup would silently accept genuinely edited migration scripts, which
 * is the failure Flyway's checksums exist to catch.
 */
public class FlywayMigrator implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlywayMigrator.class);

    /**
     * NGB has always used Flyway 3's table name. Flyway 4 renamed the default to
     * {@code flyway_schema_history}, so leaving this unset would make a modern Flyway create an
     * empty history table next to the real one and replay all 59 migrations onto a populated
     * schema.
     */
    private static final String SCHEMA_HISTORY_TABLE = "schema_version";

    private static final String LEGACY_RANK_COLUMN = "version_rank";

    private static final String ALTER_TABLE = "ALTER TABLE ";

    private DataSource dataSource;
    private String schema;
    private List<String> locations;
    private Map<String, String> placeholders = new LinkedHashMap<>();
    private String sqlMigrationPrefix = "v";

    @Override
    public void afterPropertiesSet() {
        migrate();
    }

    public void migrate() {
        final FluentConfiguration configuration = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .table(SCHEMA_HISTORY_TABLE)
                .sqlMigrationPrefix(sqlMigrationPrefix)
                .locations(locations.toArray(new String[0]))
                .placeholders(placeholders)
                // Flyway 4+ would otherwise refuse to touch a non-empty schema that has no history
                // table. NGB's schema is only ever created by these scripts, so the only way to be
                // in that state is a partially restored backup, where failing is the right answer.
                .baselineOnMigrate(false);
        final Flyway flyway = configuration.load();

        if (hasLegacySchemaHistory()) {
            LOGGER.warn("Found a Flyway 3 schema history table {}.{}, written by NGB 2.7.x or "
                    + "earlier. Converting it to the current layout and realigning checksums; this "
                    + "happens once, on the first start of the upgraded server.",
                    schema, SCHEMA_HISTORY_TABLE);
            upgradeLegacySchemaHistory();
            flyway.repair();
            LOGGER.warn("Schema history of {} converted and checksums realigned.", schema);
        }

        flyway.migrate();
    }

    /**
     * True when the schema-history table exists and still carries Flyway 3's {@code version_rank}
     * column. Uses {@link DatabaseMetaData} rather than {@code information_schema} because the two
     * flavours disagree on identifier case: Flyway 3 created the table as quoted lower case on H2
     * ({@code CATGENOME."schema_version"}) and unquoted on PostgreSQL.
     */
    private boolean hasLegacySchemaHistory() {
        try (Connection connection = dataSource.getConnection()) {
            final DatabaseMetaData metaData = connection.getMetaData();
            for (final String schemaPattern : new String[] {schema, schema.toLowerCase(),
                                                            schema.toUpperCase()}) {
                try (ResultSet columns = metaData.getColumns(null, schemaPattern,
                        SCHEMA_HISTORY_TABLE, LEGACY_RANK_COLUMN)) {
                    if (columns.next()) {
                        return true;
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to inspect the Flyway schema history table of "
                    + schema, e);
        }
    }

    /**
     * Brings a Flyway 3 {@code schema_version} table to the layout Flyway 10+ writes: no
     * {@code version_rank}, primary key on {@code installed_rank}, nullable {@code version}.
     *
     * <p>Deliberately not a Flyway migration of its own - it has to run before Flyway can record
     * anything at all. Each statement is issued separately and tolerantly, because the exact
     * constraint names differ between the two flavours and between Flyway 3.x patch releases.
     */
    private void upgradeLegacySchemaHistory() {
        final String table = qualifiedHistoryTable();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            // The primary key was on `version` and has to move to `installed_rank`. Its name
            // differs between the flavours and between Flyway 3.x patch releases, so it is looked
            // up rather than assumed.
            final String primaryKey = primaryKeyName(connection);
            if (primaryKey != null) {
                execute(statement, ALTER_TABLE + table + " DROP CONSTRAINT " + quoted(primaryKey));
            }
            execute(statement, ALTER_TABLE + table + " DROP COLUMN " + quoted(LEGACY_RANK_COLUMN));
            execute(statement, ALTER_TABLE + table + " ADD CONSTRAINT "
                    + quoted(SCHEMA_HISTORY_TABLE + "_pk") + " PRIMARY KEY ("
                    + quoted("installed_rank") + ")");
            dropNotNullOnVersion(statement, table);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to convert the Flyway schema history table of "
                    + schema + " to the current layout. Nothing beyond the statements logged above "
                    + "has run; restore the pre-upgrade backup and see "
                    + "docs/md/installation/database-upgrade.md.", e);
        }
    }

    private void execute(final Statement statement, final String sql) throws SQLException {
        LOGGER.info("Schema history upgrade: {}", sql);
        statement.execute(sql);
    }

    /**
     * {@code version} is {@code NOT NULL} under Flyway 3 and nullable from Flyway 4, which is what
     * repeatable migrations need. PostgreSQL spells the change {@code DROP NOT NULL} and H2 spells
     * it {@code SET NULL}, so both are tried; a database where neither applies simply has no such
     * constraint and is already in the wanted state.
     */
    private void dropNotNullOnVersion(final Statement statement, final String table)
            throws SQLException {
        final String prefix = ALTER_TABLE + table + " ALTER COLUMN " + quoted("version") + " ";
        try {
            execute(statement, prefix + "DROP NOT NULL");
        } catch (SQLException e) {
            LOGGER.info("Schema history upgrade: DROP NOT NULL was rejected ({}), retrying with the "
                    + "H2 spelling.", e.getMessage());
            execute(statement, prefix + "SET NULL");
        }
    }

    private String primaryKeyName(final Connection connection) throws SQLException {
        for (final String schemaPattern : new String[] {schema, schema.toLowerCase(),
                                                        schema.toUpperCase()}) {
            try (ResultSet keys = connection.getMetaData()
                    .getPrimaryKeys(null, schemaPattern, SCHEMA_HISTORY_TABLE)) {
                if (keys.next()) {
                    return keys.getString("PK_NAME");
                }
            }
        }
        return null;
    }

    /**
     * Flyway 3 created the history table quoted-lower-case on H2 and unquoted on PostgreSQL, where
     * unquoted folds to lower case anyway - so quoting the lower-case name is correct on both.
     */
    private String qualifiedHistoryTable() {
        return schema + "." + quoted(SCHEMA_HISTORY_TABLE);
    }

    private String quoted(final String identifier) {
        return "\"" + identifier + "\"";
    }

    public void setDataSource(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setSchema(final String schema) {
        this.schema = schema;
    }

    public void setLocations(final List<String> locations) {
        this.locations = locations;
    }

    public void setPlaceholders(final Map<String, String> placeholders) {
        this.placeholders = placeholders;
    }

    public void setSqlMigrationPrefix(final String sqlMigrationPrefix) {
        this.sqlMigrationPrefix = sqlMigrationPrefix;
    }
}
