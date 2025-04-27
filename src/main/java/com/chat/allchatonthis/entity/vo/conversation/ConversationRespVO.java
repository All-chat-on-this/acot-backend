package com.chat.allchatonthis.entity.vo.conversation;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * Update time
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
} 