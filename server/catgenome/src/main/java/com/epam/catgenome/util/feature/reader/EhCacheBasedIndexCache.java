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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.springframework.util.Assert;

public class EhCacheBasedIndexCache {
    private static final String INDEX_URL_REQUIRED = "Index Url required";
    private static final String CACHE_REQUIRED = "Cache required";
    private static final String INDEX_REQUIRED = "Index required";
    private final Ehcache cache;

    public EhCacheBasedIndexCache(Ehcache cache) {
        Assert.notNull(cache, CACHE_REQUIRED);
        this.cache = cache;
    }

    public void evictFromCache(String indexUrl) {
        Assert.notNull(indexUrl, INDEX_URL_REQUIRED);

        CacheIndex index = getFromCache(indexUrl);

        if (index != null) {
            cache.remove(indexUrl);
        }
    }

    public CacheIndex getFromCache(String indexUrl) {
        Assert.notNull(indexUrl, INDEX_URL_REQUIRED);

        Element element;

        if (contains(indexUrl)) {
            element = cache.get(indexUrl);
            return (CacheIndex) element.getObjectValue();
        } else {
            return null;
        }
    }

    public void putInCache(CacheIndex index, String indexUrl) {
        Assert.notNull(index, INDEX_REQUIRED);
        Assert.notNull(indexUrl, INDEX_URL_REQUIRED);

        cache.put(new Element(indexUrl, index));
    }

    public boolean contains(String indexUrl) {
        Assert.notNull(indexUrl, INDEX_URL_REQUIRED);

        Element element = null;

        try {
            element = cache.get(indexUrl);
        } catch (CacheException ignored) {
            return false;
        }

        return element != null;
    }

    public void clearCache() {
        cache.removeAll();
    }

    @Override
    public String toString() {
        return "Cache Name: " + cache.getName() + ", cacheManager: " + cache.getCacheManager() +
                " cacheSize: " + cache.getSize() + " maxEntriesInHeap: " +
                cache.getCacheConfiguration().getMaxEntriesLocalHeap() + " timeToIdle: " +
                cache.getCacheConfiguration().getTimeToIdleSeconds();
    }
}
