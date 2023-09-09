# Linux Kafka Install
Kafka 강의에서 나온 설치방안에 대해 정리한 문서입니다.
- https://www.conduktor.io/kafka/how-to-install-apache-kafka-on-linux/

## **Kafka 설치**
## Overview
먼저 카프카를 설치하는데 , 순서는 다음과 같습니다.

1. JDK version 11 설치
2. Apache Kafka binary 설치
3. binary 압축해제하여 Linux 설치
4. 환경변수 등록

## 설치
### 1. java 설치
jdk 11버전이 필수 옵션이기 때문에 설치
```bash
wget -O- https://apt.corretto.aws/corretto.key | sudo apt-key add - 
sudo add-apt-repository 'deb https://apt.corretto.aws stable main'
sudo apt-get update; sudo apt-get install -y java-11-amazon-corretto-jdk
```

java 설치결과 확인
```bash
$ java -version
openjdk version "11.0.19" 2023-04-18 LTS
OpenJDK Runtime Environment Corretto-11.0.19.7.1 (build 11.0.19+7-LTS)
OpenJDK 64-Bit Server VM Corretto-11.0.19.7.1 (build 11.0.19+7-LTS, mixed mode)
```

### 2. apache kafka 설치
- [kafka 공식 다운로드 페이지](https://kafka.apache.org/downloads) 에서 latest kafka를 설치합니다.
    - 설치대상 버전 : 3.1.0 , Scala 2.13
>꼭 binary download에서 bin파일들을 설치해야만 합니다.
>
>아니면 아래 에러발생
>
>Classpath is empty. Please build the project first e.g. by running './gradlew jar -PscalaVersion=2.13.6'

```bash
$ wget https://downloads.apache.org/kafka/3.5.1/kafka_2.12-3.5.1.tgz
```

설치한 tar파일 압축해제합니다.
```bash
$ tar xzf kafka-3.1.0-src.tgz kafka-3.1.0-src/

$ ls
kafka-3.1.0-src  kafka-3.1.0-src.tgz
```

### 3. 환경변수 설정
bashrc에 kafka bin 파일을 PATH 등록합니다.

```bash
$ vi ~/.bashrc
...
PATH="$PATH:~/kafka-3.1.0-src/bin"

$ source ~/.bashrc
```

cli 설치 확인합니다.
```bash
$ kafka-topics.sh
```


## **Zookeeper 설치**
## Overview
Zookeeper 설치 과정 순서는 다음과 같습니다.

1. 다른 프로세스에서 Zookeeper 실행
2. 하나의 Broker를 가지는 Kafka cluster를 바이너리파일로 실행

## 설치
### 1. Zookeeper 실행
바이너리파일로 Zookeeper 실행합니다.
- 실행할때 zookeeper.properties 파일경로 넣어주어야 합니다.

```bash
$ zookeeper-server-start.sh ~/kafka_2.13-3.1.0/config/zookeeper.properties
...
[2023-07-13 13:57:10,789] INFO zookeeper.request_throttler.shutdownTimeout = 10000 (org.apache.zookeeper.server.RequestThrottler)
[2023-07-13 13:57:10,843] INFO Using checkIntervalMs=60000 maxPerMinute=10000 maxNeverUsedIntervalMs=0 (org.apache.zookeeper.server.ContainerManager)
[2023-07-13 13:57:10,848] INFO ZooKeeper audit is disabled. (org.apache.zookeeper.audit.ZKAuditProvider)
```

### 2. Kafka 실행
바이너리파일로 Kafka cluster 실행합니다.

Broker는 1개로 실행
- 실행할 때 server.properties 파일경로 넣어주어야 합니다. ( Kafka 설정파일 )

```bash
$ kafka-server-start.sh ~/kafka_2.13-3.1.0/config/server.properties
...
[2023-07-13 13:59:13,500] INFO [KafkaServer id=0] started (kafka.server.KafkaServer)
[2023-07-13 13:59:13,725] INFO [BrokerToControllerChannelManager broker=0 name=alterIsr]: Recorded new controller, from now on will use broker kafka:9092 (id: 0 rack: null) (kafka.server.BrokerToControllerRequestThread)
[2023-07-13 13:59:13,725] INFO [BrokerToControllerChannelManager broker=0 name=forwarding]: Recorded new controller, from now on will use broker kafka:9092 (id: 0 rack: null) (kafka.server.BrokerToControllerRequestThread)
```

Kafka server ID=0 으로 kafka cluster가 실행된것을 확인할 수 있습니다.

## ETC
Zookeeper 및 Kafka 데이터 저장 경로 변경방안

### 1. Zookeeper
zookeeper.properties 파일에서 아래옵션 경로 변경
```bash
dataDir=/tmp/zookeeper  # 경로변경
```

### 2. Kafka
server.properties 파일에서 아래경로 옵션 변경
```bash
log.dirs=/tmp/kafka-logs  # 경로변경
```
