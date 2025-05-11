package com.chat.allchatonthis.config.metrics.online;

import com.chat.allchatonthis.config.metrics.endpoint.EndpointMetrics;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Session listener to track session destruction events (timeout, browser close, etc.)
 * to accurately update the online user count
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SessionEventListener implements HttpSessionListener {

    private final EndpointMetrics endpointMetrics;

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        log.debug("Session destroyed: {}", se.getSession().getId());
        // When a session is destroyed (timeout, browser close), decrement the user count
        endpointMetrics.userLoggedOut();
    }
} 