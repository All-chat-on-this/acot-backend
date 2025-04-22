package com.chat.allchatonthis.config.web;

import com.chat.allchatonthis.config.web.resolver.LoginUserMethodArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring MVC configuration class that customizes the web application behavior.
 * This class implements WebMvcConfigurer to override default Spring MVC configuration settings.
 */
@Configuration // Indicates that this class declares one or more @Bean methods and may be processed by Spring
@RequiredArgsConstructor // Lombok annotation that automatically generates a constructor with required fields
public class WebMvcConfig implements WebMvcConfigurer {
    // Final field that will be injected via constructor created by @RequiredArgsConstructor
    private final LoginUserMethodArgumentResolver loginUserMethodArgumentResolver;

    /**
     * Adds custom HandlerMethodArgumentResolvers to the Spring MVC configuration.
     * This method registers our custom LoginUserMethodArgumentResolver which will
     * process the @LoginUser annotation in controller methods.
     *
     * @param resolvers List of argument resolvers to be registered
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserMethodArgumentResolver);
    }

    /**
     * Configures path matching options for the application.
     * This method applies a unified URL prefix pattern to all REST controllers.
     *
     * @param configurer The PathMatchConfigurer to customize
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Adds /api prefix to all methods in classes annotated with @RestController
        // For example, @RestController methods with @GetMapping("/users") will be accessible at /api/users
        configurer.addPathPrefix("/api", c -> c.isAnnotationPresent(RestController.class));
    }

    /**
     * Configures CORS (Cross-Origin Resource Sharing) for the application.
     * This allows requests from different origins to access our API.
     *
     * @param registry The CorsRegistry to customize
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")  // Allow all origins
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")  // Allow all headers
                .allowCredentials(true)  // Allow cookies
                .maxAge(3600);  // Cache preflight requests for 1 hour
    }

    /**
     * Creates a request logging filter that logs all requests received by the application.
     * This provides detailed logging of incoming HTTP requests.
     *
     * @return A configured CommonsRequestLoggingFilter bean
     */
    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
        loggingFilter.setIncludeClientInfo(true);
        loggingFilter.setIncludeQueryString(true);
        loggingFilter.setIncludePayload(true);
        loggingFilter.setMaxPayloadLength(10000);
        loggingFilter.setIncludeHeaders(true);
        loggingFilter.setAfterMessagePrefix("REQUEST DATA: ");
        return loggingFilter;
    }
}
