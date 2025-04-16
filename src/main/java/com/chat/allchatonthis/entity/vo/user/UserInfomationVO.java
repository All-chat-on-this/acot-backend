package com.chat.allchatonthis.entity.vo.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login response value object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfomationVO {

    /**
     * User ID
     */
    private Long userId;

    /**
     * Username
     */
    private String username;
    
    /**
     * Nickname
     */
    private String nickname;
    
    /**
     * Login type (0: username/password, other values: social login types)
     */
    private Integer loginType;
    
    /**
     * JWT token
     */
    private String token;
    
    /**
     * Token expiration time in milliseconds
     */
    private Long expiresIn;
    
    /**
     * True if this is a new user created during social login
     */
    private Boolean isNewUser;
} 