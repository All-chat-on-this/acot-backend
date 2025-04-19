package com.chat.allchatonthis.es.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Document(indexName = "conversation_message")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class ConversationMessageDocument {
    
    @Id
    private Long id;
    
    @Field(type = FieldType.Keyword)
    private Long conversationId;
    
    @Field(type = FieldType.Keyword)
    private Long userId;
    
    @Field(type = FieldType.Keyword)
    private String role;
    
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;
    
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String thinkingText;
    
    @Field(type = FieldType.Date)
    private LocalDateTime createTime;
    
    @Field(type = FieldType.Date)
    private LocalDateTime updateTime;
    
    @Field(type = FieldType.Boolean)
    private Boolean deleted;
} 