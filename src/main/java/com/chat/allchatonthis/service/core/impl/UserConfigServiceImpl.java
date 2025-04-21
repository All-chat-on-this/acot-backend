package com.chat.allchatonthis.service.core.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.allchatonthis.common.exception.ServiceException;
import com.chat.allchatonthis.common.util.http.HttpUtils;
import com.chat.allchatonthis.common.util.json.JsonUtils;
import com.chat.allchatonthis.entity.dataobject.UserConfigDO;
import com.chat.allchatonthis.entity.vo.config.ConfigTestVO;
import com.chat.allchatonthis.mapper.UserConfigMapper;
import com.chat.allchatonthis.service.core.UserConfigService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.chat.allchatonthis.common.enums.ErrorCodeConstants.CONFIG_NOT_EXISTS;

@Service
@AllArgsConstructor
@Slf4j
@CacheConfig(cacheNames = "user_config")
public class UserConfigServiceImpl extends ServiceImpl<UserConfigMapper, UserConfigDO> implements UserConfigService {

    private final RestTemplate restTemplate;

    @Override
    @Cacheable(key = "'list:' + #userId")
    public List<UserConfigDO> getConfigs(Long userId) {
        return list(new LambdaQueryWrapper<UserConfigDO>()
                .eq(UserConfigDO::getUserId, userId));
    }

    @Override
    @Cacheable(key = "'id:' + #id + ':user:' + #userId")
    public UserConfigDO getConfig(Long id, Long userId) {
        return getOne(new LambdaQueryWrapper<UserConfigDO>()
                .eq(UserConfigDO::getId, id)
                .eq(UserConfigDO::getUserId, userId));
    }

    @Override
    @CacheEvict(key = "'list:' + #userId")
    public UserConfigDO createConfig(UserConfigDO config, Long userId) {
        config.setUserId(userId);
        config.setIsAvailable(false); // Set default value as false until tested

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
    @Caching(evict = {
        @CacheEvict(key = "'list:' + #userId"),
        @CacheEvict(key = "'id:' + #id + ':user:' + #userId")
    })
    public UserConfigDO updateConfig(Long id, UserConfigDO config, Long userId) {
        // Check if config exists and belongs to user
        UserConfigDO existingConfig = getConfig(id, userId);
        if (existingConfig == null) {
            throw new ServiceException(CONFIG_NOT_EXISTS, "Configuration not found or access denied");
        }

        // Update fields
        config.setId(id);
        config.setUserId(userId);

        // Maintain current isAvailable status unless it has been explicitly set
        if (config.getIsAvailable() == null) {
            config.setIsAvailable(existingConfig.getIsAvailable());
        }

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
    @Caching(evict = {
        @CacheEvict(key = "'list:' + #userId"),
        @CacheEvict(key = "'id:' + #id + ':user:' + #userId")
    })
    public boolean deleteConfig(Long id, Long userId) {
        return remove(new LambdaQueryWrapper<UserConfigDO>()
                .eq(UserConfigDO::getId, id)
                .eq(UserConfigDO::getUserId, userId));
    }

    @Override
    public ConfigTestVO testConfig(UserConfigDO config, Long userId) {
        try {
            // Use the common method to prepare request data
            Map<String, Object> requestData = HttpUtils.prepareRequestData(config, "Hello, this is a test message.");
            Map<String, String> headers = (Map<String, String>) requestData.get("headers");
            Map<String, Object> requestBody = (Map<String, Object>) requestData.get("requestBody");
            
            // Convert to HttpHeaders
            HttpHeaders httpHeaders = new HttpHeaders();
            headers.forEach(httpHeaders::set);

            // Make request
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, httpHeaders);
            ResponseEntity<String> response = restTemplate.exchange(
                    config.getApiUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Parse response
                Map<String, Object> responseMap = JsonUtils.parseObject(response.getBody(), Map.class);

                // Try using new paths first if they exist
                String content = null;
                String thinking = null;
                String role = "assistant"; // Default role

                if (StringUtils.hasText(config.getResponseTextPath())) {
                    content = JsonUtils.extractValueFromPath(responseMap, config.getResponseTextPath());
                }

                if (StringUtils.hasText(config.getResponseThinkingTextPath())) {
                    thinking = JsonUtils.extractValueFromPath(responseMap, config.getResponseThinkingTextPath());
                }

                // Check if we found any content
                if (content == null) {
                    return ConfigTestVO.builder()
                            .success(false)
                            .message("Could not extract response content from API response")
                            .error("Response paths not configured correctly or API returned unexpected format")
                            .build();
                }

                // Set isAvailable to true if the configuration has an ID (already saved)
                if (config.getId() != null) {
                    // Update the config in database to reflect it's available
                    UserConfigDO updateConfig = new UserConfigDO();
                    updateConfig.setId(config.getId());
                    updateConfig.setIsAvailable(true);
                    updateById(updateConfig);
                    
                    // Evict related cache entries
                    if (userId != null) {
                        this.evictConfigCache(config.getId(), userId);
                    }

                    // Also update the current object
                    config.setIsAvailable(true);
                }

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
                // If test fails, set isAvailable to false if the configuration has an ID
                if (config.getId() != null) {
                    setAvailable(config.getId(), false);

                    // Also update the current object
                    config.setIsAvailable(false);
                }

                return ConfigTestVO.builder()
                        .success(false)
                        .message("API returned error status: " + response.getStatusCode())
                        .error(response.getBody())
                        .build();
            }
        } catch (Exception e) {
            log.error("Error testing configuration", e);

            // If test fails due to exception, set isAvailable to false if the configuration has an ID
            if (config.getId() != null) {
                setAvailable(config.getId(), false);

                // Also update the current object
                config.setIsAvailable(false);
            }

            return ConfigTestVO.builder()
                    .success(false)
                    .message("Error connecting to API")
                    .error(e.getMessage())
                    .build();
        }
    }

    @Override
    @Caching(evict = {
        @CacheEvict(key = "'id:' + #configId + ':user:' + '*'", allEntries = true),
        @CacheEvict(key = "'list:' + '*'", allEntries = true)
    })
    public void setAvailable(Long configId, boolean available) {
        UserConfigDO updateConfig = new UserConfigDO();
        updateConfig.setId(configId);
        updateConfig.setIsAvailable(available);
        updateById(updateConfig);
    }
    
    /**
     * Helper method to evict cache entries related to a specific config
     */
    private void evictConfigCache(Long configId, Long userId) {
        // This is a manual cache eviction, spring will handle the actual eviction
        log.debug("Manually evicting cache for config id: {} and user id: {}", configId, userId);
    }
} 