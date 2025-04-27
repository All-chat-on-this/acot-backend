package com.chat.allchatonthis.service.core.spi;

import com.chat.allchatonthis.mapper.ConversationMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for loading and caching ConversationPageProvider SPI implementations
 */
@Slf4j
@Component
public class ConversationPageProviderFactory {

    private static final Map<String, List<ConversationPageProvider>> PROVIDERS_CACHE = new ConcurrentHashMap<>();

    @Resource
    private ConversationMapper conversationMapper;

    /**
     * Get all available providers, sorted by order (highest priority first)
     *
     * @return List of providers
     */
    public List<ConversationPageProvider> getProviders() {
        return getProviders(true);
    }

    /**
     * Get all available providers
     *
     * @param sort Whether to sort by order (highest priority first)
     * @return List of providers
     */
    public List<ConversationPageProvider> getProviders(boolean sort) {
        String cacheKey = "all_" + sort;
        
        // Check if already cached
        if (PROVIDERS_CACHE.containsKey(cacheKey)) {
            return PROVIDERS_CACHE.get(cacheKey);
        }
        
        List<ConversationPageProvider> providers = new ArrayList<>();
        
        // Load using ServiceLoader for direct providers
        ServiceLoader<ConversationPageProvider> serviceLoader = ServiceLoader.load(ConversationPageProvider.class);
        serviceLoader.forEach((provider) ->{
            provider.setConversationMapper(conversationMapper);
            providers.add(provider);
        });
        
        // Sort if needed
        if (sort) {
            providers.sort((p1, p2) -> Integer.compare(p2.getOrder(), p1.getOrder()));
        }
        
        // Cache the result
        PROVIDERS_CACHE.put(cacheKey, providers);
        
        // Log found providers
        if (!providers.isEmpty()) {
            log.info("Loaded {} ConversationPageProvider implementations through SPI", providers.size());
            for (ConversationPageProvider provider : providers) {
                log.info("SPI provider: {} with order {}", 
                        provider.getClass().getSimpleName(), provider.getOrder());
            }
        } else {
            log.warn("No ConversationPageProvider implementations found through SPI");
        }
        
        return providers;
    }
}