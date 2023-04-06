# Kafka_Connect_사용방안
## 0. **precondition**
해당 문서 예제를 진행하기 전에 , 꼭 상위 디렉토리의 [**Kafka_기본_사용방법**](../kafka-quickstart-tutorial/kafka_%EA%B8%B0%EB%B3%B8_%EC%82%AC%EC%9A%A9%EB%B0%A9%EB%B2%95.md)을 진행한 뒤에 실습합니다.

튜토리얼 예제는 다음 공식 docs와 블로그를 참고하여 진행하였습니다.
- https://cjw-awdsd.tistory.com/53
- 공식 docs : https://kafka.apache.org/quickstart#quickstart_kafkaconnect

## 0.1 MySQL 설치
해당 실습 예제는 MySQL을 사용하기 때문에 , linux 시스템으로 MySQL을 설치합니다.
```bash
# mysql-server 설치
$ sudo apt-get install mysql-server -y

# MySQL 3306 포트 open
$ sudo ufw allow mysql

# MySQL 실행
$ sudo systemctl start mysql
$ sudo systemctl enable mysql

# MySQL status 확인
$ systemctl status mysql
● mysql.service - MySQL Community Server
     Loaded: loaded (/lib/systemd/system/mysql.service; enabled; vendor preset: enabled)     
     Active: active (running) since Tue 2023-03-28 05:10:34 UTC; 44min ago
   Main PID: 16261 (mysqld)
     Status: "Server is operational"
      Tasks: 39 (limit: 4677)
     Memory: 362.3M
     CGroup: /system.slice/mysql.service
             └─16261 /usr/sbin/mysqld
```

설치가 완료됐다면 , MySQL에 접속하여 Default schema와 users table을 생성합니다.
```bash
# MySQL 접속
$ sudo /usr/bin/mysql -u root -p
mysql>
...
```

test schema 생성 및 test schema user 생성
```bash
mysql> CREATE SCHEMA test;

mysql> CREATE TABLE test.users (
       id INT PRIMARY KEY AUTO_INCREMENT,
       name VARCHAR(20)
      );
```

test schema 및 users table이 정상 생성됐는지 확인합니다.
```bash
mysql> show databases;
+--------------------+
| Database           |
+--------------------+
| information_schema |
| mysql              |
| performance_schema |
| sys                |
| test               |
+--------------------+
5 rows in set (0.00 sec)
...

mysql> use test
Database changed

...
mysql> show tables;
+----------------+
| Tables_in_test |
+----------------+
| users          |
+----------------+
1 row in set (0.00 sec)
```
root@localhost로 접근해야 하기 때문에 , root 계정의 비밀번호를 설정합니다.
```bash
mysql> alter user 'root'@'localhost' identified by '<비번입력>';

# 실 수행 명령어
mysql> alter user 'root'@'localhost' identified by '1234';
```

## 1. Kafka Connect를 사용하여 이벤트 스트림으로 데이터 가져오기 - 이론
카프카는 프로듀서와 컨슈머를 통해 데이터 파이프라인을 만들 수 있습니다.

예를 들어 A서버의 DB에 저장한 데이터를 Kafka Producer/Consumer를 통해 B서버의 DB로도 보낼 수 있습니다.

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

## 2. 실습
### 2.1 kafka 설치 및 실행
- [**Kafka_기본_사용방법**](../kafka-quickstart-tutorial/kafka_%EA%B8%B0%EB%B3%B8_%EC%82%AC%EC%9A%A9%EB%B0%A9%EB%B2%95.md)

위 문서를 참고하여 Kafka를 설치하고 , 주키퍼와 카프카를 실행합니다.

### 2.1 Kafka Connect 설치
connector를 동작하게하는 프로세서(서버)인 Connect를 설치합니다.

해당 튜토리얼에서는 Kafka Connect를 jar파일로 가져와서 사용합니다.
- 만약 없다면 , 아래 링크를 wget하여 connect를 받아오고 path에 입력합니다.
```
$ wget https://packages.confluent.io/archieve/6.1/confluent-community-6.1.0.tar.gz
```

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

경로 지정 후 , connect-distributed.sh 로 connect를 실행합니다.
```bash
# usecase
./bin/connect-distributed.sh ~/kafka/kafka_2.13-3.4.0/config/connect-distributed.properties

# 백그라운드 실행 옵션 -d 추가본 명령어
./bin/connect-distributed.sh -d ~/kafka/kafka_2.13-3.4.0/config/connect-distributed.properties
```

