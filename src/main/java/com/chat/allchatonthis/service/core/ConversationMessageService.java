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
     * Rename a message and generate a new AI response if it's a user message
     *
     * @param id      Message ID to rename
     * @param content New content for the message
     * @param userId  User ID for security check
     * @return The AI response message if a user message was renamed, or the updated message
     */
    ConversationMessageDO renameMessage(Long id, String content, Long userId);

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