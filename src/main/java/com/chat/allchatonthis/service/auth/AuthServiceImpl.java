package com.chat.allchatonthis.service.auth;

import com.chat.allchatonthis.common.exception.ServiceException;
import com.chat.allchatonthis.common.util.token.JwtUtils;
import com.chat.allchatonthis.config.security.model.LoginUser;
import com.chat.allchatonthis.entity.dataobject.UserDO;
import com.chat.allchatonthis.entity.vo.UserInfomationVO;
import com.chat.allchatonthis.enums.SocialTypeEnum;
import com.chat.allchatonthis.service.core.UserService;
import com.chat.allchatonthis.service.social.SocialClientService;
import com.xingyuv.jushauth.model.AuthUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

import static com.chat.allchatonthis.common.enums.ErrorCodeConstants.*;

/**
 * Authentication service implementation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserService userService;
    private final SocialClientService socialClientService;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    /**
     * Converts UserDO to LoginUser
     *
     * @param user UserDO object
     * @return LoginUser object
     */
    public static LoginUser convertToLoginUser(UserDO user) {
        if (user == null) {
            return null;
        }

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setNickname(user.getNickname());
        loginUser.setLoginType(user.getLoginType());
        loginUser.setOpenId(user.getOpenId());

        return loginUser;
    }

    @Override
    public UserInfomationVO login(String username, String password) {
        // Authenticate user through Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate JWT token
        LoginUser userDetails = (LoginUser) authentication.getPrincipal();
        String jwt = jwtUtils.generateToken(userDetails.getUsername(), userDetails.getUserId(), userDetails.getLoginType());

        // Return login response
        return createLoginResponse(userDetails, jwt, false);
    }

    @Override
    @Transactional
    public UserInfomationVO register(String username, String password, String nickname) {
        // Check if username already exists
        long count = userService.lambdaQuery().eq(UserDO::getUsername, username).count();
        if (count > 0) {
            throw new ServiceException(AUTH_USER_EXISTS.getCode(), "Username already exists");
        }

        // Create new user
        UserDO user = new UserDO()
                .setUsername(username)
                .setPassword(passwordEncoder.encode(password))
                .setLoginType(0); // 0 for regular account

        // Set nickname if provided, otherwise use username
        if (StringUtils.hasText(nickname)) {
            user.setNickname(nickname);
        } else {
            user.setNickname(username);
        }

        // Save user
        userService.save(user);

        // Generate JWT token
        String jwt = jwtUtils.generateToken(user.getUsername(), user.getId(), user.getLoginType());

        // Convert to LoginUser and create response
        LoginUser loginUser = convertToLoginUser(user);
        return createLoginResponse(loginUser, jwt, true);
    }

    @Override
    @Transactional
    public UserInfomationVO socialLogin(Integer socialType, Integer userType, String code, String state) {
        // Validate social type
        SocialTypeEnum socialTypeEnum = SocialTypeEnum.valueOfType(socialType);
        if (socialTypeEnum == null) {
            throw new ServiceException(SOCIAL_TYPE_NOT_SUPPORTED, "Unsupported social type: " + socialType);
        }

        // Get user info from social platform
        AuthUser authUser = socialClientService.getAuthUser(socialType, userType, code, state);
        if (authUser == null) {
            throw new ServiceException(SOCIAL_USER_AUTH_FAILURE.getCode(), "Failed to authenticate with social platform");
        }

        // Try to find existing user
        UserDO user = userService.lambdaQuery()
                .eq(UserDO::getOpenId, authUser.getUuid())
                .eq(UserDO::getLoginType, socialType)
                .one();

        boolean isNewUser = false;
        // If user doesn't exist, create a new one
        if (user == null) {
            isNewUser = true;
            user = createSocialUser(authUser, socialType);
        }

        // Generate JWT token
        String jwt = jwtUtils.generateToken(user.getUsername(), user.getId(), user.getLoginType());

        // Convert to LoginUser and create response
        LoginUser loginUser = convertToLoginUser(user);

        return createLoginResponse(loginUser, jwt, isNewUser);
    }

    @Override
    @Transactional
    public boolean bindSocialUser(Long userId, Integer socialType, Integer userType, String code, String state) {
        // Validate social type
        SocialTypeEnum socialTypeEnum = SocialTypeEnum.valueOfType(socialType);
        if (socialTypeEnum == null) {
            throw new ServiceException(SOCIAL_TYPE_NOT_SUPPORTED, "Unsupported social type: " + socialType);
        }

        // Get user info from social platform
        AuthUser authUser = socialClientService.getAuthUser(socialType, userType, code, state);
        if (authUser == null) {
            throw new ServiceException(SOCIAL_USER_AUTH_FAILURE.getCode(), "Failed to authenticate with social platform");
        }

        // Check if the social account is already bound to another user
        UserDO existingUser = userService.lambdaQuery()
                .eq(UserDO::getOpenId, authUser.getUuid())
                .eq(UserDO::getLoginType, socialType)
                .one();

        if (existingUser != null && !existingUser.getId().equals(userId)) {
            throw new ServiceException(SOCIAL_USER_BIND_FAILED, "This social account is already bound to another user");
        }

        // Get the user to bind
        UserDO user = userService.getById(userId);
        if (user == null) {
            throw new ServiceException(SOCIAL_USER_BIND_FAILED, "User not found");
        }

        // Update user with social info
        user.setOpenId(authUser.getUuid())
                .setLoginType(socialType);

        return userService.updateById(user);
    }

    @Override
    public boolean validateToken(String token) {
        try {
            return !jwtUtils.isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Update user's nickname and synchronize with LoginUser
     *
     * @param userId   User ID
     * @param nickname New nickname
     * @return true if update successful
     */
    @Transactional
    public boolean updateUserNickname(Long userId, String nickname) {
        // Update in database
        boolean updated = userService.updateNickname(userId, nickname);

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
    public UserInfomationVO getUserInformation(Long userId) {
        // Get user from database
        UserDO user = userService.getById(userId);
        if (user == null) {
            throw new ServiceException(AUTH_USER_NOT_FOUND.getCode(), "User not found with ID: " + userId);
        }

        // Convert to LoginUser
        LoginUser loginUser = convertToLoginUser(user);

        // Build UserInfomationVO without token information
        return UserInfomationVO.builder()
                .userId(loginUser.getUserId())
                .username(loginUser.getUsername())
                .nickname(loginUser.getNickname())
                .loginType(loginUser.getLoginType())
                .build();
    }

    @Override
    public boolean logout(String token) {
        // Validate token
        if (!validateToken(token)) {
            // If token is already invalid, consider logout successful
            return true;
        }
        
        try {
            // Get username from token
            String username = jwtUtils.extractUsername(token);
            
            // Clear SecurityContext if this user is currently logged in
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getName().equals(username)) {
                SecurityContextHolder.clearContext();
            }
            
            return true;
        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Create a new user from social login
     */
    private UserDO createSocialUser(AuthUser authUser, Integer socialType) {
        // Generate a random username
        String username = generateUniqueUsername(authUser);

        // Create new user
        UserDO user = new UserDO()
                .setUsername(username)
                .setPassword(passwordEncoder.encode(UUID.randomUUID().toString())) // Random password
                .setNickname(authUser.getNickname())
                .setLoginType(socialType)
                .setOpenId(authUser.getUuid());

        // Save user
        userService.save(user);
        return user;
    }

    /**
     * Generate a unique username based on social user info
     */
    private String generateUniqueUsername(AuthUser authUser) {
        String baseUsername = "social_" + authUser.getSource().toLowerCase() + "_user";
        String username = baseUsername;
        int suffix = 1;

        // Make sure username is unique
        while (userService.lambdaQuery().eq(UserDO::getUsername, username).count() > 0) {
            username = baseUsername + suffix;
            suffix++;
        }

        return username;
    }

    /**
     * Create login response from user details
     */
    private UserInfomationVO createLoginResponse(LoginUser userDetails, String jwt, boolean isNewUser) {
        return UserInfomationVO.builder()
                .userId(userDetails.getUserId())
                .username(userDetails.getUsername())
                .nickname(userDetails.getNickname())
                .loginType(userDetails.getLoginType())
                .token(jwt)
                .expiresIn(expiration)
                .isNewUser(isNewUser)
                .build();
    }
} 