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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.epam.catgenome.entity.BaseEntity;

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
    public static final String UNDERSCORE = "_";
    public static final String UNDERSCORE_ESCAPED = "\\\\_";
    public static final String IN_CLAUSE_PLACEHOLDER = "@in@";

    private String createIdQuery;

    private String createIdsQuery;

    private String listIdSequenceName;

    private String clearTemporaryListQuery;

    private String createTemporaryListQuery;

    private String insertTemporaryListItemQuery;

    private String createTemporaryStringListQuery;

    private String insertTemporaryStringListItemQuery;

    private String clearTemporaryStringListQuery;

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

    @Required
    public void setListIdSequenceName(final String listIdSequenceName) {
        this.listIdSequenceName = listIdSequenceName;
    }

    /**
     * Returns the next {@code Long} ID for a new temporary list.
     *
     * @return {@code Long}
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Long createListId() {
        return createId(listIdSequenceName);
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
        final List<Long> rows = LongStream.range(0L, count)
            .collect(LinkedList::new, LinkedList::add, LinkedList::addAll);
        final Long listId = createTempLongList(rows);
        // generates next values for sequence with the given name
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(HelperParameters.LIST_ID.name(), listId);
        params.addValue(HelperParameters.SEQUENCE_NAME.name(), sequenceName.trim());
        final List<Long> list = getNamedParameterJdbcTemplate().queryForList(createIdsQuery, params, Long.class);
        // clears a temporary list
        clearTempList(listId);
        return list;
    }

    /**
     * Creates a new temporary list of {@code Long} values and generates unique ID for a
     * created temporary list.
     *
     * @param list {@code Collection} specifies collection of {@code Long} values that should be
     *             associated with a temporary list if this call is succeeded
     * @return {@code Long} represents unique ID of a temporary list that has been created after
     * this call
     * @throws IllegalArgumentException will be thrown if the given <tt>list</tt> is empty
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Long createTempLongList(final Collection<Long> list) {
        Assert.isTrue(CollectionUtils.isNotEmpty(list));
        return createTempLongList(createListId(), list);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Long createTempStringList(final Collection<String> list) {
        Assert.isTrue(CollectionUtils.isNotEmpty(list));
        return createTempStringList(createListId(), list);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Long createTempList(final Collection<? extends BaseEntity> list) {
        Assert.isTrue(CollectionUtils.isNotEmpty(list));
        return createTempList(createListId(), list);
    }

    @Required
    public void setCreateTemporaryListQuery(final String createTemporaryListQuery) {
        this.createTemporaryListQuery = createTemporaryListQuery;
    }

    @Required
    public void setInsertTemporaryListItemQuery(final String insertTemporaryListItemQuery) {
        this.insertTemporaryListItemQuery = insertTemporaryListItemQuery;
    }

    /**
     * Creates a new temporary list of {@code Long} values. The created temporary list is
     * identified by the given ID. If a list has been created successfully, it will be filled
     * in by {@code Collection} of provided {@code Long} values.
     *
     * @param listId {@code Long} represents unique ID that is used to identify a temporary list
     * @param list   {@code Collection} specifies collection of {@code Long} values that should be
     *               associated with a temporary list if this call is succeeded
     * @return {@code Long} represents unique ID of a temporary list that has been created after
     * this call
     * @throws IllegalArgumentException will be thrown if <tt>listId</tt> or <tt>list</tt> are
     *                                  <tt>null</tt>, or the given <tt>list</tt> is empty
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Long createTempLongList(final Long listId, final Collection<Long> list) {
        Assert.notNull(listId);
        Assert.isTrue(CollectionUtils.isNotEmpty(list));
        // creates a new local temporary table if it doesn't exists to handle temporary lists
        getJdbcTemplate().update(createTemporaryListQuery);
        // fills in a temporary list by given values
        int i = 0;
        final Iterator<Long> iterator = list.iterator();
        final MapSqlParameterSource[] batchArgs = new MapSqlParameterSource[list.size()];
        while (iterator.hasNext()) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(HelperParameters.LIST_ID.name(), listId);
            params.addValue(HelperParameters.LIST_VALUE.name(), iterator.next());
            batchArgs[i] = params;
            i++;
        }
        getNamedParameterJdbcTemplate().batchUpdate(insertTemporaryListItemQuery, batchArgs);
        return listId;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Long createTempStringList(final Long listId, final Collection<String> list) {
        Assert.notNull(listId);
        Assert.isTrue(CollectionUtils.isNotEmpty(list));
        // creates a new local temporary table if it doesn't exists to handle temporary lists
        getJdbcTemplate().update(createTemporaryStringListQuery);
        // fills in a temporary list by given values
        int i = 0;
        final Iterator<String> iterator = list.iterator();
        final MapSqlParameterSource[] batchArgs = new MapSqlParameterSource[list.size()];
        while (iterator.hasNext()) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(HelperParameters.LIST_ID.name(), listId);
            params.addValue(HelperParameters.LIST_VALUE.name(), iterator.next());
            batchArgs[i] = params;
            i++;
        }
        getNamedParameterJdbcTemplate().batchUpdate(insertTemporaryStringListItemQuery, batchArgs);
        return listId;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Long createTempList(final Long listId, final Collection<? extends BaseEntity> list) {
        Assert.notNull(listId);
        Assert.isTrue(CollectionUtils.isNotEmpty(list));
        // creates a new local temporary table if it doesn't exists to handle temporary lists
        getJdbcTemplate().update(createTemporaryListQuery);
        // fills in a temporary list by given values
        int i = 0;
        final Iterator<? extends BaseEntity> iterator = list.iterator();
        final MapSqlParameterSource[] batchArgs = new MapSqlParameterSource[list.size()];
        while (iterator.hasNext()) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(HelperParameters.LIST_ID.name(), listId);
            params.addValue(HelperParameters.LIST_VALUE.name(), iterator.next().getId());
            batchArgs[i] = params;
            i++;
        }
        getNamedParameterJdbcTemplate().batchUpdate(insertTemporaryListItemQuery, batchArgs);
        return listId;
    }

    @Required
    public void setClearTemporaryListQuery(final String clearTemporaryListQuery) {
        this.clearTemporaryListQuery = clearTemporaryListQuery;
    }

    /**
     * Clears all records from a temporary list identified by the given ID.
     *
     * @param listId {@code Long} represents unique ID of a temporary list that has to be cleared
     * @return int represents the number of affected (deleted) rows
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int clearTempList(final Long listId) {
        Assert.notNull(listId);
        return getNamedParameterJdbcTemplate().update(clearTemporaryListQuery,
            new MapSqlParameterSource(HelperParameters.LIST_ID.name(), listId));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public int clearTempStringList(final Long listId) {
        Assert.notNull(listId);
        return getNamedParameterJdbcTemplate().update(clearTemporaryStringListQuery,
                                                  new MapSqlParameterSource(HelperParameters.LIST_ID.name(), listId));
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

    @Required
    public void setCreateTemporaryStringListQuery(String createTemporaryStringListQuery) {
        this.createTemporaryStringListQuery = createTemporaryStringListQuery;
    }

    @Required
    public void setInsertTemporaryStringListItemQuery(String insertTemporaryStringListItemQuery) {
        this.insertTemporaryStringListItemQuery = insertTemporaryStringListItemQuery;
    }

    @Required
    public void setClearTemporaryStringListQuery(String clearTemporaryStringListQuery) {
        this.clearTemporaryStringListQuery = clearTemporaryStringListQuery;
    }
}
