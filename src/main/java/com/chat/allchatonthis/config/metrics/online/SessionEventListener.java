package com.chat.allchatonthis.config.metrics.online;  // Package declaration for online user metrics components

// Dependency: Metrics service to update online user count

import com.chat.allchatonthis.config.metrics.endpoint.EndpointMetrics;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Listener to track HTTP session destruction events (timeout, manual invalidation, etc.)
 * to update the online user count accurately.
 * <p>
 * Complementary to UserActivityMetricsConfig: Handles cases where users don't explicitly log out.
 */
@Component  // Registers as a Spring-managed component
@Slf4j  // Lombok: Generates 'log' field for logging
@RequiredArgsConstructor  // Lombok: Generates constructor with 'endpointMetrics' parameter
public class SessionEventListener implements HttpSessionListener {  // Implements session lifecycle listener

    // Dependency injection: Metrics service to update online user count
    private final EndpointMetrics endpointMetrics;

    /**
     * Callback triggered when an HTTP session is destroyed.
     * <p>
     * Session destruction can occur due to:
     * - Session timeout (inactivity)
     * - Explicit session.invalidate() call
     * - Application shutdown
     *
     * @param se Event object containing the destroyed session
     */
    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        // Log the destroyed session ID for debugging
        log.debug("Session destroyed: {}", se.getSession().getId());
        // Decrement online user count (even if user didn't explicitly log out)
        endpointMetrics.userLoggedOut();
    }

    // Other HttpSessionListener methods (sessionCreated) are not implemented here
    // since we only care about session destruction for online user tracking
}