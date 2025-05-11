package com.chat.allchatonthis.controller.common;

import com.chat.allchatonthis.common.pojo.CommonResult;
import com.chat.allchatonthis.common.util.security.LoginUser;
import com.chat.allchatonthis.entity.vo.user.UserInfomationVO;
import com.chat.allchatonthis.service.core.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    /**
     * Get user information by ID
     */
    @GetMapping("/information")
    public CommonResult<UserInfomationVO> getUserInformation(@LoginUser Long userId) {
        UserInfomationVO userInfo = userService.getUserInformation(userId);
        return CommonResult.success(userInfo);
    }
    
    /**
     * Update user nickname
     */
    @PostMapping("/update-nickname")
    public CommonResult<Boolean> updateNickname(@LoginUser Long userId, @RequestParam("nickname") String nickname) {
        boolean result = userService.updateNickname(userId, nickname);
        return CommonResult.success(result);
    }
}
