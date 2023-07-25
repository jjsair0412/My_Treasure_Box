# Kafka SDK
Java 11 version과 Gradle을 통해서 Kafka SDK를 사용하는 방안에 대해 정리한 폴더입니다.
- [3.1.0 Kafka Client dependency](https://mvnrepository.com/artifact/org.apache.kafka/kafka-clients/3.1.0) 를 사용합니다.

## Project Location
상위 gradle project에 여러 하위 gradle project가 위치해 있습니다.

- [Kafka basic](./kafka-basics)
  - 자바 코드로 카프카를 어떻게 사용하는지에 대한 기초적인 내용이 정리되어 있습니다.
- [kafka wikimedia producer](./kafka-producer-wikimedia)
  - wikimedia의 스트림 값을 받아서 프로듀서가 카프카로 보내는 기초적인 로직에 대한 내용이 정리되어 있습니다.
  - 프로듀서 성능 최적화 (데이터 압축 , 스티키파티션) 관련한 정보도 작성되어 있습니다.