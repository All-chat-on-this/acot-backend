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
 * Metrics service for tracking API endpoint calls and user activity.
 * <p>
 * Core responsibilities:
 * - Register and manage Micrometer metrics (Counters for endpoint calls, Gauge for online users)
 * - Thread-safe tracking of endpoint call counts using ConcurrentHashMap
 * - Atomic operations for online user count to ensure thread safety
 */
@Component
public class EndpointMetrics {

    // Micrometer's main registry for metric management (core component for exposing metrics to monitoring systems)
    private final MeterRegistry meterRegistry;

    // Thread-safe cache to store endpoint-specific Counters (prevents duplicate Counter creation for the same endpoint)
    // Key: endpoint identifier (format: "ControllerName.MethodName")
    // Value: Micrometer Counter instance for that endpoint
    private final Map<String, Counter> endpointCounters = new ConcurrentHashMap<>();

    // Atomic integer to track online users (ensures thread-safe increment/decrement operations)
    private final AtomicInteger onlineUsers = new AtomicInteger(0);

    /**
     * Constructor: Initializes the metrics registry and registers the online users Gauge.
     *
     * @param meterRegistry Micrometer's meter registry for metric management
     */
    public EndpointMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Register Gauge metric for online users:
        // - Gauge is used to track instantaneous values (current online count)
        // - "acot.online.users" is the metric name (prefix "acot" for "All Chat On This")
        // - onlineUsers::get provides the current value via method reference
        Gauge.builder("acot.online.users", onlineUsers::get)
                .description("Number of currently online users")
                .register(meterRegistry);
    }

    /**
     * Increment call count for a specific endpoint.
     *
     * @param controllerName The name of the controller (e.g., "UserController")
     * @param methodName     The name of the method (e.g., "getUser")
     *                       <p>
     *                       Implementation notes:
     *                       - Uses ConcurrentHashMap.computeIfAbsent() to ensure thread-safe, lazy initialization of Counters:
     *                       - If the endpoint already exists in the map, returns the existing Counter
     *                       - If not, creates a new Counter with controller/method tags and registers it to the registry
     *                       - "k" in the lambda represents the endpoint key (same as the input "endpoint" variable)
     */
    public void incrementEndpointCount(String controllerName, String methodName) {
        String endpoint = controllerName + "." + methodName;
        // computeIfAbsent: Atomically checks if the endpoint exists in the map.
        // - If absent: creates a new Counter with tags for controller/method, then caches it
        // - If present: returns the existing Counter to avoid duplicate registration
        Counter counter = endpointCounters.computeIfAbsent(endpoint, k ->
                Counter.builder("acot.endpoint.calls")
                        // TAGS: Add metadata labels to metrics for filtering/grouping in monitoring dashboards
                        // These tags will appear in Prometheus/Grafana and help organize metrics by controller and method
                        .tags(Arrays.asList(
                                Tag.of("controller", controllerName),  // Tag for grouping by controller
                                Tag.of("method", methodName)))         // Tag for grouping by method
                        // DESCRIPTION: Provides human-readable explanation of the metric in monitoring systems
                        // This description will be visible in monitoring UIs to explain what this counter measures
                        .description("Number of calls to endpoint")
                        .register(meterRegistry));  // Register the Counter to Micrometer

        counter.increment();  // Increment the Counter for this endpoint call
    }

    /**
     * Increment the number of online users (called on user login).
     * <p>
     * - Called from UserActivityMetricsConfig to centralize login tracking
     * - Uses AtomicInteger.incrementAndGet() for thread-safe increment
     */
    public void userLoggedIn() {
        onlineUsers.incrementAndGet();
    }

    /**
     * Decrement the number of online users (called on user logout or session timeout).
     * <p>
     * - Called from:
     * - UserActivityMetricsConfig (for explicit logouts)
     * - SessionEventListener (for session timeouts)
     * - Uses AtomicInteger.updateAndGet() to ensure non-negative count
     */
    public void userLoggedOut() {
        // Update the value to max(0, current - 1) to prevent negative online user count
        onlineUsers.updateAndGet(current -> Math.max(0, current - 1));
    }
}