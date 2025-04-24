package com.chat.allchatonthis.config.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Cache advisor for improving cache performance and handling cache failures gracefully
 */
@Aspect
@Component
@Order(1) // Execute before default Spring caching aspects
@RequiredArgsConstructor
@Slf4j
public class CacheAdvisor {

    /**
     * Advice that wraps methods annotated with @Cacheable to provide:
     * - Graceful degradation when Redis is down
     * - Performance logging for slow cache operations
     * - Cache failure handling without application disruption
     */
    @Around("@annotation(org.springframework.cache.annotation.Cacheable)")
    public Object aroundCacheable(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        Cacheable cacheable = method.getAnnotation(Cacheable.class);
        if (cacheable == null) {
            return joinPoint.proceed(); // Not our concern
        }

        String[] cacheNames = cacheable.cacheNames().length > 0 ?
                cacheable.cacheNames() : cacheable.value();

        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();

        try {
            long startTime = System.currentTimeMillis();
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            // Log if the cache operation took longer than expected
            if (duration > 100) { // 100ms threshold
                log.warn("Slow cache operation for method {} in cache {}: {}ms",
                        methodName, String.join(",", cacheNames), duration);
            }

            return result;
        } catch (RedisConnectionFailureException e) {
            // Redis connection failure - gracefully degrade to non-cached operation
            log.error("Redis connection failure during cache operation for method {}. " +
                    "Executing without cache. Error: {}", methodName, e.getMessage());

            // Execute the method without caching
            return joinPoint.proceed();
        } catch (Exception e) {
            // Other caching errors - log and proceed without caching
            log.error("Cache operation error for method {} in cache {}: {}",
                    methodName, String.join(",", cacheNames), e.getMessage());

            // Execute the method without caching
            return joinPoint.proceed();
        }
    }

    /**
     * Advice for methods annotated with @CacheEvict to ensure cache eviction operations
     * don't disrupt application flow
     */
    @Around("@annotation(org.springframework.cache.annotation.CacheEvict)")
    public Object aroundCacheEvict(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failure during cache eviction. " +
                    "Proceeding without eviction: {}", e.getMessage());
            return joinPoint.proceed();
        } catch (Exception e) {
            log.error("Cache eviction error: {}", e.getMessage());
            return joinPoint.proceed();
        }
    }

    /**
     * Advice for methods annotated with @Caching to handle combined cache operations
     */
    @Around("@annotation(org.springframework.cache.annotation.Caching)")
    public Object aroundCaching(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failure during complex cache operation. " +
                    "Proceeding without caching: {}", e.getMessage());
            return joinPoint.proceed();
        } catch (Exception e) {
            log.error("Complex caching operation error: {}", e.getMessage());
            return joinPoint.proceed();
        }
    }
} 