package com.example.indexinitbatch.elasticIndexing.Config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MyJobConfig {
//    @Bean
//    public Job myJob(JobRepository jobRepository, @Qualifier("firstJob") Step firstJob, @Qualifier("secondJob") Step secondJob) {
//        return new JobBuilder("myJob", jobRepository)
//                .start(firstJob)
//                .next(secondJob)
//                .build();
//    }


}
