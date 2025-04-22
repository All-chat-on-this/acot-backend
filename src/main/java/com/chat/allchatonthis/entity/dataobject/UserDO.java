package com.chat.allchatonthis.entity.dataobject;

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
@TableName("user")
public class UserDO extends BaseDO {
   private Long id;
   private String username;
   private String password;
   private String nickname;
   private Integer loginType;
   private String openId;
   private String token;
}