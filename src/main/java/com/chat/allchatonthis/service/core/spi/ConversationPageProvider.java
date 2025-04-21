package com.chat.allchatonthis.service.core.spi;

import com.chat.allchatonthis.common.pojo.PageResult;
import com.chat.allchatonthis.entity.dataobject.ConversationDO;
import com.chat.allchatonthis.entity.vo.conversation.ConversationPageReqVO;

/**
 * SPI interface for conversation page retrieval
 */
public interface ConversationPageProvider {

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