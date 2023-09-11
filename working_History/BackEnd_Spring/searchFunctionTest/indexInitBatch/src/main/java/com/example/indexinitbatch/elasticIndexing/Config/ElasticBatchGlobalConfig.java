package com.example.indexinitbatch.elasticIndexing.Config;

import com.example.indexinitbatch.elasticIndexing.Entity.InfoDto;
import com.example.indexinitbatch.elasticIndexing.Entity.InfoDtoIndex;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.BatchConfigurationException;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class ElasticBatchGlobalConfig {

    private final PlatformTransactionManager transactionManager;

    /**
     *
     * @return
     *
     * 병렬처리 ( 최대 5개까지 병렬처리 )
     */
    @Bean
    public TaskExecutor taskExecutor(){
        SimpleAsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor("spring_batch");
        asyncTaskExecutor.setConcurrencyLimit(5);
        return asyncTaskExecutor;
    }

    /**
     *
     * @param repository
     * @param reader
     * @param processor
     * @param writer
     * @return
     *
     * 스탭 생성
     */
    @Bean
    public Step firstStep(
            JobRepository repository,
            @Qualifier("selectInformation") JdbcPagingItemReader reader,
            @Qualifier("processor") ItemProcessor<InfoDto, InfoDtoIndex> processor,
            @Qualifier("elasticSearchWriter")ItemWriter<InfoDtoIndex> writer){
        return new StepBuilder("firstStep",repository)
                .<InfoDto, InfoDtoIndex>chunk(1, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .taskExecutor(taskExecutor())
                .build();
    }


    /**
     *
     * @param jobRepository
     * @param firstJob
     * @return
     *
     * job 생성
     */
    @Bean
    public Job myJob(
            JobRepository jobRepository,
            @Qualifier("firstStep") Step firstJob
    ) {
        return new JobBuilder("myJob", jobRepository)
                .start(firstJob)
                .build();
    }
}
