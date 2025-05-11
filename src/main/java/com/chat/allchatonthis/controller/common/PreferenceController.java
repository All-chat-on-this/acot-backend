package com.chat.allchatonthis.controller.common;

import com.chat.allchatonthis.common.util.object.BeanUtils;
import com.chat.allchatonthis.common.util.security.SecurityUtils;
import com.chat.allchatonthis.entity.dataobject.PreferenceDO;
import com.chat.allchatonthis.entity.vo.preference.PreferenceVO;
import com.chat.allchatonthis.service.core.PreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Preference Controller
 */
@RestController
@RequestMapping("/preference")
@RequiredArgsConstructor
public class PreferenceController {
    private final PreferenceService preferenceService;

    @GetMapping("/getPreference")
    public PreferenceVO getPreference() {
        // Get current user ID
        Long userId = SecurityUtils.getLoginUserId();
        
        // Get preference for this user
        PreferenceDO preferenceDO = preferenceService.getPreference(userId);
        return PreferenceVO.fromDO(preferenceDO);
    }

    @PutMapping("/updatePreference")
    public PreferenceVO updatePreference(@RequestBody PreferenceVO preferenceVO) {
        // Get current user ID
        Long userId = SecurityUtils.getLoginUserId();
        
        // Convert to DO for service layer using BeanUtils
        PreferenceDO preferenceDO = BeanUtils.toBean(preferenceVO, PreferenceDO.class);
        
        // Update and convert back to VO
        PreferenceDO updatedDO = preferenceService.updatePreference(preferenceDO, userId);
        return PreferenceVO.fromDO(updatedDO);
    }
}
