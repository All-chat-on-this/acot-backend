package com.chat.allchatonthis.entity.dataobject;

import com.chat.allchatonthis.config.mybatis.core.dataobject.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class ConversationMessageDO extends BaseDO {
    private Long id;
    private Long conversationId; // Foreign key to ConversationDO
    private String role;
    private String content;
    private String thinkingText;
}