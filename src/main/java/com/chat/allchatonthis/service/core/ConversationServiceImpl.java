package com.chat.allchatonthis.service.core;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.allchatonthis.entity.dataobject.ConversationDO;
import com.chat.allchatonthis.mapper.ConversationMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, ConversationDO> implements ConversationService {
} 