잘 실행됐다면 , 다음 토픽 리스트 명령어를 실행했을 때, 다음 topic이 생성된 것을 확인할 수 있습니다.
```bash
$ ./bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
__consumer_offsets
connect-configs
connect-offsets
connect-status
```

### 2.2 Kafka JDBC Connector 설치
confluent.io 사이트에서 connect를 설치합니다.

confluent 공식 docs 사이트를 참고해서 , confluent hub client를 사용해 설치하거나 , zip 파일을 wget으로 가져와서 설치합니다.
- docs : https://docs.confluent.io/5.5.1/connect/kafka-connect-jdbc/index.html#jdbc-connector-source-and-sink-for-cp

해당 문서에선 Confluent Hub를 tarball로 가지고와서 confluent hub client를 설치한 이후 , connect를 설치합니다.
- tarball 설치 가이드 : https://docs.confluent.io/kafka-connectors/self-managed/confluent-hub/client.html#linux

```bash
# wget
$ wget http://client.hub.confluent.io/confluent-hub-client-latest.tar.gz

# unzip
$ tar -xvf confluent-hub-client-latest.tar.gz

# unzip 결과 확인
$ ls
bin  confluent-hub-client-latest.tar.gz  etc  share
```

confluent-hub tar파일을 압축 해제한 디렉토리 경로를 profile이나 bashrc에 export로 등록해 줍니다.

해당 문서는 root를 제외한 유저에게 등록하기 위해서 , profile에 등록합니다.
```bash
$ vi /etc/profile
...
export CONFLUENT_HOME=/home/vagrant/confluenthub
export PATH="$PATH:$CONFLUENT_HOME/bin"

# 등록
$ source /etc/profile

# 설치 확인
$ confluent-hub 
usage: confluent-hub <command> [ <args> ]

Commands are:
    help      Display help information
    install   install a component from either Confluent Hub or from a local file

See 'confluent-hub help <command>' for more information on a specific command.
```

이상태로 설치 진행하면 다음과 같은 에러가 발생합니다.
```bash
Unable to detect Confluent Platform installation. Specify --component-dir and --worker-configs explicitly. 
```

따라서 component-dir 와 worker-configs를 생성시켜준 뒤 경로를 명시해주어야 합니다.
- worker.properties는 빈 파일 입니다.
```bash
# component-dir 생성
$ mkdir connect

# worker-configs 생성
$ cd connect
$ cat worker.properties
```

설치한 confluent-hub와 만들어준 compoent dir , worker-config로 connector를 설치합니다.
- component dir, worker-config는 파일 경로를 명시해 주면 됩니다.

**해당 문서를 작성하는 시점에는 latest를 설치합니다.**

```bash
# latest 설치
$ confluent-hub install confluentinc/kafka-connect-jdbc:latest --component-dir ~/connect --worker-configs ~/connect/worker.properties

# version 명시 설치
$ confluent-hub install confluentinc/kafka-connect-jdbc:5.5.1 --component-dir ~/connect --worker-configs ~/connect/worker.properties
```

component dir 경로에 connect와 , worker-config파일에 설치된 파일 경로가 자동으로 기입된 것을 확인할 수 있습니다.
```bash
$ pwd
~/connect

$ ls
confluentinc-kafka-connect-jdbc  worker.properties

$ cat worker.properties 
plugin.path = /home/vagrant/connect
```

설치한 connect의 lib 경로에 가면 , 다양한 DB 커넥터 라이브러리들이 존재하는것을 볼 수 있습니다.

