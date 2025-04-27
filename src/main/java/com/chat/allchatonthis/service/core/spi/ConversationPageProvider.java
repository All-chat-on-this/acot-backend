package com.chat.allchatonthis.service.core.spi;

import com.chat.allchatonthis.common.pojo.PageResult;
import com.chat.allchatonthis.entity.dataobject.ConversationDO;
import com.chat.allchatonthis.entity.vo.conversation.ConversationPageReqVO;
import com.chat.allchatonthis.mapper.ConversationMapper;

/**
 * SPI interface for conversation page retrieval
 */
public interface ConversationPageProvider {

    /**
     * Set the conversation mapper
     *
     * @param conversationMapper The conversation mapper
     */
    void setConversationMapper(ConversationMapper conversationMapper);

    /**
     * Get a page of conversations for a user
     *
     * @param userId The user ID
     * @param conversationPageReqVO The pagination and search parameters
     * @return A page of conversations
     */
    PageResult<ConversationDO> getConversationPage(Long userId, ConversationPageReqVO conversationPageReqVO);

    /**
     * Get the order of this provider. Higher values indicate higher priority.
     *
     * @return The order value
     */
    int getOrder();
} 