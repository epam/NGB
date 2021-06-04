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

import java.util.List;

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
     * @param type {@code BlastDatabaseType} a type of databases
     * @return a {@code List<BlastDatabase>} from the database
     */
    public List<BlastDatabase> loadDatabases(final BlastDatabaseType type) {
        String query = type == null ? loadDatabasesQuery : loadDatabasesQuery
                + " WHERE type = " + type.getTypeId();
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
        NAME,
        PATH,
        TYPE,
        SOURCE;

        static MapSqlParameterSource getParameters(final BlastDatabase database) {
            MapSqlParameterSource params = new MapSqlParameterSource();

            params.addValue(DATABASE_ID.name(), database.getId());
            params.addValue(NAME.name(), database.getName());
            params.addValue(PATH.name(), database.getPath());
            params.addValue(TYPE.name(), database.getType().getTypeId());
            params.addValue(SOURCE.name(), database.getSource().getSourceId());

            return params;
        }

        static RowMapper<BlastDatabase> getRowMapper() {
            return (rs, rowNum) -> {
                BlastDatabase database = new BlastDatabase();

                database.setId(rs.getLong(DATABASE_ID.name()));
                database.setName(rs.getString(NAME.name()));
                database.setPath(rs.getString(PATH.name()));
                long typeVal = rs.getLong(TYPE.name());
                if (!rs.wasNull()) {
                    database.setType(BlastDatabaseType.getTypeById(typeVal));
                }
                long sourceVal = rs.getLong(SOURCE.name());
                if (!rs.wasNull()) {
                    database.setSource(BlastDatabaseSource.getSourceById(sourceVal));
                }
                return database;
            };
        }
    }
}
