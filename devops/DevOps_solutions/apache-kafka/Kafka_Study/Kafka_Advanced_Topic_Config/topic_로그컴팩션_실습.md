# topic 로그컴팩션 실습
로그컴팩션을 실습해 봅니다.

## 1. topic 생성
아래 명령어로 compact type의 토픽을 생성합니다.

실습을 위한 토픽이기 때문에 , 실 환경에선 절대 이렇게하면 안됩니다.
>min.cleanable.dirty.ratio 옵션을 정말작게 줘서 , 로그컴팩션이 계속 일어나도록 함.
>
>segment.ms 를 5000으로 두어 , 5초마다 새로운 세그먼트가 생성되게끔 함
```bash
./kafka-topics.sh --bootstrap-server localhost:9092 --create --topic employee-salary \
--partitions 1 --replication-factor 1 \
--config cleanup.policy=compact \
--config min.cleanable.dirty.ratio=0.001 \ 
--config segment.ms=5000

Created topic employee-salary.
```

생성확인

```bash
./kafka-topics.sh --bootstrap-server localhost:9092 topic employee-salary --describe 
Topic: employee-salary  TopicId: qfUuXLlzTW-2HyRWK1yVHg PartitionCount: 1       ReplicationFactor: 1    Configs: cleanup.policy=compact,segment.bytes=1073741824,min.cleanable.dirty.ratio=0.001,segment.ms=5000
        Topic: employee-salary  Partition: 0    Leader: 1       Replicas: 1     Isr: 1
```

## 2. consumer 생성
생성한 해당 ```employee-salary``` 토픽을 소비할 consumer를 생성합니다.

```bash
./kafka-console-consumer.sh --bootstrap-server localhost:9092 \
    --topic employee-salary \
    --from-beginning \
    --property print.key=true \
    --property key.separator=,
...
```

## 3. producer 생성
```employee-salary``` 토픽에 데이터를 프로듀싱할 producer를 생성합니다.

```bash
./kafka-console-producer.sh --bootstrap-server localhost:9092 \
    --topic employee-salary \
    --property parse.key=true \
    --property key.separator=,
...
```

## 데이터 전송
프로듀서를 통해 아래 데이터를 한줄씩 보내보면 , consumer에서 쫙 소비하면서 출력되는걸 볼 수 있습니다.

>가상 연봉정보
```bash
Patrick,salary: 10000
Lucy,salary: 20000
Bob,salary: 20000
Patrick,salary: 25000
Lucy,salary: 30000
Patrick,salary: 30000
```

그리고 , 아래 데이터를 보내봅니다.
>로그컴팩션 일으키기 위함

```bash
Stephane,salary: 0
```


**멈춘 이후에 , 다시 컨슈머를 실행하면 , 각 키(이름) 별 최근에 들어온 value(연봉) 값이 출력되는것을 확인할 수 있습니다.**
>--from-beginning 옵션이라 topic에 있는것들 모두 처음부터 다 들고오는데 , 예전 데이터들은 삭제되어 안들고옴