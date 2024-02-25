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
import com.epam.catgenome.entity.target.TargetGenePriority;
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
import java.util.*;

import static com.epam.catgenome.util.Utils.addClauseToQuery;
import static com.epam.catgenome.util.Utils.deSerialize;
import static com.epam.catgenome.util.Utils.serialize;

@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TargetGeneDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;
    private String targetGeneSequenceName;
    private String insertTargetGeneQuery;
    private String loadTargetGenesQuery;
    private String loadAllTargetGenesQuery;
    private String deleteTargetGenesQuery;
    private String loadSpeciesNamesQuery;
    private String loadGeneNamesQuery;

    /**
     * Creates new TargetGene records.
     * @param targetId {@code long} parameters
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<TargetGene> saveTargetGenes(final List<TargetGene> targetGenes, final long targetId) {
        final List<TargetGene> genes = new ArrayList<>();
        if (!CollectionUtils.isEmpty(targetGenes)) {
            final List<Long> newIds = getIds(targetGenes.size());
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

    @Transactional(propagation = Propagation.REQUIRED)
    public List<Long> getIds(final int count) {
        return daoHelper.createIds(targetGeneSequenceName, count);
    }

    public List<TargetGene> loadTargetGenes(final Set<Long> targetIds) {
        final String query = DaoHelper.replaceInClause(loadTargetGenesQuery, targetIds.size());
        return getJdbcTemplate().query(query, TargetGeneParameters.getRowMapper(), targetIds.toArray());
    }

    public List<TargetGene> loadTargetGenes(final String clause) {
        final String query = addClauseToQuery(loadAllTargetGenesQuery, clause);
        return getJdbcTemplate().query(query, TargetGeneParameters.getRowMapper());
    }

    /**-
     * Deletes Target genes from the database
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteTargetGenes(final Long targetId) {
        getJdbcTemplate().update(deleteTargetGenesQuery, targetId);
    }

    public List<String> loadSpeciesNames() {
        return getJdbcTemplate().queryForList(loadSpeciesNamesQuery, String.class);
    }

    public List<String> loadGeneNames() {
        return getJdbcTemplate().queryForList(loadGeneNamesQuery, String.class);
    }

    enum TargetGeneParameters {
        TARGET_GENE_ID,
        TARGET_ID,
        GENE_ID,
        ADDITIONAL_GENES,
        GENE_NAME,
        TAX_ID,
        SPECIES_NAME,
        PRIORITY;

        static MapSqlParameterSource getParameters(final TargetGene targetGene) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(TARGET_GENE_ID.name(), targetGene.getTargetGeneId());
            params.addValue(TARGET_ID.name(), targetGene.getTargetId());
            params.addValue(GENE_ID.name(), targetGene.getGeneId());
            params.addValue(ADDITIONAL_GENES.name(), serialize(targetGene.getAdditionalGenes()));
            params.addValue(GENE_NAME.name(), targetGene.getGeneName());
            params.addValue(TAX_ID.name(), targetGene.getTaxId());
            params.addValue(SPECIES_NAME.name(), targetGene.getSpeciesName());
            params.addValue(PRIORITY.name(), Optional.ofNullable(targetGene.getPriority())
                    .map(TargetGenePriority::getValue).orElse(null));
            return params;
        }

        static RowMapper<TargetGene> getRowMapper() {
            return (rs, rowNum) -> parseTargetGene(rs);
        }

        static TargetGene parseTargetGene(final ResultSet rs) throws SQLException {
            final TargetGene targetGene = TargetGene.builder()
                    .targetGeneId(rs.getLong(TARGET_GENE_ID.name()))
                    .targetId(rs.getLong(TARGET_ID.name()))
                    .geneId(rs.getString(GENE_ID.name()))
                    .geneName(rs.getString(GENE_NAME.name()))
                    .taxId(rs.getLong(TAX_ID.name()))
                    .speciesName(rs.getString(SPECIES_NAME.name()))
                    .priority(TargetGenePriority.getByValue(rs.getInt(PRIORITY.name())))
                    .build();
            final String additionalGenes = rs.getString(ADDITIONAL_GENES.name());
            if (!rs.wasNull()) {
                targetGene.setAdditionalGenes(deSerialize(additionalGenes));
            }
            return targetGene;
        }
    }
}
