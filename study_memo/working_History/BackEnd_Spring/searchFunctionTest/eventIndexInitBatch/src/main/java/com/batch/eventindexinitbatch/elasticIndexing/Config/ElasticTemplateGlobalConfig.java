package com.batch.eventindexinitbatch.elasticIndexing.Config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.RefreshPolicy;

/**
 *
 * ElasticsearchTemplate 초기화 메서드
 */
@Configuration
public class ElasticTemplateGlobalConfig {
    private final ElasticsearchTemplate customElasticTemplate;

    @Autowired
    public ElasticTemplateGlobalConfig(ElasticsearchTemplate customElasticTemplate) {
        this.customElasticTemplate=customElasticTemplate;
    }

    public ElasticsearchTemplate customElasticTemplate(){
        customElasticTemplate.setRefreshPolicy(RefreshPolicy.IMMEDIATE);
        return customElasticTemplate;
    }
}
