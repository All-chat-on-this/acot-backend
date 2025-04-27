package com.chat.allchatonthis.service.core.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.allchatonthis.common.exception.ServiceException;
import com.chat.allchatonthis.common.util.http.HttpUtils;
import com.chat.allchatonthis.common.util.json.JsonUtils;
import com.chat.allchatonthis.entity.dataobject.ConversationDO;
import com.chat.allchatonthis.entity.dataobject.ConversationMessageDO;
import com.chat.allchatonthis.entity.dataobject.UserConfigDO;
import com.chat.allchatonthis.mapper.ConversationMessageMapper;
import com.chat.allchatonthis.service.core.ConversationMessageService;
import com.chat.allchatonthis.service.core.ConversationService;
import com.chat.allchatonthis.service.core.UserConfigService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

import static com.chat.allchatonthis.common.enums.ErrorCodeConstants.*;

@Service
@AllArgsConstructor
@Slf4j
@CacheConfig(cacheNames = "conversation_message")
public class ConversationMessageServiceImpl extends ServiceImpl<ConversationMessageMapper, ConversationMessageDO> implements ConversationMessageService {

    private final ConversationService conversationService;
    private final UserConfigService userConfigService;

    @Override
    @Cacheable(key = "'conversation:' + #conversationId + ':user:' + #userId")
    public List<ConversationMessageDO> getMessages(Long conversationId, Long userId) {
        // Validate that the conversation belongs to the user
        ConversationDO conversation = conversationService.getConversation(conversationId, userId);
        if (conversation == null) {
            throw new ServiceException(CONVERSATION_NOT_EXISTS.getCode(), CONVERSATION_NOT_EXISTS.getMsg());
        }

        return list(new LambdaQueryWrapper<ConversationMessageDO>()
                .eq(ConversationMessageDO::getConversationId, conversationId)
                .orderByAsc(ConversationMessageDO::getCreateTime));
    }

