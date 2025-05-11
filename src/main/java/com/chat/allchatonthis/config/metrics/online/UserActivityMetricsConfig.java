package com.chat.allchatonthis.config.metrics.online;

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
 * Centralized configuration for user activity metrics tracking
 * <p>
 * This class consolidates all user activity tracking logic in one place
 * to avoid redundant metrics collection by multiple components.
 * <p>
 * It manages:
 * 1. Spring Security authentication events through event listeners
 * 2. HTTP session lifecycle management through SessionEventListener
 * 3. Form-based login/logout through custom handlers (if needed)
 * <p>
 * This replaces the previous redundant implementations in:
 * - CustomAuthenticationHandler
 * - UserActivityListener
 * - AuthServiceImpl metrics tracking
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class UserActivityMetricsConfig {

    private final EndpointMetrics endpointMetrics;

    /**
     * Primary method for tracking successful authentication
     */
    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        String username = authentication.getName();
        log.debug("User logged in: {}", username);
        endpointMetrics.userLoggedIn();
    }

    /**
     * Primary method for tracking logout
     */
    @EventListener
    public void handleLogout(LogoutSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        if (authentication != null) {
            String username = authentication.getName();
            log.debug("User logged out: {}", username);
        }
        endpointMetrics.userLoggedOut();
    }

    /**
     * Creates an authentication success handler that doesn't double-count metrics
     * but still provides the logging functionality
     */
    @Bean
    public AuthenticationSuccessHandler loggingAuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            String username = authentication.getName();
            log.debug("User authenticated via form login: {}", username);
            // No metrics tracking here - already handled by the event listener
        };
    }

    /**
     * Creates a logout success handler that doesn't double-count metrics
     * but still provides the logging functionality
     */
    @Bean
    public LogoutSuccessHandler loggingLogoutSuccessHandler() {
        return (request, response, authentication) -> {
            if (authentication != null) {
                String username = authentication.getName();
                log.debug("User logged out via form logout: {}", username);
            }
            // No metrics tracking here - already handled by the event listener
        };
    }
} 