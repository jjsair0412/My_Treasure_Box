package com.example.indexinitbatch.elasticIndexing.Service.Batch.Writer;

import com.example.indexinitbatch.elasticIndexing.Config.ElasticConfigure.Version_7.x.x.Version_7_ElasticGlobalConfig;
import com.example.indexinitbatch.elasticIndexing.Entity.Index.InfoDtoIndex;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Version_7_IndexWriterImpl implements IndexWriter{

    private final Version_7_ElasticGlobalConfig elasticTemplateGlobalConfig;
    @Bean
    @Override
    public ItemWriter<InfoDtoIndex> elasticSearchWriter() {
        RestHighLevelClient client = elasticTemplateGlobalConfig.elasticsearchClient();
        BulkRequest bulkRequest = new BulkRequest();

        return items -> {
            Gson gson = new Gson();

            for (InfoDtoIndex item : items) {
                IndexRequest indexRequest = new IndexRequest("info_index");
                indexRequest.source(gson.toJson(item), XContentType.JSON);

                bulkRequest.add(indexRequest);
            }

            client.bulk(bulkRequest, RequestOptions.DEFAULT);
        };
    }
}
