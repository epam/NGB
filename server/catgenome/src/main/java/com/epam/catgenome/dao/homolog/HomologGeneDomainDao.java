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
public class HomologGeneDomainDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;
    private String groupSequenceName;
    private String insertGroupQuery;
    private String deleteGroupsQuery;
    private String loadGroupQuery;

    /**
     * Persists a new or updates existing Homolog Group record.
     * @param homologGroup {@code HomologGroup} a Homolog Group to persist.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void save(final HomologGroup homologGroup) {
        homologGroup.setId(daoHelper.createId(groupSequenceName));
        getNamedParameterJdbcTemplate().update(insertGroupQuery, GroupParameters.getParameters(homologGroup));
    }

    /**
     * Deletes Homolog groups from the database
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteGroups() {
        getJdbcTemplate().update(deleteGroupsQuery);
    }

    /**
     * Loads {@code Homolog groups} from a database by parameters.
     * @param queryParameters {@code QueryParameters} query parameters
     * @return a {@code List<HomologGroup>} from the database
     */
    public List<HomologGroup> loadGroup(final QueryParameters queryParameters) {
        String query = addParametersToQuery(loadGroupQuery, queryParameters);
        return getJdbcTemplate().query(query, GroupParameters.getRowMapper());
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
            params.addValue(PRIMARY_GENE_ID.name(), homologGroup.getPrimaryGeneId());
            params.addValue(PRIMARY_GENE_TAX_ID.name(), homologGroup.getPrimaryGeneTaxId());
            params.addValue(PRIMARY_GENE_NAME.name(), homologGroup.getPrimaryGeneName());
            params.addValue(PROTEIN_NAME.name(), homologGroup.getProteinName());
            params.addValue(DATABASE_ID.name(), homologGroup.getDatabaseId());
            return params;
        }

        static RowMapper<HomologGroup> getRowMapper() {
            return (rs, rowNum) -> parseGroup(rs);
        }

        static HomologGroup parseGroup(final ResultSet rs) throws SQLException {
            final HomologGroup group = new HomologGroup();
            group.setId(rs.getLong(ID.name()));
            group.setPrimaryGeneId(PRIMARY_GENE_ID.name());
            group.setPrimaryGeneTaxId(rs.getLong(PRIMARY_GENE_TAX_ID.name()));
            group.setPrimaryGeneName(rs.getString(PRIMARY_GENE_NAME.name()));
            group.setProteinName(PROTEIN_NAME.name());
            group.setDatabaseId(rs.getLong(DATABASE_ID.name()));
            return group;
        }
    }
}
