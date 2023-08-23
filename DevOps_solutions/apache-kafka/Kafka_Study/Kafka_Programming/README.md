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
- [Kafka_실사례_아키텍처](./kafka-realExample)
  - Kafka 를 실제 아키텍처에 도입할 때 , 어떤 부분을 고려해야되고 어떻게 구성해야하는지에 대한 기본적인 설명이 들어가 있습니다.

## Kafka Extended API
Kafka 프로그래밍을 활용하여 , 발생 가능한 문제들을 해결할 수 있는 API가 있습니다.

예를들어,  
1. 외부에서 데이터를 가져와 kafka로 보내거나 ,
2. 카프카에서 외부로 보내거나 ,
3. 카프카 토픽에서 다른 카프카 토픽으로 보내거나 ,

와 같은 상황이 있을 수 있습니다.

### 1. Kafka Connect
외부에서 데이터를 가져와 카프카에 보내주거나 , 카프카에서 외부로 데이터를 전송할 수 있음
  - 여러 종류의 데이터 저장소 (RDB , S3 , elasticSearch 등 ..) 에서 여러 종류의 데이터 저장소 (RDB , S3 , elasticSearch 등 ..) 로 데이터를 이동하여 적제하거나 , 다양한 형태의 스트림값을 받아와서 다양한 형태의 저장소에 적제하고싶을때 사용합니다.

#### 1.1 Kafka Connect Slink vs Kafka Connect Source
- Kafka Connect Source는 , 외부 데이터베이스에서 데이터를 임포트할 때 사용하며 ,
- kafka Connect Slink는 , Kafka에서 target 데이터베이스로 데이터를 계속 export하기 위해 사용합니다.


### 2. Kafka Stream
카프카 토픽에서 다른 카프카 토픽으로 전환할 수 있음
  - ex) 특정 토픽에서 데이터를 읽어와서 , 어떤 종류의 계산을 한 다음 계산결과를 다른 토픽에 저장하고 싶을때 사용합니다.
  - kafka Stream을 통해 데이터 변환 , 보강 , 이상탐지 , 모니터링 및 알람 기능도 있습니다.
  - **한번에 하나의 레코드만 처리하기에 배치가 없습니다**

### 3. Schema Registry
카프카에서 스키마를 사용할 수 있음
  - 프로듀서가 카프카에게 보내는 데이터에 다른 포멧이나 필드명이 잘못되는것 처럼 데이터 형식이 달라진다면 , 해당 데이터 포멧에 맞게끔 역직렬화 구성이 되어있는 컨슈머에선 에러가 발생할것 입니다.
  - 이때 **Schmea Registry**가 필요합니다.
  - 스카마는 데이터가 어떻게 생겼는지 설명함
#### 3.0 Schema Registry의 기본 동작 방식
>Schema Registry가 아래와 같이 구성되어 있다면 ,
>**id** 에 해당하는 value가 long 타입이 아니라면 , Produce 할 때 에러 발생
>**id** 값아닌 다른 필드가 들어온다면 에러 발생

- Schema Registry
```json
{
	"type": "record",
	"name": "MyRecord",
	"namespace": "com.mycompany",
	"fields" : [
		{"name": "id", "type": "long"}
	]
}
```

- 요청 1번
  - 에러 발생 안함
```json
{
  "id": 3209979198980300300
}
```


- 요청 2번
  - 에러 발생 . id값 필드 이름이 다름
```json
{
  "idd": 3209979198980300300
}
```

- 요청 3번
  - 에러 발생 . value type missmatch
```json
{
  "id":"hello world !"
}
```

#### 3.1 Schema Registry 동작 과정
1. Kafka producer가 데이터를 kafka cluster에 보내기 전,  해당 데이터의 schema를 schema Registry에 전송
2. Schema Registry는 Kafka와 Schema Registry 유효성 검사
3. 문제가 없을 경우에 producer가 kafka cluster에게 데이터 전송
4. consumer는 일단 kafka cluster에게 데이터를 받아옴
5. consumer가 역직렬화 진행할 때 , Schema Registry에 있는 Schema를 검색해서 받아온 데이터의 역직렬화 수행

#### 3.2 Schema Registry가 가질 수 있는 Format
1. Avro
2. Json
3. Protobuf

- 관련 문서 : 
  - https://always-kimkim.tistory.com/entry/kafka101-schema-registry