```bash
$ cd ~/confluentinc-kafka-connect-jdbc/lib

$ ls -al -h
total 21M
drwxrwxr-x 2 vagrant vagrant  4.0K Apr  5 07:37 .
drwxrwxr-x 6 vagrant vagrant  4.0K Apr  5 07:37 ..
-rw-rw-r-- 1 vagrant vagrant  210K Apr  5 07:37 checker-qual-3.5.0.jar
-rw-rw-r-- 1 vagrant vagrant   17K Apr  5 07:37 common-utils-6.0.0.jar
-rw-rw-r-- 1 vagrant vagrant  311K Apr  5 07:37 jtds-1.3.1.jar
-rw-rw-r-- 1 vagrant vagrant  270K Apr  5 07:37 kafka-connect-jdbc-10.6.4.jar
-rw-rw-r-- 1 vagrant vagrant  1.3M Apr  5 07:37 mssql-jdbc-8.4.1.jre8.jar
-rw-rw-r-- 1 vagrant vagrant  4.2M Apr  5 07:37 ojdbc8-19.7.0.0.jar
-rw-rw-r-- 1 vagrant vagrant  5.1K Apr  5 07:37 ojdbc8-production-19.7.0.0.pom
-rw-rw-r-- 1 vagrant vagrant  153K Apr  5 07:37 ons-19.7.0.0.jar
-rw-rw-r-- 1 vagrant vagrant  304K Apr  5 07:37 oraclepki-19.7.0.0.jar
-rw-rw-r-- 1 vagrant vagrant  1.6M Apr  5 07:37 orai18n-19.7.0.0.jar
-rw-rw-r-- 1 vagrant vagrant  206K Apr  5 07:37 osdt_cert-19.7.0.0.jar
-rw-rw-r-- 1 vagrant vagrant  305K Apr  5 07:37 osdt_core-19.7.0.0.jar
-rw-rw-r-- 1 vagrant vagrant 1022K Apr  5 07:37 postgresql-42.4.3.jar
-rw-rw-r-- 1 vagrant vagrant   32K Apr  5 07:37 simplefan-19.7.0.0.jar
-rw-rw-r-- 1 vagrant vagrant   41K Apr  5 07:37 slf4j-api-1.7.36.jar
-rw-rw-r-- 1 vagrant vagrant  6.8M Apr  5 07:37 sqlite-jdbc-3.25.2.jar
-rw-rw-r-- 1 vagrant vagrant  1.7M Apr  5 07:37 ucp-19.7.0.0.jar
-rw-rw-r-- 1 vagrant vagrant  259K Apr  5 07:37 xdb-19.7.0.0.jar
-rw-rw-r-- 1 vagrant vagrant  1.9M Apr  5 07:37 xmlparserv2-19.7.0.0.jar
```

그리고 해당 lib 파일 경로를 , kafka의 connect-distributed.properties plugin 경로로 추가 합니다.
```bash
$ vi ~/kafka/kafka_2.13-3.4.0/config/connect-distributed.properties
...
plugin.path=/home/vagrant/connect/confluentinc-kafka-connect-jdbc/lib
```

connect-distributed를 재 실행 합니다.
```bash
# usecase
./bin/connect-distributed.sh ~/kafka/kafka_2.13-3.4.0/config/connect-distributed.properties

# 백그라운드 실행 옵션 -d 추가본 명령어
./bin/connect-distributed.sh -d ~/kafka/kafka_2.13-3.4.0/config/connect-distributed.properties
```

토픽 생성 확인
```bash
$ ./kafka-topics.sh --bootstrap-server localhost:9092 
--list
__consumer_offsets
connect-configs
connect-offsets
connect-status
...
```

커넥터가 잘 실행중인지 8083번 포트로 GET요청을 날려서 확인해봅니다.
```bash
$ curl -X GET  http://127.0.0.1:8083/connectors
[]
```

커넥터는 실행중인데 , Source Connector가 없습니다.

Source Connector는 MySQL Connector를 구성한 이후 생성합니다.


