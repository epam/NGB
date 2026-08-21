package com.epam.catgenome.util;

import com.epam.catgenome.util.feature.reader.IndexCache;
import com.epam.catgenome.util.feature.reader.EhCacheBasedIndexCache;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test features of EhCacheBasedIndexCache: general functionality of cache.
 *
 * <p>Two of the five tests here went with EhCache in Phase 3 of the Java 21 migration, because what
 * they asserted no longer exists rather than because they became inconvenient:
 * <ul>
 *   <li>{@code testMaxSizeInBytes} reached through {@code EhCacheCacheManager} to the region's
 *       {@code CacheConfiguration}, set {@code maxBytesLocalHeap} to 10 bytes and checked that the
 *       cache emptied itself. Caffeine cannot bound a cache by the retained size of an arbitrary
 *       object graph at all - which is exactly why the sizing had to go, see
 *       {@link EhCacheBasedIndexCache} - so there is no byte bound left to shrink. The
 *       entry-count bound it is replaced by is not usefully testable the same way: it is 200, and
 *       Caffeine evicts asynchronously.</li>
 *   <li>{@code testToString} asserted the exact text of {@code toString()}, including the EhCache
 *       {@code CacheManager}'s own {@code toString()} and the {@code maxBytesLocalHeap} figure, after
 *       mutating the live configuration. Both of those are gone; the string is now built from
 *       constants and {@code size()}, and pinning a diagnostic string is not worth a test.</li>
 * </ul>
 * The other three are unchanged apart from {@code getSize()}, which asks the cache itself instead of
 * going through a cache manager the index cache no longer uses.
 */

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("classpath:test-catgenome.properties")
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class EhCacheTest {

    @Autowired
    private ApplicationContext context;

    @Autowired(required = false)
    private EhCacheBasedIndexCache indexCache;

    private IndexCache index1;
    private IndexCache index2;

    @Before
    public void setup() {
        assertNotNull(context);
        assertNotNull(indexCache);

        indexCache.clearCache();
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

    // Static on purpose. As an inner class every instance carries a this$0 reference to the test, and
    // through its @Autowired ApplicationContext to the whole Spring container. That used to matter a
    // great deal - EhCache's sizing walker traversed it on every put and died with
    // InaccessibleObjectException on JDK 17 - and now matters only as a matter of hygiene.
    private static class TestIndexCache implements IndexCache {
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
        return indexCache.size();
    }
}
