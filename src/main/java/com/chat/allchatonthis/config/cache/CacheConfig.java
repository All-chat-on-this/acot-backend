package com.chat.allchatonthis.config.cache;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * Cache configuration for Redis
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Customizes the default Redis cache manager
     * 
     * @return RedisCacheManagerBuilderCustomizer instance
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return (builder) -> builder
                .withCacheConfiguration("conversation",
                        createRedisCacheConfigurationWithTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("conversation_message",
                        createRedisCacheConfigurationWithTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("user_config",
                        createRedisCacheConfigurationWithTtl(Duration.ofHours(1)))
                .withCacheConfiguration("user",
                        createRedisCacheConfigurationWithTtl(Duration.ofHours(2)));
    }

    /**
     * Creates a Redis cache configuration with a specific TTL
     * 
     * @param ttl Time-to-live duration
     * @return Redis cache configuration
     */
    private RedisCacheConfiguration createRedisCacheConfigurationWithTtl(Duration ttl) {
        // Use JSON serialization for cache values
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer()));
    }
} 