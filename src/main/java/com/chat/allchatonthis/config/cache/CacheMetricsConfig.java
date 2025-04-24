package com.chat.allchatonthis.config.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
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

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final List<String> MONITORED_CACHES = Arrays.asList(
            "conversation",
            "conversation_message",
            "user_config",
            "user",
            "preference"
    );

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
                int size = keys != null ? keys.size() : 0;
                
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
    
    /**
     * Bean for cache cleanup scheduling
     */
    @Bean
    public CacheCleanupScheduler cacheCleanupScheduler(CacheEvictionManager cacheEvictionManager) {
        return new CacheCleanupScheduler(cacheEvictionManager);
    }
    
    /**
     * Helper class to schedule cache cleanups
     */
    @Slf4j
    @RequiredArgsConstructor
    public static class CacheCleanupScheduler {
        private final CacheEvictionManager cacheEvictionManager;
        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        /**
         * Initialize scheduler for cache cleanup
         */
        @Bean
        public void init() {
            // Schedule cleanup to run at 2 AM daily
            long initialDelay = calculateInitialDelayForTime(2, 0);
            
            scheduler.scheduleAtFixedRate(
                    () -> {
                        log.info("Running scheduled cache cleanup");
                        cacheEvictionManager.clearAllCaches();
                    },
                    initialDelay,
                    TimeUnit.DAYS.toMillis(1),
                    TimeUnit.MILLISECONDS
            );
            
            log.info("Scheduled cache cleanup initialized to run daily at 2:00 AM");
        }
        
        /**
         * Calculate delay until next occurrence of specified time
         */
        private long calculateInitialDelayForTime(int targetHour, int targetMinute) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextRun = now.withHour(targetHour).withMinute(targetMinute).withSecond(0);
            
            if (now.compareTo(nextRun) > 0) {
                nextRun = nextRun.plusDays(1);
            }
            
            return Duration.between(now, nextRun).toMillis();
        }
    }
} 