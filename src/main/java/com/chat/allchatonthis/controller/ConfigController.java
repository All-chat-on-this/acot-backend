package com.chat.allchatonthis.controller;

import com.chat.allchatonthis.common.pojo.CommonResult;
import com.chat.allchatonthis.common.util.object.BeanUtils;
import com.chat.allchatonthis.common.util.security.LoginUser;
import com.chat.allchatonthis.entity.vo.config.ConfigTestVO;
import com.chat.allchatonthis.entity.vo.config.ModelStatusVO;
import com.chat.allchatonthis.entity.vo.config.ConfigCreateOrUpdateReqVO;
import com.chat.allchatonthis.entity.vo.config.ConfigRespVO;
import com.chat.allchatonthis.entity.vo.config.ConfigTestReqVO;
import com.chat.allchatonthis.entity.dataobject.UserConfigDO;
import com.chat.allchatonthis.service.core.UserConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API Configuration Controller
 */
@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class ConfigController {

    private final UserConfigService userConfigService;

    /**
     * Get all configurations for the current user
     */
    @GetMapping("/getConfigs")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<List<ConfigRespVO>> getConfigs(@LoginUser Long userId) {
        List<UserConfigDO> configs = userConfigService.getConfigs(userId);
        return CommonResult.success(BeanUtils.toBean(configs, ConfigRespVO.class));
    }

    /**
     * Get a specific configuration by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<ConfigRespVO> getConfig(@PathVariable Long id, @LoginUser Long userId) {
        UserConfigDO config = userConfigService.getConfig(id, userId);
        return CommonResult.success(BeanUtils.toBean(config, ConfigRespVO.class));
    }

    /**
     * Create a new configuration
     */
    @PostMapping("/createConfig")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<ConfigRespVO> createConfig(@RequestBody ConfigCreateOrUpdateReqVO reqVO, @LoginUser Long userId) {
        UserConfigDO config = BeanUtils.toBean(reqVO, UserConfigDO.class);
        config = userConfigService.createConfig(config, userId);
        return CommonResult.success(BeanUtils.toBean(config, ConfigRespVO.class));
    }

    /**
     * Update an existing configuration
     */
    @PutMapping("/updateConfig/{id}")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<ConfigRespVO> updateConfig(@PathVariable Long id, @RequestBody ConfigCreateOrUpdateReqVO reqVO, 
                                    @LoginUser Long userId) {
        UserConfigDO config = BeanUtils.toBean(reqVO, UserConfigDO.class);
        config = userConfigService.updateConfig(id, config, userId);
        return CommonResult.success(BeanUtils.toBean(config, ConfigRespVO.class));
    }

    /**
     * Delete a configuration
     */
    @DeleteMapping("/configs/{id}")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Boolean> deleteConfig(@PathVariable Long id, @LoginUser Long userId) {
        boolean result = userConfigService.deleteConfig(id, userId);
        return CommonResult.success(result);
    }

    /**
     * Test a configuration
     */
    @PostMapping("/test")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<ConfigTestVO> testConfig(@RequestBody ConfigTestReqVO reqVO, @LoginUser Long userId) {
        UserConfigDO config = BeanUtils.toBean(reqVO, UserConfigDO.class);
        ConfigTestVO result = userConfigService.testConfig(config, userId);
        return CommonResult.success(result);
    }

    /**
     * Get model status for a configuration
     */
    @GetMapping("/{id}/models")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<ModelStatusVO> getModelStatus(@PathVariable Long id, @LoginUser Long userId) {
        ModelStatusVO result = userConfigService.getModelStatus(id, userId);
        return CommonResult.success(result);
    }
}
