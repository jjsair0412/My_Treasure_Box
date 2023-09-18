package com.example.indexinitbatch.elasticIndexing.Service.Writer;

import com.example.indexinitbatch.elasticIndexing.Entity.InfoDtoIndex;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;

public interface IndexWriter {
    @Bean
    ItemWriter<InfoDtoIndex> elasticSearchWriter();
}
