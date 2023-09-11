package com.example.indexinitbatch.elasticIndexing.Service.Reader;

import com.example.indexinitbatch.elasticIndexing.Entity.InfoDto;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.context.annotation.Bean;

public interface RdbReader {
    @Bean
    JdbcPagingItemReader<InfoDto> selectInformation() throws Exception;
}
