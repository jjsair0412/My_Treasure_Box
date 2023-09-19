package com.example.indexinitbatch.elasticIndexing.Service.Reader;

import com.example.indexinitbatch.elasticIndexing.Entity.RepositoryDto.InfoDto;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.context.annotation.Bean;

import java.util.List;

public interface RdbReader {
    @Bean
    JdbcPagingItemReader<List<InfoDto>> selectInformation() throws Exception;
}
