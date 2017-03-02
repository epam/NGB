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

import java.util.List;

/**
 * Source:      IndexSearchResult
 * Created:     09.09.16, 17:32
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * A value object, representing result of a feature index query
 * </p>
 */
public class IndexSearchResult<T extends FeatureIndexEntry> {

    /**
     * Relevant index entries
     */
    private List<T> entries;

    /**
     * Indicates if total amount of search results exceeds limit
     */
    private boolean exceedsLimit;

    /**
     * Total amount of search results
     */
    private int totalResultsCount;

    private Integer totalPagesCount;

    public IndexSearchResult() {
        // no-op
    }

    public IndexSearchResult(List<T> entries, boolean exceedsLimit, int totalResultsCount) {
        this.entries = entries;
        this.exceedsLimit = exceedsLimit;
        this.totalResultsCount = totalResultsCount;
    }

    public void mergeFrom(IndexSearchResult<T> that) {
        entries.addAll(that.getEntries());
        exceedsLimit = exceedsLimit || that.exceedsLimit;
        totalResultsCount += that.getTotalResultsCount();
    }

    public boolean isExceedsLimit() {
        return exceedsLimit;
    }

    public void setExceedsLimit(boolean exceedsLimit) {
        this.exceedsLimit = exceedsLimit;
    }

    public List<T> getEntries() {
        return entries;
    }

    public void setEntries(List<T> entries) {
        this.entries = entries;
    }

    public int getTotalResultsCount() {
        return totalResultsCount;
    }

    public void setTotalResultsCount(int totalResultsCount) {
        this.totalResultsCount = totalResultsCount;
    }

    public Integer getTotalPagesCount() {
        return totalPagesCount;
    }

    public void setTotalPagesCount(Integer totalPagesCount) {
        this.totalPagesCount = totalPagesCount;
    }
}
