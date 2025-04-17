package com.chat.allchatonthis.service.core;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chat.allchatonthis.entity.dataobject.UserConfigDO;
import com.chat.allchatonthis.entity.vo.config.ConfigTestVO;

import java.util.List;

public interface UserConfigService extends IService<UserConfigDO> {
    
    /**
     * Get all configurations for the current user
     * 
     * @param userId The user ID
     * @return List of configurations
     */
    List<UserConfigDO> getConfigs(Long userId);
    
    /**
     * Get a specific configuration by ID
     * 
     * @param id Configuration ID
     * @param userId User ID for security check
     * @return The configuration or null if not found
     */
    UserConfigDO getConfig(Long id, Long userId);
    
    /**
     * Create a new configuration
     * 
     * @param config Configuration to create
     * @param userId User ID who owns this configuration
     * @return The created configuration with ID
     */
    UserConfigDO createConfig(UserConfigDO config, Long userId);
    
    /**
     * Update an existing configuration
     * 
     * @param id Configuration ID to update
     * @param config Updated configuration data
     * @param userId User ID for security check
     * @return The updated configuration
     */
    UserConfigDO updateConfig(Long id, UserConfigDO config, Long userId);
    
    /**
     * Delete a configuration
     * 
     * @param id Configuration ID to delete
     * @param userId User ID for security check
     * @return true if deleted successfully
     */
    boolean deleteConfig(Long id, Long userId);
    
    /**
     * Test a configuration by making a sample request to the API
     * Sets isAvailable to true if test is successful
     * 
     * @param config Configuration to test (doesn't need to be saved)
     * @param userId User ID making the request
     * @return Test response with success/failure and any results
     */
    ConfigTestVO testConfig(UserConfigDO config, Long userId);
} 