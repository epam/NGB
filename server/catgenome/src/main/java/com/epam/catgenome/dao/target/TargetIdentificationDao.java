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
import com.epam.catgenome.entity.target.TargetIdentification;
import com.epam.catgenome.util.db.PagingInfo;
import com.epam.catgenome.util.db.SortInfo;
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
import java.util.Date;
import java.util.List;

import static com.epam.catgenome.util.Utils.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TargetIdentificationDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;
    private String identificationSequenceName;
    private String insertIdentificationQuery;
    private String updateIdentificationQuery;
    private String deleteIdentificationQuery;
    private String deleteTargetIdentificationsQuery;
    private String loadTargetIdentificationsQuery;
    private String loadIdentificationsQuery;
    private String loadIdentificationQuery;
    private String totalCountQuery;

    @Transactional(propagation = Propagation.MANDATORY)
    public TargetIdentification save(final TargetIdentification identification) {
        if (identification.getId() == null) {
            identification.setId(daoHelper.createId(identificationSequenceName));
            getNamedParameterJdbcTemplate().update(insertIdentificationQuery,
                    IdentificationParameters.getParameters(identification));
        } else {
            getNamedParameterJdbcTemplate().update(updateIdentificationQuery,
                    IdentificationParameters.getParameters(identification));
        }
        return identification;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void delete(final Long id) {
        getJdbcTemplate().update(deleteIdentificationQuery, id);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteTargetIdentifications(final Long targetId) {
        getJdbcTemplate().update(deleteTargetIdentificationsQuery, targetId);
    }

    public TargetIdentification load(final long id) {
        List<TargetIdentification> identifications = getJdbcTemplate().query(loadIdentificationQuery,
                IdentificationParameters.getRowMapper(), id);
        return identifications.isEmpty() ? null : identifications.get(0);
    }

    public List<TargetIdentification> load() {
        return getJdbcTemplate().query(loadIdentificationsQuery, IdentificationParameters.getRowMapper());
    }

    public List<TargetIdentification> load(final String clause,
                                           final PagingInfo pagingInfo,
                                           final List<SortInfo> sortInfos) {
        final String query = addPagingInfoToQuery(addSortInfoToQuery(addClauseToQuery(loadIdentificationsQuery, clause),
                sortInfos), pagingInfo);
        return getJdbcTemplate().query(query, IdentificationParameters.getRowMapper());
    }

    public List<TargetIdentification> load(final String clause) {
        final String query = addClauseToQuery(loadIdentificationsQuery, clause);
        return getJdbcTemplate().query(query, IdentificationParameters.getRowMapper());
    }

    public List<TargetIdentification> loadTargetIdentifications(final long targetId) {
        return getJdbcTemplate().query(loadTargetIdentificationsQuery,
                IdentificationParameters.getRowMapper(), targetId);
    }

    public long getTotalCount(final String clause) {
        final String query = addClauseToQuery(totalCountQuery, clause);
        return getJdbcTemplate().queryForObject(query, Long.class);
    }

    enum IdentificationParameters {
        ID,
        TARGET_ID,
        NAME,
        CREATED_DATE,
        OWNER,
        GENES_OF_INTEREST,
        TRANSLATIONAL_GENES;

        static MapSqlParameterSource getParameters(final TargetIdentification identification) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(ID.name(), identification.getId());
            params.addValue(TARGET_ID.name(), identification.getTargetId());
            params.addValue(NAME.name(), identification.getName());
            params.addValue(CREATED_DATE.name(), identification.getCreatedDate());
            params.addValue(OWNER.name(), identification.getOwner());
            params.addValue(GENES_OF_INTEREST.name(), listToData(identification.getGenesOfInterest()));
            params.addValue(TRANSLATIONAL_GENES.name(), listToData(identification.getTranslationalGenes()));
            return params;
        }

        static RowMapper<TargetIdentification> getRowMapper() {
            return (rs, rowNum) -> parseIdentification(rs);
        }

        static TargetIdentification parseIdentification(final ResultSet rs) throws SQLException {
            TargetIdentification identification = TargetIdentification.builder()
                    .targetId(rs.getLong(TARGET_ID.name()))
                    .genesOfInterest(dataToList(rs.getString(GENES_OF_INTEREST.name())))
                    .translationalGenes(dataToList(rs.getString(TRANSLATIONAL_GENES.name())))
                    .build();
            identification.setId(rs.getLong(ID.name()));
            identification.setName(rs.getString(NAME.name()));
            identification.setOwner(rs.getString(OWNER.name()));
            identification.setCreatedDate(new Date(rs.getTimestamp(CREATED_DATE.name()).getTime()));
            return identification;
        }
    }
}
