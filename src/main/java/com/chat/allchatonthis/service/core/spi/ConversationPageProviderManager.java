package com.chat.allchatonthis.service.core.spi;

import com.chat.allchatonthis.common.pojo.PageResult;
import com.chat.allchatonthis.entity.dataobject.ConversationDO;
import com.chat.allchatonthis.entity.vo.conversation.ConversationPageReqVO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manager for ConversationPageProvider SPI implementations
 */
@Component
@Slf4j
public class ConversationPageProviderManager {

    private final List<ConversationPageProvider> providers = new ArrayList<>();

    @Resource
    private ConversationPageProviderFactory conversationPageProviderFactory;

    @PostConstruct
    public void init() {
        // Load providers using SPI factory
        List<ConversationPageProvider> spiProviders = conversationPageProviderFactory.getProviders();
        if (!spiProviders.isEmpty()) {
            providers.addAll(spiProviders);
        }

        // Log loaded providers
        log.info("Loaded {} ConversationPageProvider implementations", providers.size());
        for (ConversationPageProvider provider : providers) {
            log.info("ConversationPageProvider: {} with order {}",
                    provider.getClass().getSimpleName(), provider.getOrder());
        }
    }

    /**
     * Get conversation page using the available providers
     *
     * @param userId                The user ID
     * @param conversationPageReqVO The pagination and search parameters
     * @return A page of conversations
     */
    public PageResult<ConversationDO> getConversationPage(Long userId, ConversationPageReqVO conversationPageReqVO) {
        PageResult<ConversationDO> result = null;
        List<String> failedProviders = new ArrayList<>();

        // Try each provider in order until one returns a non-null result
        for (ConversationPageProvider provider : providers) {
            try {
                result = provider.getConversationPage(userId, conversationPageReqVO);
                if (result != null) {
                    log.debug("Using provider {} for conversation page", provider.getClass().getSimpleName());
                    break;
                }
            } catch (Exception e) {
                failedProviders.add(provider.getClass().getSimpleName());
                log.warn("Provider {} failed with error: {}",
                        provider.getClass().getSimpleName(), e.getMessage());
            }
        }

        // If all providers failed, return an empty result
        if (result == null) {
            if (!failedProviders.isEmpty()) {
                log.warn("All attempted providers failed to get conversation page: {}", String.join(", ", failedProviders));
            } else {
                log.warn("No suitable provider found for conversation page");
            }
            result = new PageResult<>(Collections.emptyList(), 0L);
        }

        return result;
    }
} 