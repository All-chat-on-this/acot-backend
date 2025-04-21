package com.chat.allchatonthis.service.core.spi.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chat.allchatonthis.common.pojo.PageResult;
import com.chat.allchatonthis.entity.dataobject.ConversationDO;
import com.chat.allchatonthis.entity.vo.conversation.ConversationPageReqVO;
import com.chat.allchatonthis.mapper.ConversationMapper;
import com.chat.allchatonthis.service.core.spi.ConversationPageProvider;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * MyBatis Plus implementation of the ConversationPageProvider SPI
 */
@Component
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class MybatisPlusConversationPageProvider implements ConversationPageProvider {

    private ConversationMapper conversationMapper;

    @Override
    // Not using @Cacheable here because pagination requests typically vary and caching may not be effective
    // Additionally, pagination data should be fresh to reflect the latest changes
    public PageResult<ConversationDO> getConversationPage(Long userId, ConversationPageReqVO conversationPageReqVO) {
        try {
            // Create pagination parameters
            Page<ConversationDO> page = new Page<>(conversationPageReqVO.getPageNo(), conversationPageReqVO.getPageSize());

            // Build query wrapper
            LambdaQueryWrapper<ConversationDO> queryWrapper = new LambdaQueryWrapper<ConversationDO>()
                    .eq(ConversationDO::getUserId, userId);

            // Add search condition if text is provided
            if (conversationPageReqVO.getSearchText() != null && !conversationPageReqVO.getSearchText().isEmpty()) {
                queryWrapper.like(ConversationDO::getTitle, conversationPageReqVO.getSearchText());
            }

            // Execute paginated query
            IPage<ConversationDO> resultPage = conversationMapper.selectPage(page, queryWrapper);

            // Return the paginated result
            return new PageResult<>(resultPage.getRecords(), resultPage.getTotal());
        } catch (Exception e) {
            log.error("Error retrieving conversation page with MyBatis Plus", e);
            return new PageResult<>();
        }
    }

    @Override
    public int getOrder() {
        return 100; // High priority since it's now the primary provider
    }
} 