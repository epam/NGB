package com.epam.catgenome.util.feature.reader;

import com.epam.catgenome.util.feature.reader.index.CacheIndex;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.springframework.util.Assert;

public class EhCacheBasedIndexCache {
    public static final String INDEX_URL_REQUIRED = "Index Url required";
    public static final String CACHE_REQUIRED = "Cache required";
    public static final String INDEX_REQUIRED = "Index required";
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

        Element element = null;

        try {
            element = cache.get(indexUrl);
        } catch (CacheException ignored) {
            return null;
        }

        if (element == null) {
            return null;
        }
        return (CacheIndex) element.getValue();
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
}
