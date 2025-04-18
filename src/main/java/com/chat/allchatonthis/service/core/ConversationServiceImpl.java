package com.chat.allchatonthis.service.core;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.allchatonthis.common.exception.ServiceException;
import com.chat.allchatonthis.common.pojo.PageParam;
import com.chat.allchatonthis.common.pojo.PageResult;
import com.chat.allchatonthis.entity.dataobject.ConversationDO;
import com.chat.allchatonthis.entity.vo.conversation.ConversationPageReqVO;
import com.chat.allchatonthis.mapper.ConversationMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.chat.allchatonthis.common.enums.ErrorCodeConstants.CONVERSATION_NOT_EXISTS;

@Service
@AllArgsConstructor
@Slf4j
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, ConversationDO> implements ConversationService {

    @Override
    public List<ConversationDO> getConversations(Long userId) {
        return list(new LambdaQueryWrapper<ConversationDO>()
                .eq(ConversationDO::getUserId, userId)
                .orderByDesc(ConversationDO::getUpdateTime));
    }
    
    @Override
    public PageResult<ConversationDO> getConversationPage(Long userId, PageParam pageParam) {
        // Handle ConversationPageReqVO if applicable
        String title = null;
        if (pageParam instanceof ConversationPageReqVO) {
            title = ((ConversationPageReqVO) pageParam).getTitle();
        }
        
        // Create pagination parameters
        Page<ConversationDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        
        // Build query wrapper
        LambdaQueryWrapper<ConversationDO> queryWrapper = new LambdaQueryWrapper<ConversationDO>()
                .eq(ConversationDO::getUserId, userId)
                .like(StringUtils.hasText(title), ConversationDO::getTitle, title)
                .orderByDesc(ConversationDO::getUpdateTime);
        
        // Execute paginated query
        IPage<ConversationDO> resultPage = page(page, queryWrapper);
        
        // Return the paginated result
        return new PageResult<>(resultPage.getRecords(), resultPage.getTotal());
    }

    @Override
    public ConversationDO getConversation(Long id, Long userId) {
        return getOne(new LambdaQueryWrapper<ConversationDO>()
                .eq(ConversationDO::getId, id)
                .eq(ConversationDO::getUserId, userId));
    }

    @Override
    public ConversationDO createConversation(ConversationDO conversation, Long userId) {
        // Set the user ID for this conversation
        conversation.setUserId(userId);
        
        // Save the conversation
        save(conversation);
        return conversation;
    }

    @Override
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
    public boolean deleteConversation(Long id, Long userId) {
        return remove(new LambdaQueryWrapper<ConversationDO>()
                .eq(ConversationDO::getId, id)
                .eq(ConversationDO::getUserId, userId));
    }
} 