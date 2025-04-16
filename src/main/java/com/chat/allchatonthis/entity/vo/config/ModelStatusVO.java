package com.chat.allchatonthis.entity.vo.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Status of available models for a configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelStatusVO {
    
    private boolean available;
    private List<ModelInfo> models;
    private String error;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelInfo {
        private String id;
        private String name;
        private String description;
        private int maxTokens;
        private boolean available;
    }
} 