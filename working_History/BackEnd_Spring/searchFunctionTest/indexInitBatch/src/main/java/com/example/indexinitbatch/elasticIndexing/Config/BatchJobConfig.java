package com.example.indexinitbatch.elasticIndexing.Config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BatchJobConfig {

    private final JobLauncher jobLauncher;

    @Qualifier("myJob") private final Job myjob;

    @Scheduled(cron = "0 * * * * *") // 초 분 시 일 월 요일 .. 매분 0초에 잡 수행
    public void runJob(){
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time",System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(myjob, jobParameters);
        } catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
}
