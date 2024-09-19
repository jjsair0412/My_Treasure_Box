package com.batch.eventindexinitbatch.elasticIndexing.Service.Reader;

import com.batch.eventindexinitbatch.elasticIndexing.Entity.InfoDto;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.context.annotation.Bean;

public interface RdbReader {
    @Bean
    JdbcPagingItemReader<InfoDto> selectInformation() throws Exception;
}
