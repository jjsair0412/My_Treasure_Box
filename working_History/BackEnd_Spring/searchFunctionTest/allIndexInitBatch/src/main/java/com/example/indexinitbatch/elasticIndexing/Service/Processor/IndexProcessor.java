package com.example.indexinitbatch.elasticIndexing.Service.Processor;

import com.example.indexinitbatch.elasticIndexing.Entity.RepositoryDto.InfoDto;
import com.example.indexinitbatch.elasticIndexing.Entity.Index.InfoDtoIndex;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Bean;

import java.util.List;

public interface IndexProcessor {
    @Bean
    ItemProcessor<List<InfoDto>, InfoDtoIndex> processor();
}
