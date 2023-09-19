package com.example.indexinitbatch.elasticIndexing.Config;

import com.example.indexinitbatch.elasticIndexing.Entity.RepositoryDto.InfoDto;
import com.example.indexinitbatch.elasticIndexing.Entity.Index.InfoDtoIndex;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
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

import java.util.List;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class ElasticBatchGlobalConfig {

    private final PlatformTransactionManager transactionManager;

    /**
     * 병렬처리 ( 최대 5개까지 병렬처리 )
     *
     * TaskExecutor 객체를 반환하면서 비동기 처리
     * - Step을 생성할 때 taskExecutor() 메서드 안에 사용가능
     * - SimpleAsyncTaskExecutor 는 TaskExecutor의 구현체 , 각 작업을 새로운 스레드에서 수행할 수 있게 함
     * - asyncTaskExecutor.setConcurrencyLimit(5) 로 동시에 수행할 작업을 5개로 제한,
     *   - 5개가 초과하는 추가 작업은 대기상태로 들어감
     */
    @Bean
    public TaskExecutor taskExecutor(){
        SimpleAsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor("spring_batch");
        asyncTaskExecutor.setConcurrencyLimit(5);
        return asyncTaskExecutor;
    }


    /**
     *
     * @param repository : 배치 메타데이터를 가지고있는 JobRepository
     * @param reader : 이전에 빈객체로 생성해둔 Reader를 @Qualifier("selectInformation") 로 주입
     * @param processor : 이전에 빈객체로 생성해둔 Processor를 @Qualifier("processor") 로 주입
     * @param writer : 이전에 빈객체로 생성해둔 Writer를 @Qualifier("elasticSearchWriter") 로 주입
     *
     * StepBuilder 반환
     * - "firstStep" 이라는 Step을 생성하기 위해 , StepBuilder 객채 생성 후 반환
     * - 파라미터로 Step 이름 및 JobRepository 주입
     *
     * chunk 설정
     * - 처리할 데이터 항목의 묶음 크기를 정의합니다. 여기서는 한 번에 하나의 항목만 처리하도록 설정.
     * - transactionManager는 이 chunk 처리 중에 트랜잭션 관리함
     *
     * reader , processor , writer 주입
     *
     * TaskExecutor 설정
     * - 병렬 처리를 위한 TaskExecutor 주입. Step을 병렬로 실행할 때 사용함
     */
    @Bean
    public Step firstStep(
            JobRepository repository,
            @Qualifier("selectInformation") JdbcPagingItemReader reader,
            @Qualifier("processor") ItemProcessor<List<InfoDto>, InfoDtoIndex> processor,
            @Qualifier("elasticSearchWriter") ItemWriter<InfoDtoIndex> writer){
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
     * @param jobRepository : 배치 메타데이터를 가지고있는 JobRepository
     * @param firstJob : 만들어두었던 Step 빈 객체 주입
     * JobBuilder 반환
     * - "myJob" 이라는 Job을 생성하기 위해 , JobBuilder 객채 생성 후 반환
     * - 파라미터에서 빈 객체 주입한 Step을 넣어줌
     * - Step의 순서를 나타내줄 수 있음
     *   - next() 메서드 안에 Step을 넣어주어서 , Step 순서 명시
     */
    @Bean
    public Job myJob(
            JobRepository jobRepository,
            @Qualifier("firstStep") Step firstJob
    ) {
        return new JobBuilder("myJob", jobRepository)
                .start(firstJob)
//                .next()
                .build();

    }
}
