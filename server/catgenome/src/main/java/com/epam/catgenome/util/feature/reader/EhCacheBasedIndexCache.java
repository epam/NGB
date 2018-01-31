package com.epam.catgenome.util.feature.reader;

import com.epam.catgenome.util.feature.reader.index.CacheIndex;
import com.epam.catgenome.util.feature.reader.index.Index;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.springframework.util.Assert;

public class EhCacheBasedIndexCache {
    private final Ehcache cache;

    public EhCacheBasedIndexCache(Ehcache cache) {
        Assert.notNull(cache, "Cache required");
        this.cache = cache;
    }

    public void evictFromCache(String indexUrl) {
        Assert.notNull(indexUrl, "Index Url required");

        CacheIndex index = getFromCache(indexUrl);

        if (index != null) {
            cache.remove(indexUrl);
        }
    }

    public CacheIndex getFromCache(String indexUrl) {
        Assert.notNull(indexUrl, "Index Url required");

        Element element = null;

        try {
            element = cache.get(indexUrl);
        } catch (CacheException ignored) {
        }

        if (element == null) {
            return null;
        }
        return (CacheIndex) element.getValue();
    }

    public void putInCache(CacheIndex index, String indexUrl) {
        Assert.notNull(index, "Index required");
        Assert.notNull(indexUrl, "Index Url required");

        cache.put(new Element(indexUrl, index));
    }

    public boolean contains(String indexUrl) {
        Assert.notNull(indexUrl, "Index Url required");

        Element element = null;

        try {
            element = cache.get(indexUrl);
        } catch (CacheException ignored) {
        }

        return element != null;
    }

    public void clearCache() {
        cache.removeAll();
    }
}
