# KRaft mode
Zookeeper 없이 Kafka 혼자서 동작하는 KRaft 모드를 설정하는 문서 입니다.
- **Zookeeper는 Kafka 4.0부터 완전제거될 예정**
- **KRaft mode는 prod ready가 3.3.1 버전부터 준비됨**

- https://www.conduktor.io/kafka/how-to-install-apache-kafka-on-linux-without-zookeeper-kraft-mode/

## Overview
아래 순서로 설치하게 됩니다.

1. ```kafka-storage.sh``` 바이너리 파일로 cluster ID를 생성하고 storage를 포멧합니다.
2. 바이너리 파일을 이용하여 Kafka를 실행합니다.

## 설치
### 1. cluster ID 얻기 && storage 포멧
random 값의 cluster UUID 값을 아래 바이너리파일을 실행하여 얻습니다.

```bash
$ kafka-storage.sh random-uuid
GfwUUK2UR8qEvuxJ2uttTQ
```

얻은 uuid값을 이용해서 디렉토리를 포멧합니다.
- ```server.properties``` 파일 내부에 적용된 ```log.dirs``` 경로의 디렉토리를 포멧합니다.
- 해당경로는 Kafka의 로그가 쌓이는 경로입니다.
```bash
$ kafka-storage.sh format -t <uuid> -c ~/kafka_2.13-3.1.0/config/kraft/server.properties

# 실 사용예
$ kafka-storage.sh format -t GfwUUK2UR8qEvuxJ2uttTQ -c ~/kafka_2.13-3.1.0/config/kraft/server.properties
Formatting /tmp/kraft-combined-logs
```

### 2. Kafka 실행
아래 명령어로 Kafka를 KRaft 모드로 실행합니다.
- 나오는 로그를 통해서 broker ID=1 로 Zookeeper 없이 Kafka를 실행할 수 있습니다.

```bash
$ kafka-server-start.sh ~/kafka_2.13-3.1.0/config/kraft/server.properties
...
[2023-07-13 14:11:49,307] INFO [BrokerServer id=1] Transition from STARTING to STARTED (kafka.server.BrokerServer)
[2023-07-13 14:11:49,308] INFO Kafka version: 3.1.0 (org.apache.kafka.common.utils.AppInfoParser)
[2023-07-13 14:11:49,308] INFO Kafka commitId: 37edeed0777bacb3 (org.apache.kafka.common.utils.AppInfoParser)
[2023-07-13 14:11:49,308] INFO Kafka startTimeMs: 1689257509307 (org.apache.kafka.common.utils.AppInfoParser)
[2023-07-13 14:11:49,313] INFO Kafka Server started (kafka.server.KafkaRaftServer)
[2023-07-13 14:11:49,313] INFO [Controller 1] The request from broker 1 to unfence has been granted because it has caught up with the last committed metadata offset 1. (org.apache.kafka.controller.BrokerHeartbeatManager)
[2023-07-13 14:11:49,330] INFO [Controller 1] Unfenced broker: UnfenceBrokerRecord(id=1, epoch=0) (org.apache.kafka.controller.ClusterControlManager)
[2023-07-13 14:11:49,368] INFO [BrokerLifecycleManager id=1] The broker has been unfenced. Transitioning from RECOVERY to RUNNING. (kafka.server.BrokerLifecycleManager)
```
