package com.chat.allchatonthis.service.core;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.allchatonthis.common.exception.ServiceException;
import com.chat.allchatonthis.common.util.json.JsonUtils;
import com.chat.allchatonthis.entity.vo.config.ConfigTestVO;
import com.chat.allchatonthis.entity.vo.config.ModelStatusVO;
import com.chat.allchatonthis.entity.dataobject.UserConfigDO;
import com.chat.allchatonthis.mapper.UserConfigMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class UserConfigServiceImpl extends ServiceImpl<UserConfigMapper, UserConfigDO> implements UserConfigService {

    private final RestTemplate restTemplate;

    @Override
    public List<UserConfigDO> getConfigs(Long userId) {
        return list(new LambdaQueryWrapper<UserConfigDO>()
                .eq(UserConfigDO::getUserId, userId)
                .orderByDesc(UserConfigDO::getUpdateTime));
    }

    @Override
    public UserConfigDO getConfig(Long id, Long userId) {
        return getOne(new LambdaQueryWrapper<UserConfigDO>()
                .eq(UserConfigDO::getId, id)
                .eq(UserConfigDO::getUserId, userId));
    }

    @Override
    public UserConfigDO createConfig(UserConfigDO config, Long userId) {
        config.setUserId(userId);
        
        // Ensure headers exists
        if (config.getHeaders() == null) {
            config.setHeaders(new HashMap<>());
        }
        
        // Ensure "Content-Type: application/json" is present in headers
        if (!config.getHeaders().containsKey("Content-Type")) {
            config.getHeaders().put("Content-Type", "application/json");
        }
        
        save(config);
        return config;
    }

    @Override
    public UserConfigDO updateConfig(Long id, UserConfigDO config, Long userId) {
        // Check if config exists and belongs to user
        UserConfigDO existingConfig = getConfig(id, userId);
        if (existingConfig == null) {
            throw new ServiceException("Configuration not found or access denied");
        }
        
        // Update fields
        config.setId(id);
        config.setUserId(userId);
        
        // Ensure "Content-Type: application/json" is present in headers
        if (config.getHeaders() == null) {
            config.setHeaders(new HashMap<>());
        }
        if (!config.getHeaders().containsKey("Content-Type")) {
            config.getHeaders().put("Content-Type", "application/json");
        }
        
        updateById(config);
        return config;
    }

    @Override
    public boolean deleteConfig(Long id, Long userId) {
        return remove(new LambdaQueryWrapper<UserConfigDO>()
                .eq(UserConfigDO::getId, id)
                .eq(UserConfigDO::getUserId, userId));
    }

    @Override
    public ConfigTestVO testConfig(UserConfigDO config, Long userId) {
        try {
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            
            // Add API key to appropriate location
            if ("header".equals(config.getApiKeyPlacement()) || config.getApiKeyPlacement() == null) {
                // Default Authorization header
                headers.set("Authorization", "Bearer " + config.getApiKey());
            } else if ("custom_header".equals(config.getApiKeyPlacement()) && config.getApiKeyHeader() != null) {
                // Custom header
                headers.set(config.getApiKeyHeader(), config.getApiKey());
            }
            
            // Add custom headers
            if (config.getHeaders() != null) {
                config.getHeaders().forEach(headers::set);
            }
            
            // Prepare request body
            Map<String, Object> requestBody = new HashMap<>(config.getRequestTemplate());
            
            // Add API key to request body if needed
            if ("body".equals(config.getApiKeyPlacement()) && config.getApiKeyBodyPath() != null) {
                requestBody.put(config.getApiKeyBodyPath(), config.getApiKey());
            }
            
            // Make request
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    config.getApiUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Parse response
                Map<String, Object> responseMap = JsonUtils.parseObject(response.getBody(), Map.class);
                
                // Extract data using the response template paths
                String roleField = config.getResponseTemplate().get("roleField").toString();
                String contentField = config.getResponseTemplate().get("contentField").toString();
                Object thinkingTextField = config.getResponseTemplate().get("thinkingTextField");
                
                String role = extractValueFromPath(responseMap, roleField);
                String content = extractValueFromPath(responseMap, contentField);
                String thinking = thinkingTextField != null ? 
                        extractValueFromPath(responseMap, thinkingTextField.toString()) : null;
                
                // Create success response
                return ConfigTestVO.builder()
                        .success(true)
                        .message("Connection successful")
                        .response(ConfigTestVO.ConfigTestResponseData.builder()
                                .role(role)
                                .content(content)
                                .thinking(thinking)
                                .build())
                        .build();
            } else {
                return ConfigTestVO.builder()
                        .success(false)
                        .message("API returned error status: " + response.getStatusCode())
                        .error(response.getBody())
                        .build();
            }
        } catch (Exception e) {
            log.error("Error testing configuration", e);
            return ConfigTestVO.builder()
                    .success(false)
                    .message("Error connecting to API")
                    .error(e.getMessage())
                    .build();
        }
    }

    @Override
    public ModelStatusVO getModelStatus(Long configId, Long userId) {
        // Check if config exists and belongs to user
        UserConfigDO config = getConfig(configId, userId);
        if (config == null) {
            return ModelStatusVO.builder()
                    .available(false)
                    .error("Configuration not found or access denied")
                    .build();
        }
        
        try {
            // Look for model information in the request template
            String modelId = null;
            if (config.getRequestTemplate().containsKey("model")) {
                modelId = config.getRequestTemplate().get("model").toString();
            }
            
            List<ModelStatusVO.ModelInfo> models = new ArrayList<>();
            
            // If we found a model, add it to the list
            if (modelId != null) {
                models.add(ModelStatusVO.ModelInfo.builder()
                        .id(modelId)
                        .name(modelId)
                        .description("Found in configuration")
                        .maxTokens(4096) // Default value
                        .available(true)
                        .build());
            }
            
            return ModelStatusVO.builder()
                    .available(true)
                    .models(models)
                    .build();
        } catch (Exception e) {
            log.error("Error getting model status", e);
            return ModelStatusVO.builder()
                    .available(false)
                    .error(e.getMessage())
                    .build();
        }
    }
    
    /**
     * Extract a value from a nested JSON structure using dot notation path
     * 
     * @param data The data structure to extract from
     * @param path Path in dot notation (e.g., "choices[0].message.content")
     * @return The extracted value as a string or null if not found
     */
    @SuppressWarnings("unchecked")
    private String extractValueFromPath(Map<String, Object> data, String path) {
        String[] parts = path.split("\\.");
        Object current = data;
        
        for (String part : parts) {
            if (current == null) {
                return null;
            }
            
            // Handle array notation like choices[0]
            if (part.contains("[") && part.contains("]")) {
                String arrayName = part.substring(0, part.indexOf('['));
                int index = Integer.parseInt(part.substring(part.indexOf('[') + 1, part.indexOf(']')));
                
                if (current instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) current;
                    if (map.containsKey(arrayName) && map.get(arrayName) instanceof List) {
                        List<Object> array = (List<Object>) map.get(arrayName);
                        if (index < array.size()) {
                            current = array.get(index);
                        } else {
                            return null;
                        }
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                // Regular property access
                if (current instanceof Map) {
                    current = ((Map<String, Object>) current).get(part);
                } else {
                    return null;
                }
            }
        }
        
        return current != null ? current.toString() : null;
    }
} 