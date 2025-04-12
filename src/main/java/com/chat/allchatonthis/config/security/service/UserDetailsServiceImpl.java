package com.chat.allchatonthis.config.security.service;

import com.chat.allchatonthis.entity.dataobject.UserDO;
import com.chat.allchatonthis.config.security.model.LoginUser;
import com.chat.allchatonthis.service.core.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * UserDetailsService implementation for Spring Security
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Find user by username
        UserDO user = userService.lambdaQuery().eq(UserDO::getUsername, username).one();
        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        // Convert to LoginUser
        return convertToLoginUser(user);
    }

    /**
     * Find user by social credentials (openId and socialType)
     */
    public UserDetails loadUserBySocialCredentials(String openId, Integer socialType) throws UsernameNotFoundException {
        // Find user by openId and loginType
        UserDO user = userService.lambdaQuery()
                .eq(UserDO::getOpenId, openId)
                .eq(UserDO::getLoginType, socialType)
                .one();
        if (user == null) {
            throw new UsernameNotFoundException("User not found with openId: " + openId);
        }

        // Convert to LoginUser
        return convertToLoginUser(user);
    }

    /**
     * Convert UserDO to LoginUser
     */
    private LoginUser convertToLoginUser(UserDO user) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setPassword(user.getPassword());
        loginUser.setNickname(user.getNickname());
        loginUser.setLoginType(user.getLoginType());
        loginUser.setOpenId(user.getOpenId());
        loginUser.setPermissions(Collections.emptyList()); // Set permissions if needed

        return loginUser;
    }
} 