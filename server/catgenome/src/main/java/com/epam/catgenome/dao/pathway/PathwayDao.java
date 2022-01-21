/*
 * MIT License
 *
 * Copyright (c) 2022 EPAM Systems
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
package com.epam.catgenome.dao.pathway;

import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.pathway.Pathway;
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
import java.util.List;

@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PathwayDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;
    private String pathwaySequenceName;
    private String insertPathwayQuery;
    private String deletePathwayQuery;
    private String loadPathwayQuery;
    private String loadPathwaysQuery;

    /**
     * Persists new Pathway record.
     * @param pathway {@code Pathway} a Pathway to persist.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void savePathway(final Pathway pathway) {
        pathway.setPathwayId(daoHelper.createId(pathwaySequenceName));
        final MapSqlParameterSource params = PathwayParameters.getParameters(pathway);
        getNamedParameterJdbcTemplate().update(insertPathwayQuery, params);
    }

    /**
     * Deletes Pathway from the database
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deletePathway(final Long pathwayId) {
        getJdbcTemplate().update(deletePathwayQuery, pathwayId);
    }

    /**
     * Loads {@code Pathway} from a database by id.
     * @param pathwayId {@code long} query parameters
     * @return a {@code Pathway} from the database
     */
    public Pathway loadPathway(final long pathwayId) {
        List<Pathway> pathways = getJdbcTemplate().query(loadPathwayQuery,
                PathwayParameters.getRowMapper(), pathwayId);
        return CollectionUtils.isEmpty(pathways) ? null : pathways.get(0);
    }

    /**
     * Loads {@code Pathways} from a database.
     * @return a {@code List<Pathway>} from the database
     */
    public List<Pathway> loadAllPathways() {
        return getJdbcTemplate().query(loadPathwaysQuery, PathwayParameters.getRowMapper());
    }

    enum PathwayParameters {
        BIO_DATA_ITEM_ID,
        NAME,
        TYPE,
        PATH,
        SOURCE,
        FORMAT,
        CREATED_DATE,
        BUCKET_ID,
        PRETTY_NAME,
        OWNER,

        PATHWAY_ID,
        PATHWAY_DESC;

        static MapSqlParameterSource getParameters(final Pathway pathway) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(PATHWAY_ID.name(), pathway.getPathwayId());
            params.addValue(BIO_DATA_ITEM_ID.name(), pathway.getBioDataItemId());
            params.addValue(PATHWAY_DESC.name(), pathway.getPathwayDesc());

            return params;
        }

        static RowMapper<Pathway> getRowMapper() {
            return (rs, rowNum) -> parsePathway(rs);
        }

        static Pathway parsePathway(final ResultSet rs) throws SQLException {
            Pathway pathway = Pathway.builder()
                    .pathwayId(rs.getLong(PATHWAY_ID.name()))
                    .pathwayDesc(rs.getString(PATHWAY_DESC.name()))
                    .build();
            pathway.setBioDataItemId(rs.getLong(BIO_DATA_ITEM_ID.name()));
            pathway.setName(rs.getString(NAME.name()));
            pathway.setType(BiologicalDataItemResourceType.getById(rs.getLong(TYPE.name())));
            pathway.setPrettyName(rs.getString(PRETTY_NAME.name()));
            pathway.setOwner(rs.getString(OWNER.name()));
            pathway.setPath(rs.getString(PATH.name()));
            pathway.setSource(rs.getString(SOURCE.name()));
            pathway.setFormat(BiologicalDataItemFormat.getById(rs.getLong(FORMAT.name())));
            pathway.setCreatedDate(rs.getDate(CREATED_DATE.name()));
            pathway.setBucketId(rs.getLong(BUCKET_ID.name()));
            return pathway;
        }
    }
}
