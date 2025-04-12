package com.chat.allchatonthis.mapper;

import com.chat.allchatonthis.entity.dataobject.UserDO;
import com.chat.allchatonthis.config.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapperX<UserDO> {
}
