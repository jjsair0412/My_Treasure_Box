package com.example.indexinitbatch.elasticIndexing.Service.Processor;

import com.example.indexinitbatch.elasticIndexing.Entity.InfoDto;
import com.example.indexinitbatch.elasticIndexing.Entity.InfoDtoIndex;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Bean;

public interface IndexProcessor {
    @Bean
    ItemProcessor<InfoDto, InfoDtoIndex> processor();
}
