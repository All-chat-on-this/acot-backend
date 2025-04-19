package com.chat.allchatonthis.es.repository;

import com.chat.allchatonthis.es.document.ConversationDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationRepository extends ElasticsearchRepository<ConversationDocument, Long> {
    
    /**
     * Find conversations by userId and deleted status
     */
    Page<ConversationDocument> findByUserIdAndDeleted(Long userId, Boolean deleted, Pageable pageable);
    
    /**
     * Search for conversations by title
     */
    Page<ConversationDocument> findByUserIdAndTitleContainingAndDeleted(Long userId, String title, Boolean deleted, Pageable pageable);
    
    /**
     * Delete conversation by id and userId
     */
    void deleteByIdAndUserId(Long id, Long userId);
}