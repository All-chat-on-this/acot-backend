package com.chat.allchatonthis.entity.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.chat.allchatonthis.config.mybatis.core.dataobject.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("user_config")
public class UserConfigDO extends BaseDO {
    private Long id;
    private Boolean isAvailable; // Whether the configuration is available for use
    private Long userId; // Foreign key to UserDO
    private String name; // Configuration name
    private String apiUrl; // API URL for requests
    private String apiKey; // API key for authentication

    // API key placement strategy
    private String apiKeyPlacement; // 'header', 'body', or 'custom_header'
    private String apiKeyHeader; // Custom header name when apiKeyPlacement is 'custom_header'
    private String apiKeyBodyPath; // JSON path when apiKeyPlacement is 'body'

    // JSON Templates
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> requestTemplate; // JSON template for requests

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> responseTemplate; // JSON template for response handling

    // Custom headers
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> headers; // Additional HTTP headers

    // Request and Response handling strategy
    private String requestTextPath; // JSON path for request text
    private String responseTextPath; // JSON path for response text
    private String responseThinkingTextPath; // JSON path for thinking text in response
}