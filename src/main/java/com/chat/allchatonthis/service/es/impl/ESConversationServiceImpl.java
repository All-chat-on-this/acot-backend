package com.chat.allchatonthis.service.es.impl;

import com.chat.allchatonthis.common.pojo.PageResult;
import com.chat.allchatonthis.entity.dataobject.ConversationDO;
import com.chat.allchatonthis.entity.dataobject.ConversationMessageDO;
import com.chat.allchatonthis.entity.vo.conversation.ConversationPageReqVO;
import com.chat.allchatonthis.es.document.ConversationDocument;
import com.chat.allchatonthis.es.document.ConversationMessageDocument;
import com.chat.allchatonthis.es.mapper.ESEntityMapper;
import com.chat.allchatonthis.es.repository.ConversationMessageRepository;
import com.chat.allchatonthis.es.repository.ConversationRepository;
import com.chat.allchatonthis.service.es.ESConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ESConversationServiceImpl implements ESConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    @Async
    public ConversationDocument saveConversation(ConversationDO conversation) {
        try {
            ConversationDocument document = ESEntityMapper.toConversationDocument(conversation);
            if (document == null) {
                return null;
            }

            return conversationRepository.save(document);
        } catch (Exception e) {
            log.error("Error saving conversation to Elasticsearch", e);
            return null;
        }
    }

    @Override
    @Async
    public void saveConversationMessage(ConversationMessageDO message, Long userId) {
        try {
            ConversationMessageDocument document = ESEntityMapper.toConversationMessageDocument(message, userId);
            if (document == null) {
                return;
            }

            messageRepository.save(document);
        } catch (Exception e) {
            log.error("Error saving conversation message to Elasticsearch", e);
        }
    }

    @Override
    @Async
    public ConversationDocument updateConversation(ConversationDO conversation) {
        try {
            // Check if conversation exists in ES
            if (!conversationRepository.existsById(conversation.getId())) {
                return saveConversation(conversation);
            }

            ConversationDocument document = ESEntityMapper.toConversationDocument(conversation);
            if (document == null) {
                return null;
            }

            return conversationRepository.save(document);
        } catch (Exception e) {
            log.error("Error updating conversation in Elasticsearch", e);
            return null;
        }
    }

    @Override
    @Async
    public void deleteConversation(Long id, Long userId) {
        try {
            conversationRepository.deleteByIdAndUserId(id, userId);
            deleteConversationMessages(id, userId);
        } catch (Exception e) {
            log.error("Error deleting conversation from Elasticsearch", e);
        }
    }

    @Override
    @Async
    public void deleteConversationMessage(Long id) {
        try {
            messageRepository.deleteById(id);
        } catch (Exception e) {
            log.error("Error deleting conversation message from Elasticsearch", e);
        }
    }

    @Override
    @Async
    public void deleteConversationMessages(Long conversationId, Long userId) {
        try {
            messageRepository.deleteByConversationIdAndUserId(conversationId, userId);
        } catch (Exception e) {
            log.error("Error deleting conversation messages from Elasticsearch", e);
        }
    }

    @Override
    public PageResult<ConversationDocument> searchConversations(Long userId, ConversationPageReqVO reqVO) {
        try {
            String searchText = reqVO.getSearchText();

            // If no search text is provided, return regular paginated results
            if (!StringUtils.hasText(searchText)) {
                PageRequest pageRequest = PageRequest.of(
                        reqVO.getPageNo() - 1,
                        reqVO.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "updateTime"));

                Page<ConversationDocument> page = conversationRepository.findByUserIdAndDeleted(
                        userId, false, pageRequest);

                return new PageResult<>(page.getContent(), page.getTotalElements());
            }

            // Search directly in titles first
            Page<ConversationDocument> directResults = conversationRepository.findByUserIdAndTitleContainingAndDeleted(
                    userId, searchText, false,
                    PageRequest.of(reqVO.getPageNo() - 1, reqVO.getPageSize(), Sort.by(Sort.Direction.DESC, "updateTime")));

            // If we have enough direct results, return them
            if (directResults.getTotalElements() >= reqVO.getPageSize()) {
                return new PageResult<>(directResults.getContent(), directResults.getTotalElements());
            }

            // Otherwise, find conversations that have matching messages
            Criteria messageUserCriteria = new Criteria("userId").is(userId);
            Criteria messageDeletedCriteria = new Criteria("deleted").is(false);

            // Create criteria for content and thinking text
            Criteria contentCriteria = new Criteria("content").contains(searchText);
            Criteria thinkingCriteria = new Criteria("thinkingText").contains(searchText);

            // Combine criteria
            Criteria finalCriteria = messageUserCriteria.and(messageDeletedCriteria).and(
                    new Criteria().or(contentCriteria).or(thinkingCriteria));

            // Build the query
            Query messageQuery = new CriteriaQuery(finalCriteria);

            // Execute the search
            SearchHits<ConversationMessageDocument> messageHits = elasticsearchOperations.search(
                    messageQuery, ConversationMessageDocument.class);

            // Extract conversation IDs from message search results
            Set<Long> conversationIds = new HashSet<>();
            for (SearchHit<ConversationMessageDocument> hit : messageHits.getSearchHits()) {
                conversationIds.add(hit.getContent().getConversationId());
            }

            // Add direct result IDs to the set
            for (ConversationDocument doc : directResults.getContent()) {
                conversationIds.add(doc.getId());
            }

            List<ConversationDocument> combinedResults = new ArrayList<>(directResults.getContent());

            // If we still need more results, get conversations by the IDs found in message search
            if (combinedResults.size() < reqVO.getPageSize() && !conversationIds.isEmpty()) {
                // Remove IDs that are already in the direct results
                for (ConversationDocument doc : directResults.getContent()) {
                    conversationIds.remove(doc.getId());
                }

                if (!conversationIds.isEmpty()) {
                    // Get additional conversations
                    List<ConversationDocument> additionalConversations = new ArrayList<>();

                    // We'll do it in batches to avoid too large queries
                    List<Long> idsList = new ArrayList<>(conversationIds);
                    for (int i = 0; i < idsList.size(); i += 100) {
                        int end = Math.min(i + 100, idsList.size());
                        List<Long> batchIds = idsList.subList(i, end);

                        // Get conversations for this batch of IDs
                        List<ConversationDocument> batch = new ArrayList<>();
                        conversationRepository.findAllById(batchIds).forEach(conversationDocument -> {
                            if (conversationDocument.getUserId().equals(userId) && !conversationDocument.getDeleted()) {
                                batch.add(conversationDocument);
                            }
                        });

                        additionalConversations.addAll(batch);

                        if (combinedResults.size() + additionalConversations.size() >= reqVO.getPageSize()) {
                            break;
                        }
                    }

                    // Sort by update time
                    additionalConversations.sort((a, b) -> b.getUpdateTime().compareTo(a.getUpdateTime()));

                    // Add to results (up to page size limit)
                    int remaining = reqVO.getPageSize() - combinedResults.size();
                    if (additionalConversations.size() > remaining) {
                        additionalConversations = additionalConversations.subList(0, remaining);
                    }

                    combinedResults.addAll(additionalConversations);
                }
            }

            return new PageResult<>(combinedResults, (long) combinedResults.size());
        } catch (Exception e) {
            log.error("Error searching conversations in Elasticsearch", e);
            return new PageResult<>();
        }
    }
} 