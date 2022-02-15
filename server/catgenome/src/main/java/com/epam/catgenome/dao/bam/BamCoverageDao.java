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
package com.epam.catgenome.dao.bam;

import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.bam.BamCoverage;
import com.epam.catgenome.util.db.Filter;
import lombok.AllArgsConstructor;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.epam.catgenome.util.Utils.addFiltersToQuery;

@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BamCoverageDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;
    private String sequenceName;
    private String insertQuery;
    private String deleteByIdsQuery;
    private String loadQuery;
    private String loadByBamIdQuery;

    /**
     * Persists new BamCoverage record
     * @param coverage {@code BamCoverage} a BamCoverage to persist.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void save(final BamCoverage coverage) {
        final MapSqlParameterSource params = Parameters.getParameters(coverage);
        getNamedParameterJdbcTemplate().update(insertQuery, params);
    }

    /**
     * Deletes BamCoverages from the database
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void delete(final Set<Long> coverageIds) {
        final String query = DaoHelper.replaceInClause(deleteByIdsQuery, coverageIds.size());
        getJdbcTemplate().update(query, coverageIds.toArray());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Long createId() {
        return daoHelper.createId(sequenceName);
    }

    /**
     * Loads {@code BamCoverage} from a database
     * @return a {@code BamCoverage} from the database
     */
    public List<BamCoverage> loadByBamId(final Set<Long> bamIds) {
        final String query = DaoHelper.replaceInClause(loadByBamIdQuery, bamIds.size());
        return getJdbcTemplate().query(query, Parameters.getRowMapper(), bamIds.toArray());
    }

    /**
     * Loads {@code BamCoverages} from a database
     * @return a {@code List<BamCoverage>} from the database
     */
    public List<BamCoverage> load(final Long bamId, final Integer step) {
        final List<Filter> filters = new ArrayList<>();
        final Filter bamIdFilter = Filter.builder()
                .field("bam_id")
                .operator("=")
                .value(String.valueOf(bamId))
                .build();
        filters.add(bamIdFilter);
        if (step != null) {
            final Filter stepFilter = Filter.builder()
                    .field("step")
                    .operator("=")
                    .value(String.valueOf(step))
                    .build();
            filters.add(stepFilter);
        }
        final String query = addFiltersToQuery(loadQuery, filters);
        return getJdbcTemplate().query(query, Parameters.getRowMapper());
    }

    /**
     * Loads {@code BamCoverages} from a database
     * @return a {@code List<BamCoverage>} from the database
     */
    public List<BamCoverage> loadAll() {
        return getJdbcTemplate().query(loadQuery, Parameters.getRowMapper());
    }

    enum Parameters {
        COVERAGE_ID,
        BAM_ID,
        STEP,
        COVERAGE;

        static MapSqlParameterSource getParameters(final BamCoverage coverage) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(COVERAGE_ID.name(), coverage.getCoverageId());
            params.addValue(BAM_ID.name(), coverage.getBamId());
            params.addValue(STEP.name(), coverage.getStep());
            params.addValue(COVERAGE.name(), coverage.getCoverage());

            return params;
        }

        static RowMapper<BamCoverage> getRowMapper() {
            return (rs, rowNum) -> parse(rs);
        }

        static BamCoverage parse(final ResultSet rs) throws SQLException {
            return BamCoverage.builder()
                    .coverageId(rs.getLong(COVERAGE_ID.name()))
                    .bamId(rs.getLong(BAM_ID.name()))
                    .step(rs.getInt(STEP.name()))
                    .coverage(rs.getFloat(COVERAGE.name()))
                    .build();
        }
    }
}
