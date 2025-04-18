package com.chat.allchatonthis.entity.vo.conversation;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Conversation Message Response VO
 */
@Data
public class ConversationMessageRespVO {

    /**
     * Message ID
     */
    private Long id;

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

    /**
     * Creation time
     */
    private LocalDateTime createTime;

    /**
     * Update time
     */
    private LocalDateTime updateTime;
} 