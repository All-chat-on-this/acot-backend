package com.chat.allchatonthis.service.core.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.allchatonthis.common.exception.ServiceException;
import com.chat.allchatonthis.common.pojo.PageResult;
import com.chat.allchatonthis.entity.dataobject.ConversationDO;
import com.chat.allchatonthis.entity.dataobject.ConversationMessageDO;
import com.chat.allchatonthis.entity.vo.conversation.ConversationPageReqVO;
import com.chat.allchatonthis.mapper.ConversationMapper;
import com.chat.allchatonthis.mapper.ConversationMessageMapper;
import com.chat.allchatonthis.service.core.ConversationService;
import com.chat.allchatonthis.service.core.spi.ConversationPageProviderManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.chat.allchatonthis.common.enums.ErrorCodeConstants.CONVERSATION_NOT_EXISTS;

@Service
@AllArgsConstructor
@Slf4j
@CacheConfig(cacheNames = "conversation")
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, ConversationDO> implements ConversationService {

    private final ConversationMessageMapper conversationMessageMapper;
    private final ConversationPageProviderManager conversationPageProviderManager;

    @Override
    @Cacheable(key = "'list:' + #userId")
    public List<ConversationDO> getConversations(Long userId) {
        return list(new LambdaQueryWrapper<ConversationDO>()
                .eq(ConversationDO::getUserId, userId));
    }

    @Override
    public PageResult<ConversationDO> getConversationPage(Long userId, ConversationPageReqVO conversationPageReqVO) {
        // Page requests are not cached as they depend on dynamic parameters
        return conversationPageProviderManager.getConversationPage(userId, conversationPageReqVO);
    }

    @Override
    @Cacheable(key = "'id:' + #id + ':user:' + #userId")
    public ConversationDO getConversation(Long id, Long userId) {
        return getOne(new LambdaQueryWrapper<ConversationDO>()
                .eq(ConversationDO::getId, id)
                .eq(ConversationDO::getUserId, userId));
    }

    @Override
    @Transactional
    @CacheEvict(key = "'list:' + #userId")
    public ConversationDO createConversation(ConversationDO conversation, Long userId) {
        // Set the user ID for this conversation
        conversation.setUserId(userId);

        // Save the conversation
        save(conversation);

        return conversation;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(key = "'list:' + #userId"),
            @CacheEvict(key = "'id:' + #id + ':user:' + #userId")
    })
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

        return conversation;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(key = "'list:' + #userId"),
            @CacheEvict(key = "'id:' + #id + ':user:' + #userId")
    })
    public boolean deleteConversation(Long id, Long userId) {
        // First, delete all messages in the conversation
        ConversationDO conversation = getConversation(id, userId);
        if (conversation == null) {
            return false;
        }

        conversationMessageMapper.delete(new LambdaQueryWrapper<ConversationMessageDO>()
                .eq(ConversationMessageDO::getConversationId, id));

        // Delete the conversation from database
        return remove(new LambdaQueryWrapper<ConversationDO>()
                .eq(ConversationDO::getId, id)
                .eq(ConversationDO::getUserId, userId));
    }
} 