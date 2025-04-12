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
public class UserConfigDO extends BaseDO {
    private Long id;
    private Long userId; // Foreign key to UserDO
    private String configKey;
    private String configValue;
}