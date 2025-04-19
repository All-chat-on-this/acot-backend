package com.chat.allchatonthis.es.mapper;

import com.chat.allchatonthis.entity.dataobject.ConversationDO;
import com.chat.allchatonthis.entity.dataobject.ConversationMessageDO;
import com.chat.allchatonthis.es.document.ConversationDocument;
import com.chat.allchatonthis.es.document.ConversationMessageDocument;

/**
 * Utility class for mapping between database entities and Elasticsearch documents
 */
public class ESEntityMapper {

    /**
     * Convert ConversationDO to ConversationDocument
     *
     * @param conversation The conversation entity from database
     * @return The corresponding Elasticsearch document
     */
    public static ConversationDocument toConversationDocument(ConversationDO conversation) {
        if (conversation == null) {
            return null;
        }

        return new ConversationDocument()
                .setId(conversation.getId())
                .setUserId(conversation.getUserId())
                .setTitle(conversation.getTitle())
                .setCreateTime(conversation.getCreateTime())
                .setUpdateTime(conversation.getUpdateTime())
                .setDeleted(conversation.getDeleted() != null && conversation.getDeleted());
    }

    /**
     * Convert ConversationMessageDO to ConversationMessageDocument
     *
     * @param message The message entity from database
     * @param userId  The user ID
     * @return The corresponding Elasticsearch document
     */
    public static ConversationMessageDocument toConversationMessageDocument(ConversationMessageDO message, Long userId) {
        if (message == null) {
            return null;
        }

        return new ConversationMessageDocument()
                .setId(message.getId())
                .setConversationId(message.getConversationId())
                .setUserId(userId)
                .setRole(message.getRole())
                .setContent(message.getContent())
                .setThinkingText(message.getThinkingText())
                .setCreateTime(message.getCreateTime())
                .setUpdateTime(message.getUpdateTime())
                .setDeleted(message.getDeleted() != null && message.getDeleted());
    }
} 