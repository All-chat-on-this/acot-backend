package com.chat.allchatonthis.service.core.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.allchatonthis.common.exception.ServiceException;
import com.chat.allchatonthis.config.security.model.LoginUser;
import com.chat.allchatonthis.entity.dataobject.UserDO;
import com.chat.allchatonthis.entity.vo.user.UserInfomationVO;
import com.chat.allchatonthis.mapper.UserMapper;
import com.chat.allchatonthis.service.core.UserService;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;

import static com.chat.allchatonthis.common.enums.ErrorCodeConstants.AUTH_USER_NOT_FOUND;

@Service
@AllArgsConstructor
@CacheConfig(cacheNames = "user")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {
    
    @Override
    @CacheEvict(key = "#userId")
    @Transactional
    public boolean updateNickname(Long userId, String nickname) {
        if (userId == null || nickname == null || nickname.isBlank()) {
            return false;
        }
        
        boolean updated = lambdaUpdate()
                .eq(UserDO::getId, userId)
                .set(UserDO::getNickname, nickname)
                .update();
                
        if (updated) {
            // Update in current security context if this user is logged in
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
                if (loginUser.getUserId().equals(userId)) {
                    loginUser.setNickname(nickname);
                }
            }
        }
        
        return updated;
    }
    
    @Override
    @Cacheable(key = "#id")
    public UserDO getById(Serializable id) {
        return super.getById(id);
    }
    
    @Override
    public UserInfomationVO getUserInformation(Long userId) {
        // Get user from database
        UserDO user = getById(userId);
        if (user == null) {
            throw new ServiceException(AUTH_USER_NOT_FOUND.getCode(), "User not found with ID: " + userId);
        }

        // Build UserInfomationVO
        return UserInfomationVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .loginType(user.getLoginType())
                .build();
    }
}
