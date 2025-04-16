package com.chat.allchatonthis.config.web.resolver;

import com.chat.allchatonthis.common.util.security.LoginUser;
import com.chat.allchatonthis.common.util.security.SecurityUtils;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Handler for @LoginUser annotation
 * This class resolves the @LoginUser annotation to automatically inject the current user's ID
 * into controller method parameters.
 * 
 * HandlerMethodArgumentResolver is a Spring interface that allows customizing how controller
 * method parameters are resolved. This implementation specifically handles parameters annotated
 * with @LoginUser by providing the authenticated user's ID.
 */
@Component // Registers this class as a Spring bean to be automatically detected and injected
public class LoginUserMethodArgumentResolver implements HandlerMethodArgumentResolver {

    /**
     * Determines if this resolver can handle a specific parameter.
     * 
     * @param parameter The method parameter to check
     * @return true if the parameter has @LoginUser annotation and is of type Long
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUser.class) && 
               parameter.getParameterType().equals(Long.class);
    }

    /**
     * Resolves the parameter value when a supported parameter is encountered.
     * For @LoginUser annotation, this returns the ID of the currently authenticated user.
     * 
     * @param parameter The method parameter to resolve
     * @param mavContainer The ModelAndViewContainer for the current request
     * @param webRequest The current request
     * @param binderFactory The factory to create WebDataBinder instances
     * @return The current user's ID retrieved from SecurityUtils
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                 NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        return SecurityUtils.getLoginUserId();
    }
}