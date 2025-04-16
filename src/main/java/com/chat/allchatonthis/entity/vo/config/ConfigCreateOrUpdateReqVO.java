package com.chat.allchatonthis.entity.vo.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * API Configuration Create/Update Request VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigCreateOrUpdateReqVO {
    
    private String name;
    private String apiUrl;
    private String apiKey;
    private String apiKeyPlacement;
    private String apiKeyHeader;
    private String apiKeyBodyPath;
    private Map<String, Object> requestTemplate;
    private Map<String, Object> responseTemplate;
    private Map<String, String> headers;
} 