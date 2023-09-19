package com.example.indexinitbatch.elasticIndexing.Reader;

import com.example.indexinitbatch.elasticIndexing.Entity.RepositoryDto.InfoDto;
import com.example.indexinitbatch.elasticIndexing.Service.Reader.RdbReader;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RdbReaderImplTest {

    @Autowired private RdbReader repository;

    @Test
    void 페이징_테스트() throws Exception {
        JdbcPagingItemReader<InfoDto> reader = repository.selectInformation();

        InfoDto info;
        while ((info=reader.read())!=null) {
            System.out.println(info.getFirstInfoId()); // or use a logger
            System.out.println(info.getName()); // or use a logger
            System.out.println(info.getCategory()); // or use a logger
            System.out.println(info.getAge()); // or use a logger

            System.out.println("---------------");
        }
    }
}