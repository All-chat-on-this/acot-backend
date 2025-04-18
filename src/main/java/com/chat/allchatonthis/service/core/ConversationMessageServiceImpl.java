package com.chat.allchatonthis.service.core;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.allchatonthis.common.exception.ServiceException;
import com.chat.allchatonthis.entity.dataobject.ConversationDO;
import com.chat.allchatonthis.entity.dataobject.ConversationMessageDO;
import com.chat.allchatonthis.entity.dataobject.UserConfigDO;
import com.chat.allchatonthis.entity.vo.config.ConfigTestVO;
import com.chat.allchatonthis.mapper.ConversationMessageMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.chat.allchatonthis.common.enums.ErrorCodeConstants.*;

@Service
@AllArgsConstructor
@Slf4j
public class ConversationMessageServiceImpl extends ServiceImpl<ConversationMessageMapper, ConversationMessageDO> implements ConversationMessageService {

    private final ConversationService conversationService;
    private final UserConfigService userConfigService;

    @Override
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
    public ConversationMessageDO createMessage(ConversationMessageDO message, Long userId) {
        // Validate that the conversation belongs to the user
        ConversationDO conversation = conversationService.getConversation(message.getConversationId(), userId);
        if (conversation == null) {
            throw new ServiceException(CONVERSATION_NOT_EXISTS.getCode(), CONVERSATION_NOT_EXISTS.getMsg());
        }

        save(message);

        // Update the conversation's update time
        conversationService.lambdaUpdate()
                .eq(ConversationDO::getId, message.getConversationId())
                .update(new ConversationDO().setId(message.getConversationId()));

        return message;
    }

    @Override
    @Transactional
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
                .setRole("user")
                .setContent(userMessage);
        save(userMessageDO);

        try {
            // Prepare a copy of the config for testing
            UserConfigDO requestConfig = new UserConfigDO();
            requestConfig.setId(config.getId());
            requestConfig.setApiUrl(config.getApiUrl());
            requestConfig.setApiKey(config.getApiKey());
            requestConfig.setApiKeyPlacement(config.getApiKeyPlacement());
            requestConfig.setApiKeyHeader(config.getApiKeyHeader());
            requestConfig.setApiKeyBodyPath(config.getApiKeyBodyPath());
            requestConfig.setHeaders(config.getHeaders());

            // Modify request template with the conversation context
            Map<String, Object> requestTemplate = new HashMap<>(config.getRequestTemplate());

            // Here we would typically add the message history and current message to the request
            // This is a simplified implementation - in a real app, you'd handle context window management

            requestConfig.setRequestTemplate(requestTemplate);
            requestConfig.setResponseTemplate(config.getResponseTemplate());

            // Make the API call
            ConfigTestVO response = userConfigService.testConfig(requestConfig, userId);

            if (!response.isSuccess()) {
                throw new ServiceException(API_CALL_FAILED.getCode(), response.getMessage());
            }

            // Create assistant message with the response
            ConversationMessageDO assistantMessageDO = new ConversationMessageDO()
                    .setConversationId(conversationId)
                    .setRole(response.getResponse().getRole() != null ? response.getResponse().getRole() : "assistant")
                    .setContent(response.getResponse().getContent())
                    .setThinkingText(response.getResponse().getThinking());
            save(assistantMessageDO);

            // Update the conversation's update time
            conversationService.lambdaUpdate()
                    .eq(ConversationDO::getId, conversationId)
                    .update(new ConversationDO().setId(conversationId));

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
    public boolean deleteMessage(Long id, Long userId) {
        ConversationMessageDO message = getMessage(id, userId);
        if (message == null) {
            return false;
        }

        return removeById(id);
    }

    @Override
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
} 