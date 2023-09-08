package com.example.indexinitbatch.elasticIndexing.Service;

import com.example.indexinitbatch.elasticIndexing.Entity.InfoDto;
import com.example.indexinitbatch.elasticIndexing.Repository.RdbRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Configuration
@Service
public class SimpleJobConfiguration {

    private final RdbRepository repository;
    @Bean
    public ItemReader<String> readDatabase(){
        return new ItemReader<String>() {
            @Override
            public String read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
                for (InfoDto infoDto : repository.selectInformation()) {

                }

                return "Read OK";
            }
        };
    }
}
