# Batch JobExecutionListener
## Overview
SpringBatch에서 모든 작업 **( Reader , Proccesser , Writer )** 이 수행된 이후 또는 작업 수행 전 특정 로직을 수행하는 방법에 대해 기술합니다.

## 사용 interface
두가지중 한가지를 사용하면 됩니다.
### 1. JobExecutionListener
- 전체 Job의 시작과 완료 시에 실행될 로직을 지정할 수 있습니다.
    - 메서드 종류 : **beforeJob** , **afterJob**

### 2. StepExecutionListener
- 개별 Step의 시작과 완료 시에 실행될 로직을 지정할 수 있습니다.
    - 메서드 종류 : **beforeStep** , **afterStep**

## 사용방안
위 인터페이스 두개 중 한개를 재 정의하여 사용합니다.

### 1. JobExecutionListener
```jobExecution.getExitStatus()``` 또는 ```jobExecution.getStatus()``` 메서드로 batch 작업의 완료상태를 체크할 수 있습니다.

- ```jobExecution.getExitStatus()```
    - ```ExitStatus``` 를 반환하고, ```COMPLETED``` 또는 ```FAILED``` 의 상태값을 가짐
- ```jobExecution.getStatus()``` 
    - ```COMPLETED```, ```STARTED```, ```FAILED``` 등의 상태값을 가짐



코드상 사용 사례
```java
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

public class MyJobListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        // Job 시작 전에 실행될 로직
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        // Job 완료 후에 실행될 로직
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            // Job이 정상적으로 완료되었을 때 실행될 로직
            System.out.println("Job completed successfully!");
            // 여기에 원하는 추가 로직을 넣습니다.
        }
    }
}
```

인터페이스로 구현체를 생성한 뒤 , 해당 리스너를 SpringBatchJob에 추가합니다.
```java
@Bean
public Job myJob() {
    return jobBuilderFactory.get("myJob")
            .listener(new MyJobListener()) // 리스너 추가
            .start(myStep())
            .build();
}
```
### 2. StepExecutionListener
- StepExecution 또한 JobExecution과 비슷하게 사용하면 됩니다.
```java
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

public class coustomStepExecution implements StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        if(stepExecution.getStatus() == BatchStatus.COMPLETED) {
            // Step 수행 전 수행 로직 - 성공
        } else if (stepExecution.getStatus() == BatchStatus.FAILED) {
            // Step 수행 전 수행 로직 - 실패
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.getStatus() == BatchStatus.COMPLETED) {
            // Step 수행 후 수행 로직 - 성공
        } else if (stepExecution.getStatus() == BatchStatus.FAILED) {
            // Step 수행 후 수행 로직 - 실패
        }
        return ExitStatus.COMPLETED; // ExitStatus 반환 해야 함
    }
}
```java