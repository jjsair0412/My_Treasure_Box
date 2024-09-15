package com.batch.eventindexinitbatch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableBatchProcessing
public class EventIndexInitBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventIndexInitBatchApplication.class, args);
    }

}
