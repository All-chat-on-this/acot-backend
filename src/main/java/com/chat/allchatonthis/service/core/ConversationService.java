package com.chat.allchatonthis.service.core;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chat.allchatonthis.common.pojo.PageParam;
import com.chat.allchatonthis.common.pojo.PageResult;
import com.chat.allchatonthis.entity.dataobject.ConversationDO;
import com.chat.allchatonthis.entity.vo.conversation.ConversationPageReqVO;

import java.util.List;

public interface ConversationService extends IService<ConversationDO> {
    
    /**
     * Get all conversations for the current user
     * 
     * @param userId The user ID
     * @return List of conversations
     */
    List<ConversationDO> getConversations(Long userId);
    
    /**
     * Get conversations for the current user with pagination
     * 
     * @param userId The user ID
     * @param pageParam Pagination parameters
     * @return Paginated list of conversations
     */
    PageResult<ConversationDO> getConversationPage(Long userId, PageParam pageParam);
    
    /**
     * Get a specific conversation by ID
     * 
     * @param id Conversation ID
     * @param userId User ID for security check
     * @return The conversation or null if not found
     */
    ConversationDO getConversation(Long id, Long userId);
    
    /**
     * Create a new conversation
     * 
     * @param conversation Conversation to create
     * @param userId User ID who owns this conversation
     * @return The created conversation with ID
     */
    ConversationDO createConversation(ConversationDO conversation, Long userId);
    
    /**
     * Update an existing conversation
     * 
     * @param id Conversation ID to update
     * @param conversation Updated conversation data
     * @param userId User ID for security check
     * @return The updated conversation
     */
    ConversationDO updateConversation(Long id, ConversationDO conversation, Long userId);
    
    /**
     * Delete a conversation
     * 
     * @param id Conversation ID to delete
     * @param userId User ID for security check
     * @return true if deleted successfully
     */
    boolean deleteConversation(Long id, Long userId);
} 