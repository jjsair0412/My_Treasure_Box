package com.example.indexinitbatch.elasticIndexing.Config.ElasticConfigure.Version_7.x.x;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;

/**
 *
 * ElasticsearchTemplate 초기화 메서드
 *
 * ElasticSearch 버전이 7.x.x 일 때 해당
 */
@Configuration
public class Version_7_ElasticGlobalConfig extends AbstractElasticsearchConfiguration {
    @Override
    public RestHighLevelClient elasticsearchClient() {
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo("localhost:9200")
                .withBasicAuth("test","test1234")
                .build();
        return RestClients.create(clientConfiguration).rest();
    }

}
