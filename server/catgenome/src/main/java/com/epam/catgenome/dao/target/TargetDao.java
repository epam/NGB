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
import com.epam.catgenome.entity.target.AlignmentStatus;
import com.epam.catgenome.entity.target.PatentsSearchStatus;
import com.epam.catgenome.entity.target.Target;
import com.epam.catgenome.entity.target.TargetGene;
import com.epam.catgenome.util.db.PagingInfo;
import com.epam.catgenome.util.db.SortInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
import java.util.List;

import static com.epam.catgenome.util.Utils.addClauseToQuery;
import static com.epam.catgenome.util.Utils.addPagingInfoToQuery;
import static com.epam.catgenome.util.Utils.addSortInfoToQuery;
import static com.epam.catgenome.util.Utils.dataToList;
import static com.epam.catgenome.util.Utils.listToData;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TargetDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;
    private String targetSequenceName;
    private String insertTargetQuery;
    private String updateTargetQuery;
    private String updateAlignmentQuery;
    private String updatePatentsSearchQuery;
    private String deleteTargetQuery;
    private String loadTargetQuery;
    private String loadTargetsQuery;
    private String loadTargetsForAlignmentQuery;
    private String loadTargetsForPatentsSearchQuery;
    private String loadAllTargetsQuery;
    private String totalCountQuery;
    private String productsQuery;

    /**
     * Creates new Target record or updates an existing.
     * @param target {@code Target} a Target to save.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Target saveTarget(final Target target) {
        if (target.getTargetId() == null) {
            target.setTargetId(daoHelper.createId(targetSequenceName));
            getNamedParameterJdbcTemplate().update(insertTargetQuery, TargetParameters.getParameters(target));
        } else {
            getNamedParameterJdbcTemplate().update(updateTargetQuery, TargetParameters.getParameters(target));
        }
        return target;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void updateAlignment(final Target target) {
        getNamedParameterJdbcTemplate().update(updateAlignmentQuery, TargetParameters.getParameters(target));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void updatePatentsSearchStatus(final Target target) {
        getNamedParameterJdbcTemplate().update(updatePatentsSearchQuery, TargetParameters.getParameters(target));
    }

    /**
     * Deletes Target from the database
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteTarget(final Long targetId) {
        getJdbcTemplate().update(deleteTargetQuery, targetId);
    }

    /**
     * Loads {@code Target} from a database by id.
     * @param targetId {@code long} query parameters
     * @return a {@code Target} from the database
     */
    public Target loadTarget(final long targetId) {
        List<Target> targets = getJdbcTemplate().query(loadTargetQuery,
                TargetParameters.getExtendedRowExtractor(), targetId);
        return targets.isEmpty() ? null : targets.get(0);
    }

    /**
     * Loads {@code Targets} from a database.
     * @return a {@code Page<Target>} from the database
     */
    public List<Target> loadTargets(final String clause, final PagingInfo pagingInfo, final List<SortInfo> sortInfos) {
        final String query = addPagingInfoToQuery(addSortInfoToQuery(addClauseToQuery(loadTargetsQuery, clause),
                sortInfos), pagingInfo);
        return getJdbcTemplate().query(query, TargetParameters.getRowMapper());
    }

    public List<Target> loadTargets(final String clause, final List<SortInfo> sortInfos) {
        final String query = addSortInfoToQuery(addClauseToQuery(loadTargetsQuery, clause), sortInfos);
        return getJdbcTemplate().query(query, TargetParameters.getRowMapper());
    }

    public List<Target> loadTargetsForAlignment() {
        return getJdbcTemplate().query(loadTargetsForAlignmentQuery, TargetParameters.getExtendedRowExtractor());
    }

    public List<Target> getTargetsForPatentsSearch() {
        return getJdbcTemplate().query(loadTargetsForPatentsSearchQuery, TargetParameters.getExtendedRowExtractor());
    }

    public long getTotalCount(final String clause) {
        final String query = addClauseToQuery(totalCountQuery, clause);
        return getJdbcTemplate().queryForObject(query, Long.class);
    }

    public List<Target> loadAllTargets() {
        return getJdbcTemplate().query(loadAllTargetsQuery, TargetParameters.getRowMapper());
    }

    enum TargetParameters {
        TARGET_ID,
        TARGET_NAME,
        OWNER,
        ALIGNMENT_STATUS,
        PATENTS_SEARCH_STATUS,
        DISEASES,
        PRODUCTS;

        static MapSqlParameterSource getParameters(final Target target) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(TARGET_ID.name(), target.getTargetId());
            params.addValue(TARGET_NAME.name(), target.getTargetName());
            params.addValue(OWNER.name(), target.getOwner());
            params.addValue(ALIGNMENT_STATUS.name(), target.getAlignmentStatus().getValue());
            params.addValue(PATENTS_SEARCH_STATUS.name(), target.getPatentsSearchStatus().getValue());
            params.addValue(DISEASES.name(), listToData(target.getDiseases()));
            params.addValue(PRODUCTS.name(), listToData(target.getProducts()));
            return params;
        }

        static RowMapper<Target> getRowMapper() {
            return (rs, rowNum) -> parseTarget(rs);
        }

        static Target parseTarget(final ResultSet rs) throws SQLException {
            return Target.builder()
                    .targetId(rs.getLong(TARGET_ID.name()))
                    .targetName(rs.getString(TARGET_NAME.name()))
                    .owner(rs.getString(OWNER.name()))
                    .alignmentStatus(AlignmentStatus.getByValue(rs.getInt(ALIGNMENT_STATUS.name())))
                    .patentsSearchStatus(PatentsSearchStatus.getByValue(rs.getInt(PATENTS_SEARCH_STATUS.name())))
                    .diseases(dataToList(rs.getString(DISEASES.name())))
                    .products(dataToList(rs.getString(PRODUCTS.name())))
                    .build();
        }
        static ResultSetExtractor<List<Target>> getExtendedRowExtractor() {
            return (rs) -> {
                long targetId = 0;
                Target target;
                TargetGene targetGene;
                List<Target> targets = new ArrayList<>();
                List<TargetGene> targetGenes = new ArrayList<>();
                while (rs.next()) {
                    if (targetId != rs.getLong(TARGET_ID.name())) {
                        targetGenes = new ArrayList<>();
                        target = TargetDao.TargetParameters.parseTarget(rs);
                        target.setTargetGenes(targetGenes);
                        targets.add(target);
                        targetId = rs.getLong(TARGET_ID.name());
                    }
                    targetGene = TargetGeneDao.TargetGeneParameters.parseTargetGene(rs);
                    if (targetGene.getTargetGeneId() != 0) {
                        targetGenes.add(targetGene);
                    }
                }
                return targets;
            };
        }
    }
}
