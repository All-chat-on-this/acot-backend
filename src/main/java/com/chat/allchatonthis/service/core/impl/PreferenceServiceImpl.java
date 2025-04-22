package com.chat.allchatonthis.service.core.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.allchatonthis.entity.dataobject.PreferenceDO;
import com.chat.allchatonthis.entity.dataobject.UserConfigDO;
import com.chat.allchatonthis.mapper.PreferenceMapper;
import com.chat.allchatonthis.service.core.PreferenceService;
import com.chat.allchatonthis.service.core.UserConfigService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@AllArgsConstructor
@Slf4j
@CacheConfig(cacheNames = "preference")
public class PreferenceServiceImpl extends ServiceImpl<PreferenceMapper, PreferenceDO> implements PreferenceService {
    
    private final UserConfigService userConfigService;
    
    /**
     * Get preference for a specific user
     * If no preference exists, create a default one
     */
    @Override
    @Cacheable(key = "#userId")
    public PreferenceDO getPreference(Long userId) {
        // Find preference by userId
        PreferenceDO preference = getOne(new LambdaQueryWrapper<PreferenceDO>()
                .eq(PreferenceDO::getUserId, userId.toString()));
        
        // If preference doesn't exist, create a default one for this user
        if (preference == null) {
            preference = createDefaultPreference(userId);
        }
        
        return preference;
    }
    
    /**
     * Update preference for a specific user
     * If saveApiKey is set to false, clear all API keys in user configs
     */
    @Override
    @Transactional
    @CacheEvict(key = "#userId")
    public PreferenceDO updatePreference(PreferenceDO preference, Long userId) {
        // Get existing preference
        PreferenceDO existingPreference = getPreference(userId);
        
        // Update only the fields that are provided
        if (preference.getTheme() != null) {
            existingPreference.setTheme(preference.getTheme());
        }
        
        if (preference.getLanguage() != null) {
            existingPreference.setLanguage(preference.getLanguage());
        }
        
        // For boolean fields, always consider them provided
        existingPreference.setShowThinking(preference.isShowThinking());
        
        // Check if saveApiKey was changed from true to false
        boolean saveApiKeyChanged = existingPreference.isSaveApiKey() && !preference.isSaveApiKey();
        existingPreference.setSaveApiKey(preference.isSaveApiKey());
        
        // Save the updated preference
        updateById(existingPreference);
        
        // Handle API keys if saveApiKey is now false
        if (saveApiKeyChanged) {
            clearAllApiKeys(userId);
        }
        
        return existingPreference;
    }
    
    /**
     * Create a default preference for a given user
     */
    private PreferenceDO createDefaultPreference(Long userId) {
        // Get the system default locale
        String defaultLanguage = Locale.getDefault().getLanguage();
        // Ensure we only use supported languages (en or zh, defaulting to en)
        String language = "en";
        if ("zh".equals(defaultLanguage)) {
            language = "zh";
        }
        
        PreferenceDO preference = new PreferenceDO()
                .setUserId(userId.toString())
                .setTheme("dreamlikeColorLight")
                .setShowThinking(false)
                .setSaveApiKey(true)
                .setLanguage(language);
        
        save(preference);
        return preference;
    }
    
    /**
     * Clear all API keys for a given user
     * Called when saveApiKey preference is changed to false
     */
    private void clearAllApiKeys(Long userId) {
        // Get all configurations for this user
        List<UserConfigDO> configs = userConfigService.getConfigs(userId);
        
        // For each config, remove the API key
        for (UserConfigDO config : configs) {
            if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
                UserConfigDO updatedConfig = new UserConfigDO().setId(config.getId()).setApiKey(null);
                userConfigService.updateById(updatedConfig);
            }
        }
        
        log.info("Cleared all API keys for user {}", userId);
    }
}
