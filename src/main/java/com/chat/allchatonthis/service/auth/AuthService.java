package com.chat.allchatonthis.service.auth;

import com.chat.allchatonthis.entity.vo.LoginResponseVO;

/**
 * Authentication service interface
 */
public interface AuthService {

    /**
     * Login with username and password
     *
     * @param username Username
     * @param password Password
     * @return Login response with JWT token
     */
    LoginResponseVO login(String username, String password);

    /**
     * Login with social media (OAuth2)
     *
     * @param socialType Social type (e.g., QQ, WeChat)
     * @param userType   User type (e.g., 1 for normal user, 2 for admin)
     * @param code       Authorization code
     * @param state      State param from OAuth
     * @return Login response with JWT token
     */
    LoginResponseVO socialLogin(Integer socialType, Integer userType, String code, String state);

    /**
     * Bind social account to an existing user
     *
     * @param userId     User ID
     * @param socialType Social type
     * @param userType   User type
     * @param code       Authorization code
     * @param state      State param from OAuth
     * @return True if binding successful
     */
    boolean bindSocialUser(Long userId, Integer socialType,Integer userType, String code, String state);

    /**
     * Validate token
     *
     * @param token JWT token
     * @return True if token is valid
     */
    boolean validateToken(String token);

    /**
     * Update user's nickname and synchronize with LoginUser
     *
     * @param userId   User ID
     * @param nickname New nickname
     * @return true if update successful
     */
    boolean updateUserNickname(Long userId, String nickname);
} 