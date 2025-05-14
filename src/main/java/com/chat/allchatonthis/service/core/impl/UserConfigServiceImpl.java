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
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
        config.setLastUsedTime(LocalDateTime.now());

        // Only set false if isAvailable is null - allows frontend to set a successful state
        if (config.getIsAvailable() == null) {
            config.setIsAvailable(false); // Set default value as false until tested
        }

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
        config.setLastUsedTime(LocalDateTime.now());

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
    public ConfigTestVO testConfig(UserConfigDO config, Long userId,String secretKey) {
        try {
            // set the secret key in config
            if (StringUtils.hasText(secretKey)) {
                config.setSecretKey(secretKey);
            }

            // Use the common method to prepare request data
            Map<String, Object> requestData = HttpUtils.prepareRequestData(config, "Hello, nice to meet you.");
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

            log.info("API response: {}", response);

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
                    updateConfig.setLastUsedTime(LocalDateTime.now());
                    updateById(updateConfig);

                    // Evict related cache entries
                    if (userId != null) {
                        this.evictConfigCache(config.getId(), userId);
                    }

                    // Also update the current object
                    config.setIsAvailable(true);
                    config.setLastUsedTime(LocalDateTime.now());
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
                    setAvailableAndUpdateLastUsedTime(config.getId(), false);

                    // Also update the current object
                    config.setIsAvailable(false);
                    config.setLastUsedTime(LocalDateTime.now());
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
                setAvailableAndUpdateLastUsedTime(config.getId(), false);

                // Also update the current object
                config.setIsAvailable(false);
                config.setLastUsedTime(LocalDateTime.now());
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
    public void setAvailableAndUpdateLastUsedTime(Long configId, boolean available) {
        UserConfigDO updateConfig = new UserConfigDO();
        updateConfig.setId(configId);
        updateConfig.setIsAvailable(available);
        updateConfig.setLastUsedTime(LocalDateTime.now());
        updateById(updateConfig);
    }

    /**
     * Helper method to evict cache entries related to a specific config
     */
    private void evictConfigCache(Long configId, Long userId) {
        // This is a manual cache eviction, spring will handle the actual eviction
        log.debug("Manually evicting cache for config id: {} and user id: {}", configId, userId);
    }

    @Override
    @CacheEvict(key = "'list:' + #userId")
    public UserConfigDO createDefaultConfig(Long userId) {
        UserConfigDO config = new UserConfigDO();

        // Set basic properties
        config.setName("Default Configuration");
        config.setApiUrl("https://api.siliconflow.cn/v1/chat/completions");
        config.setApiKey(""); // Empty by default, user needs to provide their own API key
        config.setApiKeyPlacement("header");
        config.setIsAvailable(false); // Set to false until tested with a valid API key

        // Set request template
        Map<String, Object> requestTemplate = new HashMap<>();
        requestTemplate.put("n", 1);
        requestTemplate.put("stop", null);
        requestTemplate.put("model", "Qwen/QwQ-32B");

        Map<String, Object> function = new HashMap<>();
        function.put("name", "");
        function.put("strict", false);
        function.put("parameters", new HashMap<>());
        function.put("description", "");

        Map<String, Object> tool = new HashMap<>();
        tool.put("type", "function");
        tool.put("function", function);

        requestTemplate.put("tools", List.of(tool));
        requestTemplate.put("top_k", 50);
        requestTemplate.put("top_p", 0.7);
        requestTemplate.put("stream", false);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", "Hello, nice to meet you.");

        requestTemplate.put("messages", List.of(message));
        requestTemplate.put("max_tokens", 512);
        requestTemplate.put("temperature", 0.7);

        Map<String, Object> responseFormat = new HashMap<>();
        responseFormat.put("type", "text");
        requestTemplate.put("response_format", responseFormat);
        requestTemplate.put("frequency_penalty", 0.5);

        config.setRequestTemplate(requestTemplate);

        // Set response template
        Map<String, Object> responseTemplate = new HashMap<>();
        responseTemplate.put("id", "1234567890");
        responseTemplate.put("model", "Qwen/QwQ-32B");

        Map<String, Object> usage = new HashMap<>();
        usage.put("total_tokens", 915);
        usage.put("prompt_tokens", 141);
        usage.put("completion_tokens", 774);

        Map<String, Object> completionTokensDetails = new HashMap<>();
        completionTokensDetails.put("reasoning_tokens", 681);
        usage.put("completion_tokens_details", completionTokensDetails);

        responseTemplate.put("usage", usage);
        responseTemplate.put("object", "chat.completion");

        Map<String, Object> choices = new HashMap<>();
        choices.put("index", 0);

        Map<String, Object> choiceMessage = new HashMap<>();
        choiceMessage.put("role", "assistant");
        choiceMessage.put("content", "");
        choiceMessage.put("reasoning_content", "Hello, how can I help you?");

        List<Map<String, Object>> toolCalls = new ArrayList<>();

        // First tool call
        Map<String, Object> toolCall1 = new HashMap<>();
        toolCall1.put("id", "1234567890");
        toolCall1.put("type", "function");

        Map<String, Object> function1 = new HashMap<>();
        function1.put("name", "get_market_growth");

        Map<String, Object> arguments1 = new HashMap<>();
        arguments1.put("year", 2025);
        arguments1.put("country", "China");

        function1.put("arguments", arguments1);
        toolCall1.put("function", function1);
        toolCalls.add(toolCall1);

        // Second tool call
        Map<String, Object> toolCall2 = new HashMap<>();
        toolCall2.put("id", "1234567890");
        toolCall2.put("type", "function");

        Map<String, Object> function2 = new HashMap<>();
        function2.put("name", "get_policy_updates");

        Map<String, Object> arguments2 = new HashMap<>();
        arguments2.put("year", 2025);
        arguments2.put("country", "China");

        function2.put("arguments", arguments2);
        toolCall2.put("function", function2);
        toolCalls.add(toolCall2);

        // Third tool call
        Map<String, Object> toolCall3 = new HashMap<>();
        toolCall3.put("id", "1234567890");
        toolCall3.put("type", "function");

        Map<String, Object> function3 = new HashMap<>();
        function3.put("name", "get_technical_challenges");

        Map<String, Object> arguments3 = new HashMap<>();
        arguments3.put("year", 2025);
        arguments3.put("country", "China");

        function3.put("arguments", arguments3);
        toolCall3.put("function", function3);
        toolCalls.add(toolCall3);

        choiceMessage.put("tool_calls", toolCalls);
        choices.put("message", choiceMessage);
        choices.put("finish_reason", "tool_calls");

        responseTemplate.put("choices", List.of(choices));
        responseTemplate.put("created", System.currentTimeMillis());
        responseTemplate.put("system_fingerprint", "");

        config.setResponseTemplate(responseTemplate);

        // Set headers
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        config.setHeaders(headers);

        // Set message paths
        config.setRequestMessageGroupPath("messages");
        config.setRequestRolePathFromGroup("role");
        config.setRequestTextPathFromGroup("content");
        config.setResponseTextPath("choices[0].message.content");
        config.setResponseThinkingTextPath("choices[0].message.reasoning_content");

        // Set role fields
        config.setRequestUserRoleField("user");
        config.setRequestAssistantField("assistant");
        config.setRequestSystemField("system");

        // Save the configuration
        return createConfig(config, userId);
    }
}
    