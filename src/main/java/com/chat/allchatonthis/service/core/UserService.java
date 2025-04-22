package com.chat.allchatonthis.service.core;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chat.allchatonthis.entity.dataobject.UserDO;
import com.chat.allchatonthis.entity.vo.user.UserInfomationVO;

public interface UserService extends IService<UserDO> {
    /**
     * Update user's nickname
     *
     * @param userId User ID
     * @param nickname New nickname
     * @return true if update successful, false otherwise
     */
    boolean updateNickname(Long userId, String nickname);
    
    /**
     * Get user information by user ID
     *
     * @param userId User ID
     * @return User information
     */
    UserInfomationVO getUserInformation(Long userId);
}
