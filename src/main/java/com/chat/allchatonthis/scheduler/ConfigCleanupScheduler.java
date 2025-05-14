package com.chat.allchatonthis.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chat.allchatonthis.entity.dataobject.UserConfigDO;
import com.chat.allchatonthis.mapper.UserConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler service that handles cleaning up unused configurations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigCleanupScheduler {

    private final UserConfigMapper userConfigMapper;

    /**
     * Scheduled task to delete configurations that haven't been used for a year
     * Runs once a month at midnight on the 1st day of the month
     */
    @Scheduled(cron = "0 0 0 1 * ?") // Run at midnight on the 1st day of every month
    public void cleanupUnusedConfigurations() {
        log.info("Starting scheduled cleanup of unused configurations");
        
        // Use the default of one year
        cleanupConfigurationsOlderThan(1);
    }
    
    /**
     * Manual cleanup method that can be triggered by administrators
     * Deletes configurations that haven't been updated for the specified number of years
     *
     * @param years The number of years of inactivity before a configuration is deleted
     * @return The number of configurations deleted
     */
    public int cleanupConfigurationsOlderThan(int years) {
        try {
            if (years <= 0) {
                log.warn("Invalid years parameter: {}. Must be greater than 0.", years);
                return 0;
            }
            
            // Calculate the cutoff date
            LocalDateTime cutoffDate = LocalDateTime.now().minusYears(years);
            
            log.info("Cleaning up configurations not updated since {}", cutoffDate);
            
            // Find all configurations that haven't been updated since the cutoff date
            LambdaQueryWrapper<UserConfigDO> query = new LambdaQueryWrapper<UserConfigDO>()
                    .lt(UserConfigDO::getUpdateTime, cutoffDate);
            
            // Get the list of configurations to delete
            List<UserConfigDO> configsToDelete = userConfigMapper.selectList(query);
            
            if (configsToDelete.isEmpty()) {
                log.info("No unused configurations found for deletion");
                return 0;
            }
            
            log.info("Found {} unused configurations to delete", configsToDelete.size());
            
            // Delete the configurations
            int deletedCount = userConfigMapper.delete(query);
            
            log.info("Successfully deleted {} unused configurations", deletedCount);
            
            return deletedCount;
        } catch (Exception e) {
            log.error("Error while cleaning up unused configurations", e);
            return 0;
        }
    }
}