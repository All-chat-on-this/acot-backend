package com.chat.allchatonthis.entity.vo.conversation;

import lombok.Data;

/**
 * Conversation Message Create Request VO
 */
@Data
public class ConversationMessageCreateReqVO {

    /**
     * Conversation ID
     */
    private Long conversationId;

    /**
     * Message role (system, assistant, user)
     */
    private String role;

    /**
     * Message content
     */
    private String content;

    /**
     * Thinking text (Chain of Thought) if available
     */
    private String thinkingText;
} 