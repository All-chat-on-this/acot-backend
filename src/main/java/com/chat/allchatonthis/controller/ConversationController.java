package com.chat.allchatonthis.controller;

import com.chat.allchatonthis.common.pojo.CommonResult;
import com.chat.allchatonthis.common.pojo.PageParam;
import com.chat.allchatonthis.common.pojo.PageResult;
import com.chat.allchatonthis.common.util.object.BeanUtils;
import com.chat.allchatonthis.common.util.security.LoginUser;
import com.chat.allchatonthis.entity.dataobject.ConversationDO;
import com.chat.allchatonthis.entity.vo.conversation.ConversationCreateOrUpdateReqVO;
import com.chat.allchatonthis.entity.vo.conversation.ConversationPageReqVO;
import com.chat.allchatonthis.entity.vo.conversation.ConversationRespVO;
import com.chat.allchatonthis.service.core.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Conversation Controller
 */
@RestController
@RequestMapping("/conversation")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    /**
     * Get all conversations for the current user
     */
    @GetMapping("/getConversations")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<List<ConversationRespVO>> getConversations(@LoginUser Long userId) {
        List<ConversationDO> conversations = conversationService.getConversations(userId);
        return CommonResult.success(BeanUtils.toBean(conversations, ConversationRespVO.class));
    }
    
    /**
     * Get conversations for the current user with pagination
     */
    @GetMapping("/page")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<PageResult<ConversationRespVO>> getConversationPage(ConversationPageReqVO pageVO, @LoginUser Long userId) {
        PageResult<ConversationDO> pageResult = conversationService.getConversationPage(userId, pageVO);
        return CommonResult.success(BeanUtils.toBean(pageResult, ConversationRespVO.class));
    }

    /**
     * Get a specific conversation by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<ConversationRespVO> getConversation(@PathVariable Long id, @LoginUser Long userId) {
        ConversationDO conversation = conversationService.getConversation(id, userId);
        return CommonResult.success(BeanUtils.toBean(conversation, ConversationRespVO.class));
    }

    /**
     * Create a new conversation
     */
    @PostMapping("/createConversation")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<ConversationRespVO> createConversation(@RequestBody ConversationCreateOrUpdateReqVO reqVO, @LoginUser Long userId) {
        ConversationDO conversation = BeanUtils.toBean(reqVO, ConversationDO.class);
        conversation = conversationService.createConversation(conversation, userId);
        return CommonResult.success(BeanUtils.toBean(conversation, ConversationRespVO.class));
    }

    /**
     * Update an existing conversation
     */
    @PutMapping("/updateConversation/{id}")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<ConversationRespVO> updateConversation(@PathVariable Long id, @RequestBody ConversationCreateOrUpdateReqVO reqVO, 
                                                   @LoginUser Long userId) {
        ConversationDO conversation = BeanUtils.toBean(reqVO, ConversationDO.class);
        conversation = conversationService.updateConversation(id, conversation, userId);
        return CommonResult.success(BeanUtils.toBean(conversation, ConversationRespVO.class));
    }

    /**
     * Delete a conversation
     */
    @DeleteMapping("/conversations/{id}")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Boolean> deleteConversation(@PathVariable Long id, @LoginUser Long userId) {
        boolean result = conversationService.deleteConversation(id, userId);
        return CommonResult.success(result);
    }
} 