# Kafka Console Producer CLI
```kafka-console-producer.sh``` 바이너리 파일로 Kafka broker에 요청 보내기

## Overview
- Key가 없는 경우
- Key가 있는 경우

먼저 Kafka-topic으로 파티션 1개짜리 토픽 생성
```bash
kafka-topics.sh --bootstrap-server localhost:9092 --topic first_topic --create --partitions 1
```
## Producing
```kafka-console-producer.sh``` 바이너리 파일로 , 프로듀서 실행시킵니다.
- ```>``` 로 들어왓다면, 프로듀싱할 준비가 완료된것

```bash
kafka-console-producer.sh --bootstrap-server localhost:9092 --topic first_topic
> 
```

```>``` 상태로 특정문자열 입력 후 Ctrl+c 버튼으로 producer에서 나옵니다.

엔터칠때마다 카프카에 메세지를 보낸것.

### Producer 사용 - acks 옵션 추가
topic과 bootstrap server를 지정한 뒤 , ```--producer-property``` 옵션으로 acks 모드도 설정합니다.
- acks=all is 모든 브로커(리더 , ISR)가 메세지 확인된것을 리턴

```bash
kafka-console-producer.sh --bootstrap-server localhost:9092 --topic first_topic --producer-property acks=all
>
```

### Producer 사용 - 없는 Topic에 입력
존재하지 않는 Topic에 입력하면 , 생성됨
- 에러메세지는 리더가 없다는 뜻
- 에러생성후 리더 생성됨 , 파티션 개수 1개
    - 그러나 자동토픽생성은 disable되는게 best practice

```bash
kafka-console-producer.sh --bootstrap-server localhost:9092 --topic new_topic
>test
[2023-07-13 14:58:07,049] WARN [Producer clientId=console-producer] Error while fetching metadata with correlation id 3 : {new_topic_test=LEADER_NOT_AVAILABLE} (org.apache.kafka.clients.NetworkClient)
[2023-07-13 14:58:07,231] WARN [Producer clientId=console-producer] Error while fetching metadata with correlation id 4 : {new_topic_test=LEADER_NOT_AVAILABLE} (org.apache.kafka.clients.NetworkClient)
> ..

kafka-topics.sh --bootstrap-server localhost:9092 --list
first_topic
new_topic
new_topic_test
```

### Producer 사용 - Key 지정
key를 지정해서 보냅니다.
- ```--property``` 옵션으로 key true , key.separator=: 옵션 줘서 key-value를 입력하게끔 함.

```bash
kafka-console-producer.sh --bootstrap-server localhost:9092 --topic first_topic --property parse.key=true --property key.separator=:
>test:jinseong
```

만약 key:value를 입력안하면 , 아래와 같은 에러 발생
```bash
...
kafka-console-producer.sh --bootstrap-server localhost:9092 --topic first_topic --property parse.key=true --property key.separator=:
>test
org.apache.kafka.common.KafkaException: No key found on line 1: test
        at kafka.tools.ConsoleProducer$LineMessageReader.readMessage(ConsoleProducer.scala:292)
        at kafka.tools.ConsoleProducer$.main(ConsoleProducer.scala:51)
        at kafka.tools.ConsoleProducer.main(ConsoleProducer.scala)
```