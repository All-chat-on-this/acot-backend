package com.chat.allchatonthis.entity.vo.conversation;

import lombok.Data;

/**
 * Conversation Message Rename Request VO
 */
@Data
public class ConversationMessageRenameReqVO {

    /**
     * Message ID to rename
     */
    private Long id;

    /**
     * New message content
     */
    private String content;

    /**
     * Secret key to decrypt the api key
     */
    private String secretKey;
} 