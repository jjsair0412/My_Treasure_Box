package com.example.indexinitbatch.elasticIndexing.Service.Writer;

import com.example.indexinitbatch.elasticIndexing.Entity.InfoDtoIndex;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;

public interface IndexWriter {
    @Bean
    ItemWriter<InfoDtoIndex> elasticSearchWriter();
}
