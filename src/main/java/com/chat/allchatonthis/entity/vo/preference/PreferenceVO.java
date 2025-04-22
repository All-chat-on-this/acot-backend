package com.chat.allchatonthis.entity.vo.preference;

import com.chat.allchatonthis.common.util.object.BeanUtils;
import com.chat.allchatonthis.entity.dataobject.PreferenceDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Preference value object for API responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class PreferenceVO {
    private String theme;
    private boolean showThinking;
    private boolean saveApiKey;
    private String language;
    
    /**
     * Convert from DO to VO
     */
    public static PreferenceVO fromDO(PreferenceDO preferenceDO) {
        if (preferenceDO == null) {
            return null;
        }
        
        // Use BeanUtils for automatic conversion
        return BeanUtils.toBean(preferenceDO, PreferenceVO.class);
    }
} 