# Kafka SDK
Java 11 version과 Gradle을 통해서 Kafka SDK를 사용하는 방안에 대해 정리한 폴더입니다.
- [3.1.0 Kafka Client dependency](https://mvnrepository.com/artifact/org.apache.kafka/kafka-clients/3.1.0) 를 사용합니다.

## kafka connect hub URL
- https://www.confluent.io/hub/

## Project Location
상위 gradle project에 여러 하위 gradle project가 위치해 있습니다.
>만약 카프카 컨슈머 , 프로듀서를 구현해야 한다면 , [Kafka basic](./kafka-basics) 문서가 아닌 , [kafka wikimedia producer](./kafka-producer-wikimedia), [kafka wikimedia consumer](./kafka-consumer-opensearch) 문서를 참고해야 합니다.
>[Kafka basic](./kafka-basics) 문서는 , kafka에 대한 기본적인 이론이 정리되어 있습니다.

- [Kafka basic](./kafka-basics)
  - 자바 코드로 카프카를 어떻게 사용하는지에 대한 기초적인 내용이 정리되어 있습니다.
- [kafka wikimedia producer](./kafka-producer-wikimedia)
  - wikimedia의 스트림 값을 받아서 프로듀서가 카프카로 보내는 기초적인 로직에 대한 내용이 정리되어 있습니다.
  - 프로듀서 성능 최적화 (데이터 압축 , 스티키파티션) 관련한 정보도 작성되어 있습니다.
- [kafka wikimedia consumer](./kafka-consumer-opensearch)
  - kafka cluster에 적제된 스트림 값을 , consumer가 소비하는 로직에 대한 내용이 정리되어 있습니다,
  - cosumer 성능 최적화 (offset commit 시기 조절) 등에 대한 로직이 정리되어 있습니다.
  - 또한 대량의 데이터를 consumer가 BulkRequest 객체를 통해 어떻게 소비하는지에 대한 내용까지 있습니다.

## Kafka Extended API
Kafka 프로그래밍을 활용하여 , 발생 가능한 문제들을 해결할 수 있는 API가 있습니다.

예를들어,  
1. 외부에서 데이터를 가져와 kafka로 보내거나 ,
2. 카프카에서 외부로 보내거나 ,
3. 카프카 토픽에서 다른 카프카 토픽으로 보내거나 ,

와 같은 상황이 있을 수 있습니다.

### 1. Kafka Connect
외부에서 데이터를 가져와 카프카에 보내주거나 , 카프카에서 외부로 데이터를 전송할 수 있음

### 2. Kafka Stream
카프카 토픽에서 다른 카프카 토픽으로 전환할 수 있음

### 3. Schema Registry
카프카에서 스키마를 사용할 수 있음
- 관련 문서 : 
  - https://always-kimkim.tistory.com/entry/kafka101-schema-registry