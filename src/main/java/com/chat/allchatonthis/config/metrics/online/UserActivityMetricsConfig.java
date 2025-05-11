package com.chat.allchatonthis.config.metrics.online;  // Package declaration for online user metrics configuration

// Dependency: Metrics service to update online user count

import com.chat.allchatonthis.config.metrics.endpoint.EndpointMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

/**
 * Centralized configuration for tracking user login/logout activity metrics.
 * <p>
 * Core responsibilities:
 * - Listen to Spring Security authentication events (login/logout)
 * - Avoid redundant metrics collection by consolidating tracking logic
 * - Provide custom handlers for form-based login/logout (without duplicate metrics)
 */
@Configuration  // Marks this class as a Spring configuration class
@RequiredArgsConstructor  // Lombok: Generates constructor with 'endpointMetrics' parameter
@Slf4j  // Lombok: Generates 'log' field for logging
public class UserActivityMetricsConfig {

    // Dependency injection: Metrics service to update online user count
    private final EndpointMetrics endpointMetrics;

    /**
     * Event listener for successful user authentication (login).
     * <p>
     * Triggered by Spring Security when a user successfully authenticates.
     *
     * @param event The authentication success event containing user details
     */
    @EventListener  // Listens to ApplicationEvent of type AuthenticationSuccessEvent
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        // Extract authentication object from the event
        Authentication authentication = event.getAuthentication();
        // Get the username from the authentication object
        String username = authentication.getName();
        // Log the login event for auditing
        log.debug("User logged in: {}", username);
        // Increment online user count via metrics service
        endpointMetrics.userLoggedIn();
    }

    /**
     * Event listener for successful user logout.
     * <p>
     * Triggered by Spring Security when a user successfully logs out.
     *
     * @param event The logout success event (may contain user details)
     */
    @EventListener  // Listens to ApplicationEvent of type LogoutSuccessEvent
    public void handleLogout(LogoutSuccessEvent event) {
        // Extract authentication object from the event (may be null in some cases)
        Authentication authentication = event.getAuthentication();
        if (authentication != null) {
            // Get the username from the authentication object
            String username = authentication.getName();
            // Log the logout event for auditing
            log.debug("User logged out: {}", username);
        }
        // Decrement online user count via metrics service
        endpointMetrics.userLoggedOut();
    }

    /**
     * Custom authentication success handler for form-based logins.
     * <p>
     * Purpose: Provide logging without duplicating metrics (already handled by event listener).
     *
     * @return A custom AuthenticationSuccessHandler instance
     */
    @Bean  // Registers this as a Spring bean
    public AuthenticationSuccessHandler loggingAuthenticationSuccessHandler() {
        return (request, response, authentication) -> {  // Lambda implementation of the handler
            // Get the username from the authentication object
            String username = authentication.getName();
            // Log the form-based login for debugging
            log.debug("User authenticated via form login: {}", username);
            // No metrics tracking here (already handled by handleAuthenticationSuccess)
        };
    }

    /**
     * Custom logout success handler for form-based logouts.
     * <p>
     * Purpose: Provide logging without duplicating metrics (already handled by event listener).
     *
     * @return A custom LogoutSuccessHandler instance
     */
    @Bean  // Registers this as a Spring bean
    public LogoutSuccessHandler loggingLogoutSuccessHandler() {
        return (request, response, authentication) -> {  // Lambda implementation of the handler
            if (authentication != null) {
                // Get the username from the authentication object
                String username = authentication.getName();
                // Log the form-based logout for debugging
                log.debug("User logged out via form logout: {}", username);
            }
            // No metrics tracking here (already handled by handleLogout)
        };
    }
}