package com.epam.catgenome.util;

import com.epam.catgenome.util.feature.reader.IndexCache;
import com.epam.catgenome.util.feature.reader.EhCacheBasedIndexCache;
import net.sf.ehcache.Cache;
import net.sf.ehcache.config.CacheConfiguration;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test features of EhCacheBasedIndexCache: general functionality of cache.
 */

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("classpath:application.properties")
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class EhCacheTest {

    @Autowired
    private ApplicationContext context;

    @Autowired(required = false)
    private EhCacheBasedIndexCache indexCache;

    @Autowired
    private EhCacheCacheManager cacheManager;

    private IndexCache index1;
    private IndexCache index2;
    private String indexCacheName = "indexCache";

    @Before
    public void setup() {
        assertNotNull(context);
        assertNotNull(indexCache);
        assertNotNull(cacheManager);

        index1 = new TestIndexCache("indexName1");
        index2 = new TestIndexCache("indexName2");
        indexCache.putInCache(index1, "1");
        indexCache.putInCache(index2, "2");
    }

    @Test
    public void testGetAndEvictCache() {
        assertEquals(2, getSize());
        assertTrue(indexCache.contains("1"));

        IndexCache receivedIndex = indexCache.getFromCache("1");
        assertEquals(index1, receivedIndex);

        indexCache.evictFromCache("1");
        assertEquals(1, getSize());
        assertNull(indexCache.getFromCache("1"));
    }

    @Test
    public void testClearCache() {
        indexCache.clearCache();
        assertEquals(0, getSize());
    }

    @Test
    public void testMaxSizeInBytes() {
        Cache cache = cacheManager.getCacheManager().getCache(indexCacheName);
        CacheConfiguration cacheConfiguration = cache.getCacheConfiguration();
        Long maxSizeInBytes = cacheConfiguration.getMaxBytesLocalHeap();

        cacheConfiguration.setMaxBytesLocalHeap(10L);
        assertEquals(0, getSize());

        cacheConfiguration.setMaxBytesLocalHeap(maxSizeInBytes);
        indexCache.putInCache(index1, "1");
        assertEquals(1, getSize());
    }

    @Test
    public void testToString() {
        Cache cache = cacheManager.getCacheManager().getCache(indexCacheName);
        CacheConfiguration cacheConfiguration = cache.getCacheConfiguration();
        Long maxSizeInBytes = cacheConfiguration.getMaxBytesLocalHeap();
        String cacheName = cacheConfiguration.getName();
        Long timeToIdleSeconds = cacheConfiguration.getTimeToIdleSeconds();

        cacheConfiguration.setMaxBytesLocalHeap(1L);
        cacheConfiguration.setName("TestCache");
        cacheConfiguration.setTimeToIdleSeconds(100);

        assertEquals("Cache Name: TestCache, cacheManager: " + cache.getCacheManager() +
                " cacheSize: 0 maxBytesLocalHeap: 1 timeToIdle: 100", indexCache.toString());
        cacheConfiguration.setMaxBytesLocalHeap(maxSizeInBytes);
        cacheConfiguration.setName(cacheName);
        cacheConfiguration.setTimeToIdleSeconds(timeToIdleSeconds);
    }

    private class TestIndexCache implements IndexCache {
        private String name;

        TestIndexCache(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            TestIndexCache testIndex = (TestIndexCache) o;
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
