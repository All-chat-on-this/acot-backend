package com.chat.allchatonthis.entity.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("conversation_message")
public class ConversationMessageDO extends BaseDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private Long configId;
    private String role;
    private String content;
    private String thinkingText;
}