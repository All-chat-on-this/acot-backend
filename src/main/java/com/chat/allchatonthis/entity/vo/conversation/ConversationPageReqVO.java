package com.chat.allchatonthis.entity.vo.conversation;

import com.chat.allchatonthis.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "对话分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ConversationPageReqVO extends PageParam {

    @Schema(description = "对话标题，模糊匹配", example = "问题")
    private String title;
} 