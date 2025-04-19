package com.chat.allchatonthis.service.es;

import com.chat.allchatonthis.common.pojo.PageResult;
import com.chat.allchatonthis.entity.dataobject.ConversationDO;
import com.chat.allchatonthis.entity.dataobject.ConversationMessageDO;
import com.chat.allchatonthis.entity.vo.conversation.ConversationPageReqVO;
import com.chat.allchatonthis.es.document.ConversationDocument;

import java.util.concurrent.CompletableFuture;

/**
 * Elasticsearch service for conversation operations
 */
public interface ESConversationService {

    /**
     * Save conversation to Elasticsearch
     *
     * @param conversation The conversation to save
     * @return The saved conversation document
     */
    ConversationDocument saveConversation(ConversationDO conversation);

    /**
     * Save conversation message to Elasticsearch
     *
     * @param conversationMessage The message to save
     * @param userId The user ID
     */
    void saveConversationMessage(ConversationMessageDO conversationMessage, Long userId);

    /**
     * Update conversation in Elasticsearch
     *
     * @param conversation The conversation to update
     * @return The updated conversation document
     */
    CompletableFuture<ConversationDocument> updateConversation(ConversationDO conversation);

    /**
     * Delete conversation from Elasticsearch
     *
     * @param id The conversation ID
     * @param userId The user ID
     */
    void deleteConversation(Long id, Long userId);

    /**
     * Delete conversation message from Elasticsearch
     *
     * @param id The message ID
     */
    void deleteConversationMessage(Long id);

    /**
     * Delete all messages in a conversation from Elasticsearch
     *
     * @param conversationId The conversation ID
     * @param userId The user ID
     */
    void deleteConversationMessages(Long conversationId, Long userId);

    /**
     * Search for conversations
     *
     * @param userId The user ID
     * @param reqVO Page request with search parameters
     * @return Paginated result of conversations
     */
    PageResult<ConversationDocument> searchConversations(Long userId, ConversationPageReqVO reqVO);
} 