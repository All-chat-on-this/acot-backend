package com.chat.allchatonthis.entity.vo.conversation;

import lombok.Data;

/**
 * Request VO for sending a message and getting an AI response
 */
@Data
public class ConversationSendMessageReqVO {

    /**
     * Conversation ID
     */
    private Long conversationId;

    /**
     * Configuration ID to use
     */
    private Long configId;

    /**
     * Message content
     */
    private String message;

    /**
     * Secret key for decrypting the api key
     */
    private String secretKey;
} 