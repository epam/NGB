/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
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

package com.epam.catgenome.dao;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Source:      DaoHelper.java
 * Created:     10/29/15, 7:24 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code DaoHelper} represents DAO which provides miscellaneous utilities and calls to
 * support interaction between database and application.
 * <p>
 * {@code DaoHelper} is designed to keep different DAO activities shared between different
 * DAOs oriented to deal with certain business entity.
 */
public class DaoHelper extends NamedParameterJdbcDaoSupport {

    private static final String UNDERSCORE = "_";
    private static final String UNDERSCORE_ESCAPED = "\\\\_";
    private static final String IN_CLAUSE_PLACEHOLDER = "@in@";
    public static final String QUOTE = "'";

    private String createIdQuery;

    private String createIdsQuery;

    @Required
    public void setCreateIdQuery(final String createIdQuery) {
        this.createIdQuery = createIdQuery;
    }

    /**
     * Generates and returns the next value for a sequence with the given name.
     *
     * @param sequenceName {@code String} specifies full-qualified name of sequence which
     *                     next value should be returned by a call
     * @return {@code Long}
     * @throws IllegalArgumentException will be thrown if the provided <tt>sequenceName</tt>
     *                                  id <tt>null</tt> or empty string
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Long createId(final String sequenceName) {
        Assert.isTrue(StringUtils.isNotBlank(sequenceName));
        return getNamedParameterJdbcTemplate().queryForObject(createIdQuery,
            new MapSqlParameterSource(HelperParameters.SEQUENCE_NAME.name(), sequenceName), Long.class);
    }

    public static String getQueryFilledWithIdArray(String queryTemplate, Collection<?> ids) {
        return queryTemplate.replaceAll(IN_CLAUSE_PLACEHOLDER,
                "(" + ids.stream().filter(Objects::nonNull)
                        .map(Object::toString).collect(Collectors.joining(",")) + ")");
    }

    public static String getQueryFilledWithStringArray(String queryTemplate, Collection<String> ids) {
        return queryTemplate.replaceAll(IN_CLAUSE_PLACEHOLDER,
                "(" + ids.stream().filter(Objects::nonNull)
                        .map(s -> QUOTE + s + QUOTE).collect(Collectors.joining(",")) + ")");
    }

    public static String getQueryFilledWithTempTable(String queryTemplate, Collection<?> ids) {
        return queryTemplate.replaceAll(IN_CLAUSE_PLACEHOLDER,
                ids.stream().filter(Objects::nonNull)
                        .map(Object::toString).collect(Collectors.joining(",")));
    }

    @Required
    public void setCreateIdsQuery(final String createIdsQuery) {
        this.createIdsQuery = createIdsQuery;
    }

    /**
     * Returns {@code List} which contains next values for sequence with the given name.
     *
     * @param sequenceName {@code String} specifies full-qualified name of sequence which
     *                     next values should be returned by a call
     * @param count        int specifies the number of next values are should be retrieved
     * @return {@code List} list of next values for sequence; list.size() == count
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<Long> createIds(final String sequenceName, final int count) {
        Assert.isTrue(StringUtils.isNotBlank(sequenceName));
        if (count == 0) {
            return Collections.emptyList();
        }
        // creates a new temporary list: list.size() == count
        final List<String> rows = LongStream.range(0L, count).mapToObj(l -> "(" + l + ") ")
            .collect(Collectors.toList());
        String query = getQueryFilledWithTempTable(createIdsQuery, rows);
        // generates next values for sequence with the given name
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(HelperParameters.SEQUENCE_NAME.name(), sequenceName.trim());
        return getNamedParameterJdbcTemplate().queryForList(query, params, Long.class);
    }

    /**
     * Escapes underscore '_' symbol with backslash
     * @param query from LIKE clause
     * @return query where underscore is escaped with backslash
     */
    public String escapeUnderscore(String query) {
        return query.replaceAll(UNDERSCORE, UNDERSCORE_ESCAPED);
    }

    /**
     * Replaces a IN clause placeholder (@in@) with a valid list of SQL query placeholders (?, ?, ...)
     * @param query a query to replace IN clause placeholder
     * @param paramsCount size of IN clause
     * @return an SQL query with replaced IN clause placeholder
     */
    public static String replaceInClause(String query, int paramsCount) {
        return query.replace(IN_CLAUSE_PLACEHOLDER, IntStream.range(0, paramsCount)
            .mapToObj(s -> "?")
            .collect(Collectors.joining(", ")));
    }

    enum HelperParameters {
        LIST_ID,
        LIST_VALUE,
        SEQUENCE_NAME
    }

}