### 2.3 Connector 설치 - Mysql Connector 설치
Connector에서 Mysql을 사용하기 위해 Mysql의 Connector를 설치 합니다.
- [공식 설치 링크](https://dev.mysql.com/downloads/connector/j/)

mysql이 설치된 vm os version은 ubuntu 20.04를 사용하기에 , 해당 버전에 맞는 커넥터를 설치합니다.

홈페이지에서 링크 따라간다음 No thanks, 링크를 wget으로 deb파일 설치합니다.
```bash
$ wget ~

$ ls
mysql-connector-j_8.0.32-1ubuntu20.04_all.deb
```
이후 해당 deb파일을 압축해제해서 , 아래 경로로 이동 후 jar파일을 확인합니다.

```bash
# deb파일 dpkg
$ dpkg -x mysql-connector-j_8.0.32-1ubuntu20.04_all.deb  . 

# .jar 위치로 이동
$ cd ~/mysql-connector/usr/share/java

# connector jar파일 확인
$ ls
mysql-connector-j-8.0.32.jar
```

그리고 나온 mysql-connector-j-8.0.32.jar 파일을  connector에 등록해야하기 때문에, kafka 디렉토리의 lib 안에 넣습니다.
```bash
cp /home/vagrant/mysql-connector/usr/share/java/mysql-connector-j-8.0.32.jar  ~/kafka/kafka_2.13-3.4.0/libs
```

connect-distributed를 재 실행 합니다.
```bash
# usecase
./bin/connect-distributed.sh ~/kafka/kafka_2.13-3.4.0/config/connect-distributed.properties

# 백그라운드 실행 옵션 -d 추가본 명령어
./bin/connect-distributed.sh -d ~/kafka/kafka_2.13-3.4.0/config/connect-distributed.properties
```

지금까지 설정한 플러그인이 모두 정상 등록되었는지 확인합니다.
```bash
curl -X GET curl 127.0.0.1:8083/connector-plugins
```

### 2.4 Connector 생성
Source Connector를 json 형식으로 만든 다음. 해당 json을 Connector 포트인 8083번에 post를 날려서 생성해 줍니다.

먼저, Source Connector 명세를 가진 json을 생성합니다.
```json
$ cat mySourceConnect.json
{
    "name": "jjs-source-connect",
    "config": {
        "connector.class": "io.confluent.connect.jdbc.JdbcSourceConnector",
        "connection.url": "jdbc:mysql://127.0.0.1:3306/test",
        "connection.user":"root",
        "connection.password":"1234",
        "mode":"incrementing",
        "incrementing.column.name" : "id",
        "table.whitelist" : "users",
        "topic.prefix" : "example_topic_",
        "tasks.max" : "1"
    }
}
```

각 필드에대한 설명은 다음과 같습니다.
- **name** : Source Connector 이름
- **connector.class** : 커넥터 종류(JdbcSourceConnector 사용)
- **connection.url** : jdbc기 때문에 , DB 접근정보 입력
- **connection.user** : DB 접근 계정
- **connection.password** : DB 접근 계정 비밀번호 . (여긴 비번없어서 공백)
- **mode** : 테이블에 데이터가 추가됐을 때 데이터를 polling 하는 방식의 종류 선언 (모드 종류는 아래)
  - bulk : 데이터를 폴링할 때 마다 전체 테이블을 복사
  - incrementing : 특정 컬럼의 중가분만 감지되며, 기존 행의 수정과 삭제는 감지되지 않음
  - incrementing.column.name : incrementing 모드에서 새 행을 감지하는 데 사용할 컬럼명
  - timestamp : timestamp형 컬럼일 경우, 새 행과 수정된 행을 감지함
  - timestamp.column.name : timestamp 모드에서 COALESCE SQL 함수를 사용하여 새 행 또는 수정된 행을 감지
  - timestamp+incrementing : 위의 두 컬럼을 모두 사용하는 옵션

- **incrementing.column.name** : incrementing mode일 때 자동 증가 column 이름
- **table.whitelist** :  데이터를 변경을 감지할 table 이름
- **config.topic.prefix** : kafka 토픽에 저장될 이름 형식 지정. 위 같은경우 whitelist를 뒤에 붙여 example_topic_users에 데이터가 들어감
- **tasks.max** : 커넥터에 대한 작업자 수

***자세한 속성은 다음 공식문서 확인***
- https://docs.confluent.io/kafka-connectors/jdbc/current/sink-connector/sink_config_options.html#writes
 

생성한 명세를 8083 포트로 post 요청 보내서 Connector를 생성합니다.
```bash
curl -X POST -H "content-Type:application/json" http://localhost:8083/connectors -d @mySourceConnect.json
```

정상적으로 생성된 것을 확인합니다.
```bash
$ curl  -X GET  http://127.0.0.1:8083/connectors
["jjs-source-connect"]
```

### 2.5 DB 데이터 insert
이제 mysql DB에 데이터를 INSERT 시킵시다.
```bash
mysql> use test;

# users 테이블 있는지 확인
mysql> show tables;

```

```bash
# 토픽 생성됬는지 확인
$ ./bin/kafka-topics.sh --list --bootstrap-server localhost:9092
```


### 2.6 Sink Connector 생성
생성된 토픽을 받을 대상 DB의 Sink Connector를 생성합니다.

이 문서는 local mysql -> local mysql 이기 때문에 , 그냥 로컬에서 진행하고 커넥터도 동일합니다.
```json
{
    "name": "jjs-pksink-connect-three",
    "config": {
        "connector.class": "io.confluent.connect.jdbc.JdbcSinkConnector",
        "connection.url": "jdbc:mysql://localhost:3306/test",
        "connection.user":"root",
        "connection.password":"1234",
        "auto.create":"true",
        "auto.evolve":"true",
        "delete.enabled":"false",
        "tasks.max":"1",
        "topics":"example_topic_users"
    }
}
```