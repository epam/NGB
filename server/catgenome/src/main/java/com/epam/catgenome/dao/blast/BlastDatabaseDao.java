/*
 * MIT License
 *
 * Copyright (c) 2016-2021 EPAM Systems
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
package com.epam.catgenome.dao.blast;

import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.blast.BlastDatabase;
import com.epam.catgenome.entity.blast.BlastDatabaseSource;
import com.epam.catgenome.entity.blast.BlastDatabaseType;
import com.epam.catgenome.util.db.QueryParameters;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static com.epam.catgenome.util.Utils.addParametersToQuery;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BlastDatabaseDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;
    private String databaseSequenceName;
    private String insertDatabaseQuery;
    private String updateDatabaseQuery;
    private String deleteDatabaseQuery;
    private String loadDatabasesQuery;
    private String loadDatabaseQuery;

    /**
     * Persists a new or updates existing Blast database record.
     * @param database {@code BlastDatabase} a Blast database to persist.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveDatabase(final BlastDatabase database) {
        if (database.getId() == null) {
            database.setId(daoHelper.createId(databaseSequenceName));
            getNamedParameterJdbcTemplate().update(insertDatabaseQuery, DatabaseParameters.getParameters(database));
        } else {
            getNamedParameterJdbcTemplate().update(updateDatabaseQuery, DatabaseParameters.getParameters(database));
        }
    }

    /**
     * Deletes a Blast database entity, specified by ID, from the database
     * @param id ID of a record to delete
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteDatabase(final long id) {
        getJdbcTemplate().update(deleteDatabaseQuery, id);
    }

    /**
     * Loads {@code Blast databases} from a database by type.
     * @param queryParameters {@code QueryParameters} query parameters
     * @return a {@code List<BlastDatabase>} from the database
     */
    public List<BlastDatabase> loadDatabases(final QueryParameters queryParameters) {
        String query = addParametersToQuery(loadDatabasesQuery, queryParameters);
        return getJdbcTemplate().query(query, DatabaseParameters.getRowMapper());
    }

    /**
     * Loads a {@code Blast database} instance from a database by it's ID.
     * @param id {@code long} an ID of a database
     * @return a {@code BlastDatabase} instance from the database
     */
    public BlastDatabase loadDatabase(final long id) {
        List<BlastDatabase> blastDatabases = getJdbcTemplate().query(loadDatabaseQuery,
                DatabaseParameters.getRowMapper(), id);
        return blastDatabases.isEmpty() ? null : blastDatabases.get(0);
    }

    enum DatabaseParameters {
        DATABASE_ID,
        DATABASE_NAME,
        DATABASE_PATH,
        DATABASE_TYPE,
        DATABASE_SOURCE,
        DATABASE_REFERENCE_ID;

        static MapSqlParameterSource getParameters(final BlastDatabase database) {
            MapSqlParameterSource params = new MapSqlParameterSource();

            params.addValue(DATABASE_ID.name(), database.getId());
            params.addValue(DATABASE_NAME.name(), database.getName());
            params.addValue(DATABASE_PATH.name(), database.getPath());
            params.addValue(DATABASE_TYPE.name(), database.getType().getTypeId());
            params.addValue(DATABASE_SOURCE.name(), database.getSource().getSourceId());
            params.addValue(DATABASE_REFERENCE_ID.name(), database.getReferenceId());

            return params;
        }

        static RowMapper<BlastDatabase> getRowMapper() {
            return (rs, rowNum) -> parseDatabase(rs);
        }

        static BlastDatabase parseDatabase(final ResultSet rs) throws SQLException {
            final BlastDatabase database = new BlastDatabase();
            database.setId(rs.getLong(DATABASE_ID.name()));
            database.setName(rs.getString(DATABASE_NAME.name()));
            database.setPath(rs.getString(DATABASE_PATH.name()));
            final long typeVal = rs.getLong(DATABASE_TYPE.name());
            if (!rs.wasNull()) {
                database.setType(BlastDatabaseType.getTypeById(typeVal));
            }
            final long sourceVal = rs.getLong(DATABASE_SOURCE.name());
            if (!rs.wasNull()) {
                database.setSource(BlastDatabaseSource.getSourceById(sourceVal));
            }
            final long referenceId = rs.getLong(DATABASE_REFERENCE_ID.name());
            if (!rs.wasNull()) {
                database.setReferenceId(referenceId);
            }
            return database;
        }
    }
}
