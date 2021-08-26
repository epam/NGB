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
import com.epam.catgenome.entity.externaldb.homologene.Gene;
import com.epam.catgenome.util.db.QueryParameters;
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
import java.util.ArrayList;
import java.util.List;

import static com.epam.catgenome.util.Utils.addParametersToQuery;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HomologGeneDescDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;
    private String sequenceName;
    private String insertQuery;
    private String deleteAllQuery;
    private String loadQuery;

    /**
     * Persists a new or updates existing Gene Description record.
     * @param gene {@code Gene } a Gene Description to persist.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Gene save(final Gene gene) {
        gene.setGeneId(daoHelper.createId(sequenceName));
        getNamedParameterJdbcTemplate().update(insertQuery, GeneDescParameters.getParameters(gene));
        return gene;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void save(final List<Gene> genes) {
        if (!CollectionUtils.isEmpty(genes)) {
            ArrayList<MapSqlParameterSource> params = new ArrayList<>(genes.size());
            for (Gene gene : genes) {
                MapSqlParameterSource param = GeneDescParameters.getParameters(gene);
                params.add(param);
            }
            getNamedParameterJdbcTemplate().batchUpdate(insertQuery,
                    params.toArray(new MapSqlParameterSource[genes.size()]));
        }
    }

    /**
     * Deletes Gene descriptions from the database
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteAll() {
        getJdbcTemplate().update(deleteAllQuery);
    }

    /**
     * Loads {@code Gene description} from a database by parameters.
     * @param queryParameters {@code QueryParameters} query parameters
     * @return a {@code List<Gene>} from the database
     */
    public List<Gene> load(final QueryParameters queryParameters) {
        String query = addParametersToQuery(loadQuery, queryParameters);
        return getJdbcTemplate().query(query, GeneDescParameters.getRowMapper());
    }

    enum GeneDescParameters {
        GENE_ID,
        GROUP_ID,
        SYMBOL,
        TITLE,
        TAX_ID,
        PROT_GI,
        PROT_ACC,
        PROT_LEN,
        NUC_GI,
        NUC_ACC,
        LOCUS_TAG;

        static MapSqlParameterSource getParameters(final Gene gene) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(GENE_ID.name(), gene.getGeneId());
            params.addValue(SYMBOL.name(), gene.getSymbol());
            params.addValue(TITLE.name(), gene.getTitle());
            params.addValue(TAX_ID.name(), gene.getTaxId());
            params.addValue(PROT_GI.name(), gene.getProtGi());
            params.addValue(PROT_ACC.name(), gene.getProtAcc());
            params.addValue(PROT_LEN.name(), gene.getProtLen());
            params.addValue(NUC_GI.name(), gene.getNucGi());
            params.addValue(NUC_ACC.name(), gene.getNucAcc());
            params.addValue(LOCUS_TAG.name(), gene.getLocusTag());
            return params;
        }

        static RowMapper<Gene> getRowMapper() {
            return (rs, rowNum) -> parseGene(rs);
        }

        static Gene parseGene(final ResultSet rs) throws SQLException {
            return Gene.builder()
                    .geneId(rs.getLong(GENE_ID.name()))
                    .groupId(rs.getLong(GROUP_ID.name()))
                    .symbol(rs.getString(SYMBOL.name()))
                    .title(rs.getString(TITLE.name()))
                    .taxId(rs.getLong(TAX_ID.name()))
                    .protGi(rs.getLong(PROT_GI.name()))
                    .protAcc(rs.getString(PROT_ACC.name()))
                    .protLen(rs.getLong(PROT_LEN.name()))
                    .nucGi(rs.getLong(NUC_GI.name()))
                    .nucAcc(rs.getString(NUC_ACC.name()))
                    .locusTag(rs.getString(LOCUS_TAG.name()))
                    .build();
        }
    }
}
