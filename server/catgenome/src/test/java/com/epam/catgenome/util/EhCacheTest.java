package com.epam.catgenome.util;

import com.epam.catgenome.util.feature.reader.CacheIndex;
import com.epam.catgenome.util.feature.reader.EhCacheBasedIndexCache;
import net.sf.ehcache.Cache;
import net.sf.ehcache.config.CacheConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.junit.Test;

/**
 * Test features of EhCacheBasedIndexCache
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class EhCacheTest {
    @Autowired
    private ApplicationContext context;

    @Autowired
    EhCacheBasedIndexCache indexCache;

    @Autowired
    private EhCacheCacheManager cacheManager;

    private CacheIndex index1;
    private CacheIndex index2;
    private String indexCacheName = "indexCache";

    @Before
    public void setup() {
        Assert.assertNotNull(context);
        Assert.assertNotNull(indexCache);
        Assert.assertNotNull(cacheManager);

        index1 = new TestIndex("indexName1");
        index2 = new TestIndex("indexName2");
        indexCache.putInCache(index1, "1");
        indexCache.putInCache(index2, "2");
    }

    @Test
    public void testGetAndEvictCache() {
        Assert.assertEquals(2, getSize());
        Assert.assertTrue(indexCache.contains("1"));

        CacheIndex receivedIndex = indexCache.getFromCache("1");
        Assert.assertEquals(index1, receivedIndex);

        indexCache.evictFromCache("1");
        Assert.assertEquals(1, getSize());
        Assert.assertNull(indexCache.getFromCache("1"));
    }

    @Test
    public void testClearCache() {
        indexCache.clearCache();
        Assert.assertEquals(0, getSize());
    }

    @Test
    public void testMaxSizeInBytes() {
        Cache cache = cacheManager.getCacheManager().getCache(indexCacheName);
        CacheConfiguration cacheConfiguration = cache.getCacheConfiguration();
        Long maxSizeInBytes = cacheConfiguration.getMaxBytesLocalHeap();

        cacheConfiguration.setMaxBytesLocalHeap(10L);
        Assert.assertEquals(0, getSize());

        cacheConfiguration.setMaxBytesLocalHeap(maxSizeInBytes);
        indexCache.putInCache(index1, "1");
        Assert.assertEquals(1, getSize());
    }

    private class TestIndex implements CacheIndex {
        private String name;

        public TestIndex(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestIndex testIndex = (TestIndex) o;

            return name != null ? name.equals(testIndex.name) : testIndex.name == null;
        }

        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }
    }

    private int getSize() {
        return cacheManager.getCacheManager().getCache(indexCacheName).getSize();
    }
}
