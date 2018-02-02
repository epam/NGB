package com.epam.catgenome.util.feature.reader;

import com.epam.catgenome.util.feature.reader.index.CacheIndex;
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

        return cache.isKeyInCache(indexUrl);
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
