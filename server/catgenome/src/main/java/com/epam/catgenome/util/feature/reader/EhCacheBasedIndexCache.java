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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class EhCacheBasedIndexCache {
    private static final String INDEX_URL_REQUIRED = "Index Url required";
    private static final String INDEX_REQUIRED = "Index required";
    private static final String INDEX_CACHE = "indexCache";

    @Autowired
    private EhCacheCacheManager cacheManager;

    public void evictFromCache(String indexUrl) {
        Assert.notNull(indexUrl, INDEX_URL_REQUIRED);

        IndexCache index = getFromCache(indexUrl);
        if (index != null) {
            cacheManager.getCacheManager().getCache(INDEX_CACHE).remove(indexUrl);
        }
    }

    public IndexCache getFromCache(String indexUrl) {
        Assert.notNull(indexUrl, INDEX_URL_REQUIRED);

        Element element = cacheManager.getCacheManager().getCache(INDEX_CACHE).get(indexUrl);
        if (element != null) {
            return (IndexCache) element.getObjectValue();
        } else {
            return null;
        }
    }

    public void putInCache(IndexCache index, String indexUrl) {
        Assert.notNull(index, INDEX_REQUIRED);
        Assert.notNull(indexUrl, INDEX_URL_REQUIRED);

        cacheManager.getCacheManager().getCache(INDEX_CACHE).put(new Element(indexUrl, index));
    }

    public boolean contains(String indexUrl) {
        Assert.notNull(indexUrl, INDEX_URL_REQUIRED);

        Element element;
        try {
            element = cacheManager.getCacheManager().getCache(INDEX_CACHE).get(indexUrl);
        } catch (CacheException ex) {
            return false;
        }
        return element != null;
    }

    public void clearCache() {
        cacheManager.getCacheManager().getCache(INDEX_CACHE).removeAll();
    }

    @Override
    public String toString() {
        Ehcache cache = cacheManager.getCacheManager().getCache(INDEX_CACHE);

        return "Cache Name: " + cache.getName() + ", cacheManager: " + cache.getCacheManager() +
                " cacheSize: " + cache.getSize() + " maxBytesLocalHeap: " +
                cache.getCacheConfiguration().getMaxBytesLocalHeap() + " timeToIdle: " +
                cache.getCacheConfiguration().getTimeToIdleSeconds();
    }
}
