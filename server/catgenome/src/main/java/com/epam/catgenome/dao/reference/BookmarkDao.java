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

package com.epam.catgenome.dao.reference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.reference.Bookmark;
import com.epam.catgenome.entity.reference.Chromosome;

/**
 * <p>
 * A DAO class to handle database operations with {@code Bookmark} entities.
 * </p>
 */
public class BookmarkDao extends NamedParameterJdbcDaoSupport {
    private String bookmarkSequenceName;
    private String bookmarkItemSequenceName;

    private String insertBookmarkQuery;
    private String updateBookmarkQuery;
    private String loadAllBookmarksQuery;
    private String searchBookmarksQuery;
    private String searchBookmarkCountQuery;
    private String loadBookmarkByIdQuery;
    private String loadBookmarksByIdsQuery;
    private String deleteBookmarkQuery;

    private String insertBookmarkItemsQuery;
    private String deleteBookmarkItemsQuery;
    private String loadBookmarksItemsQuery;

    @Autowired
    private DaoHelper daoHelper;

    /**
     * Saves a database entity to a database. To perform a {@code Bookmark} update, an ID should be set.
     * @param bookmark a {@code Bookmark} to save
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveBookmark(Bookmark bookmark) {
        if (bookmark.getId() == null) {
            bookmark.setId(daoHelper.createId(bookmarkSequenceName));
            getNamedParameterJdbcTemplate().update(insertBookmarkQuery, BookmarkParameters.getParameters(bookmark));
        } else {
            getNamedParameterJdbcTemplate().update(updateBookmarkQuery, BookmarkParameters.getParameters(bookmark));
        }
    }

    /**
     * Loads {@code Bookmark} entities, saved for given project ID and chromosome ID
     * @return {@code List&lt;Bookmark&gt;}
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<Bookmark> loadAllBookmarks(long userId) {
        return getJdbcTemplate().query(loadAllBookmarksQuery, BookmarkParameters.getRowMapper(), userId);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public List<Bookmark> searchBookmarks(String searchStr, long userId, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(BookmarkParameters.BOOKMARK_NAME.name(), searchStr + '%');
        params.addValue(BookmarkParameters.CREATED_BY.name(), userId);
        params.addValue("SEARCH_LIMIT", limit);

        return getNamedParameterJdbcTemplate().query(searchBookmarksQuery, params, BookmarkParameters.getRowMapper());
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public int searchBookmarkCount(String searchStr, long userId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(BookmarkParameters.BOOKMARK_NAME.name(), searchStr + '%');
        params.addValue(BookmarkParameters.CREATED_BY.name(), userId);

        return getNamedParameterJdbcTemplate().queryForObject(searchBookmarkCountQuery, params, Integer.class);
    }

    /**
     * Loads a {@code Bookmark} entity, saved for given ID
     * @param bookmarkId {@code Long} a {@code Bookmark} to load
     * @return {@code Bookmark}
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public Bookmark loadBookmarkById(long bookmarkId) {
        List<Bookmark> bookmarks = getJdbcTemplate().query(loadBookmarkByIdQuery, BookmarkParameters.getRowMapper(),
                bookmarkId);
        return bookmarks.isEmpty() ? null : bookmarks.get(0);
    }

    /**
     * Loads a {@code List} of {@code Bookmark} entities, saved for given IDs
     * @param bookmarkIds {@code List} of {@code Bookmark} IDs to load
     * @return {@code Bookmark}
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<Bookmark> loadBookmarksByIds(final Collection<Long> bookmarkIds) {
        if (bookmarkIds == null || bookmarkIds.isEmpty()) {
            return Collections.emptyList();
        }

        Long listId = daoHelper.createTempLongList(bookmarkIds);
        List<Bookmark> bookmarks = getJdbcTemplate().query(loadBookmarksByIdsQuery, BookmarkParameters.getRowMapper(),
                listId);
        daoHelper.clearTempList(listId);

        return bookmarks;
    }

    /**
     * Deletes a {@code Bookmark} entity, specified by ID
     * @param bookmarkId {@code Long} an ID of a bookmark to delete
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteBookmark(long bookmarkId) {
        getJdbcTemplate().update(deleteBookmarkQuery, bookmarkId);
    }

    /**
     * Saves bookmarked {@code ProjectItem} entities into the database
     * @param bookmarkItems a {@code List&lt;ProjectItem&gt;} of bookmarked project items to save
     * @param bookmarkId an ID of a bookmark to save items
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void insertBookmarkItems(List<BiologicalDataItem> bookmarkItems, long bookmarkId) {
        if (bookmarkItems == null || bookmarkItems.isEmpty()) {
            return;
        }
        getNamedParameterJdbcTemplate().batchUpdate(insertBookmarkItemsQuery, BookmarkItemParameters.getParameters(
                bookmarkItems, bookmarkId, daoHelper, bookmarkItemSequenceName));
    }

    /**
     * Deletes bookmarked project items for a given bookmark ID
     * @param bookmarkId {@code Long} a bookmark ID to delete items
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteBookmarkItems(long bookmarkId) {
        getJdbcTemplate().update(deleteBookmarkItemsQuery, bookmarkId);
    }

    /**
     * Loads bookmarked project items by given list of bookmark IDs
     * @param bookmarkIds a {@code List&lt;Long&gt;} of bookmarks IDs to load items for
     * @return a {@code Map} of {@code List}s of {@code ProjectItem}, mapped to their bookmark's IDs
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Map<Long, List<BiologicalDataItem>> loadBookmarkItemsByBookmarkIds(Collection<Long> bookmarkIds) {
        if (bookmarkIds == null || bookmarkIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Long listId = daoHelper.createTempLongList(bookmarkIds);
        Map<Long, List<BiologicalDataItem>> itemsMap = new HashMap<>();

        getJdbcTemplate().query(loadBookmarksItemsQuery, rs -> {
            BiologicalDataItem dataItem = BiologicalDataItemDao.BiologicalDataItemParameters.getRowMapper()
                .mapRow(rs, 0);
            long bookmarkId = rs.getLong(BookmarkItemParameters.BOOKMARK_ID.name());
            if (!itemsMap.containsKey(bookmarkId)) {
                itemsMap.put(bookmarkId, new ArrayList<>());
            }
            itemsMap.get(bookmarkId).add(dataItem);
        }, listId);

        daoHelper.clearTempList(listId);
        return itemsMap;
    }

    enum BookmarkParameters {
        BOOKMARK_ID,
        BOOKMARK_NAME,
        START_INDEX,
        END_INDEX,
        REFERRED_CHROMOSOME_ID,
        CHROMOSOME_NAME,
        CREATED_BY,
        CREATED_DATE,
        OWNER;

        private static MapSqlParameterSource getParameters(Bookmark bookmark) {
            MapSqlParameterSource params = new MapSqlParameterSource();

            params.addValue(BOOKMARK_ID.name(), bookmark.getId());
            params.addValue(BOOKMARK_NAME.name(), bookmark.getName());
            params.addValue(START_INDEX.name(), bookmark.getStartIndex());
            params.addValue(END_INDEX.name(), bookmark.getEndIndex());
            params.addValue(REFERRED_CHROMOSOME_ID.name(), bookmark.getChromosome().getId());
            params.addValue(CREATED_BY.name(), bookmark.getCreatedBy());
            params.addValue(CREATED_DATE.name(), bookmark.getCreatedDate());
            params.addValue(OWNER.name(), bookmark.getOwner());

            return params;
        }

        private static RowMapper<Bookmark> getRowMapper() {
            return (rs, rowNum) -> {
                Bookmark bookmark = new Bookmark();

                bookmark.setId(rs.getLong(BOOKMARK_ID.name()));
                bookmark.setName(rs.getString(BOOKMARK_NAME.name()));
                bookmark.setStartIndex(rs.getInt(START_INDEX.name()));
                bookmark.setEndIndex(rs.getInt(END_INDEX.name()));
                bookmark.setChromosome(new Chromosome(rs.getLong(REFERRED_CHROMOSOME_ID.name())));
                bookmark.getChromosome().setName(rs.getString(CHROMOSOME_NAME.name()));
                bookmark.setCreatedBy(rs.getLong(CREATED_BY.name()));
                bookmark.setCreatedDate(new Date(rs.getTimestamp(CREATED_DATE.name()).getTime()));
                bookmark.setOwner(rs.getString(OWNER.name()));

                return bookmark;
            };
        }
    }

    enum BookmarkItemParameters {
        BOOKMARK_ITEM_ID,
        BOOKMARK_ID,
        BIOLOGICAL_ITEM_ID;

        private static MapSqlParameterSource[] getParameters(List<BiologicalDataItem> items, long bookmarkId, DaoHelper
                daoHelper, String bookmarkItemSequenceName) {
            MapSqlParameterSource[] params = new MapSqlParameterSource[items.size()];

            List<Long> ids = daoHelper.createIds(bookmarkItemSequenceName, items.size());

            for (int i = 0; i < items.size(); i++) {
                MapSqlParameterSource param = new MapSqlParameterSource();

                param.addValue(BOOKMARK_ITEM_ID.name(), ids.get(i));
                param.addValue(BOOKMARK_ID.name(), bookmarkId);
                param.addValue(BIOLOGICAL_ITEM_ID.name(), BiologicalDataItem.getBioDataItemId(
                        items.get(i)));

                params[i] = param;
            }

            return params;
        }
    }

    @Required
    public void setBookmarkSequenceName(String bookmarkSequenceName) {
        this.bookmarkSequenceName = bookmarkSequenceName;
    }

    @Required
    public void setBookmarkItemSequenceName(String bookmarkItemSequenceName) {
        this.bookmarkItemSequenceName = bookmarkItemSequenceName;
    }

    @Required
    public void setInsertBookmarkQuery(String insertBookmarkQuery) {
        this.insertBookmarkQuery = insertBookmarkQuery;
    }

    @Required
    public void setUpdateBookmarkQuery(String updateBookmarkQuery) {
        this.updateBookmarkQuery = updateBookmarkQuery;
    }

    @Required
    public void setLoadAllBookmarksQuery(String loadAllBookmarksQuery) {
        this.loadAllBookmarksQuery = loadAllBookmarksQuery;
    }

    @Required
    public void setInsertBookmarkItemsQuery(String insertBookmarkItemsQuery) {
        this.insertBookmarkItemsQuery = insertBookmarkItemsQuery;
    }

    @Required
    public void setDeleteBookmarkItemsQuery(String deleteBookmarkItemsQuery) {
        this.deleteBookmarkItemsQuery = deleteBookmarkItemsQuery;
    }

    @Required
    public void setLoadBookmarksItemsQuery(String loadBookmarksItemsQuery) {
        this.loadBookmarksItemsQuery = loadBookmarksItemsQuery;
    }

    @Required
    public void setDeleteBookmarkQuery(String deleteBookmarkQuery) {
        this.deleteBookmarkQuery = deleteBookmarkQuery;
    }

    @Required
    public void setLoadBookmarkByIdQuery(String loadBookmarkByIdQuery) {
        this.loadBookmarkByIdQuery = loadBookmarkByIdQuery;
    }

    @Required
    public void setLoadBookmarksByIdsQuery(String loadBookmarksByIdsQuery) {
        this.loadBookmarksByIdsQuery = loadBookmarksByIdsQuery;
    }

    @Required
    public void setSearchBookmarksQuery(String searchBookmarksQuery) {
        this.searchBookmarksQuery = searchBookmarksQuery;
    }

    @Required
    public void setSearchBookmarkCountQuery(String searchBookmarkCountQuery) {
        this.searchBookmarkCountQuery = searchBookmarkCountQuery;
    }
}
