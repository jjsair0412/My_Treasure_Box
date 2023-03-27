# kafka-quickstart-tutorial
## **precondition**
해당 문서는 apache kafka 기본 계념을 잡기 위해 docker나 k8s 없이 그냥 설치하여 , producer, consumer , topic 등을 실습하는 튜토리얼 문서 입니다.

apache kafka의 공식 download 사이트는 다음과 같습니다.
- https://kafka.apache.org/downloads

해당 문서에서 사용된 kafka version은 **2.13.0** 입니다.

튜토리얼은 kafka 공식 문서와 블로그를 참조하였습니다.
- https://kafka.apache.org/quickstart
- https://soyoung-new-challenge.tistory.com/61

***kafka를 실행하기 위해선 , local java version이 꼭 8 이상이여야만 합니다 !***

## 0. 환경 구성
kafka를 사용하기 위해서 , local에는 무조건 java 8 이상의 java가 설치되어있어야만 합니다.

**주의**
-  readlink which 명령어로 나온 java 경로를 전부다 넣는게 아니라 , jre 경로 전까지만 넣어야 합니다 . !

```bash
$ sudo apt-get install openjdk-8-jdk -y

# 설치 확인
$ java -version
```

설치된 jdk를 java home path로 설정합니다.

ubuntu 전체 유저 ( root 제외 ) 에 해당 환경변수를 등록해야 하기 때문에 /etc/profile에 넣어줍니다.
- 만약 root user도 적용하고 싶다면  , /root/.bashrc 에 export명령어를 등록합니다.
- login user (ubuntu에 ssh 연결 , id paswd 입력 연결) 는 기본적으로 /etc/profile 설정을 가지고 로그인합니다.
- 그러나 root user는 , profile이 아닌 /root/.bashrc를 갖고 올라와서 로그인 합니다. 

```bash
# profile
$ vi /etc/profile

# 환경변수 추가
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/
export PATH="$PATH:$JAVA_HOME"

# source로 재시작안해도 반영되게끔 구성
$ source /etc/environment
```

jdk 환경변수가 잘 세팅되었는지 확인합니다.
```bash
$ echo $JAVA_HOME
/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java
```

java가 잘 설치되었는지 확인합니다.
```bash
$ java -version
openjdk version "1.8.0_362"
OpenJDK Runtime Environment (build 1.8.0_362-8u362-ga-0ubuntu1~20.04.1-b09)
OpenJDK 64-Bit Server VM (build 25.362-b09, mixed mode)
```

## 1. kafka 설치
### 1.1 kafka 압축 파일 및 해제
wget으로 apache kafka를 받아온 뒤 tar파일을 압축 해제 합니다..

```bash
$wget https://downloads.apache.org/kafka/3.4.0/kafka_2.13-3.4.0.tgz

$tar -xzf kafka_2.13-3.4.0.tgz 

$ls
kafka_2.13-3.4.0  kafka_2.13-3.4.0.tgz
```

## 2. kafka envirement 설정
apache kafka는 실행하기 위해서 **Zookeeper**나 **KRaft**를 꼭 함께 사용해야만 합니다.

해당 튜토리얼에선 두가지 모두 정리하지만 , 실제 실습할땐 둘중 하나만 사용해야만 합니다. !

Zookeeper나 KRaft 모두 kafka tar파일 안에 설정파일과 실행 쉘 스크립트 파일을 가지고 있습니다.
### 2.1 Zookeeper / KRaft 설정
#### 2.1.1 Zookeeper conf파일 확인
먼저 Zookeeper conf파일을 확인합니다.

해당 conf파일에서 Zookeeper의 세부 설정값을 변경하여 설치가 가능합니다.
- 외부 포트나 ip바인딩을 해당 conf파일로 진행합니다.
```bash
# Zookeeper
vi ~/kafka_2.13-3.4.0/config/zookeeper.properties

# KRaft
vi ~/kafka_2.13-3.4.0/config/kraft/server.properties
```

## 3. Zookeeper 실행
만들어둔 properties를 가지고 Zookeeper를 실행합니다.
```bash
# zookeeper
$ vi bin/zookeeper-server-start.sh
```

Zookeeper를 daemon으로 실행시키기 위해 , -daemon 파라미터값을 스크립트 돌릴 때 같이 부여하여 Zookeeper를 실행합니다.

그러나 튜토리얼 문서기 때문에 , 로그확인을 위해 그냥 실행합니다.
```bash
$ cd ~/kafka/kafka_2.13-3.4.0/bin

# 실수행 명령어
$ ./zookeeper-server-start.sh ~/kafka/kafka_2.13-3.4.0/config/zookeeper.properties

# daemon 실행
$ ./zookeeper-server-start.sh -daemon ~/kafka/kafka_2.13-3.4.0/config/zookeeper.properties
```

실행 로그를 확인합니다.
```bash
...
[2023-03-26 13:08:23,514] INFO zookeeper.request_throttler.shutdownTimeout = 10000 (org.apache.zookeeper.server.RequestThrottler)[2023-03-26 13:08:23,535] INFO Using checkIntervalMs=60000 maxPerMinute=10000 maxNeverUsedIntervalMs=0 (org.apache.zookeeper.server.ContainerManager)[2023-03-26 13:08:23,536] INFO ZooKeeper audit is disabled. (org.apache.zookeeper.audit.ZKAuditProvider)
```


