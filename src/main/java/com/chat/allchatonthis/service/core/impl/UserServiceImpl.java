package com.chat.allchatonthis.service.core.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.allchatonthis.entity.dataobject.UserDO;
import com.chat.allchatonthis.mapper.UserMapper;
import com.chat.allchatonthis.service.core.UserService;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.Serializable;

@Service
@AllArgsConstructor
@CacheConfig(cacheNames = "user")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {
    
    @Override
    @CacheEvict(key = "#userId")
    public boolean updateNickname(Long userId, String nickname) {
        if (userId == null || nickname == null || nickname.isBlank()) {
            return false;
        }
        
        return lambdaUpdate()
                .eq(UserDO::getId, userId)
                .set(UserDO::getNickname, nickname)
                .update();
    }
    
    @Override
    @Cacheable(key = "#id")
    public UserDO getById(Serializable id) {
        return super.getById(id);
    }
}
