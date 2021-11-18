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
package com.epam.catgenome.dao.blast;

import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.blast.BlastDatabaseOrganism;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BlastDatabaseOrganismDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;
    private String databaseOrganismSequenceName;
    private String insertDatabaseOrganismQuery;
    private String deleteDatabaseOrganismsQuery;
    private String loadDatabaseOrganismsQuery;

    /**
     * Persists new Blast database organism records.
     * @param organisms {@code BlastDatabaseOrganism} Blast database organisms to persist.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void save(final List<BlastDatabaseOrganism> organisms) {
        if (!CollectionUtils.isEmpty(organisms)) {
            final List<Long> newIds = daoHelper.createIds(databaseOrganismSequenceName, organisms.size());
            final MapSqlParameterSource[] params = new MapSqlParameterSource[organisms.size()];
            for (int i = 0; i < organisms.size(); i++) {
                organisms.get(i).setDatabaseOrganismId(newIds.get(i));
                params[i] = DatabaseOrganismParameters.getParameters(organisms.get(i));
            }
            getNamedParameterJdbcTemplate().batchUpdate(insertDatabaseOrganismQuery, params);
        }
    }

    /**
     * Deletes a Blast database organisms entities, specified by BLAST database ID, from the database
     * @param databaseId ID of records to delete
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void delete(final long databaseId) {
        getJdbcTemplate().update(deleteDatabaseOrganismsQuery, databaseId);
    }

    /**
     * Loads {@code BlastDatabaseOrganism organisms} from a database by database id.
     * @param databaseId {@code long} an ID of a database
     * @return a {@code List<BlastDatabaseOrganism>} from the database
     */
    public List<BlastDatabaseOrganism> loadDatabaseOrganisms(final long databaseId) {
        return getJdbcTemplate().query(loadDatabaseOrganismsQuery,
                DatabaseOrganismParameters.getRowMapper(), databaseId);
    }

    enum DatabaseOrganismParameters {
        DATABASE_ORGANISM_ID,
        DATABASE_ID,
        TAX_ID;

        static MapSqlParameterSource getParameters(final BlastDatabaseOrganism organism) {
            MapSqlParameterSource params = new MapSqlParameterSource();

            params.addValue(DATABASE_ORGANISM_ID.name(), organism.getDatabaseOrganismId());
            params.addValue(DATABASE_ID.name(), organism.getDatabaseId());
            params.addValue(TAX_ID.name(), organism.getTaxId());

            return params;
        }

        static RowMapper<BlastDatabaseOrganism> getRowMapper() {
            return (rs, rowNum) -> parseDatabaseOrganism(rs);
        }

        static BlastDatabaseOrganism parseDatabaseOrganism(final ResultSet rs) throws SQLException {
            return BlastDatabaseOrganism.builder()
                    .databaseOrganismId(rs.getLong(DATABASE_ORGANISM_ID.name()))
                    .databaseId(rs.getLong(DATABASE_ID.name()))
                    .taxId(rs.getLong(TAX_ID.name()))
                    .build();
        }
    }
}
