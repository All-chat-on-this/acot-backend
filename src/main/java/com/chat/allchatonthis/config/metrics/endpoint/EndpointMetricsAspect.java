package com.chat.allchatonthis.config.metrics.endpoint;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;

/**
 * AOP aspect for intercepting controller calls to track metrics
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class EndpointMetricsAspect {

    private final EndpointMetrics endpointMetrics;

    /**
     * Pointcut for all controller methods
     */
    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void controllerPointcut() {
    }

    /**
     * Advice to execute before controller methods
     */
    @Before("controllerPointcut()")
    public void beforeControllerMethod(JoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Class<?> controllerClass = method.getDeclaringClass();
            String controllerName = controllerClass.getSimpleName();
            String methodName = method.getName();

            // Check if this is an actual API endpoint by looking for HTTP method annotations
            if (isHttpEndpoint(method)) {
                // Record the endpoint call
                endpointMetrics.incrementEndpointCount(controllerName, methodName);

                // Log endpoint call for debugging
                log.debug("Endpoint called: {}.{}", controllerName, methodName);
            }
        } catch (Exception e) {
            // Don't let metrics collection failures affect the application
            log.error("Error recording endpoint metrics", e);
        }
    }

    /**
     * Determines if a method is an HTTP endpoint by checking for REST annotations
     */
    private boolean isHttpEndpoint(Method method) {
        return method.isAnnotationPresent(RequestMapping.class) ||
                method.isAnnotationPresent(GetMapping.class) ||
                method.isAnnotationPresent(PostMapping.class) ||
                method.isAnnotationPresent(PutMapping.class) ||
                method.isAnnotationPresent(DeleteMapping.class) ||
                method.isAnnotationPresent(PatchMapping.class);
    }
} 