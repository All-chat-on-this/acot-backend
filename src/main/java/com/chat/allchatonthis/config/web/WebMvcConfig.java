package com.chat.allchatonthis.config.web;

import com.chat.allchatonthis.config.web.resolver.LoginUserMethodArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
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
}
