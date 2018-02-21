package com.epam.catgenome.util;

import com.epam.catgenome.util.feature.reader.IndexCache;
import com.epam.catgenome.util.feature.reader.EhCacheBasedIndexCache;
import net.sf.ehcache.Cache;
import net.sf.ehcache.config.CacheConfiguration;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

/**
 * Test features of EhCacheBasedIndexCache: general functionality of cache.
 */

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("classpath:test-catgenome.properties")
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class EhCacheTest {

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired(required = false)
    private EhCacheBasedIndexCache indexCache;

    @Autowired
    private EhCacheCacheManager cacheManager;

    @Autowired
    private ConfigurableEnvironment environment;

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
        Boolean indexCacheStatus = Boolean.valueOf(context.getEnvironment().getProperty("server.cache.enabled"));
        assertTrue(indexCacheStatus);
        assertNotNull(indexCache);

        ConfigurableEnvironment env = context.getEnvironment();
        MutablePropertySources propertySources = env.getPropertySources();
        Map<String, Object> map = new HashMap<>();
        map.put("server.cache.enabled","false");
        propertySources
                .addFirst(new MapPropertySource("newmap", map));

        indexCacheStatus = Boolean.valueOf(context.getEnvironment().getProperty("server.cache.enabled"));
        assertFalse(indexCacheStatus);
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

    @Test
    public void testMaxSizeInBytes() {
        Cache cache = cacheManager.getCacheManager().getCache(INDEX_CACHE_NAME);
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
        Cache cache = cacheManager.getCacheManager().getCache(INDEX_CACHE_NAME);
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
        return cacheManager.getCacheManager().getCache(INDEX_CACHE_NAME).getSize();
    }
}
