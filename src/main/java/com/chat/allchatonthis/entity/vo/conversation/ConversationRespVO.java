package com.chat.allchatonthis.entity.vo.conversation;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Conversation Response VO
 */
@Data
public class ConversationRespVO {

    /**
     * Conversation ID
     */
    private Long id;

    /**
     * User ID
     */
    private Long userId;

    /**
     * Conversation title
     */
    private String title;

    /**
     * Creation time
     */
    private LocalDateTime createTime;

    /**
     * Update time
     */
    private LocalDateTime updateTime;
} 