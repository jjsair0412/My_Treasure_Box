package com.example.indexinitbatch.elasticIndexing.Config.ElasticConfigure.Version_7.x.x;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.http.HttpHeaders;

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

        HttpHeaders defaultHeaders = new HttpHeaders();
        defaultHeaders.set("Connection", "keep-alive");

        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo("localhost:9200")
                .withBasicAuth("test","test1234")
                .withSocketTimeout(15000)  // 15초로 설정
                .withConnectTimeout(10000)  // 10초로 설정
                .withDefaultHeaders(defaultHeaders)  // keep-alive 헤더 설정
                .build();

        return RestClients.create(clientConfiguration).rest();
    }

}
