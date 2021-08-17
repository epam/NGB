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
public class HomologGeneDescDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;
    private String geneDescSequenceName;
    private String insertGeneDescQuery;
    private String deleteGeneDescQuery;
    private String loadGeneDescQuery;

    /**
     * Persists a new or updates existing Gene Description record.
     * @param gene {@code Gene } a Gene Description to persist.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void save(final Gene gene) {
        gene.setGeneId(daoHelper.createId(geneDescSequenceName));
        getNamedParameterJdbcTemplate().update(insertGeneDescQuery, GeneDescParameters.getParameters(gene));
    }

    /**
     * Deletes Gene descriptions from the database
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteGeneDesc() {
        getJdbcTemplate().update(deleteGeneDescQuery);
    }

    /**
     * Loads {@code Gene description} from a database by parameters.
     * @param queryParameters {@code QueryParameters} query parameters
     * @return a {@code List<Gene>} from the database
     */
    public List<Gene> loadGeneDesc(final QueryParameters queryParameters) {
        String query = addParametersToQuery(loadGeneDescQuery, queryParameters);
        return getJdbcTemplate().query(query, GeneDescParameters.getRowMapper());
    }

    enum GeneDescParameters {
        ID,
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
            params.addValue(ID.name(), gene.getGeneId());
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
            final Gene gene = new Gene();
            gene.setGeneId(rs.getLong(ID.name()));
            gene.setSymbol(rs.getString(SYMBOL.name()));
            gene.setTitle(rs.getString(TITLE.name()));
            gene.setTaxId(rs.getLong(TAX_ID.name()));
            gene.setProtGi(rs.getLong(PROT_GI.name()));
            gene.setProtAcc(rs.getString(PROT_ACC.name()));
            gene.setProtLen(rs.getLong(PROT_LEN.name()));
            gene.setNucGi(rs.getLong(NUC_GI.name()));
            gene.setNucAcc(rs.getString(NUC_ACC.name()));
            gene.setLocusTag(rs.getString(LOCUS_TAG.name()));
            return gene;
        }
    }
}
