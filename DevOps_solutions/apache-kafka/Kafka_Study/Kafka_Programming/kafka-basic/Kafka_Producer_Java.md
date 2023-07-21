# Kafka Producer Java API

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

## 1. Kafka Producer 만들기
Kafka Producer를 만들기 위해선 , 아래 단계를 거쳐야 합니다.

1. create Producer Properties
2. create the Producer
3. send data
4. flush and close producer

**아래 코드와 주석을 참고합니다.**
[ProducerDemo 예제 코드](./src/main/java/io/Conduktor/demos/kafka/ProducerDemo.java)


## 2. Kafka Callbacks
java Callback 인터페이스를 통해서 프로듀서가 어떤 파티션의 어떤 오프셋으로 보내는지 확인하는것이 가능합니다.

***StickyPartitioner*** 로 특정 파티션에 메세지를 보낼 수 있습니다.

**아래 코드와 주석을 참고합니다.**
[ProducerDemoWithCallback 예제 코드](./src/main/java/io/Conduktor/demos/kafka/ProducerDemoWithCallback.java)

실행 결과 , 다음과같은 로그를 확인할 수 있습니다.
- demo_java topic
- 1번 Partition에 전송
- offset 0
- timestamp 1689836591204
- hasOffset true
```logcatfilter
topic: demo_java
Partition: 1
offset: 0
timestamp: 1689836591204
hasOffset: true
```

## 3. Sticky Partition
카프카는 프로듀서가 한번 데이터를 전송할 때 , 한번에 처리할 수 있는 최대 양 (16KB) 만큼 하나의 파티션에 넣게 됩니다.
- [관련 설명](../../Kafka_CLI/Kafka_Console_Consumer.md#1-producing)

따라서 아래 예제코드 또한 동일하게 Sticky로 하나의 파티션에 16KB만큼 넣게되는데, properties에 RoundRobin option과 batch.size를 조절하는 key-value를 넣음으로써 강제로 조절할 수 있습니다.
- **그러나 PROD 환경에서는 default option으로 그냥 쓰는게 가장 안전함..**
- [ProducerDemoWithCallback 예제 코드](./src/main/java/io/Conduktor/demos/kafka/ProducerDemoWithCallback.java)

```java
...
properties.setProperty("batch.size","400");

// 전체 파티션에 RoundRobin으로 데이터 전송
properties.setProperty("partitioner.class", RoundRobinPartitioner.class.getName());
...
```

## 4. producing  with key
Apache Kafka로 데이터를 producer가 전송할 때 , key값도 같이 보낼 수 있습니다.

**key값이 같은 value들은 모두 동일한 토픽 내부 파티션에 적재되게 됩니다.**
- 아래 예제 코드 로그를 확인해보면 , 동일한 key를 가진 value들은 동일한 partition에 전송되게 됩니다.
- [ProducerDemoKeys 예제 코드](./src/main/java/io/Conduktor/demos/kafka/ProducerDemoKeys.java)

로그는 다음과 같습니다.
```logcatfilter
[kafka-producer-network-thread | producer-1] INFO ProducerDemo - key : id_2 | partition : 2
[kafka-producer-network-thread | producer-1] INFO ProducerDemo - key : id_4 | partition : 2
[kafka-producer-network-thread | producer-1] INFO ProducerDemo - key : id_5 | partition : 2
[kafka-producer-network-thread | producer-1] INFO ProducerDemo - key : id_7 | partition : 2
[kafka-producer-network-thread | producer-1] INFO ProducerDemo - key : id_9 | partition : 2
[kafka-producer-network-thread | producer-1] INFO ProducerDemo - key : id_2 | partition : 2
[kafka-producer-network-thread | producer-1] INFO ProducerDemo - key : id_4 | partition : 2
[kafka-producer-network-thread | producer-1] INFO ProducerDemo - key : id_5 | partition : 2
[kafka-producer-network-thread | producer-1] INFO ProducerDemo - key : id_7 | partition : 2
[kafka-producer-network-thread | producer-1] INFO ProducerDemo - key : id_9 | partition : 2
[kafka-producer-network-thread | producer-1] INFO ProducerDemo - key : id_0 | partition : 1
[kafka-producer-network-thread | producer-1] INFO ProducerDemo - key : id_8 | partition : 1
[kafka-producer-network-thread | producer-1] INFO ProducerDemo - key : id_0 | partition : 1
[kafka-producer-network-thread | producer-1] INFO ProducerDemo - key : id_8 | partition : 1
[kafka-producer-network-thread | producer-1] INFO ProducerDemo - key : id_1 | partition : 0
[kafka-producer-network-thread | producer-1] INFO ProducerDemo - key : id_3 | partition : 0
[kafka-producer-network-thread | producer-1] INFO ProducerDemo - key : id_6 | partition : 0
[kafka-producer-network-thread | producer-1] INFO ProducerDemo - key : id_1 | partition : 0
[kafka-producer-network-thread | producer-1] INFO ProducerDemo - key : id_3 | partition : 0
[kafka-producer-network-thread | producer-1] INFO ProducerDemo - key : id_6 | partition : 0
```