package com.chat.allchatonthis.service.core;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.allchatonthis.entity.dataobject.UserConfigDO;
import com.chat.allchatonthis.mapper.UserConfigMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserConfigServiceImpl extends ServiceImpl<UserConfigMapper, UserConfigDO> implements UserConfigService {
} 