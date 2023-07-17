# Kafka Basic

## 문서 색인
- [Kafka](#1-kafka-producer-만들기)

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
[ProducerDemo](./src/main/java/io/Conduktor/demos/kafka/ProducerDemo.java)


## 2. Kafka Callbacks
java Callback 인터페이스를 통해서 프로듀서가 어떤 파티션의 어떤 오프셋으로 보내는지 확인하는것이 가능합니다.

***StickyPartitioner*** 로 특정 파티션에 메세지를 보낼 수 있습니다.

**아래 코드와 주석을 참고합니다.**
[ProducerDemoWithCallback](./src/main/java/io/Conduktor/demos/kafka/ProducerDemoWithCallback.java)