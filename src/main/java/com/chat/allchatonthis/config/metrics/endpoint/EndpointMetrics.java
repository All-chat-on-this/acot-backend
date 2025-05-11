package com.chat.allchatonthis.config.metrics.endpoint;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Metrics service for tracking API endpoint calls and user activity
 */
@Component
public class EndpointMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> endpointCounters = new ConcurrentHashMap<>();
    private final AtomicInteger onlineUsers = new AtomicInteger(0);
    
    public EndpointMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Register online users gauge
        Gauge.builder("acot.online.users", onlineUsers::get)
                .description("Number of currently online users")
                .register(meterRegistry);
    }
    
    /**
     * Increment call count for a specific endpoint
     * 
     * @param controllerName The name of the controller
     * @param methodName The name of the method
     */
    public void incrementEndpointCount(String controllerName, String methodName) {
        String endpoint = controllerName + "." + methodName;
        Counter counter = endpointCounters.computeIfAbsent(endpoint, k -> 
            Counter.builder("acot.endpoint.calls")
                   .tags(Arrays.asList(
                       Tag.of("controller", controllerName),
                       Tag.of("method", methodName)))
                   .description("Number of calls to endpoint")
                   .register(meterRegistry));
                   
        counter.increment();
    }
    
    /**
     * Increment the number of online users
     * 
     * This is called from UserActivityMetricsConfig which centralizes
     * all user login tracking to prevent redundant tracking.
     */
    public void userLoggedIn() {
        onlineUsers.incrementAndGet();
    }
    
    /**
     * Decrement the number of online users
     * 
     * This is called from UserActivityMetricsConfig which centralizes
     * all user logout tracking to prevent redundant tracking.
     * Additionally called from SessionEventListener to track session timeouts.
     */
    public void userLoggedOut() {
        onlineUsers.updateAndGet(current -> Math.max(0, current - 1));
    }
} 