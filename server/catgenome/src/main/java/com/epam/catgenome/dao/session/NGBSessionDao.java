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

package com.epam.catgenome.dao.session;

import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.session.NGBSession;
import com.epam.catgenome.entity.session.NGBSessionFilter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NGBSessionDao  extends NamedParameterJdbcDaoSupport {

    private static final Pattern WHERE_PATTERN = Pattern.compile("@WHERE@");
    private static final String AND = " AND ";
    private static final String WHERE = "WHERE ";
    private static final String LIKE = " LIKE '%";

    @Autowired
    private DaoHelper daoHelper;

    private String ngbSessionSequenceName;

    private String createNgbSessionQuery;
    private String filterNgbSessionsQuery;
    private String loadNgbSessionQuery;
    private String deleteNgbSessionQuery;

    @Transactional(propagation = Propagation.MANDATORY)
    public NGBSession create(final NGBSession session) {
        session.setId(daoHelper.createId(ngbSessionSequenceName));
        getNamedParameterJdbcTemplate().update(createNgbSessionQuery, NGBSessionParameters.getParameters(session));
        return session;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public List<NGBSession> filter(final NGBSessionFilter filter) {
        return getJdbcTemplate().query(WHERE_PATTERN.matcher(filterNgbSessionsQuery)
                .replaceFirst(generateSessionFilter(filter)), NGBSessionParameters.getRowMapper()
        );
    }

    public Optional<NGBSession> load(final Long id) {
        return getJdbcTemplate()
                .query(loadNgbSessionQuery, NGBSessionParameters.getRowMapper(), id)
                .stream()
                .findFirst();
    }

    public Optional<NGBSession> delete(final Long id) {
        final Optional<NGBSession> loaded = load(id);
        loaded.ifPresent(session -> getJdbcTemplate().update(deleteNgbSessionQuery, id));
        return loaded;
    }

    public void setNgbSessionSequenceName(String ngbSessionSequenceName) {
        this.ngbSessionSequenceName = ngbSessionSequenceName;
    }

    enum NGBSessionParameters {
        ID,
        NAME,
        DESCRIPTION,
        REFERENCE_ID,
        CHROMOSOME,
        START_POSITION,
        END_POSITION,
        OWNER,
        SESSION_VALUE;

        public static RowMapper<NGBSession> getRowMapper() {
            return (rs, rowNum) -> {
                final NGBSession loaded = new NGBSession();
                loaded.setId(rs.getLong(ID.name()));
                loaded.setName(rs.getString(NAME.name()));
                loaded.setDescription(rs.getString(DESCRIPTION.name()));
                loaded.setReferenceId(rs.getLong(REFERENCE_ID.name()));
                loaded.setChromosome(rs.getString(CHROMOSOME.name()));
                loaded.setStart(rs.getLong(START_POSITION.name()));
                loaded.setEnd(rs.getLong(END_POSITION.name()));
                loaded.setOwner(rs.getString(OWNER.name()));
                loaded.setSessionValue(rs.getString(SESSION_VALUE.name()));
                return loaded;
            };
        }

        static MapSqlParameterSource getParameters(final NGBSession session) {
            final MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(ID.name(), session.getId());
            params.addValue(NAME.name(), session.getName());
            params.addValue(DESCRIPTION.name(), session.getDescription());
            params.addValue(REFERENCE_ID.name(), session.getReferenceId());
            params.addValue(CHROMOSOME.name(), session.getChromosome());
            params.addValue(START_POSITION.name(), session.getStart());
            params.addValue(END_POSITION.name(), session.getEnd());
            params.addValue(OWNER.name(), session.getOwner());
            params.addValue(SESSION_VALUE.name(), session.getSessionValue());
            return params;
        }
    }

    private String generateSessionFilter(NGBSessionFilter filter) {
        final StringBuilder result = new StringBuilder();
        if (isFilterEmpty(filter)) {
            return result.toString();
        }

        if(StringUtils.isNotEmpty(filter.getChromosome())) {
            result.append(
                    WHERE + NGBSessionParameters.CHROMOSOME.name() + " LIKE '" + filter.getChromosome() + "'"
            );
        }

        result.append(likeFilter(result, filter.getName(), NGBSessionParameters.NAME));
        result.append(likeFilter(result, filter.getOwner(), NGBSessionParameters.OWNER));
        result .append(likeFilter(result, filter.getDescription(), NGBSessionParameters.DESCRIPTION));

        if(CollectionUtils.isNotEmpty(filter.getReferenceIds())) {
            result.append(
                    (StringUtils.isEmpty(result) ? WHERE : AND) + NGBSessionParameters.REFERENCE_ID.name()
                        + " in ("
                        + filter.getReferenceIds().stream().map(Object::toString)
                                .collect(Collectors.joining(", "))
                        + ")"
            );
        }



        if(filter.getStart() != null) {
            result.append(
                    (StringUtils.isEmpty(result) ? WHERE : AND) + NGBSessionParameters.START_POSITION.name()
                    + " > " + filter.getStart()
            );
        }

        if(filter.getEnd() != null) {
            result.append(
                    (StringUtils.isEmpty(result) ? WHERE : AND) + NGBSessionParameters.END_POSITION.name()
                    + " < " + filter.getEnd()
            );
        }
        return result.toString();
    }

    private String likeFilter(StringBuilder result, String value, NGBSessionParameters parameter) {
        if (StringUtils.isNotEmpty(value)) {
            return (StringUtils.isEmpty(result) ? WHERE : AND) + parameter.name()
                    + LIKE + value + "%'";
        }
        return "";
    }

    private boolean isFilterEmpty(NGBSessionFilter filter) {
        return filter == null ||
                StringUtils.isEmpty(filter.getChromosome()) && StringUtils.isEmpty(filter.getName()) &&
                        StringUtils.isEmpty(filter.getOwner()) && CollectionUtils.isEmpty(filter.getReferenceIds()) &&
                        StringUtils.isEmpty(filter.getDescription()) && filter.getStart() == null 
                        && filter.getEnd() == null;
    }

    public void setCreateNgbSessionQuery(String createNgbSessionQuery) {
        this.createNgbSessionQuery = createNgbSessionQuery;
    }

    public void setFilterNgbSessionsQuery(String filterNgbSessionsQuery) {
        this.filterNgbSessionsQuery = filterNgbSessionsQuery;
    }

    public void setLoadNgbSessionQuery(String loadNgbSessionQuery) {
        this.loadNgbSessionQuery = loadNgbSessionQuery;
    }

    public void setDeleteNgbSessionQuery(String deleteNgbSessionQuery) {
        this.deleteNgbSessionQuery = deleteNgbSessionQuery;
    }
}
