# Aapache Kafka
## Directory Map
- [Kafka-basic-start](./kafka-quickstart-tutorial/)
- [Kafka udemy 강의 이력](./Kafka_Study/)

# Kafka란 ?
미국 linkedin에서 개발했으며, pub-sub 모델의 메시지 큐 종류 입니다.
## 1. Kafka를 왜 쓸까 ?
Kafka docs에서는 이벤트 스트리밍을 위해 Kafka를 사용한다고 합니다.

이때 이벤트 스트리밍은 , 수많은 소프트웨어들이 실시간으로 상시 작동하기 위한 기술을 의미합니다.

예를들어 DB , IOT 센서 , 클라우드 서비스 APP에서 발생하는 이벤트를 나중에 검색할 수 있도록 지속적으로 저장하거나 다른 대상 기술로 전달하여 적제적소에 올바른 위치에 있도록 도와주는것이 이벤트 스트리밍 입니다.

따라서 , **Kafka는 여러 APP에서 발생한 이벤트를 효과적으로 처리하기 위해 사용** 하며 , **이벤트 스트리밍 플랫폼** 이라 할 수 있습니다.

- Kafka는 수많은 어플리케이션이나 서비스에서 발생하는 이벤트 (메시지) 를 
실시간으로 처리하고 모니터링하기 위해서 사용합니다.

## 2. Kafka의 주요 기능
Kafka의 주요 기능은 세 분류로 나눌 수 있습니다.
1. 다른 시스템에서 데이터를 지속적으로 import/export 작업을 포함하여 이벤트 스트림을 게시 (write)하고 구독 (read) 합니다 .
    - 실시간 처리 가능
2. 원하는 기간 동안 지속적이고 안정적으로 이벤트 스트림을 저장합니다 .
    - 이벤트 (메시지) 저장 가능
3. 발생 시 또는 소급하여 이벤트 스트림을 처리합니다 .

또한 TCP 프로토콜로 작동하기에 서버와 클라이언트 구조로 구성되어 있으며 , 베어메탈이나 클라우드 서비스 , k8s 위에 올라가서 작동할 수 있도록 분산되어 개발됐기에 확장성이 뛰어납니다.

## 3. Kafka 구성 요소
### 3.1 Zookeeper
- Kafka의 메타데이터(metadata) 관리 및 브로커의 정상상태 점검(health check) 을 담당 합니다. 

### 3.2 Kafka || Kafka cluster
- 여러 대의 브로커를 구성한 클러스터를 의미 합니다.

### 3.3 broker
- 카프카 애플리케이션이 설치된 서버 또는 노드를 의미 합니다.

### 3.4 event || message || record
- 카프카는 APP에서 어떤 일이 일어낫다 라는 사실을 기록하고 , 해당 데이터를 Kafka에서 데이터 형식으로 읽거나 씁니다.
- 프로듀서가 브로커로 전송하거나 컨슈머가 브로커로부터 읽어가는 데이터 조각을 말합니다.

### 3.5 Producers
- 프로듀서는 events를 생성하여 Kafka로 전달하는 클라이언트 계층 APP 입니다.

### 3.6 consumer
- Kafka에서 events를 읽고 처리하는 APP 입니다.

### 3.7 Topic
- Kafka는 events를 토픽별로 저장하고 영구적(설정에 따라 다름) 으로 저장됩니다.
-Topic은 큐 (선입선출) 구조를 갖고 있으며 , 토픽의 이벤트는 필요한 만큼 자주 읽을 수 있습니다. 
- 기존 메시징 시스템과 달리 이벤트는 소비 후 삭제되지 않습니다. 대신 **Kafka가 주제별 구성 설정을 통해 이벤트를 유지해야 하는 기간을 정의**합니다. 그 후에는 이전 이벤트가 삭제됩니다. 
- Kafka의 성능은 데이터 크기와 관련하여 실질적으로 일정하므로 장기간 데이터를 저장해도 괜찮습니다.

### 3.8 partition
- topic은 여러 버킷에 분산되어 저장됩니다.

- 분산저장은 client app이 여러 브로커에서 병렬 처리가 가능하게끔 하여 속도 향상과 확장성에 도움을 주는데, 이러한 **events를 topic에 분산 저장 할 수 있게끔 Topic을 여러개로 나눈것**을 의미합니다.


![partition][partition]

[partition]:./images/partition.PNG


- 사진에서 P1, P2, P3, P4가 파티션들입니다.