package com.chat.allchatonthis.entity.vo.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for configuration testing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigTestVO {
    
    private boolean success;
    private String message;
    
    // Response data when successful
    private ConfigTestResponseData response;
    
    // Error details when failed
    private String error;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigTestResponseData {
        private String role;
        private String content;
        private String thinking;
    }
} 