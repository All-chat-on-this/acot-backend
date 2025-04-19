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
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
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
    private final ElasticsearchRestTemplate elasticsearchRestTemplate;

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
            
            // Build a complex query that searches both conversation titles and message content/thinking text
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                    .must(QueryBuilders.termQuery("userId", userId))
                    .must(QueryBuilders.termQuery("deleted", false));
            
            // Search within conversation title
            BoolQueryBuilder shouldQuery = QueryBuilders.boolQuery()
                    .should(QueryBuilders.matchQuery("title", searchText));
            
            boolQuery.must(shouldQuery);
            
            // Create the search query
            NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                    .withQuery(boolQuery)
                    .withPageable(PageRequest.of(
                            reqVO.getPageNo() - 1,
                            reqVO.getPageSize(),
                            Sort.by(Sort.Direction.DESC, "updateTime")))
                    .build();
            
            // Execute the search
            Page<ConversationDocument> directResults = conversationRepository.findByUserIdAndTitleContainingAndDeleted(
                    userId, searchText, false, 
                    PageRequest.of(reqVO.getPageNo() - 1, reqVO.getPageSize(), Sort.by(Sort.Direction.DESC, "updateTime")));
            
            // If we have enough direct results, return them
            if (directResults.getTotalElements() >= reqVO.getPageSize()) {
                return new PageResult<>(directResults.getContent(), directResults.getTotalElements());
            }
            
            // Otherwise, find conversations that have matching messages
            BoolQueryBuilder messageQuery = QueryBuilders.boolQuery()
                    .must(QueryBuilders.termQuery("userId", userId))
                    .must(QueryBuilders.termQuery("deleted", false));
            
            // Search within message content and thinking text
            BoolQueryBuilder messageTextQuery = QueryBuilders.boolQuery()
                    .should(QueryBuilders.matchQuery("content", searchText))
                    .should(QueryBuilders.matchQuery("thinkingText", searchText));
            
            messageQuery.must(messageTextQuery);
            
            NativeSearchQuery messageSearchQuery = new NativeSearchQueryBuilder()
                    .withQuery(messageQuery)
                    .build();
            
            SearchHits<ConversationMessageDocument> messageHits = elasticsearchRestTemplate.search(
                    messageSearchQuery, ConversationMessageDocument.class);
            
            // Extract conversation IDs from message search results
            Set<Long> conversationIds = new HashSet<>();
            messageHits.forEach(hit -> conversationIds.add(hit.getContent().getConversationId()));
            
            // Add direct result IDs to the set
            directResults.forEach(doc -> conversationIds.add(doc.getId()));
            
            List<ConversationDocument> combinedResults = new ArrayList<>();
            
            // Add direct results first
            combinedResults.addAll(directResults.getContent());
            
            // If we still need more results, get conversations by the IDs found in message search
            if (combinedResults.size() < reqVO.getPageSize() && !conversationIds.isEmpty()) {
                // Remove IDs that are already in the direct results
                directResults.forEach(doc -> conversationIds.remove(doc.getId()));
                
                if (!conversationIds.isEmpty()) {
                    BoolQueryBuilder idQuery = QueryBuilders.boolQuery()
                            .must(QueryBuilders.termQuery("userId", userId))
                            .must(QueryBuilders.termQuery("deleted", false))
                            .must(QueryBuilders.termsQuery("_id", conversationIds));
                    
                    NativeSearchQuery idSearchQuery = new NativeSearchQueryBuilder()
                            .withQuery(idQuery)
                            .withPageable(PageRequest.of(0, reqVO.getPageSize() - combinedResults.size(), 
                                    Sort.by(Sort.Direction.DESC, "updateTime")))
                            .build();
                    
                    SearchHits<ConversationDocument> idSearchHits = elasticsearchRestTemplate.search(
                            idSearchQuery, ConversationDocument.class);
                    
                    idSearchHits.forEach(hit -> combinedResults.add(hit.getContent()));
                }
            }
            
            return new PageResult<>(combinedResults, (long) combinedResults.size());
        } catch (Exception e) {
            log.error("Error searching conversations in Elasticsearch", e);
            return new PageResult<>();
        }
    }
} 