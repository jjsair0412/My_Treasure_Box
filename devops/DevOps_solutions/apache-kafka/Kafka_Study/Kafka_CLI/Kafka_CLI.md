# Kafka CLI
[Kafka_설치](./Kafka_study용_설치/Linux_Kafka_install.md) 문서대로 Kafka를 설치하고 linux PATH에 제대로 바이너리파일이 잡혔다는 가정 하에 해당문서를 작성합니다.

## 명령어 구조
```--bootstrap-server``` 옵션은 Kafka Cluster가 설치된 URL 주소가 value로 들어갑니다.

```bash
ex )
$ kafka-topics.sh --bootstrap-server localhost:9092 --list 
```

## Kafka Topic 관리
Kafka-topic.sh binary file을 통해 Topic 대상으로 아래 관리명령을 내릴 수 있습니다.

1. Topic 생성
2. Topic List 확인
3. Topic Describe
4. Topic 내부 파티션 개수 증가
5. Topic 제거

### Topic 생성
아래 명령어로 토픽을 생성합니다.
- ```--topic``` : topic 이름
- ```--create``` : 생성명령

```bash
kafka-topics.sh --bootstrap-server localhost:9092 --topic first_topic --create 
```

토픽의 partitions 개수를 지정하면서 토픽을 생성합니다.
```bash
kafka-topics.sh --bootstrap-server localhost:9092 --topic second_topic --create --partitions 3
```

replication factor 개수와 partitions 개수를 지정하면서 토픽을 생성합니다.
- 당연한 말이지만 , replication factor 개수는 broker 개수보다 많으면 안됩니다.
>ex ) replication-factor가 3개인 경우가 best option
```bash
# broker가 1개면 아래명령어는 실패함. replication-factor가 2개라서.
kafka-topics.sh --bootstrap-server localhost:9092 --topic third_topic --create --partitions 3 --replication-factor 2
```

### Topic list 확인
생성된 Topic들을 ```--list``` 로 listup 합니다.

```bash
kafka-topics.sh --bootstrap-server localhost:9092 --list 
first_topic
second_topic
third_topic
```

### Topic Describe
생성된 Topic중 특정 Topic을 ```--Describe``` 옵션으로 상세정보를 확인합니다.
- partition 개수등 상세 정보를 확인할 수 있습니다.
- Isr, Replicas 등 정수값으로 표현되는데 , 이는 파티션이 생성된 위치의 broker ID를 나타냅니다.
- Broker가 1개인 경우에는 아래 명령어 결과처럼 0 0 0 0 으로 출력됩니다. - 갈곳이없어서
>따라서 Partition의 정수값은 Partition 파티션의 ID를 나타내는데 , Leader , Replicas , Isr 은 해당 토픽의 파티션을 갖고있는 브로커의 ID를 가르킵니다.

```bash
kafka-topics.sh --bootstrap-server localhost:9092 --topic first_topic --describe
Topic: first_topic      TopicId: UiSiOgJlT1WKRI-bkYc8xA PartitionCount: 1       ReplicationFactor: 1    Configs: segment.bytes=1073741824
        Topic: first_topic      Partition: 0    Leader: 0       Replicas: 0     Isr: 0
```

### Topic Delete
생성된 Topic은 ```--delete``` 명령어로 delete Topic하면 됩니다.

```bash
kafka-topics.sh --bootstrap-server localhost:9092 --topic first_topic --delete
```

제거결과 확인
```bash
kafka-topics.sh --bootstrap-server localhost:9092 --list 
second_topic
third_topic
```