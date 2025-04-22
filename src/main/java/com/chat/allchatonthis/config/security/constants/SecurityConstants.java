package com.chat.allchatonthis.config.security.constants;

import java.util.Arrays;
import java.util.List;

/**
 * Security related constants 
 */
public class SecurityConstants {
    /**
     * List of path patterns that should be publicly accessible without authentication
     */
    public static final List<String> PUBLIC_PATH_PATTERNS = Arrays.asList(
            "/api/auth/**",
            "/api/social/**",
            "/api/public/**"
    );
} 