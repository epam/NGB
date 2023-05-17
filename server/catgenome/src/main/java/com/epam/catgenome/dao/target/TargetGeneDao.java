/*
 * MIT License
 *
 * Copyright (c) 2023 EPAM Systems
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
package com.epam.catgenome.dao.target;

import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.target.TargetGene;
import lombok.AllArgsConstructor;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TargetGeneDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;
    private String targetGeneSequenceName;
    private String insertTargetGeneQuery;
    private String loadTargetGenesQuery;
    private String deleteTargetGenesQuery;
    private String loadSpeciesNamesQuery;

    /**
     * Creates new TargetGene records.
     * @param targetId {@code long} parameters
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<TargetGene> saveTargetGenes(final List<TargetGene> targetGenes, final long targetId) {
        final List<TargetGene> genes = new ArrayList<>();
        if (!CollectionUtils.isEmpty(targetGenes)) {
            final List<Long> newIds = daoHelper.createIds(targetGeneSequenceName, targetGenes.size());
            final MapSqlParameterSource[] params = new MapSqlParameterSource[targetGenes.size()];
            for (int i = 0; i < newIds.size(); i++) {
                final TargetGene targetGene = targetGenes.get(i);
                targetGene.setTargetGeneId(newIds.get(i));
                targetGene.setTargetId(targetId);
                params[i] = TargetGeneDao.TargetGeneParameters.getParameters(targetGene);
                genes.add(targetGene);
            }
            getNamedParameterJdbcTemplate().batchUpdate(insertTargetGeneQuery, params);
        }
        return genes;
    }

    public List<TargetGene> loadTargetGenes(final Set<Long> targetIds) {
        final String query = DaoHelper.replaceInClause(loadTargetGenesQuery, targetIds.size());
        return getJdbcTemplate().query(query, TargetGeneParameters.getRowMapper(), targetIds.toArray());
    }

    /**
     * Deletes Target genes from the database
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteTargetGenes(final Long targetId) {
        getJdbcTemplate().update(deleteTargetGenesQuery, targetId);
    }

    public List<String> loadSpeciesNames() {
        return getJdbcTemplate().queryForList(loadSpeciesNamesQuery, String.class);
    }

    enum TargetGeneParameters {
        TARGET_GENE_ID,
        TARGET_ID,
        GENE_ID,
        GENE_NAME,
        TAX_ID,
        SPECIES_NAME,
        PRIORITY;

        static MapSqlParameterSource getParameters(final TargetGene targetGene) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(TARGET_GENE_ID.name(), targetGene.getTargetGeneId());
            params.addValue(TARGET_ID.name(), targetGene.getTargetId());
            params.addValue(GENE_ID.name(), targetGene.getGeneId());
            params.addValue(GENE_NAME.name(), targetGene.getGeneName());
            params.addValue(TAX_ID.name(), targetGene.getTaxId());
            params.addValue(SPECIES_NAME.name(), targetGene.getSpeciesName());
            params.addValue(PRIORITY.name(), targetGene.getPriority());
            return params;
        }

        static RowMapper<TargetGene> getRowMapper() {
            return (rs, rowNum) -> parseTargetGene(rs);
        }

        static TargetGene parseTargetGene(final ResultSet rs) throws SQLException {
            return TargetGene.builder()
                    .targetGeneId(rs.getLong(TARGET_GENE_ID.name()))
                    .targetId(rs.getLong(TARGET_ID.name()))
                    .geneId(rs.getString(GENE_ID.name()))
                    .geneName(rs.getString(GENE_NAME.name()))
                    .taxId(rs.getLong(TAX_ID.name()))
                    .speciesName(rs.getString(SPECIES_NAME.name()))
                    .priority(rs.getInt(PRIORITY.name()))
                    .build();
        }
    }
}
