/*
 * MIT License
 *
 * Copyright (c) 2018 EPAM Systems
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

package com.epam.catgenome.util.feature.reader;

import com.epam.catgenome.constant.MessagesConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import static com.epam.catgenome.component.MessageHelper.getMessage;

@Service
@ConditionalOnProperty(value = "server.index.cache.enabled", havingValue = "true")
public class CaffeineBasedIndexCache {
    private static final String INDEX_CACHE = "indexCache";

    @Autowired
    private CacheManager cacheManager;

    /**
     * Retrieves an index from the cache by URL.
     */
    public IndexCache getFromCache(String indexUrl) {
        Assert.notNull(indexUrl, getMessage(MessagesConstants.ERROR_INDEX_URL_NOT_SPECIFIED));
        Cache cache = getCache();
        org.springframework.cache.Cache.ValueWrapper wrapper = cache.get(indexUrl);
        return wrapper != null ? (IndexCache) wrapper.get() : null;
    }

    /**
     * Puts an index into the cache.
     */
    public void putInCache(IndexCache index, String indexUrl) {
        Assert.notNull(index, getMessage(MessagesConstants.ERROR_INDEX_NOT_SPECIFIED));
        Assert.notNull(indexUrl, getMessage(MessagesConstants.ERROR_INDEX_URL_NOT_SPECIFIED));
        getCache().put(indexUrl, index);
    }

    /**
     * Removes an index from the cache.
     */
    public void evictFromCache(String indexUrl) {
        Assert.notNull(indexUrl, getMessage(MessagesConstants.ERROR_INDEX_URL_NOT_SPECIFIED));
        getCache().evict(indexUrl);
    }

    /**
     * Checks if the index URL is cached.
     */
    public boolean contains(String indexUrl) {
        Assert.notNull(indexUrl, getMessage(MessagesConstants.ERROR_INDEX_URL_NOT_SPECIFIED));
        return getCache().get(indexUrl) != null;
    }

    /**
     * Clears all entries in the index cache.
     */
    public void clearCache() {
        getCache().clear();
    }

    /**
     * Returns a string representation of the cache.
     */
    @Override
    public String toString() {
        Cache cache = getCache();
        return "Cache Name: " + cache.getName() + ", CacheManager: " + cacheManager;
    }

    // Helper to get the cache (fail-fast if not found)
    private Cache getCache() {
        Cache cache = cacheManager.getCache(INDEX_CACHE);
        if (cache == null) {
            throw new IllegalStateException("Cache '" + INDEX_CACHE + "' not found in CacheManager");
        }
        return cache;
    }
}
