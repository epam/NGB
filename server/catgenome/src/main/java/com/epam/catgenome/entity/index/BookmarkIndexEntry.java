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

package com.epam.catgenome.entity.index;

import com.epam.catgenome.entity.reference.Bookmark;

/**
 * Source:      BookmarkIndexEntry
 * Created:     19.10.16, 17:59
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * A FeatureIndexEntry object, representing a bookmark in a feature index
 * </p>
 */
public class BookmarkIndexEntry extends FeatureIndexEntry {
    private Bookmark bookmark;

    public BookmarkIndexEntry() {
        super.setFeatureType(FeatureType.BOOKMARK);
    }

    public BookmarkIndexEntry(Bookmark bookmark) {
        super.setFeatureType(FeatureType.BOOKMARK);
        this.startIndex = bookmark.getStartIndex();
        this.endIndex = bookmark.getEndIndex();
        this.bookmark = bookmark;
        this.chromosome = bookmark.getChromosome();
        this.featureFileId = bookmark.getId();
        this.featureId = bookmark.getName();
        this.featureName = bookmark.getName();
    }

    public Bookmark getBookmark() {
        return bookmark;
    }

    public void setBookmark(Bookmark bookmark) {
        this.bookmark = bookmark;
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        BookmarkIndexEntry that = (BookmarkIndexEntry) o;
        return (bookmark != null) ? bookmark.equals(that.bookmark) : (that.bookmark == null);
    }

    @Override public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (bookmark != null ? bookmark.hashCode() : 0);
        return result;
    }
}
