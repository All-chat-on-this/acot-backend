package com.chat.allchatonthis.controller;

import com.chat.allchatonthis.common.pojo.CommonResult;
import com.chat.allchatonthis.common.util.object.BeanUtils;
import com.chat.allchatonthis.common.util.security.LoginUser;
import com.chat.allchatonthis.entity.dataobject.ConversationMessageDO;
import com.chat.allchatonthis.entity.vo.conversation.ConversationMessageCreateReqVO;
import com.chat.allchatonthis.entity.vo.conversation.ConversationMessageRespVO;
import com.chat.allchatonthis.entity.vo.conversation.ConversationSendMessageReqVO;
import com.chat.allchatonthis.service.core.ConversationMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Conversation Message Controller
 */
@RestController
@RequestMapping("/conversation/message")
@RequiredArgsConstructor
public class ConversationMessageController {

    private final ConversationMessageService conversationMessageService;

    /**
     * Get all messages for a conversation
     */
    @GetMapping("/getMessages")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<List<ConversationMessageRespVO>> getMessages(@RequestParam Long conversationId, @LoginUser Long userId) {
        List<ConversationMessageDO> messages = conversationMessageService.getMessages(conversationId, userId);
        return CommonResult.success(BeanUtils.toBean(messages, ConversationMessageRespVO.class));
    }

    /**
     * Get a specific message by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<ConversationMessageRespVO> getMessage(@PathVariable Long id, @LoginUser Long userId) {
        ConversationMessageDO message = conversationMessageService.getMessage(id, userId);
        return CommonResult.success(BeanUtils.toBean(message, ConversationMessageRespVO.class));
    }

    /**
     * Create a new message
     */
    @PostMapping("/createMessage")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<ConversationMessageRespVO> createMessage(@RequestBody ConversationMessageCreateReqVO reqVO, @LoginUser Long userId) {
        ConversationMessageDO message = BeanUtils.toBean(reqVO, ConversationMessageDO.class);
        message = conversationMessageService.createMessage(message, userId);
        return CommonResult.success(BeanUtils.toBean(message, ConversationMessageRespVO.class));
    }

    /**
     * Send a message and get AI response
     */
    @PostMapping("/sendMessage")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<ConversationMessageRespVO> sendMessage(@RequestBody ConversationSendMessageReqVO reqVO, @LoginUser Long userId) {
        ConversationMessageDO responseMessage = conversationMessageService.sendMessage(reqVO.getMessage(), reqVO.getConfigId(), reqVO.getConversationId(), userId);
        return CommonResult.success(BeanUtils.toBean(responseMessage, ConversationMessageRespVO.class));
    }

    /**
     * Delete a message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Boolean> deleteMessage(@PathVariable Long id, @LoginUser Long userId) {
        boolean result = conversationMessageService.deleteMessage(id, userId);
        return CommonResult.success(result);
    }

    /**
     * Delete all messages for a conversation
     */
    @DeleteMapping("/conversation/{conversationId}")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Boolean> deleteConversationMessages(@PathVariable Long conversationId, @LoginUser Long userId) {
        boolean result = conversationMessageService.deleteMessagesByConversationId(conversationId, userId);
        return CommonResult.success(result);
    }
} 