package com.epam.catgenome.util;

import com.epam.catgenome.util.feature.reader.IndexCache;
import com.epam.catgenome.util.feature.reader.EhCacheBasedIndexCache;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.junit.Test;
import javax.cache.Cache;
import static org.junit.Assert.*;

/**
 * Test features of EhCacheBasedIndexCache: general functionality of cache.
 */

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("classpath:test-catgenome.properties")
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class EhCacheTest {

    @Autowired
    private ApplicationContext context;

    @Autowired(required = false)
    private EhCacheBasedIndexCache indexCache;

    @Autowired
    private JCacheCacheManager cacheManager;

    private IndexCache index1;
    private IndexCache index2;
    private static final String INDEX_CACHE_NAME = "indexCache";

    @Before
    public void setup() {
        assertNotNull(context);
        assertNotNull(cacheManager);

        index1 = new TestIndexCache("indexName1");
        index2 = new TestIndexCache("indexName2");
        indexCache.putInCache(index1, "1");
        indexCache.putInCache(index2, "2");
    }

    @Test
    public void testCacheProperty() {
        Boolean indexCacheStatus = Boolean.valueOf(context.getEnvironment().getProperty("server.index.cache.enabled"));
        assertTrue(indexCacheStatus);
        assertNotNull(indexCache);
    }

    @Test
    public void testGetAndEvictCache() {
        assertEquals(2, getSize());
        assertTrue(indexCache.contains("1"));
        assertTrue(indexCache.contains("2"));

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
        Cache<Object, Object> cache = cacheManager.getCacheManager().getCache(INDEX_CACHE_NAME);
        int count = 0;
        for (Cache.Entry<Object, Object> objectObjectEntry : cache) {
            count++;
        }
        return count;
    }
}
