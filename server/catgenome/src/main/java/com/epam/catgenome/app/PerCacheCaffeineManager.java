package com.epam.catgenome.app;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.util.concurrent.TimeUnit;

public class PerCacheCaffeineManager extends CaffeineCacheManager {

    @NotNull
    @Override
    protected Cache<Object, Object> createNativeCaffeineCache(String name) {
        Caffeine<Object, Object> builder = switch (name) {
            case "proteinTrack" -> Caffeine.newBuilder()
                    .maximumSize(1000)
                    .expireAfterAccess(5, TimeUnit.SECONDS)
                    .recordStats();

            case "indexCache" -> Caffeine.newBuilder()
                    .maximumSize(1000)
                    .expireAfterAccess(600, TimeUnit.SECONDS)
                    .recordStats();

            case "aclCache" -> {
                int ttl = 300;
                String period = System.getProperty("security.acl.cache.period");
                if (period != null && !period.isEmpty()) {
                    try {
                        ttl = Integer.parseInt(period);
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
                yield Caffeine.newBuilder()
                        .maximumSize(100)
                        .expireAfterAccess(ttl, TimeUnit.SECONDS)
                        .recordStats();
            }

            default -> Caffeine.newBuilder()
                    .maximumSize(100)
                    .expireAfterAccess(300, TimeUnit.SECONDS)
                    .recordStats();
        };
        return builder.build();
    }
}
