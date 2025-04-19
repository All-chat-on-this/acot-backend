package com.chat.allchatonthis.service.core.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.allchatonthis.entity.dataobject.UserDO;
import com.chat.allchatonthis.mapper.UserMapper;
import com.chat.allchatonthis.service.core.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {
    
    @Override
    public boolean updateNickname(Long userId, String nickname) {
        if (userId == null || nickname == null || nickname.isBlank()) {
            return false;
        }
        
        return lambdaUpdate()
                .eq(UserDO::getId, userId)
                .set(UserDO::getNickname, nickname)
                .update();
    }
}
