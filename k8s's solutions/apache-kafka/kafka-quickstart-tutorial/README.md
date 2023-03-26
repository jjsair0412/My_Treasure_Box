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

ubuntu 전체 유저에 설정해야하기 때문에 bashrc에 export를 넣어줍니다.

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
- 만약 java_home path에 

## 5. Topic 생성하기
