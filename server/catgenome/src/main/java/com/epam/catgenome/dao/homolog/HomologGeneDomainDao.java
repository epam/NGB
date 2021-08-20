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
import com.epam.catgenome.entity.externaldb.homologene.Domain;
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
public class HomologGeneDomainDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;
    private String sequenceName;
    private String insertQuery;
    private String deleteAllQuery;
    private String loadQuery;

    /**
     * Persists a new Gene Domain record.
     * @param domain {@code Domain} a Gene domain to persist.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void save(final Domain domain) {
        long newId = daoHelper.createId(sequenceName);
        getNamedParameterJdbcTemplate().update(insertQuery, DomainParameters.getParameters(newId, domain));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void save(final List<Domain> domains) {
        if (!CollectionUtils.isEmpty(domains)) {
            List<Long> newIds = daoHelper.createIds(sequenceName, domains.size());
            List<MapSqlParameterSource> params = new ArrayList<>(domains.size());
            for (int i = 0; i < domains.size(); i++) {
                MapSqlParameterSource param = DomainParameters.getParameters(newIds.get(i), domains.get(i));
                params.add(param);
            }

            getNamedParameterJdbcTemplate().batchUpdate(insertQuery,
                    params.toArray(new MapSqlParameterSource[domains.size()]));
        }
    }

    /**
     * Deletes Gene domains from the database
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteAll() {
        getJdbcTemplate().update(deleteAllQuery);
    }

    /**
     * Loads {@code Gene domains} from a database by parameters.
     * @param queryParameters {@code QueryParameters} query parameters
     * @return a {@code List<Domain>} from the database
     */
    public List<Domain> load(final QueryParameters queryParameters) {
        String query = addParametersToQuery(loadQuery, queryParameters);
        return getJdbcTemplate().query(query, DomainParameters.getRowMapper());
    }

    enum DomainParameters {
        ID,
        GENE_ID,
        BEGIN,
        END,
        PSSMID,
        CDDID,
        CDDNAME;

        static MapSqlParameterSource getParameters(final long id, final Domain domain) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(ID.name(), id);
            params.addValue(GENE_ID.name(), domain.getGeneId());
            params.addValue(BEGIN.name(), domain.getBegin());
            params.addValue(END.name(), domain.getEnd());
            params.addValue(PSSMID.name(), domain.getPssmId());
            params.addValue(CDDID.name(), domain.getCddId());
            params.addValue(CDDNAME.name(), domain.getCddName());
            return params;
        }

        static RowMapper<Domain> getRowMapper() {
            return (rs, rowNum) -> parseDomain(rs);
        }

        static Domain parseDomain(final ResultSet rs) throws SQLException {
            return Domain.builder()
                    .id(rs.getLong(ID.name()))
                    .geneId(rs.getLong(GENE_ID.name()))
                    .begin(rs.getLong(BEGIN.name()))
                    .end(rs.getLong(END.name()))
                    .pssmId(rs.getLong(PSSMID.name()))
                    .cddId(rs.getString(CDDID.name()))
                    .cddName(rs.getString(CDDNAME.name()))
                    .build();
        }
    }
}
