# Kafka Consumer Group
```--group``` 파라미터를 통해 kafka-consumer-group으로 broker에게 데이터 받기

## Overview
- 동일 그룹 내에서 여러 컨슈머들이 Broker에게 데이터 수집하기
    - 하나의 컨슈머가 여러 파티션에게 데이터 받기
-[consumer_group_관리](#consumer-group-관리)
    - consumer-group list up
    - consumer group describe
    - consumer group 제거 
    - [임시 consumer group](#etc---임시-consumer-group)
    - [Consumer Group offset 재 설정](#consumer-group-offset-관리)

## 전재 조건
### 1. Topic 생성
파티션 3개짜리 토픽 생성

```bash
kafka-topics.sh --bootstrap-server localhost:9092 --topic third_topic --create --partitions 3
```

## 1. Consumer 생성
컨슈머를 생성할 때 , ```--group``` 파라미터의 value로 group ID를 지정함으로써 Group을 생성하면서 consumer를 생성합니다.

```bash
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic third_topic --group my-first-application
```

## 2. producer 생성
thrid_topic 에 데이터를 프로듀싱할 producer를 생성합니다.
- ```RoundRobinPartitioner``` 설정으로 모든 파티션에게 데이터 전달 _ prod 용으로 사용 안됨.

```bash
kafka-console-producer.sh --bootstrap-server localhost:9092 --producer-property partitioner.class=org.apache.kafka.clients.producer.RoundRobinPartitioner --topic third_topic
>test
...
```

### 3. 새로운 Consumer 생성
새로운 terminal을 생성해서 , 아래 2개 컨슈머를 생성합니다.
- 아래 2개 consumer를 포함해서 지금까지 생성한 consumer는 모두 같은 consumer-group에 들어가있기 때문에 , producer에서 데이터를 프로듀싱하면 (라운드로빈옵션이 켜져있어서) 순차적으로 각 consumer에 데이터가 출력되는것을 확인할 수 있습니다.

```bash
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic third_topic --group my-first-application
```

## 결과 확인
consumer 개수가 파티션보다 적을때 까지 , consumer는 RoumdRobin으로 파티션들에게 매핑되어 데이터를 소비합니다.
- consumer들끼리 차례대로 데이터가 소비됩니다.

***만약, consumer 개수가 파티션보다 많아진다면 , 마지막으로 추가된 consumer는 standby 상태로 남기 때문에 데이터를 읽어오지 않습니다.***

또한 , 만약 모든 consumer가 제거된 이후에 (토픽 및 파티션은 존재하는 상태로) producer가 해당 토픽에게 계속 데이터를 프로듀싱 한다면 , **해당 토픽을 소비하는 consumer가 생성되는 동시에 생성된 consumer가 지금껏 출력되지 못하고 broker queue에 쌓여있던 데이터를 한꺼번에 출력합니다.**
- 아래 명령어처럼 다른 consumer group일 경우에도 ```--from-beginning``` 파라미터가 작동하면서 모든 데이터를 한꺼번에 출력함

```bash
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic third_topic --group my-second-application --from-beginning
```

>그러나 , 위 명령어를 한번 더 실행해보면 consumer가 없는 상태에서 producer가 보낸 데이터가 출력되지 않습니다.

그 이유는 , consumer-group의 consumer-offset이 커밋된 순간부터 처음이기 때문에 직전에 다 받아오면서 commit했고 그 이후에 데이터가 들어가지 않았다면 데이터를 소비하지 않기 때문입니다.
- ***consumer-offset 때문***

## consumer-group 관리
```kafka-consumer-groups.sh ``` 바이너리 파일로 consumer-group 관리

### 1. listup 
```--list``` 파라미터로 consumer-group list up

```bash
$ kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
my-first-application
my-second-application
```

### 2. describe
```--describe``` 파라미터로 특정 consumer group 상세정보 확인
- ```LAG``` : 프로듀싱만되고 소비되지 않은 데이터갯수 출력
- ```TOPIC``` : 읽어오고 있는 Topic 이름정보
- ```CONSUMER-ID``` , ```HOST``` , ```CLIENT-ID``` 들이 0인 이유는 broker가 1개이기 때문

```bash
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group my-second-application
GROUP                 TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID     HOST            CLIENT-ID
my-second-application third_topic     0          6               6               0               -               -               -
my-second-application third_topic     1          6               6               0               -               -               -
my-second-application third_topic     2          7               7               0               -               -               -
```

## ETC - 임시 consumer-group
만약 아래 명령어처럼 특정 Topic의 데이터를 소비하는 consumer를 생성하며 , consumer-group을 지정해주지 않았을경우에

```bash
$ kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic third_topic --from-beginning
world
sdf
asdf
dsf
df
...
```

임시 consumer-group이 생성됩니다.
```bash
$ kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
console-consumer-43994 # 임시 consumer-group
my-first-application
my-second-application
```

**이때 생성된 ```console-consumer-43994``` 와 같은 consumer group은 임시로 생성된 consumer-group이기 때문에 , 잠시후 제거되게 됩니다. 따라서 해당 consumer-group은 사용해서는 안되며 , 사전 정의된 consumer-group만 사용해야 합니다.**


## consumer group offset 관리
consumer group은 consumer offset을 broker에게 가끔씩 commit하여 내가 어디까지 데이터를 갖고왔다를 알립니다.

**이 consumer offset를 cli로 재 설정할 수 있습니다.**

### Overview
>offset을 재 설정하기 위해서 반드시 해당 consumer-group이 중지된 상태여야만 합니다.

1. Consumer Offset 시작/정지
2. Offsets 재 시작
3. Console Consumer 재 시작 후 확인

먼저 my-first-application consumer group을 확인합니다.
- LAG 에 7이 출력된것을 보아 , 7개의 데이터가 소비되지않고 있다는것을 확인할 수 있습니다.

```bash
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group my-first-application
GROUP                TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID     HOST            CLIENT-ID
my-first-application third_topic     0          6               13              7               -               -               -
my-first-application third_topic     1          7               14              7               -               -               -
my-first-application third_topic     2          7               14              7               -               -               -
```

## 1. offset 재 설정
아래 명령어로 dry-run 을 먼저 수행
```--reset-offsets ``` 과 ```--to-earliest``` 파라미터를 사용합니다.
- ```--to-earliest``` : 토픽에 존재하는 가장 이른 데이터
- ```--reset-offsets ``` : 지정한 consumer-group의 offset을 맨 처음으로 재 설정
- ```--dry-run``` : 실제 수행하지 않고 결과 예상

```bash
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group my-first-application --reset-offsets --to-earliest --topic third_topic --dry-run

GROUP                          TOPIC                          PARTITION  NEW-OFFSET     
my-first-application           third_topic                    0          0
my-first-application           third_topic                    1          0
my-first-application           third_topic                    2          0
```

```--execute``` 파라미터로 실제 offset 재 설정 명령어 init

```bash
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group my-first-application --reset-offsets --to-earliest --topic third_topic --execute
GROUP                          TOPIC                          PARTITION  NEW-OFFSET
my-first-application           third_topic                    0          0
my-first-application           third_topic                    1          0
my-first-application           third_topic                    2          0
```

```--describe``` 파라미터로 해당 consumer-group LAG 확인
- offset을 초기화햇기 때문에 , LAG가 LOG-END-OFFSET 값과 동일해진것을 확인할 수 있음.

```bash
$ kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group my-first-application

GROUP                TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID     HOST            CLIENT-ID
my-first-application third_topic     0          0               13              13              -               -               -
my-first-application third_topic     1          0               14              14              -               -               -
my-first-application third_topic     2          0               14              14              -               -               -
```

해당 consumer-group을 가진 consumer 재 실행해 보면 , offset이 초기화됬기 때문에 모든 데이터를 소비하는것을 확인할 수 있습니다.
```bash
$ kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic third_topic --group my-first-application

world
sdf
asdf
dsf
...
```

다시 describe 명령어로 해당 consumer-group을 조회해보면 , LAG가 0으로 소비하지 못한 데이터가 없는것을 확인할 수 있습니다.
```bash
$ kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group my-first-application

GROUP                TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID     HOST            CLIENT-ID
my-first-application third_topic     0          13              13              0               -               -               -
my-first-application third_topic     1          14              14              0               -               -               -
my-first-application third_topic     2          14              14              0               -               -               -
```