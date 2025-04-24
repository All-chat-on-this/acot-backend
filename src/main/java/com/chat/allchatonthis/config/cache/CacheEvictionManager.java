package com.chat.allchatonthis.config.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Manages cache eviction policies and provides methods for cache management
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheEvictionManager {

    // List of cache names managed by the application
    private static final List<String> CACHE_NAMES = Arrays.asList(
            "conversation",
            "conversation_message",
            "user_config",
            "user",
            "preference"
    );
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Clears all caches by cache name
     */
    public void clearAllCaches() {
        log.info("Clearing all application caches");
        CACHE_NAMES.forEach(this::clearCache);
    }

    /**
     * Clears a specific cache by name
     *
     * @param cacheName The name of the cache to clear
     */
    public void clearCache(String cacheName) {
        log.info("Clearing cache: {}", cacheName);
        String keyPattern = "acot_" + cacheName + "::*";
        Set<String> keys = redisTemplate.keys(keyPattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Cleared {} keys from cache {}", keys.size(), cacheName);
        } else {
            log.info("No keys found to clear in cache {}", cacheName);
        }
    }

    /**
     * Clear cache for specific user
     *
     * @param userId The user ID
     */
    public void clearUserCache(Long userId) {
        log.info("Clearing caches for user: {}", userId);

        // Clear user-specific caches
        List<String> userCachePatterns = Arrays.asList(
                "acot_user::*" + userId + "*",
                "acot_user_config::list:" + userId,
                "acot_preference::" + userId
        );

        userCachePatterns.forEach(pattern -> {
            Set<String> keys = redisTemplate.keys(pattern);
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared {} keys matching pattern {}", keys.size(), pattern);
            }
        });
    }
} 