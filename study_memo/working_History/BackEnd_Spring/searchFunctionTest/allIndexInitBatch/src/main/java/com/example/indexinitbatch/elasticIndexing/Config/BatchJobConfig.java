package com.example.indexinitbatch.elasticIndexing.Config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BatchJobConfig {


    /**
     * JobLauncher ,  @Qualifier("myJob") private final Job myjob
     * - JobLauncher 사용하도록 주입
     * - 미리 생성해둔 Job 빈 객체를 @Qualifier("myJob") 으로 주입
     */
    private final JobLauncher jobLauncher;

    @Qualifier("myJob")
    private final Job myjob;

    @Scheduled(cron = "0 * * * * *") // 초 분 시 일 월 요일 .. 매분 0초에 잡 수행
    public void runJob(){
        try {
            /**
             * 모든 Job을 대상으로 새로운 JobParameter 를 생성하게끔 하여 , 동시성이슈 해결
             *
             * System.currentTimeMillis()를 사용하여 현재 시간을 파라미터로 추가함으로써
             * 동일한 잡도 다양한 파라미터로 여러 번 실행될 수 있도록 함.
             *
             * 이는 잡의 동시성 문제를 해결하는 데 도움이 됨.
             */
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time",System.currentTimeMillis())
                    .toJobParameters();

            /**
             * Job , JobParameters 파라미터로 받아서 jobLauncher.run() 메서드 호출하여 Job 수행
             */
            jobLauncher.run(myjob, jobParameters);
        } catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
}
