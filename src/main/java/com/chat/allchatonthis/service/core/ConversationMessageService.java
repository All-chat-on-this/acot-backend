package com.chat.allchatonthis.service.core;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chat.allchatonthis.entity.dataobject.ConversationMessageDO;

import java.util.List;

public interface ConversationMessageService extends IService<ConversationMessageDO> {

    /**
     * Get all messages for a specific conversation
     *
     * @param conversationId The conversation ID
     * @param userId         User ID for security check
     * @return List of conversation messages
     */
    List<ConversationMessageDO> getMessages(Long conversationId, Long userId);

    /**
     * Get a specific message by ID
     *
     * @param id     Message ID
     * @param userId User ID for security check
     * @return The message or null if not found
     */
    ConversationMessageDO getMessage(Long id, Long userId);

    /**
     * Create a new message
     *
     * @param message Message to create
     * @param userId  User ID for security check
     * @return The created message with ID
     */
    ConversationMessageDO createMessage(ConversationMessageDO message, Long userId);

    /**
     * Create a user message and generate an AI response
     *
     * @param userMessage    User's message
     * @param configId       ID of the configuration to use
     * @param conversationId Conversation ID
     * @param userId         User ID
     * @return The AI response message
     */
    ConversationMessageDO sendMessage(String userMessage, Long configId, Long conversationId, Long userId);

    /**
     * Delete a message
     *
     * @param id     Message ID to delete
     * @param userId User ID for security check
     * @return true if deleted successfully
     */
    boolean deleteMessage(Long id, Long userId);

    /**
     * Delete all messages for a conversation
     *
     * @param conversationId Conversation ID
     * @param userId         User ID for security check
     * @return true if deleted successfully
     */
    boolean deleteMessagesByConversationId(Long conversationId, Long userId);

    /**
     * Mark a configuration as available after successful use in conversation
     *
     * @param configId Configuration ID to mark as available
     * @param userId   User ID for security check
     * @return true if marked successfully
     */
    boolean markConfigurationAsAvailable(Long configId, Long userId);
} 