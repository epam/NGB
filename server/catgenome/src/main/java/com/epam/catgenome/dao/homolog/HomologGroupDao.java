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
import com.epam.catgenome.entity.externaldb.homolog.HomologType;
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
public class HomologGroupDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;
    private String sequenceName;
    private String insertQuery;
    private String deleteQuery;
    private String loadQuery;

    /**
     * Persists a new or updates existing Homolog Group record.
     * @param homologGroup {@code HomologGroup} a Homolog Group to persist.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public HomologGroup save(final HomologGroup homologGroup) {
        homologGroup.setGroupId(daoHelper.createId(sequenceName));
        getNamedParameterJdbcTemplate().update(insertQuery, GroupParameters.getParameters(homologGroup));
        return homologGroup;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public long nextVal() {
        return daoHelper.createId(sequenceName);
    }

    public List<Long> nextVal(final int size) {
        return daoHelper.createIds(sequenceName, size);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void save(final List<HomologGroup> groups) {
        if (!CollectionUtils.isEmpty(groups)) {
            List<MapSqlParameterSource> params = new ArrayList<>(groups.size());
            for (HomologGroup group : groups) {
                MapSqlParameterSource param = GroupParameters.getParameters(group);
                params.add(param);
            }
            getNamedParameterJdbcTemplate().batchUpdate(insertQuery,
                    params.toArray(new MapSqlParameterSource[groups.size()]));
        }
    }

    /**
     * Deletes Homolog groups from the database
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void delete(final Long id) {
        getJdbcTemplate().update(deleteQuery, id);
    }

    /**
     * Loads {@code Homolog groups} from a database by parameters.
     * @param queryParameters {@code QueryParameters} query parameters
     * @return a {@code List<HomologGroup>} from the database
     */
    public List<HomologGroup> load(final QueryParameters queryParameters) {
        String query = addParametersToQuery(loadQuery, queryParameters);
        return getJdbcTemplate().query(query, GroupParameters.getRowMapper());
    }

    enum  GroupParameters{
        GROUP_ID,
        PRIMARY_GENE_ID,
        PRIMARY_GENE_TAX_ID,
        TYPE,
        DATABASE_ID,
        GENE_NAME,
        PROTEIN_NAME,
        HOMOLOG_DATABASE;

        static MapSqlParameterSource getParameters(final HomologGroup homologGroup) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(GROUP_ID.name(), homologGroup.getGroupId());
            params.addValue(PRIMARY_GENE_ID.name(), homologGroup.getGeneId());
            params.addValue(PRIMARY_GENE_TAX_ID.name(), homologGroup.getTaxId());
            params.addValue(TYPE.name(), homologGroup.getType().getId());
            params.addValue(DATABASE_ID.name(), homologGroup.getDatabaseId());
            return params;
        }

        static RowMapper<HomologGroup> getRowMapper() {
            return (rs, rowNum) -> parseGroup(rs);
        }

        static HomologGroup parseGroup(final ResultSet rs) throws SQLException {
            return HomologGroup.builder()
                    .groupId(rs.getLong(GROUP_ID.name()))
                    .geneId(rs.getLong(PRIMARY_GENE_ID.name()))
                    .taxId(rs.getLong(PRIMARY_GENE_TAX_ID.name()))
                    .type(HomologType.getById(rs.getInt(TYPE.name())))
                    .geneName(rs.getString(GENE_NAME.name()))
                    .proteinName(rs.getString(PROTEIN_NAME.name()))
                    .homologDatabase(rs.getString(HOMOLOG_DATABASE.name()))
                    .build();
        }
    }
}
