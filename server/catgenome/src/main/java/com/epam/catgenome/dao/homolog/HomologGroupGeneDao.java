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
import com.epam.catgenome.entity.externaldb.homolog.HomologGroupGene;
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
public class HomologGroupGeneDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;
    private String groupGeneSequenceName;
    private String insertGroupGeneQuery;
    private String deleteGroupGenesQuery;
    private String loadGroupGeneQuery;

    /**
     * Persists a new or updates existing Homolog Group Gene record.
     * @param gene {@code HomologGroupGene} a Homolog Group Gene to persist.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void save(final HomologGroupGene gene) {
        gene.setId(daoHelper.createId(groupGeneSequenceName));
        getNamedParameterJdbcTemplate().update(insertGroupGeneQuery, GroupGeneParameters.getParameters(gene));
    }

    /**
     * Deletes Homolog groups from the database
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteGroupGenes() {
        getJdbcTemplate().update(deleteGroupGenesQuery);
    }

    /**
     * Loads {@code Homolog groups} from a database by parameters.
     * @param queryParameters {@code QueryParameters} query parameters
     * @return a {@code List<HomologGroup>} from the database
     */
    public List<HomologGroupGene> loadGroupGene(final QueryParameters queryParameters) {
        String query = addParametersToQuery(loadGroupGeneQuery, queryParameters);
        return getJdbcTemplate().query(query, GroupGeneParameters.getRowMapper());
    }

    enum GroupGeneParameters {
        ID,
        GROUP_ID,
        GENE_ID,
        TAX_ID;

        static MapSqlParameterSource getParameters(final HomologGroupGene gene) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(ID.name(), gene.getId());
            params.addValue(GROUP_ID.name(), gene.getGroupId());
            params.addValue(GENE_ID.name(), gene.getGeneId());
            params.addValue(TAX_ID.name(), gene.getTaxId());
            return params;
        }

        static RowMapper<HomologGroupGene> getRowMapper() {
            return (rs, rowNum) -> parseGroupGene(rs);
        }

        static HomologGroupGene parseGroupGene(final ResultSet rs) throws SQLException {
            final HomologGroupGene gene = new HomologGroupGene();
            gene.setId(rs.getLong(ID.name()));
            gene.setGroupId(rs.getLong(GROUP_ID.name()));
            gene.setGeneId(rs.getLong(GENE_ID.name()));
            gene.setTaxId(rs.getLong(TAX_ID.name()));
            return gene;
        }
    }
}
