package com.chat.allchatonthis.scheduler.cache;

import com.chat.allchatonthis.config.cache.CacheEvictionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for cache cleanups
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class CacheCleanupScheduler {
    private final CacheEvictionManager cacheEvictionManager;

    /**
     * Scheduled method for cache cleanup
     * Runs at 2:00 AM every day
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupCache() {
        log.info("Running scheduled cache cleanup");
        cacheEvictionManager.clearAllCaches();
    }
}
