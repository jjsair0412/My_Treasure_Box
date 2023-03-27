# Kafka_Connect_사용방안
## 0. **precondition**
해당 문서 예제를 진행하기 전에 , 꼭 상위 디렉토리의 [**Kafka_기본_사용방법**](../kafka-quickstart-tutorial/kafka_%EA%B8%B0%EB%B3%B8_%EC%82%AC%EC%9A%A9%EB%B0%A9%EB%B2%95.md)을 진행한 뒤에 실습합니다.

튜토리얼 예제는 다음 공식 docs와 블로그를 참고하여 진행하였습니다.
- https://cjw-awdsd.tistory.com/53
- 공식 docs : https://kafka.apache.org/quickstart#quickstart_kafkaconnect

## 1. Kafka Connect를 사용하여 이벤트 스트림으로 데이터 가져오기 - 이론
카프카는 프로듀서와 컨슈머를 통해 데이터 파이프라인을 만들 수 있습니다.

예를 들어 A서버의 DB에 저장한 데이터를 Kafka Producer/Consumer를 통해 B서버의 DB로도 보낼 수 있다. 

이런 데이터 파이프라인이 여러개라면 , 반복적으로 파이프라인을 사람이 구성해줘야 하는데 , 이런 귀찮음을 해결해주기 위해서 Kafka Connect를 사용합니다.
- 이걸 잘 사용하면 , msa환경에서 데이터 무결성을 체크할수 있을지 모르겠습니다. !

Kafka Connect를 사용하면 , 외부 시스템에서 Kafka로 , 또는 Kafka에서 외부 시스템으로 데이터를 지속적으로 수집할 수 있습니다.

외부 시스템과 상호작용하기위한 미들웨어라고 생각하면 됩니다.

![test-2][test-2]

[test-2]:./images/test-2.PNG
- Kafka Connect 파이프라인 아키텍처.
- 출처 : https://cjw-awdsd.tistory.com/53

위 아키텍쳐에서 , 각 구성요소는 다음을 의미합니다.
1. Connect
- connector를 동작하게하는 프로세서(서버)
2. Connector
- Data Source(DB)의 데이터를 처리하는 소스가 들어있는 jar파일
3. Source Connector
- data source에 담긴 데이터를 topic에 담는 역할(Producer)을 하는 connector
4. Sink Connector
- topic에 담긴 데이터를 특정 data source로 보내는 역할(Consumer 역할)을 하는 connector


Kafka Connect는 REST API로 Connector를 등록 및 사용할 수 있습니다.

또한 Connect는 단일 모드(Standalone) 와 분산모드 (Distributed)로 이루어져 있습니다.
1. 단일 모드(Standalone)
- 하나의 Connect만 사용하는 모드

2. 분산 모드(Distributed)
- 여러개의 Connect를 한개의 클러스트로 묶어서 사용하는 모드.
  특정 Connect가 장애가 발생해도 나머지 Connect가 대신 처리하도록 함


### 1.1 jar파일 가져오기
해당 튜토리얼에서는 Kafka Connect를 jar파일로 가져와서 사용합니다.

따라서 아래 경로에 있는 connect-standlone.properties 파일에 , connect-file-3.4.0.jar 파일 경로를 지정해주어야 합니다.
```bash
# connect-standlone.properties 경로
$ pwd
~/kafka/kafka_2.13-3.4.0/config/connect-standalone.properties
```

해당 jar는 아래 경로에 위치합니다.
```bash
$ pwd
~/kafka/kafka_2.13-3.4.0/libs/connect-file-3.4.0.jar
```

vi에디터로 connect-standlone.properties 파일에 해당 경로 입력해줍니다.
```bash
$ cat ~/kafka/kafka_2.13-3.4.0/config/connect-standalone.properties
...
plugin.path=~/kafka_2.13-3.4.0/libs/connect-file-3.4.0.jar
```

