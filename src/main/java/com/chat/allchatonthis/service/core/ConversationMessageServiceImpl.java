package com.chat.allchatonthis.service.core;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.allchatonthis.entity.dataobject.ConversationMessageDO;
import com.chat.allchatonthis.mapper.ConversationMessageMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ConversationMessageServiceImpl extends ServiceImpl<ConversationMessageMapper, ConversationMessageDO> implements ConversationMessageService {
} 