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

import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.epam.catgenome.constant.MessagesConstants;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Caches the tribble/tabix index objects the forked htsjdk readers under
 * {@code util.feature.reader} build, keyed by index URL.
 *
 * <p>Backed by EhCache 2 until Phase 3 of the Java 21 migration: Spring 6 removed
 * {@code org.springframework.cache.ehcache} outright, and EhCache 2.10.1 sizes a
 * {@code maxBytesLocalHeap} region by walking the cached object graph with
 * {@code Field.setAccessible}, which no JDK past 16 permits without {@code --add-opens} on
 * whatever packages the graph happens to reach. The five-method surface below is unchanged - it is
 * called from the readers, {@code FileManager} and eight managers - only the store behind it is now
 * a Caffeine cache. The class name is kept for the same reason: Phase 7 deletes it, the readers and
 * the {@code server.index.cache.enabled} property together with the htsjdk fork, and renaming it in
 * the meantime would churn 20 files for nothing.
 *
 * <p><b>The eviction bound changed with the store.</b> EhCache bounded the region by heap bytes
 * (100 MB); Caffeine cannot measure the retained size of an arbitrary object graph, and asking it to
 * would reintroduce exactly the reflective walk this replaces. The bound is an entry count instead.
 * The time-to-idle is unchanged at 600 s, and in practice that was always the binding constraint:
 * NGB holds one index per open file, so a browsing session reaches tens of entries, not hundreds.
 */
@Service
@ConditionalOnProperty(value = "server.index.cache.enabled", havingValue = "true")
public class EhCacheBasedIndexCache {

    private static final long MAX_ENTRIES = 200;
    private static final long TIME_TO_IDLE_SECONDS = 600;

    private final Cache<String, IndexCache> cache = Caffeine.newBuilder()
            .maximumSize(MAX_ENTRIES)
            .expireAfterAccess(TIME_TO_IDLE_SECONDS, TimeUnit.SECONDS)
            .build();

    public void evictFromCache(String indexUrl) {
        Assert.notNull(indexUrl, getMessage(MessagesConstants.ERROR_INDEX_URL_NOT_SPECIFIED));

        cache.invalidate(indexUrl);
    }

    public IndexCache getFromCache(String indexUrl) {
        Assert.notNull(indexUrl, getMessage(MessagesConstants.ERROR_INDEX_URL_NOT_SPECIFIED));

        return cache.getIfPresent(indexUrl);
    }

    public void putInCache(IndexCache index, String indexUrl) {
        Assert.notNull(index, getMessage(MessagesConstants.ERROR_INDEX_NOT_SPECIFIED));
        Assert.notNull(indexUrl, getMessage(MessagesConstants.ERROR_INDEX_URL_NOT_SPECIFIED));

        cache.put(indexUrl, index);
    }

    public boolean contains(String indexUrl) {
        Assert.notNull(indexUrl, getMessage(MessagesConstants.ERROR_INDEX_URL_NOT_SPECIFIED));

        return cache.getIfPresent(indexUrl) != null;
    }

    public void clearCache() {
        cache.invalidateAll();
    }

    /**
     * @return the number of entries currently held. Caffeine evicts asynchronously, so this is
     *         {@code cleanUp()}-ed first to make the count exact for callers that have just removed
     *         something - {@code EhCache.getSize()}, which this replaces, was exact.
     */
    public int size() {
        cache.cleanUp();
        return (int) cache.estimatedSize();
    }

    @Override
    public String toString() {
        return "Cache Name: indexCache, cacheSize: " + size()
                + " maxEntries: " + MAX_ENTRIES
                + " timeToIdle: " + TIME_TO_IDLE_SECONDS;
    }
}
