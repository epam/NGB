/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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
package com.epam.catgenome.dao.homolog;

import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.blast.BlastDatabase;
import com.epam.catgenome.entity.blast.BlastDatabaseSource;
import com.epam.catgenome.entity.blast.BlastDatabaseType;
import com.epam.catgenome.entity.externaldb.homolog.HomologGroup;
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
public class HomologDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;
    private String groupSequenceName;
    private String insertGroupQuery;
    private String updateGroupQuery;
    private String deleteDatabaseQuery;
    private String loadDatabasesQuery;
    private String loadDatabaseQuery;

    /**
     * Persists a new or updates existing Homolog Group record.
     * @param homologGroup {@code HomologGroup} a Homolog Group to persist.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveDatabase(final HomologGroup homologGroup) {
        if (homologGroup.getId() == null) {
            homologGroup.setId(daoHelper.createId(groupSequenceName));
            getNamedParameterJdbcTemplate().update(insertGroupQuery, GroupParameters.getParameters(homologGroup));
        } else {
            getNamedParameterJdbcTemplate().update(updateGroupQuery, GroupParameters.getParameters(homologGroup));
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
        return getJdbcTemplate().query(query, GroupParameters.getRowMapper());
    }

    /**
     * Loads a {@code Blast database} instance from a database by it's ID.
     * @param id {@code long} an ID of a database
     * @return a {@code BlastDatabase} instance from the database
     */
    public BlastDatabase loadDatabase(final long id) {
        List<BlastDatabase> blastDatabases = getJdbcTemplate().query(loadDatabaseQuery,
                GroupParameters.getRowMapper(), id);
        return blastDatabases.isEmpty() ? null : blastDatabases.get(0);
    }

    enum GroupParameters {
        ID,
        PRIMARY_GENE_ID,
        PRIMARY_GENE_TAX_ID,
        PRIMARY_GENE_NAME,
        PROTEIN_NAME,
        DATABASE_ID;

        static MapSqlParameterSource getParameters(final HomologGroup homologGroup) {
            MapSqlParameterSource params = new MapSqlParameterSource();

            params.addValue(ID.name(), homologGroup.getId());
            params.addValue(PRIMARY_GENE_ID.name(), homologGroup.getPrimary_gene_name());
            params.addValue(DATABASE_PATH.name(), homologGroup.getPath());
            params.addValue(DATABASE_TYPE.name(), homologGroup.getType().getTypeId());
            params.addValue(DATABASE_SOURCE.name(), homologGroup.getSource().getSourceId());

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
            return database;
        }
    }
}