    @Override
    @Cacheable(key = "'id:' + #id + ':user:' + #userId")
    public ConversationMessageDO getMessage(Long id, Long userId) {
        ConversationMessageDO message = getById(id);
        if (message == null) {
            return null;
        }

        // Validate that the conversation belongs to the user
        ConversationDO conversation = conversationService.getConversation(message.getConversationId(), userId);
        if (conversation == null) {
            return null;
        }

        return message;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(key = "'conversation:' + #conversationId + ':user:' + #userId")
    })
    public ConversationMessageDO sendMessage(String userMessage, Long configId, Long conversationId, Long userId) {
        // Validate that the conversation belongs to the user
        ConversationDO conversation = conversationService.getConversation(conversationId, userId);
        if (conversation == null) {
            throw new ServiceException(CONVERSATION_NOT_EXISTS.getCode(), CONVERSATION_NOT_EXISTS.getMsg());
        }

        // Get the configuration
        UserConfigDO config = userConfigService.getConfig(configId, userId);
        if (config == null) {
            throw new ServiceException(CONFIGURATION_NOT_EXISTS.getCode(), CONFIGURATION_NOT_EXISTS.getMsg());
        }

        // Create user message
        ConversationMessageDO userMessageDO = new ConversationMessageDO()
                .setConversationId(conversationId)
                .setConfigId(configId)
                .setRole("user")
                .setContent(userMessage);
        save(userMessageDO);

        try {
            // Get previous messages in the conversation for history
            List<ConversationMessageDO> previousMessages = list(new LambdaQueryWrapper<ConversationMessageDO>()
                    .eq(ConversationMessageDO::getConversationId, conversationId)
                    .orderByAsc(ConversationMessageDO::getCreateTime));

            // Generate the assistant response
            ConversationMessageDO assistantMessageDO = generateAssistantResponse(userMessage, config, conversationId, configId, previousMessages);

            // Update the conversation's update time
            conversation.setUpdateTime(assistantMessageDO.getUpdateTime());
            conversationService.updateById(conversation);

            // Mark the configuration as available since it was successfully used
            markConfigurationAsAvailable(configId, userId);

            return assistantMessageDO;
        } catch (Exception e) {
            log.error("Error sending message", e);

            // Create an error message
            ConversationMessageDO errorMessageDO = new ConversationMessageDO()
                    .setConversationId(conversationId)
                    .setRole("system")
                    .setContent("Error: " + e.getMessage());
            save(errorMessageDO);

            if (e instanceof ServiceException) {
                throw e;
            }

            throw new ServiceException(MESSAGE_SEND_FAILED.getCode(), e.getMessage());
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(key = "'id:' + #id + ':user:' + #userId"),
        @CacheEvict(key = "'conversation:' + #conversationId + ':user:' + #userId", condition = "#result == true")
    })
    public boolean deleteMessage(Long id, Long userId) {
        ConversationMessageDO message = getMessage(id, userId);
        if (message == null) {
            return false;
        }
        
        Long conversationId = message.getConversationId();
        return removeById(id);
    }

    @Override
    @Transactional
    @CacheEvict(key = "'conversation:' + #conversationId + ':user:' + #userId")
    public boolean deleteMessagesByConversationId(Long conversationId, Long userId) {
        // Validate that the conversation belongs to the user
        ConversationDO conversation = conversationService.getConversation(conversationId, userId);
        if (conversation == null) {
            return false;
        }

        return remove(new LambdaQueryWrapper<ConversationMessageDO>()
                .eq(ConversationMessageDO::getConversationId, conversationId));
    }

    @Override
    public boolean markConfigurationAsAvailable(Long configId, Long userId) {
        UserConfigDO config = userConfigService.getConfig(configId, userId);
        if (config == null) {
            return false;
        }

        // Update isAvailable to true
        userConfigService.setAvailable(configId, true);

        return true;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(key = "'id:' + #id + ':user:' + #userId"),
        @CacheEvict(key = "'conversation:' + #conversationId + ':user:' + #userId", condition = "#result != null")
    })
    public ConversationMessageDO renameMessage(Long id, String content, Long userId) {
        // Get the message and verify ownership
        ConversationMessageDO message = getMessage(id, userId);
        if (message == null) {
            throw new ServiceException(MESSAGE_NOT_EXISTS.getCode(), MESSAGE_NOT_EXISTS.getMsg());
        }

        Long conversationId = message.getConversationId();

        // Update the message content
        message.setContent(content);
        updateById(message);

        // If it's a user message, delete subsequent messages and generate a new response
        if ("user".equals(message.getRole())) {
            // Delete all messages that came after this one
            List<ConversationMessageDO> messages = getMessages(conversationId, userId);
            boolean foundTargetMessage = false;
            for (ConversationMessageDO msg : messages) {
                if (foundTargetMessage) {
                    deleteMessage(msg.getId(), userId);
                }
                if (msg.getId().equals(id)) {
                    foundTargetMessage = true;
                }
            }

            // Now generate a new response using the same logic as sendMessage
            Long configId = message.getConfigId();
            
            // Validate the config exists
            UserConfigDO config = userConfigService.getConfig(configId, userId);
            if (config == null) {
                throw new ServiceException(CONFIGURATION_NOT_EXISTS.getCode(), CONFIGURATION_NOT_EXISTS.getMsg());
            }

            try {
                // Get updated messages in the conversation for history (after deletions)
                List<ConversationMessageDO> updatedMessages = getMessages(conversationId, userId);

                // Generate the assistant response
                ConversationMessageDO assistantMessageDO = generateAssistantResponse(content, config, conversationId, configId, updatedMessages);

                // Update the conversation's update time
                ConversationDO conversation = conversationService.getById(conversationId);
                conversation.setUpdateTime(assistantMessageDO.getUpdateTime());
                conversationService.updateById(conversation);

                // Mark the configuration as available since it was successfully used
                markConfigurationAsAvailable(configId, userId);

                return assistantMessageDO;
            } catch (Exception e) {
                log.error("Error generating response after renaming message", e);

                // Create an error message
                ConversationMessageDO errorMessageDO = new ConversationMessageDO()
                        .setConversationId(conversationId)
                        .setRole("system")
                        .setContent("Error: " + e.getMessage());
                save(errorMessageDO);

                if (e instanceof ServiceException) {
                    throw e;
                }

                throw new ServiceException(MESSAGE_SEND_FAILED.getCode(), e.getMessage());
            }
        }

        // For non-user messages, just return the updated message
        return message;
    }
    
    /**
     * Generates an assistant response based on the user message and conversation history
     *
     * @param userMessage The message from the user
     * @param config The API configuration to use
     * @param conversationId The conversation ID
     * @param configId The configuration ID
     * @param conversationMessages The messages in the conversation for context
     * @return The generated assistant message
     * @throws ServiceException If the API call fails
     */
    private ConversationMessageDO generateAssistantResponse(
            String userMessage, 
            UserConfigDO config, 
            Long conversationId, 
            Long configId,
            List<ConversationMessageDO> conversationMessages) {
            
        // Use the common method to prepare request data with conversation history
        Map<String, Object> requestData = HttpUtils.prepareRequestData(config, userMessage, conversationMessages);
        Map<String, String> headers = (Map<String, String>) requestData.get("headers");
        Map<String, Object> requestBody = (Map<String, Object>) requestData.get("requestBody");

        // Make the actual HTTP request
        String requestBodyStr = JsonUtils.toJsonString(requestBody);
        String responseStr = HttpUtils.post(config.getApiUrl(), headers, requestBodyStr);

        if (responseStr == null) {
            throw new ServiceException(API_CALL_FAILED.getCode(), "API returned null response");
        }

        // Parse the response
        Map<String, Object> responseMap = JsonUtils.parseObject(responseStr, Map.class);

        // Extract content and thinking text from the response using the specified paths
        String content = null;
        String thinking = null;

        if (StringUtils.hasText(config.getResponseTextPath())) {
            content = JsonUtils.extractValueFromPath(responseMap, config.getResponseTextPath());
        }

        if (StringUtils.hasText(config.getResponseThinkingTextPath())) {
            thinking = JsonUtils.extractValueFromPath(responseMap, config.getResponseThinkingTextPath());
        }

        if (content == null) {
            throw new ServiceException(API_CALL_FAILED.getCode(), "Could not extract response content");
        }

        // Create assistant message with the response
        ConversationMessageDO assistantMessageDO = new ConversationMessageDO()
                .setConversationId(conversationId)
                .setConfigId(configId)
                .setRole("assistant")
                .setContent(content)
                .setThinkingText(thinking);
        save(assistantMessageDO);
        
        return assistantMessageDO;
    }
} 