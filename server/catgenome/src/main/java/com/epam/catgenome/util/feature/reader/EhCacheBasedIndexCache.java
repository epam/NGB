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

import static com.epam.catgenome.component.MessageHelper.getMessage;
import com.epam.catgenome.constant.MessagesConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.cache.Cache;

@Service
@ConditionalOnProperty(value = "server.index.cache.enabled", havingValue = "true")
public class EhCacheBasedIndexCache {
    private static final String INDEX_CACHE = "indexCache";

    @Autowired
    private JCacheCacheManager cacheManager;

    public void evictFromCache(String indexUrl) {
        Assert.notNull(indexUrl, getMessage(MessagesConstants.ERROR_INDEX_URL_NOT_SPECIFIED));

        IndexCache index = getFromCache(indexUrl);
        if (index != null) {
            cacheManager.getCacheManager().getCache(INDEX_CACHE).remove(indexUrl);
        }
    }

    public IndexCache getFromCache(String indexUrl) {
        Assert.notNull(indexUrl, getMessage(MessagesConstants.ERROR_INDEX_URL_NOT_SPECIFIED));

        return (IndexCache) cacheManager.getCacheManager().getCache(INDEX_CACHE).get(indexUrl);
    }

    public void putInCache(IndexCache index, String indexUrl) {
        Assert.notNull(indexUrl, getMessage(MessagesConstants.ERROR_INDEX_NOT_SPECIFIED));
        Assert.notNull(indexUrl, getMessage(MessagesConstants.ERROR_INDEX_URL_NOT_SPECIFIED));

        cacheManager.getCacheManager().getCache(INDEX_CACHE).put(indexUrl, index);
    }

    public boolean contains(String indexUrl) {
        Assert.notNull(indexUrl, getMessage(MessagesConstants.ERROR_INDEX_URL_NOT_SPECIFIED));

        return cacheManager.getCacheManager().getCache(INDEX_CACHE).containsKey(indexUrl);
    }

    public void clearCache() {
        cacheManager.getCacheManager().getCache(INDEX_CACHE).removeAll();
    }

    @Override
    public String toString() {
        Cache<Object, Object> cache = cacheManager.getCacheManager().getCache(INDEX_CACHE);

        return "Cache Name: " + cache.getName() + ", cacheManager: " + cache.getCacheManager();
    }
}
