package com.chat.allchatonthis.controller;

import com.chat.allchatonthis.common.pojo.CommonResult;
import com.chat.allchatonthis.common.util.security.LoginUser;
import com.chat.allchatonthis.entity.vo.user.UserInfomationVO;
import com.chat.allchatonthis.service.auth.AuthService;
import com.chat.allchatonthis.service.social.SocialClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for authentication and social login
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SocialClientService socialClientService;

    /**
     * Regular login with username and password
     */
    @PostMapping("/login")
    public CommonResult<UserInfomationVO> login(@RequestParam String username, @RequestParam String password) {
        UserInfomationVO response = authService.login(username, password);
        return CommonResult.success(response);
    }

    /**
     * Register a new user
     */
    @PostMapping("/register")
    public CommonResult<UserInfomationVO> register(
            @RequestParam String username, 
            @RequestParam String password,
            @RequestParam(required = false) String nickname) {
        UserInfomationVO response = authService.register(username, password, nickname);
        return CommonResult.success(response);
    }

    /**
     * Social login callback
     */
    @GetMapping("/social/callback")
    public CommonResult<UserInfomationVO> socialLogin(
            @RequestParam("type") Integer socialType,
            @RequestParam("user_type") Integer userType,
            @RequestParam("code") String code,
            @RequestParam("state") String state) {
        UserInfomationVO response = authService.socialLogin(socialType, userType, code, state);
        return CommonResult.success(response);
    }

    /**
     * Get social authorization URL
     */
    @GetMapping("/social/authorize")
    public CommonResult<Map<String, String>> getAuthorizeUrl(
            @RequestParam("type") Integer socialType,
            @RequestParam("user_type") Integer userType,
            @RequestParam("redirect_uri") String redirectUri) {
        String authorizeUrl = socialClientService.getAuthorizeUrl(socialType, userType, redirectUri);
        Map<String, String> result = new HashMap<>();
        result.put("authorizeUrl", authorizeUrl);
        return CommonResult.success(result);
    }

    /**
     * Bind social account to an existing user
     */
    @PostMapping("/social/bind")
    public CommonResult<Boolean> bindSocialUser(
            @RequestParam("userId") Long userId,
            @RequestParam("type") Integer socialType,
            @RequestParam("user_type") Integer userType,
            @RequestParam("code") String code,
            @RequestParam("state") String state) {
        boolean result = authService.bindSocialUser(userId, socialType, userType, code, state);
        return CommonResult.success(result);
    }

    /**
     * Validate token
     */
    @GetMapping("/validate")
    public CommonResult<Boolean> validateToken(@RequestParam("token") String token) {
        boolean valid = authService.validateToken(token);
        return CommonResult.success(valid);
    }

    /**
     * Logout the user
     */
    @PostMapping("/logout")
    public CommonResult<Boolean> logout(@RequestParam("token") String token) {
        boolean result = authService.logout(token);
        return CommonResult.success(result);
    }
}