## 4. kafka 실행
다른 터미널을 열고 , kafka를 실행합니다.

kafka또한 데몬으로 실행하기 위해선 , -daemon 파라미터를 추가로 부여해야만 합니다.
```bash
$ cd ~/kafka/kafka_2.13-3.4.0/bin

# 일반 실행
$ ./kafka-server-start.sh config/server.properties

# daemon 실행
$ ./kafka-server-start.sh -daemon config/server.properties
```
- 만약 java_home path에 bin폴더까지 추가되어 있다면 , 환경변수를 아래 경로 level에 맞게 변경해줍니다.
```
/usr/lib/jvm/java-8-openjdk-amd64/
```

## 5. event를 저장할 topic 생성하기
Kafka는 여러 시스템에서 event ( 문서에서 레코드 또는 메시지 라고도 함 ) 를 읽고, 쓰고, 저장하고, 처리할 수 있는 분산 이벤트 스트리밍 플랫폼 입니다.

이러한 여러 시스템에서 나오는 여러 이벤트들 ( 결제 거래, 휴대폰의 지리적 위치 업데이트 ) 을 저장할 topic을 event 생성하기 전 만들어주어야 합니다. 

bin파일의 kafka-topic.sh 쉘 스크립트로 카프카 토픽을 생성합니다.
command의 구성요소 설명은 다음과 같습니다.
0. bin/kafka-topics.sh : 토픽 실행 파일 
1. --create : 토픽 생성 파라미터
2. bootstarap.server : 호스트/포트 지정
    - bootstrap.server의 자세한 설명은 상위 디렉토리에 있습니다.
    - 
3. --replication-factor : replica 갯수 지정 (복제본의 수)
4. --partitions : paritions 갯수 지정 (토픽을 몇개로 나눌 것인가)
    - 파티션관련 이론 정보 : https://kafka.apache.org/documentation/#intro_concepts_and_terms
    - 파티션들이 모인 한 묶음을 토픽이라고 생각하면 됩니다.
5.  --topic [이름] : 생성될 토픽의 이름 지정

새로운 터미널을 열고 , 아래 명령어로 토픽을 생성합니다.
```bash
$ cd ~/kafka/kafka_2.13-3.4.0/bin

$ ./kafka-topics.sh --create --topic jjs-events --bootstrap-server localhost:9092
Created topic jjs-events.
```

jjs-events topic이 생성됩니다.

생성된 토픽을 아래 명령어로 확인해 봅니다.
```bash
$ cd ~/kafka/kafka_2.13-3.4.0/bin

$ ./kafka-topics.sh --list --bootstrap-server localhost:9092
jjs-events
```

## 6. 생성한 topic에 몇가지 event 쓰기
이전에 생성한 jjs-events 토픽에 event를 넣습니다.

kafka client는 이벤트 쓰기 (또는 읽기) 작업을 수행하기 위해 네트워크를 통하여 kafka 브로커와 통신합니다.
- kafka 브로커는 , 카프카 자체를 의미합니다. 
- 관련 문서 : https://always-kimkim.tistory.com/entry/kafka101-broker

일단 브로커는 event를 수신하면 필요한 기간 동안 (영원히도 가능) 지속적이고 내 결함성 있는 방식으로 이벤트를 저장합니다.

./kafka-console-consumer.sh 쉘 스크립트로 topic안에 넣을 이벤트를 생성하거나 읽을 수 있습니다.

command로 이벤트를 topic에 작성합니다.

```bash
$ cd ~/kafka/kafka_2.13-3.4.0/bin

$ ./kafka-console-producer.sh --topic {topic_name} --bootstrap-server localhost:9092

$ ./kafka-console-producer.sh --topic jjs-events --bootstrap-server localhost:9092
>hi
>test
...
```

>에 메시지를 작성하고 , 엔터를 누르면 브로커안에 토픽속 파티션에 메시지가 큐(선입선출) 방식으로 들어가게 됩니다.

메시지를 작성하는건 ```Ctrl-C```로 언제든지 빠져나올 수 있습니다,

테스트를 위해 빠져나오지 않고 , 메시지 읽는 터미널을 하나 새로 켜서 메시지가 지속적으로 새로고침되는것을 확인해 봅니다.

## 6. 생성한 topic에 들어간 event 읽기
topic 안에 파티션 속 차례대로 들어간 메세지를 이제 읽어보도록 하겠습니다.

새로운 터미널을 열고 , 아래 command로 topic에 들어간 events (메시지)를 읽어봅니다.
```bash
$ cd ~/kafka/kafka_2.13-3.4.0/bin

$ ./kafka-console-consumer.sh --topic jjs-events --from-beginning --bootstrap-server localhost:9092
hi
test
```

events를 생성하는 대로 브로커 내부 토픽에 출력되는것을 확인할 수 있습니다.
- 좌측 : event 읽기
- 우측 : event 쓰기

![test-1][test-1]

[test-1]:./images/test-1.PNG

- 이러한 kafka를 잘 사용하면 , 카톡같은 실시간 메신저 application도 만들 수 있을것 같습니다.