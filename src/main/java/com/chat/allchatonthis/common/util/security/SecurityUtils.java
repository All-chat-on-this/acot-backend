package com.chat.allchatonthis.common.util.security;

import com.chat.allchatonthis.config.security.model.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Security utilities for accessing the current user
 * <p>
 * This class implements the following security principles:
 * 1. Centralized Authentication Access - Provides a single point to retrieve user info
 * 2. Thread-Local Security Context - Uses Spring's SecurityContextHolder
 * 3. Null Safety - Implements proper null checks for unauthenticated scenarios
 * 4. Type Safety - Uses instanceof before casting to prevent ClassCastExceptions
 */
public class SecurityUtils {

    /**
     * Get the currently logged-in user
     * <p>
     * Security Implementation:
     * - Accesses the thread-local SecurityContextHolder to get current Authentication
     * - Performs null-check on Authentication to handle unauthenticated states
     * - Uses type checking with instanceof for safe principal casting
     * - Returns null instead of throwing exceptions when user is not authenticated
     *
     * @return The LoginUser or null if not authenticated
     */
    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }

        if (authentication.getPrincipal() instanceof LoginUser) {
            return (LoginUser) authentication.getPrincipal();
        }

        return null;
    }

    /**
     * Get the current user ID from the security context
     * <p>
     * Design Pattern:
     * - Method composition - Delegates to getLoginUser() for consistent security validation
     * - Implements the Convenience Method pattern to provide direct access to a frequently needed property
     * - Avoids code duplication by reusing the authentication verification logic
     *
     * @return User ID or null if not authenticated
     */
    public static Long getLoginUserId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getUserId() : null;
    }

    /**
     * Get the current user's nickname
     * <p>
     * Security Considerations:
     * - Maintains the same authentication verification chain as other methods
     * - Provides limited data exposure (only nickname instead of full user object)
     * - Returns consistent null value when user is not authenticated
     *
     * @return Nickname or null if not authenticated
     */
    public static String getLoginUserNickname() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getNickname() : null;
    }
}