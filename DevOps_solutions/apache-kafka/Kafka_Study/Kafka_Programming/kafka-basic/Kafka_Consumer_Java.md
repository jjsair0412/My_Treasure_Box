# Kafka Consumer Java

## Dependencies
아래 디팬던시 필요
- ***Kafka-clients*** : Kafka 연결사용
- ***slf4j-*** : 로깅용

```bash
dependencies {
    // https://mvnrepository.com/artifact/org.apache.kafka/kafka-clients
    implementation 'org.apache.kafka:kafka-clients:3.1.0'
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    implementation 'org.slf4j:slf4j-api:1.7.36'
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
    implementation 'org.slf4j:slf4j-simple:1.7.36'

}
```


## 1. Kafka consumer 만들기
Kafka consumer를 만들기 위해선 , 아래 단계를 거쳐야 합니다.

1. create Producer Properties
2. create the consumer
3. consumer poll

**아래 코드와 주석을 참고합니다.**
- [ConsumerDemo 예제 코드](./src/main/java/io/Conduktor/demos/kafka/ConsumerDemo.java)

로그를 확인해보면 , 아래 순서로 consumer가 동작한다는것을 알 수 있습니다.

1. Consumer Group에 join
위 코드를 살펴보면 , 처음 properties에 group.id를 지정해 줍니다.
- 이것은 Consumer Group Id를 의미하며 , 처음 consumer가 생성될 때 , 해당 group Id의 이름을 가진 consumer group을 생성하고 join 합니다.

```java
String groupId = "my-java-appilcation";
...
properties.setProperty("group.id",groupId);
```

consumer group join log
```logcatfilter
(Re-)joining group
Successfully joined group with generation Generation{generationId=1, memberId='consumer-my-java-appilcation-1-dbc5987c-a9fc-4a98-9f41-7b40fa7c67b5', protocol='range'}
...
```

2. offset 관리
해당 코드에선 ```auto.offset.reset``` 옵션으로 ```earliest``` 를 두었습니다.

consumer group에 join된 이후 , consumer는 offset이 어떻게 설정되어있는지 확인한 이후 , 해당 예제처럼 설정된 정보가 없다면 ```auto.offset.reset``` 를 따릅니다.

로그에서 확인할 수 있습니다.
```logcatfilter
...
Resetting offset for partition demo_java-2 to position FetchPosition{offset=0, offsetEpoch=Optional.empty, currentLeader=LeaderAndEpoch{leader=Optional[cluster.playground.cdkt.io:9107 (id: 22 rack: 1)], epoch=0}}.
Resetting offset for partition demo_java-0 to position FetchPosition{offset=0, offsetEpoch=Optional.empty, currentLeader=LeaderAndEpoch{leader=Optional[cluster.playground.cdkt.io:9118 (id: 32 rack: 2)], epoch=0}}.
Resetting offset for partition demo_java-1 to position FetchPosition{offset=0, offsetEpoch=Optional.empty, currentLeader=LeaderAndEpoch{leader=Optional[cluster.playground.cdkt.io:9127 (id: 6 rack: 0)], epoch=0}}.
...
```

3. data poll
offset을 earliset 옵션으로 모든 파티션에 대해 0으로 초기화한 이후 부터 , 0번째 offset 데이터를 poll해오기 시작합니다.
- 가능할 경우 broker에게 1MB 데이터를 한번에 가져올 수 있습니다.

```logcatfilter
[main] INFO ConsumerDemo - Polling
[main] INFO ConsumerDemo - KEY : null Value : hello world12
[main] INFO ConsumerDemo - Partition : 0 Offset : 0
[main] INFO ConsumerDemo - KEY : null Value : hello world13
[main] INFO ConsumerDemo - Partition : 0 Offset : 1
[main] INFO ConsumerDemo - KEY : null Value : hello world14
[main] INFO ConsumerDemo - Partition : 0 Offset : 2
[main] INFO ConsumerDemo - KEY : null Value : hello world15
...
```

### consumer 재 수행
만약 consumer group에 join된 이후 consumer를 재 시작하면 , consumer group에 Rejoin하게 되고 , 고 사이에 들어간 데이터를 받는데까진 시간이 걸립니다.

offset을 commit한 부분부터 데이터를 갖고오기에 새로운 데이터를 받기 전까지 데이터를 poll하지 않습니다.

## 2. Kafka consumer 안전하게 종료하기
위의 예제는 , Kafka consumer가 poll하는 동작을 무한루프로 수행했기에 , 재 시작했을 때 문제가 발생합니다.

따라서 우아하게 종료하는 방법은 다음과 같습니다.
- [ConsumerDemoWithShutdown 예제 코드](./src/main/java/io/Conduktor/demos/kafka/ConsumerDemoWithShutdown.java)

### 코드 동작순서
1. addShutdownHook() 스레드의 Run() 익명함수 동작
- java를 종료함과 동시에
- 여기선 java가 종료됨을 알고 , ```consumer.wakeup()``` 메서드를 동작시킵니다.
- 이후 실행중인 mainThread에 join 됩니다.
2. WakeupException 호출
- try - catch의 WakeupException() 으로 잡히면서 poll 동작이 멈추게 되고,
- ```consumer.close()``` 메서드가 호출되면서 ***offset을 commit하고 consumer가 consumer group에서 삭제되면서***
- consumer는 종료합니다.


로그는 다음과같이 출력됩니다.
```logcatfilter
...
종료를 감지함. consumer.wakeup() 메서드를 호출하고 나갈 예정...
consumer가 shutdownd을 시작함
[Consumer clientId=consumer-my-java-appilcation-1, groupId=my-java-appilcation] Revoke previously assigned partitions demo_java-0, demo_java-1, demo_java-2
[Consumer clientId=consumer-my-java-appilcation-1, groupId=my-java-appilcation] Member consumer-my-java-appilcation-1-7af2a551-c891-4de5-bf1f-1f6550291434 sending LeaveGroup request to coordinator cluster.playground.cdkt.io:9102 (id: 2147483629 rack: null) due to the consumer is being closed
[Consumer clientId=consumer-my-java-appilcation-1, groupId=my-java-appilcation] Resetting generation due to: consumer pro-actively leaving the group
[Consumer clientId=consumer-my-java-appilcation-1, groupId=my-java-appilcation] Request joining group due to: consumer pro-actively leaving the group
[main] INFO org.apache.kafka.common.metrics.Metrics - Metrics scheduler closed
[main] INFO org.apache.kafka.common.metrics.Metrics - Closing reporter org.apache.kafka.common.metrics.JmxReporter
[main] INFO org.apache.kafka.common.metrics.Metrics - Metrics reporters closed
[main] INFO org.apache.kafka.common.utils.AppInfoParser - App info kafka.consumer for consumer-my-java-appilcation-1 unregistered
```