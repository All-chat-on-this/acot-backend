package com.chat.allchatonthis.controller;

import com.chat.allchatonthis.common.pojo.CommonResult;
import com.chat.allchatonthis.entity.vo.LoginResponseVO;
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
@RequestMapping("/api")
@RequiredArgsConstructor
public class LoginController {

    private final AuthService authService;
    private final SocialClientService socialClientService;

    /**
     * Regular login with username and password
     */
    @PostMapping("/auth/login")
    public CommonResult<LoginResponseVO> login(@RequestParam String username, @RequestParam String password) {
        LoginResponseVO response = authService.login(username, password);
        return CommonResult.success(response);
    }

    /**
     * Social login callback
     */
    @GetMapping("/auth/social/callback")
    public CommonResult<LoginResponseVO> socialLogin(
            @RequestParam("type") Integer socialType,
            @RequestParam("user_type") Integer userType,
            @RequestParam("code") String code,
            @RequestParam("state") String state) {
        LoginResponseVO response = authService.socialLogin(socialType, userType, code, state);
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
    @PostMapping("/auth/social/bind")
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
    @GetMapping("/auth/validate")
    public CommonResult<Boolean> validateToken(@RequestParam("token") String token) {
        boolean valid = authService.validateToken(token);
        return CommonResult.success(valid);
    }
}
