package com.chat.allchatonthis.config.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for cache monitoring
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class CacheMetricsConfig {

    private static final List<String> MONITORED_CACHES = Arrays.asList(
            "conversation",
            "conversation_message",
            "user_config",
            "user",
            "preference"
    );
    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Creates a scheduled task to monitor cache sizes
     */
    @Bean
    public ScheduledExecutorService cacheMonitorScheduler() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Schedule cache monitoring task to run every hour
        scheduler.scheduleAtFixedRate(
                this::monitorCacheSizes,
                1, // initial delay
                60, // period
                TimeUnit.MINUTES // time unit
        );

        return scheduler;
    }

    /**
     * Logs the sizes of all monitored caches
     */
    private void monitorCacheSizes() {
        log.info("=== Cache Size Report ===");

        MONITORED_CACHES.forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                // Get estimated size by counting keys with the cache name pattern
                String keyPattern = "acot_" + cacheName + "::*";
                Set<String> keys = redisTemplate.keys(keyPattern);
                int size = keys.size();

                log.info("Cache '{}': {} entries", cacheName, size);

                // Log if cache size seems unusually large (might need tuning)
                if (size > 10000) {
                    log.warn("Cache '{}' has grown very large ({} entries). Consider review.",
                            cacheName, size);
                }
            } else {
                log.warn("Cache '{}' not found in cache manager", cacheName);
            }
        });

        log.info("=========================");
    }

} 