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
import com.epam.catgenome.entity.pathway.PathwayOrganism;
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
public class PathwayOrganismDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;
    private String pathwayOrganismSequenceName;
    private String insertPathwayOrganismQuery;
    private String deletePathwayOrganismQuery;
    private String loadPathwayOrganismQuery;

    /**
     * Persists new PathwayOrganism records.
     * @param taxIds {@code Set<Long>} taxonomy IDs to persist.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<PathwayOrganism> savePathwayOrganisms(final Set<Long> taxIds, final long pathwayId) {
        final List<PathwayOrganism> organisms = new ArrayList<>();
        if (!CollectionUtils.isEmpty(taxIds)) {
            int i = 0;
            final List<Long> newIds = daoHelper.createIds(pathwayOrganismSequenceName, taxIds.size());
            final MapSqlParameterSource[] params = new MapSqlParameterSource[taxIds.size()];
            for (Long taxId: taxIds) {
                PathwayOrganism organism = PathwayOrganism.builder()
                        .pathwayOrganismId(newIds.get(i))
                        .pathwayId(pathwayId)
                        .taxId(taxId)
                        .build();
                organisms.add(organism);
                params[i] = PathwayOrganismParameters.getParameters(organism);
                i++;
            }
            getNamedParameterJdbcTemplate().batchUpdate(insertPathwayOrganismQuery, params);
        }
        return organisms;
    }

    /**
     * Deletes Pathway organisms from the database
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deletePathwayOrganisms(final Long pathwayId) {
        getJdbcTemplate().update(deletePathwayOrganismQuery, pathwayId);
    }

    /**
     * Loads {@code List<PathwayOrganism>} from a database by pathway id.
     * @param pathwayId {@code long} query parameters
     * @return a {@code List<PathwayOrganism>} from the database
     */
    public List<PathwayOrganism> loadPathwayOrganisms(final long pathwayId) {
        return getJdbcTemplate().query(loadPathwayOrganismQuery,
                PathwayOrganismParameters.getRowMapper(), pathwayId);
    }

    enum PathwayOrganismParameters {
        PATHWAY_ORGANISM_ID,
        PATHWAY_ID,
        TAX_ID;

        static MapSqlParameterSource getParameters(final PathwayOrganism species) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(PATHWAY_ORGANISM_ID.name(), species.getPathwayOrganismId());
            params.addValue(PATHWAY_ID.name(), species.getPathwayId());
            params.addValue(TAX_ID.name(), species.getTaxId());

            return params;
        }

        static RowMapper<PathwayOrganism> getRowMapper() {
            return (rs, rowNum) -> parsePathwayOrganism(rs);
        }

        static PathwayOrganism parsePathwayOrganism(final ResultSet rs) throws SQLException {
            return PathwayOrganism.builder()
                    .pathwayOrganismId(rs.getLong(PATHWAY_ORGANISM_ID.name()))
                    .pathwayId(rs.getLong(PATHWAY_ID.name()))
                    .taxId(rs.getLong(TAX_ID.name()))
                    .build();
        }
    }
}
