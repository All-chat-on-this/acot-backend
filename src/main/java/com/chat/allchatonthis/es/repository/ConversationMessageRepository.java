package com.chat.allchatonthis.es.repository;

import com.chat.allchatonthis.es.document.ConversationMessageDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationMessageRepository extends ElasticsearchRepository<ConversationMessageDocument, Long> {
    /**
     * Delete conversation messages by conversationId and userId (logical delete)
     */
    void deleteByConversationIdAndUserId(Long conversationId, Long userId);
} 