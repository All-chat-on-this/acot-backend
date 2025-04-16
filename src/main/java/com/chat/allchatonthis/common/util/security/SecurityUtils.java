package com.chat.allchatonthis.common.util.security;

import com.chat.allchatonthis.config.security.model.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Security utilities for accessing the current user
 */
public class SecurityUtils {

    /**
     * Get the currently logged in user
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
     * 
     * @return User ID or null if not authenticated
     */
    public static Long getLoginUserId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getUserId() : null;
    }
    
    /**
     * Get the current user's nickname
     * 
     * @return Nickname or null if not authenticated
     */
    public static String getLoginUserNickname() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getNickname() : null;
    }
} 