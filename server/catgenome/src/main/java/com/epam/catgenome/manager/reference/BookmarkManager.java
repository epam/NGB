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

package com.epam.catgenome.manager.reference;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.epam.catgenome.dao.reference.BookmarkDao;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.index.BookmarkIndexEntry;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.index.IndexSearchResult;
import com.epam.catgenome.entity.reference.Bookmark;
import com.epam.catgenome.util.AuthUtils;

/**
 * {@code BookmarkManager} represents a service class designed to encapsulate all business
 * logic operations required to manage {@code} Bookmark objects
 */
@Service
public class BookmarkManager {
    @Autowired
    private BookmarkDao bookmarkDao;

    /**
     * Saves a given {@code Project} entity to a databases with it's bookmarked {@code ProjectItem}s
     * @param bookmark {@code Bookmark} entity to save
     * @return a saved {@code Bookmark}
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Bookmark saveBookmark(Bookmark bookmark) throws IOException {
        if (bookmark.getId() != null) {
            bookmarkDao.deleteBookmarkItems(bookmark.getId());
        } else {
            bookmark.setCreatedBy(AuthUtils.getCurrentUserId());
            bookmark.setCreatedDate(new Date());
        }

        bookmarkDao.saveBookmark(bookmark);
        bookmarkDao.insertBookmarkItems(bookmark.getOpenedItems(), bookmark.getId());

        return bookmark;
    }

    /**
     * Loads {@code Bookmark} entities for current user
     * @return a {@code List} of {@code Bookmark} entities
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<Bookmark> loadBookmarksByProject() {
        return bookmarkDao.loadAllBookmarks(AuthUtils.getCurrentUserId());
    }

    /**
     * Loads a {@code Bookmark} entity for a given ID
     * @param bookmarkId to load
     * @return a {@code Bookmark} entity
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Bookmark loadBookmark(long bookmarkId) {
        Bookmark bookmark = bookmarkDao.loadBookmarkById(bookmarkId);
        if (bookmark != null) {
            Map<Long, List<BiologicalDataItem>> itemMap = bookmarkDao.loadBookmarkItemsByBookmarkIds(
                    Collections.singletonList(bookmark.getId()));

            bookmark.setOpenedItems(itemMap.get(bookmark.getId()));

        }

        return bookmark;
    }

    /**
     * Loads {@code Bookmark} entities for a given collection of IDs
     * @param bookmarkIds collection of IDs to load
     * @return a {@code List} of {@code Bookmark} entities
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public List<Bookmark> loadBookmarksByIds(Collection<Long> bookmarkIds) {
        List<Bookmark> bookmarks = bookmarkDao.loadBookmarksByIds(bookmarkIds);

        if (!bookmarks.isEmpty()) {
            Map<Long, List<BiologicalDataItem>> itemMap = bookmarkDao.loadBookmarkItemsByBookmarkIds(bookmarkIds);
            for (Bookmark b : bookmarks) {
                b.setOpenedItems(itemMap.get(b.getId()));
            }
        }

        return bookmarks;
    }

    /**
     * Deletes a {@code Bookmark} entity, specified by ID, with all it's bookmarked items
     * @param bookmarkId {@code Long} an ID of a bookmark to delete
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteBookmark(long bookmarkId) {
        Bookmark bookmark = bookmarkDao.loadBookmarkById(bookmarkId);
        Assert.notNull(bookmark);
        bookmarkDao.deleteBookmarkItems(bookmarkId);
        bookmarkDao.deleteBookmark(bookmarkId);
    }

    /**
     * Searches bookmarks in the database
     * @param searchStr a bookmark's name prefix to search for
     * @param limit search result count
     * @return an {@link IndexSearchResult} object, that contains search results
     */
    public IndexSearchResult<FeatureIndexEntry> searchBookmarks(String searchStr, int limit) {
        int bookmarksCount = bookmarkDao.searchBookmarkCount(searchStr, AuthUtils.getCurrentUserId());
        List<Bookmark> bookmarks = bookmarkDao.searchBookmarks(searchStr, AuthUtils.getCurrentUserId(), limit);
        List<FeatureIndexEntry> bookmarkEntries = bookmarks.stream().map(BookmarkIndexEntry::new).collect(
            Collectors.toList());

        return new IndexSearchResult<>(bookmarkEntries, bookmarksCount > limit, bookmarksCount);
    }
}
