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
import com.epam.catgenome.entity.externaldb.homologene.Alias;
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
public class HomologGeneAliasDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;
    private String sequenceName;
    private String insertQuery;
    private String deleteAllQuery;
    private String loadQuery;

    /**
     * Persists a new Gene alias records.
     * @param alias {@code Alias} a Gene alias to persist.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void save(final Alias alias) {
        long newId = daoHelper.createId(sequenceName);
        getNamedParameterJdbcTemplate().update(insertQuery, AliasParameters.getParameters(newId, alias));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void save(final List<Alias> aliases) {
        if (!CollectionUtils.isEmpty(aliases)) {
            List<Long> newIds = daoHelper.createIds(sequenceName, aliases.size());
            ArrayList<MapSqlParameterSource> params = new ArrayList<>(aliases.size());
            for (int i = 0; i < aliases.size(); i++) {
                MapSqlParameterSource param = AliasParameters.getParameters(newIds.get(i), aliases.get(i));
                params.add(param);
            }
            getNamedParameterJdbcTemplate().batchUpdate(insertQuery,
                    params.toArray(new MapSqlParameterSource[aliases.size()]));
        }
    }

    /**
     * Deletes Gene alias from the database
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteAll() {
        getJdbcTemplate().update(deleteAllQuery);
    }

    /**
     * Loads {@code Gene alias} from a database by parameters.
     * @param queryParameters {@code QueryParameters} query parameters
     * @return a {@code List<Alias>} from the database
     */
    public List<Alias> load(final QueryParameters queryParameters) {
        String query = addParametersToQuery(loadQuery, queryParameters);
        return getJdbcTemplate().query(query, AliasParameters.getRowMapper());
    }

    enum AliasParameters {
        ID,
        GENE_ID,
        NAME;

        static MapSqlParameterSource getParameters(final long id, final Alias alias) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(ID.name(), id);
            params.addValue(GENE_ID.name(), alias.getGeneId());
            params.addValue(NAME.name(), alias.getName());
            return params;
        }

        static RowMapper<Alias> getRowMapper() {
            return (rs, rowNum) -> parseAlias(rs);
        }

        static Alias parseAlias(final ResultSet rs) throws SQLException {
            return Alias.builder()
                    .id(rs.getLong(ID.name()))
                    .geneId(rs.getLong(GENE_ID.name()))
                    .name(rs.getString(NAME.name()))
                    .build();
        }
    }
}
