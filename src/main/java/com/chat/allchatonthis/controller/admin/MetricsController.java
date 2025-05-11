package com.chat.allchatonthis.controller.admin;

import com.chat.allchatonthis.common.pojo.CommonResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.search.Search;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for exposing application metrics
 */
@RestController
@RequestMapping("/metrics")
@RequiredArgsConstructor
@Slf4j
public class MetricsController {

    private final MeterRegistry meterRegistry;

    /**
     * Get the current number of online users
     */
    @GetMapping("/online-users")
    public CommonResult<Integer> getOnlineUsers() {
        // Find the gauge and get its value
        Integer onlineUsers = (int) meterRegistry.get("acot.online.users").gauge().value();
        return CommonResult.success(onlineUsers);
    }

    /**
     * Get endpoint call statistics
     * This endpoint requires admin access
     */
    @GetMapping("/endpoints")
    public CommonResult<Map<String, Object>> getEndpointStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Get all endpoint call counters
        Search.in(meterRegistry)
            .name("acot.endpoint.calls")
            .counters()
            .forEach(counter -> {
                Iterable<Tag> tags = counter.getId().getTags();
                
                String controller = "";
                String method = "";
                
                // Iterate through tags to find controller and method
                for (Tag tag : tags) {
                    if ("controller".equals(tag.getKey())) {
                        controller = tag.getValue();
                    } else if ("method".equals(tag.getKey())) {
                        method = tag.getValue();
                    }
                }
                
                String endpoint = controller + "." + method;
                stats.put(endpoint, (int) counter.count());
            });
        
        return CommonResult.success(stats);
    }

    /**
     * Get a summary of system metrics
     */
    @GetMapping("/summary")
    public CommonResult<Map<String, Object>> getMetricsSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        // Get online users
        Integer onlineUsers = (int) meterRegistry.get("acot.online.users").gauge().value();
        summary.put("onlineUsers", onlineUsers);
        
        // Count total API calls (sum of all endpoint counters)
        double totalApiCalls = Search.in(meterRegistry)
            .name("acot.endpoint.calls")
            .counters()
            .stream()
            .mapToDouble(counter -> counter.count())
            .sum();
        summary.put("totalApiCalls", (int) totalApiCalls);
        
        return CommonResult.success(summary);
    }
} 