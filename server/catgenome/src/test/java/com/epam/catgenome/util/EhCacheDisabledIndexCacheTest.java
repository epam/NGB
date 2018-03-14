package com.epam.catgenome.util;

import com.epam.catgenome.util.feature.reader.EhCacheBasedIndexCache;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

/**
 * Test features of EhCacheBasedIndexCache: if index cache is disabled in properties, it must be null.
 */

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("classpath:test-catgenome-cache-disable.properties")
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class EhCacheDisabledIndexCacheTest {

    @Autowired
    private ApplicationContext context;

    @Autowired(required = false)
    private EhCacheBasedIndexCache indexCache;

    @Test
    public void testCacheProperty() {
        assertNotNull(context);
        Boolean indexCachePropertyStatus =
                Boolean.valueOf(context.getEnvironment().getProperty("server.index.cache.enabled"));
        assertFalse("Index cache property must be false", indexCachePropertyStatus);
        assertNull("Index cache must be null", indexCache);
    }
}
