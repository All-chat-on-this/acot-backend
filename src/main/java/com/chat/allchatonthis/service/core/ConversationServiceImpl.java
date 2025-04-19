package com.chat.allchatonthis.service.core;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.allchatonthis.common.exception.ServiceException;
import com.chat.allchatonthis.common.pojo.PageResult;
import com.chat.allchatonthis.entity.dataobject.ConversationDO;
import com.chat.allchatonthis.entity.vo.conversation.ConversationPageReqVO;
import com.chat.allchatonthis.es.document.ConversationDocument;
import com.chat.allchatonthis.mapper.ConversationMapper;
import com.chat.allchatonthis.service.es.ESConversationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.chat.allchatonthis.common.enums.ErrorCodeConstants.CONVERSATION_NOT_EXISTS;

@Service
@AllArgsConstructor
@Slf4j
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, ConversationDO> implements ConversationService {

    private final ESConversationService esConversationService;
    private final ConversationMessageService conversationMessageService;

    @Override
    public List<ConversationDO> getConversations(Long userId) {
        return list(new LambdaQueryWrapper<ConversationDO>()
                .eq(ConversationDO::getUserId, userId)
                .orderByDesc(ConversationDO::getUpdateTime));
    }

    @Override
    public PageResult<ConversationDO> getConversationPage(Long userId, ConversationPageReqVO conversationPageReqVO) {
        String searchText = conversationPageReqVO.getSearchText();

        // If no search text is provided, use regular database query
        if (!StringUtils.hasText(searchText)) {
            // Create pagination parameters
            Page<ConversationDO> page = new Page<>(conversationPageReqVO.getPageNo(), conversationPageReqVO.getPageSize());

            // Build query wrapper
            LambdaQueryWrapper<ConversationDO> queryWrapper = new LambdaQueryWrapper<ConversationDO>()
                    .eq(ConversationDO::getUserId, userId)
                    .orderByDesc(ConversationDO::getUpdateTime);

            // Execute paginated query
            IPage<ConversationDO> resultPage = page(page, queryWrapper);

            // Return the paginated result
            return new PageResult<>(resultPage.getRecords(), resultPage.getTotal());
        }

        // Use Elasticsearch for complex search (title, message content, thinking text)
        PageResult<ConversationDocument> esResult = esConversationService.searchConversations(userId, conversationPageReqVO);

        if (esResult == null || esResult.getList().isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }

        // Convert ES documents to ConversationDOs
        List<Long> conversationIds = esResult.getList().stream()
                .map(ConversationDocument::getId)
                .collect(Collectors.toList());

        if (conversationIds.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }

        // Get the actual conversation records from database
        List<ConversationDO> conversations = listByIds(conversationIds);

        // Sort conversations according to the order they appear in the ES results
        List<ConversationDO> orderedConversations = conversationIds.stream()
                .map(id -> conversations.stream()
                        .filter(conv -> conv.getId().equals(id))
                        .findFirst()
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new PageResult<>(orderedConversations, esResult.getTotal());
    }

    @Override
    public ConversationDO getConversation(Long id, Long userId) {
        return getOne(new LambdaQueryWrapper<ConversationDO>()
                .eq(ConversationDO::getId, id)
                .eq(ConversationDO::getUserId, userId));
    }

    @Override
    @Transactional
    public ConversationDO createConversation(ConversationDO conversation, Long userId) {
        // Set the user ID for this conversation
        conversation.setUserId(userId);

        // Save the conversation
        save(conversation);

        // Index in Elasticsearch
        esConversationService.saveConversation(conversation);

        return conversation;
    }

    @Override
    @Transactional
    public ConversationDO updateConversation(Long id, ConversationDO conversation, Long userId) {
        // Check if conversation exists and belongs to user
        ConversationDO existingConversation = getConversation(id, userId);
        if (existingConversation == null) {
            throw new ServiceException(CONVERSATION_NOT_EXISTS.getCode(), CONVERSATION_NOT_EXISTS.getMsg());
        }

        // Update fields
        conversation.setId(id);
        conversation.setUserId(userId);  // Ensure the user ID remains the same

        updateById(conversation);

        // Update in Elasticsearch
        esConversationService.updateConversation(conversation);

        return conversation;
    }

    @Override
    @Transactional
    public boolean deleteConversation(Long id, Long userId) {
        // First, delete all messages in the conversation
        conversationMessageService.deleteMessagesByConversationId(id, userId);

        // Delete from Elasticsearch
        esConversationService.deleteConversation(id, userId);

        // Delete the conversation from MySQL
        return remove(new LambdaQueryWrapper<ConversationDO>()
                .eq(ConversationDO::getId, id)
                .eq(ConversationDO::getUserId, userId));
    }
} 