# Kafka Advanced Topic Config
아래 topic들의 구성 추가 , 제거방안들을 사용해서 topic log 관리전략을 변경하거나 , topic들의 세부값들을 조정할 수 있습니다.

## Topic 구성
Topic은 아래와 같은 구성 매개변수들을 갖고있습니다.
- 복제 개수 개수
- 파티션 개수
- 메시지 크기, 압축 수준
- 로그 관리 정책
- 최소 ISR
- ETC

이러한 구성값들을 각 토픽마다 다르게 구성하여 , Kafka broker 성능을 최적화 할 수 있습니다.

### 매개변수 지정하여 Topic 생성하기
먼저 아래 두 명령어를 통해 , 토픽을 생성하고 , 생성한 토픽 정보를 읽어옵니다.

>Configs: 에 원래는 비어있어야 함. kafka-configs 로 kakfa-config를 추가하면 추가됨.

```bash
./kafka-topics.sh --create --bootstrap-server localhost:9092 --topic configured-topic --replication-factor 1 --partitions 3 
Created topic configured-topic.

# topic 구성 확인
./kafka-topics.sh --bootstrap-server localhost:9092 --topic configured-topic --describe 
Topic: configured-topic TopicId: dReKk595SQiZychlLD7WLw PartitionCount: 3       ReplicationFactor: 1    Configs: segment.bytes=1073741824
        Topic: configured-topic Partition: 0    Leader: 1       Replicas: 1     Isr: 1
        Topic: configured-topic Partition: 1    Leader: 1       Replicas: 1     Isr: 1
        Topic: configured-topic Partition: 2    Leader: 1       Replicas: 1     Isr: 1
```

### kafka-conifgs 명령어를 통해 topic에 Config 추가하기
#### 동적 구성 추가
먼저 topic의 config를 추가하기 위해서 , 변경 대상이 되는 구성을 동적 구성으로 추가시켜주어야 합니다.

아래 명령어로 동적 구성을 추가하게 됩니다.
- --entity-type : 구성을 추가할 리소스의 타입
- --entity-name : 구성을 추가할 리소스의 이름
- --alter : 구성변경 (구성을 변경하기 위해선 , 구성 유형을 명시해주어야 하기에 해당옵션 필요함)
- --add-config : 동적 구성 정보 지정

```bash
./kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name configured-topic --alter --add-config min.insync.replicas=2
Completed updating config for topic configured-topic.
```

구성이 추가되면 , ```--describe``` 명령어로 추가된것을 확인할 수 있음
>DEFAULT_CONFIG 는 1이었던것 또한 확인할 수 있음

```bash
./kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name configured-topic --describe                                
Dynamic configs for topic configured-topic are:
  min.insync.replicas=2 sensitive=false synonyms={DYNAMIC_TOPIC_CONFIG:min.insync.replicas=2, DEFAULT_CONFIG:min.insync.replicas=1}
```

#### 결과 확인
Conifgs에 추가된 동적 구성이 추가된것을 볼 수 있음
>min.insync.replicas=2

```bash
./kafka-topics.sh --bootstrap-server localhost:9092 --topic configured-topic --describe 

Topic: configured-topic TopicId: dReKk595SQiZychlLD7WLw PartitionCount: 3       ReplicationFactor: 1    Configs: min.insync.replicas=2,segment.bytes=1073741824
        Topic: configured-topic Partition: 0    Leader: 1       Replicas: 1     Isr: 1
        Topic: configured-topic Partition: 1    Leader: 1       Replicas: 1     Isr: 1
        Topic: configured-topic Partition: 2    Leader: 1       Replicas: 1     Isr: 1
```

### kafka-conifgs 명령어를 통해 topic에 Config 제거하기
추가된 Conifg를 제거할 수 도 있습니다.

아래 명령어로 제거
- --alter : 구성 유형 명시
- --delete-config : 제거할 유형 명시

```bash
./kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name configured-topic --alter --delete-config min.insync.replicas
Completed updating config for topic configured-topic.
```

다시 topic정보 확인해보면, 제거된것을 볼 수 있음
```bash
./kafka-topics.sh --bootstrap-server localhost:9092 --topic configured-topic --describe                                                             

Topic: configured-topic TopicId: dReKk595SQiZychlLD7WLw PartitionCount: 3       ReplicationFactor: 1    Configs: segment.bytes=1073741824
        Topic: configured-topic Partition: 0    Leader: 1       Replicas: 1     Isr: 1
        Topic: configured-topic Partition: 1    Leader: 1       Replicas: 1     Isr: 1
        Topic: configured-topic Partition: 2    Leader: 1       Replicas: 1     Isr: 1
```