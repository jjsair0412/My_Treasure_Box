package com.batch.eventindexinitbatch.elasticIndexing.Service.Processor;

import com.batch.eventindexinitbatch.elasticIndexing.Entity.InfoDto;
import com.batch.eventindexinitbatch.elasticIndexing.Entity.InfoDtoIndex;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Bean;

public interface IndexProcessor {
    @Bean
    ItemProcessor<InfoDto, InfoDtoIndex> processor();
}
