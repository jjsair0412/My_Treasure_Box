package com.example.indexinitbatch.elasticIndexing.Config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class MyStepConfig {

    private final PlatformTransactionManager transactionManager;
//    @Bean
//    public Step firstStep(JobRepository repository){
//        return new StepBuilder("firstStep",repository)
//                .<String, String>chunk(1000, transactionManager)
//                .reader()
//                .writer()
//                .build();
//    }
}
