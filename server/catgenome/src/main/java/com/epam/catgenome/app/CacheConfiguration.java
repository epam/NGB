package com.epam.catgenome.app;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfiguration {

    @Bean
    public CacheManager cacheManager() {
        PerCacheCaffeineManager cacheManager = new PerCacheCaffeineManager();
        cacheManager.setCacheNames(java.util.Arrays.asList("aclCache", "indexCache", "proteinTrack"));
        return cacheManager;
    }
}
