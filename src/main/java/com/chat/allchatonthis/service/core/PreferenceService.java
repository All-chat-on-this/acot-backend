package com.chat.allchatonthis.service.core;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chat.allchatonthis.entity.dataobject.PreferenceDO;

public interface PreferenceService extends IService<PreferenceDO> {
    /**
     * Get the preference for a specific user
     * If no preference exists, create a default one
     * 
     * @param userId The user ID
     * @return The user's preference
     */
    PreferenceDO getPreference(Long userId);
    
    /**
     * Update the preference for a specific user
     * 
     * @param preference The preference to update
     * @param userId The user ID
     * @return The updated preference
     */
    PreferenceDO updatePreference(PreferenceDO preference, Long userId);
}
