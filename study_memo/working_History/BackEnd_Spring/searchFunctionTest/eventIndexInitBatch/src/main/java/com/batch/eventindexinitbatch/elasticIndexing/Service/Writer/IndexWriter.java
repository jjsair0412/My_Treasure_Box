package com.batch.eventindexinitbatch.elasticIndexing.Service.Writer;

import com.batch.eventindexinitbatch.elasticIndexing.Entity.InfoDtoIndex;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;

public interface IndexWriter {
    @Bean
    ItemWriter<InfoDtoIndex> elasticSearchWriter();
}
