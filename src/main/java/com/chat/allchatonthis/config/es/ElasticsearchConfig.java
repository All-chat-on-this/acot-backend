package com.chat.allchatonthis.config.es;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.chat.allchatonthis.es.repository")
@EnableAsync
public class ElasticsearchConfig extends ElasticsearchConfiguration {
    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUri;

    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
                .connectedTo(elasticsearchUri)
                .build();
    }
}
