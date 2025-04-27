package com.chat.allchatonthis.entity.vo.conversation;

import com.baomidou.mybatisplus.annotation.TableId;
import com.chat.allchatonthis.entity.dataobject.UserConfigDO;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fhs.core.trans.anno.Trans;
import com.fhs.core.trans.constant.TransType;
import com.fhs.core.trans.vo.TransPojo;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Conversation Message Response VO
 */
@Data
public class ConversationMessageRespVO implements TransPojo {

    /**
     * Message ID
     */
    @TableId
    private Long id;

    /**
     * Conversation ID
     */
    private Long conversationId;

    /**
     * Config ID
     */
    @Trans(type = TransType.SIMPLE, target = UserConfigDO.class, fields = "name", ref = "configName")
    private Long configId;

    /**
     * Config name
     */
    private String configName;

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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * Update time
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
} 