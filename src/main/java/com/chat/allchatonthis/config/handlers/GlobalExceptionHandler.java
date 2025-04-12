package com.chat.allchatonthis.config.handlers;

import com.chat.allchatonthis.common.exception.ServiceException;
import com.chat.allchatonthis.common.pojo.CommonResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.chat.allchatonthis.common.enums.ErrorCodeConstants.*;

/**
 * Global exception handler
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle service exceptions
     */
    @ExceptionHandler(ServiceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Object> handleServiceException(ServiceException e) {
        log.error("Service exception: {}", e.getMessage(), e);
        return CommonResult.error(e.getCode(), e.getMessage());
    }

    /**
     * Handle authentication exceptions
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public CommonResult<Object> handleBadCredentialsException(BadCredentialsException e) {
        log.error("Bad credentials: {}", e.getMessage(), e);
        return CommonResult.error(AUTH_LOGIN_BAD_CREDENTIALS, "Bad credentials");
    }

    /**
     * Handle disabled user exceptions
     */
    @ExceptionHandler(DisabledException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public CommonResult<Object> handleDisabledException(DisabledException e) {
        log.error("User disabled: {}", e.getMessage(), e);
        return CommonResult.error(AUTH_LOGIN_USER_DISABLED, "User is disabled");
    }

    /**
     * Handle username not found exceptions
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CommonResult<Object> handleUsernameNotFoundException(UsernameNotFoundException e) {
        log.error("User not found: {}", e.getMessage(), e);
        return CommonResult.error(AUTH_LOGIN_FAILED, e.getMessage());
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResult<Object> handleException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return CommonResult.error(500, "Internal server error");
    }
}