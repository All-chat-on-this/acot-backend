package com.chat.allchatonthis.config.metrics.endpoint;  // Package declaration for metrics aspect components

// Lombok annotation: Generates constructor with required arguments (final fields)

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
 * AOP aspect to intercept REST controller method calls and track endpoint metrics.
 * <p>
 * Core responsibilities:
 * - Identify REST controller methods using AOP pointcuts
 * - Validate if methods are actual HTTP endpoints (not internal helper methods)
 * - Trigger metric collection via EndpointMetrics for valid API calls
 */
@Aspect  // Marks this class as an AOP aspect
@Component  // Registers as a Spring-managed component
@RequiredArgsConstructor  // Lombok: Generates constructor with 'endpointMetrics' parameter
@Slf4j  // Lombok: Generates 'log' field for logging
public class EndpointMetricsAspect {

    // Dependency injection: Metrics service to record endpoint calls
    private final EndpointMetrics endpointMetrics;

    /**
     * Pointcut definition: Matches all classes annotated with @RestController.
     * <p>
     * Pointcut expression: "@within(org.springframework.web.bind.annotation.RestController)"
     * - @within: Matches types (classes) annotated with the specified annotation
     * - Ensures only REST controllers are intercepted (not service/DAO classes)
     */
    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void controllerPointcut() {
        // Pointcut method (no implementation needed)
    }

    /**
     * Before advice: Executes before methods matching 'controllerPointcut()'.
     * <p>
     * Purpose: Collect metrics for API calls before the actual business logic runs.
     *
     * @param joinPoint Context object providing details about the intercepted method call
     */
    @Before("controllerPointcut()")  // Applies this advice to the defined pointcut
    public void beforeControllerMethod(JoinPoint joinPoint) {
        try {
            // 1. Extract method signature from the join point
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            // 2. Get the actual Method object being called
            Method method = signature.getMethod();
            // 3. Get the declaring class (controller class) of the method
            Class<?> controllerClass = method.getDeclaringClass();
            // 4. Extract simple class name (e.g., "UserController")
            String controllerName = controllerClass.getSimpleName();
            // 5. Extract method name (e.g., "getUser")
            String methodName = method.getName();

            // 6. Validate if the method is an actual HTTP endpoint (not a helper method)
            if (isHttpEndpoint(method)) {
                // 7. Record the endpoint call via EndpointMetrics
                endpointMetrics.incrementEndpointCount(controllerName, methodName);
                // 8. Log debug information for monitoring
                log.debug("Endpoint called: {}.{}", controllerName, methodName);
            }
        } catch (Exception e) {
            // 9. Handle exceptions gracefully (metrics failure shouldn't break business logic)
            log.error("Error recording endpoint metrics", e);
        }
    }

    /**
     * Determines if a method is an HTTP endpoint by checking for REST annotations.
     * <p>
     * Checks for:
     * - @RequestMapping (generic HTTP method)
     * - @GetMapping, @PostMapping, etc. (specific HTTP methods)
     *
     * @param method The method to inspect
     * @return true if the method is an API endpoint, false otherwise
     */
    private boolean isHttpEndpoint(Method method) {
        // Check if the method has any of the HTTP mapping annotations
        return method.isAnnotationPresent(RequestMapping.class) ||  // Generic mapping
                method.isAnnotationPresent(GetMapping.class) ||      // GET requests
                method.isAnnotationPresent(PostMapping.class) ||     // POST requests
                method.isAnnotationPresent(PutMapping.class) ||      // PUT requests
                method.isAnnotationPresent(DeleteMapping.class) ||   // DELETE requests
                method.isAnnotationPresent(PatchMapping.class);      // PATCH requests
    }
}