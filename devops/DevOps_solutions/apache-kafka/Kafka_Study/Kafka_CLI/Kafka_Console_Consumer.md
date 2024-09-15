# Kafka Console Consumer 
```kafka-console-consumer.sh``` 바이너리 파일로 Kafka broker에 요청 보내기

## Overview
1. topic tail에서부터 데이터 읽기
2. topic의 처음부터 데이터 읽기
3. key-value 확인

## topic tail에서부터 데이터 읽기

먼저 , partitions 3개짜리 토픽을 생성
```bash
kafka-topics.sh --bootstrap-server localhost:9092 --topic second_topic --create --partitions 3
```

```kafka-console-consumer.sh``` 바이너리로 생성한 second_topic 소비하겠다고 명령
- 아직 아무런 메세지를 second_topic으로 보내지 않았기 때문에 가만히있음.

```bash
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic second_topic
```

켜놓은 상태로 터미널에서 실습

### 1. Producing
아래 명령어로 프로듀서 실행
- ```--producer-property``` 옵션으로 RoundRobinPartitioner 등록
    - 해당 옵션으로 모든 파티션에 적재되는 데이터 총량에 의미를 두지 않고 그냥 라운드로빈으로 모든 파티션에 데이터를 넣는다. - prod에는 적당하지 않음..
    - 현재의 Kafka는 해당옵션을 주지 않고 default 옵션으로 , 하나의 파티션에만 데이터를 집어넣는데 , 만약 데이터가 들어가고있는 파티션의 용량이 ```16 킬로바이트`` 가 넘어간다면 , 다음 파티션에 저장되는 형태를 가진다.


메세지를 보낼때마다 ```kafka-console-consumer.sh``` 를 켜놓은 terminal에서 메시지가 출력됨
```bash
kafka-console-producer.sh --bootstrap-server localhost:9092 --producer-property partitioner.class=org.apache.kafka.clients.producer.RoundRobinPartitioner --topic second_topic
>test
```

## topic의 처음부터 데이터 읽기
```kafka-console-consumer.sh``` 바이너리의 ```--from-beginning``` 옵션 사용
- 메세지는 전부다 출력되지만 메세지가 순서대로 출력되지 않음. 그 이유는 ***파티션이 3개고 , 데이터는 라운드 로빈으로 저장됐기 때문에 보낸 메시지 순서대로 나오지 않음.*** 

```bash
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic second_topic --from-beginning
```

## key-value 확인
아래 옵션들로 메세지가 어떤 파티션에 들어갔는지 확인
- ```--formatter kafka.tools.DefaultMessageFormatter``` : CLI 명령어 출력 포맷
- ```print.timestamp=true ``` :  메시지 수신 시간
- ```print.key=true print.value=true``` : 파티션 key value 출력
- ```print.partition=true``` : 메세지가 할당된 파티션 숫자 출력
- ```--from-beginning``` : 처음부터 메시지 출력

**해당 출력대로 , 라운드로빈으로 파티션에 들어갔으며 메시지를 소비할 땐 토픽 내부의 파티션끼리의 순서를 보장할 수 없기 때문에 출력의 순서가 보낸순서대로 나오지 않는것이다.**

```bash
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic second_topic --formatter kafka.tools.DefaultMessageFormatter --property print.timestamp=true --property print.key=true --property print.value=true --property print.partition=true --from-beginning
CreateTime:1689261300020        Partition:1     null    test
CreateTime:1689261429936        Partition:1     null    tesaera
```