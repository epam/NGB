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
import com.epam.catgenome.entity.externaldb.homologene.Alias;
import com.epam.catgenome.entity.externaldb.homologene.Domain;
import com.epam.catgenome.entity.externaldb.homologene.Gene;
import com.epam.catgenome.util.db.Filter;
import com.epam.catgenome.util.db.QueryParameters;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.epam.catgenome.util.Utils.addClauseToQuery;
import static com.epam.catgenome.util.Utils.addFiltersToQuery;
import static com.epam.catgenome.util.Utils.addParametersToQuery;
import static com.epam.catgenome.util.db.DBQueryUtils.IN_CLAUSE;
import static org.apache.commons.lang3.StringUtils.join;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HomologGroupGeneDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;
    private String sequenceName;
    private String insertQuery;
    private String deleteQuery;
    private String loadQuery;
    private String totalCountQuery;
    private String loadGroupIdsQuery;

    /**
     * Persists a new Homolog group gene record.
     * @param gene {@code HomologGroupGene} a Homolog group gene to persist.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void save(final HomologGroupGene gene) {
        long newId = daoHelper.createId(sequenceName);
        getNamedParameterJdbcTemplate().update(insertQuery, GroupGeneParameters.getParameters(newId, gene));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void save(final List<HomologGroupGene> genes) {
        if (!CollectionUtils.isEmpty(genes)) {
            List<Long> newIds = daoHelper.createIds(sequenceName, genes.size());
            List<MapSqlParameterSource> params = new ArrayList<>(genes.size());
            for (int i = 0; i < genes.size(); i++) {
                MapSqlParameterSource param = GroupGeneParameters.getParameters(newIds.get(i), genes.get(i));
                params.add(param);
            }
            getNamedParameterJdbcTemplate().batchUpdate(insertQuery,
                    params.toArray(new MapSqlParameterSource[genes.size()]));
        }
    }

    /**
     * Deletes Homolog groups genes from the database
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void delete(final Long id) {
        getJdbcTemplate().update(deleteQuery, id);
    }

    /**
     * Loads {@code Homolog groups gene} from a database by parameters.
     * @param queryParameters {@code QueryParameters} query parameters
     * @return a {@code List<Gene>} from the database
     */
    public List<Gene> load(final QueryParameters queryParameters) {
        String query = addParametersToQuery(loadQuery, queryParameters);
        return getJdbcTemplate().query(query, GroupGeneParameters.getExtendedRowExtractor());
    }

    public List<String> loadGroupIds(final QueryParameters queryParameters) {
        String pageQuery = addParametersToQuery(loadGroupIdsQuery, queryParameters);
        return getJdbcTemplate().queryForList(pageQuery, String.class);
    }

    public List<Long> loadAllGroupIds(final List<Filter> filters) {
        String query = addFiltersToQuery(loadGroupIdsQuery, filters);
        return getJdbcTemplate().queryForList(query, Long.class);
    }

    public List<String> loadGroupsByGeneIds(final List<Long> geneIds) {
        final String clause = String.format(IN_CLAUSE, "gene_id", join(geneIds, ","));
        final String query = addClauseToQuery(loadGroupIdsQuery, clause);
        return getJdbcTemplate().queryForList(query, String.class);
    }

    enum GroupGeneParameters {
        GROUP_GENE_ID,
        GROUP_ID,
        GENE_ID,
        TAX_ID,
        DATABASE_ID;

        static MapSqlParameterSource getParameters(final long id, final HomologGroupGene gene) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(GROUP_GENE_ID.name(), id);
            params.addValue(GROUP_ID.name(), gene.getGroupId());
            params.addValue(GENE_ID.name(), gene.getGeneId());
            params.addValue(TAX_ID.name(), gene.getTaxId());
            params.addValue(DATABASE_ID.name(), gene.getDatabaseId());
            return params;
        }

        static RowMapper<HomologGroupGene> getRowMapper() {
            return (rs, rowNum) -> parseGroupGene(rs);
        }

        static HomologGroupGene parseGroupGene(final ResultSet rs) throws SQLException {
            return HomologGroupGene.builder()
                    .groupGeneId(rs.getLong(GROUP_GENE_ID.name()))
                    .groupId(rs.getLong(GROUP_ID.name()))
                    .geneId(rs.getLong(GENE_ID.name()))
                    .taxId(rs.getLong(TAX_ID.name()))
                    .databaseId(rs.getLong(DATABASE_ID.name()))
                    .build();
        }

        static ResultSetExtractor<List<Gene>> getExtendedRowExtractor() {
            return (rs) -> {
                long geneId = 0;
                long domainId = 0;
                Gene gene;
                Domain domain;
                Alias alias;
                List<Gene> genes = new ArrayList<>();
                List<Domain> domains = new ArrayList<>();
                Set<String> aliases = new HashSet<>();
                while (rs.next()) {
                    if (geneId != rs.getLong(GENE_ID.name())) {
                        domains = new ArrayList<>();
                        aliases = new HashSet<>();
                        gene = HomologGeneDescDao.GeneDescParameters.parseGene(rs);
                        gene.setDomains(domains);
                        gene.setAliases(aliases);
                        genes.add(gene);
                        geneId = rs.getLong(GENE_ID.name());
                    }
                    domain = HomologGeneDomainDao.DomainParameters.parseDomain(rs);
                    if (domain.getDomainId() != 0 && domain.getDomainId() != domainId) {
                        domains.add(domain);
                        domainId = domain.getDomainId();
                    }
                    alias = HomologGeneAliasDao.AliasParameters.parseAlias(rs);
                    if (alias.getAliasId() != 0) {
                        aliases.add(alias.getName());
                    }
                }
                return genes;
            };
        }
    }
}
