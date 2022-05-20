# Redis info
- 해당 문서는 redis에 대한 이론과 사용 방안에대해 설명합니다.

## **Redis 정리 잘되어있는 블로그**

[Redis Cluster 구축 및 성능 튜닝](https://backtony.github.io/redis/2021-09-03-redis-3/#1-redis-cluster-%EA%B5%AC%EC%B6%95-%EB%B0%8F-%EC%9A%B4%EC%98%81)

## Redis cluster info

[Redis Cluster](https://lascrea.tistory.com/214)

## **Redis.conf info**

[해피쿠 블로그 - [Redis] Redis.conf에 대해 파헤쳐보자! (Snapshot 설정, AOF 설정)](https://www.happykoo.net/@happykoo/posts/42)

## Redis commands

[Redis 명령어](https://github.com/jjsair0412/kubernetes_info/blob/main/redis/redis%20command.md#redis-command)

---

## **redis 및 도커 설치**

### Redis 설치방법
1. **Redis 기본 설치방법**

[Redis 설치 및 실행](https://hyunalee.tistory.com/17)

[Redis Quick Start - Redis](https://redis.io/topics/quickstart)

Redis 설치 방법

```bash
$ wget http://download.redis.io/redis-stable.tar.gz
$ tar xvzf redis-stable.tar.gz
$ cd redis-stable

# make 없다면
$ sudo apt install make

$ make

$ sudo apt install redis-server
```


[Install Docker Engine on Ubuntu](https://docs.docker.com/engine/install/ubuntu/#installation-methods)

## RedisConfig 구성 방법

1. **redis.conf 파일 생성**

각 노드별 클러스터 개수만큼 폴더 생성 후 redis.conf 파일 생성

```bash
$ mkdir 7000 , 7001, 7002, 7003 ...

$ touch redis.conf
```

1. **redis.conf 파일 구성**

각 폴더에 redis.conf파일을 구성한다.

redis.conf sample file

[RedisInfo/redisconfigfile.sh at main · jjsair0412/RedisInfo](https://github.com/jjsair0412/RedisInfo/blob/main/redisconfigfile.sh)

### conf 주요부분

```bash
bind 0.0.0.0 
# 외부접속을 허용하기 위해서 bind 부분을 0.0.0.0으로 수정

cluster-enabled yes
# cluster mode를 허용하기위해 yes

logfile /home/chj/myHome/install/redis-cluster/7000/log/redis.log
# log기록이 남는 파일 위치
# 셈플코드에는 주석처리 되어 있음.

dir /home/ubuntu/redis-slaves/7005
# redis.conf 파일 위치. 여기서는 7005 폴더 안에 위치한다.

port 7005
# 해당 redis port번호 작성. 클러스터마다 달라야 한다.

replicaof <masterip> <masterport>
# redis.conf파일에서 master cluster를 구성할 수 있음.
# 그런데 나는 일단 redis 클러스터 올려 놓은 후 명령어로 master-slave 구성할거라
# 주석처리 했음
```

**관련 포스팅**

[redis configuration file](http://redisgate.kr/redis/server/redis_conf_han.php)

redis.conf파일에서 replicaof 옵션 사용해 master-slave 설정하는 방법 블로그

[Redis master / slave 설정](https://anywon.tistory.com/6?category=318748)

1. **redis 실행**

만들어준 redis.conf파일을 이용해서 redis 실행

```bash
# 사용 방법
$ redis-server <redis.conf_file_path>

# 7000번 폴더의 redis.conf파일을 이용해서 redis 올리는 예
$ redis-server ./7000/redis.conf
```

1. **각 redis 외부접속 확인**

redis-cli 명령어를 통해서 redis 접속 테스트

```bash
$ redis-cli -c -p <port-번호> -h <ip-addr>
```

나의 경우에는 aws ec2 인스턴스를 활용해서 서버를 올려놓고 ,

각 ec2에 redis를 구성했기 때문에 , ec2의 public ip를 통해서 접근. 

당연히 보안그룹에 포트포워딩 설정.

만약 폐쇄망이라면 해당 서버 ip를 작성.

1. **redis 클러스터 구성**

redis-cli 명령어를 통해서 master-slave 구성.

```bash
$ redis-cli --cluster craete <ip-addr>:<redis-port> <ip-addr>:<redis2-port> ,,, --cluster-replicas <master별 slave 개수 설정>

# 사용 예
$ redis-cli --cluster create 127.0.0.1:5001 127.0.0.1:5002 127.0.0.1:5003 127.0.0.1:5004 127.0.0.1:5005 127.0.0.1:5006 --cluster-replicas 1
```

주의할 점

create할때 ip주소를 해당 redis 접속하기위한 public ip를 주어야 함. 

aws ec2인스턴스 내부에 redis가 설치되어 있다면 , ec2의 public ip를 주어야 한다.

폐쇄망이라면 해당 서버 ip를 주어야 한다.

관련 포스팅

[redis redis-cli cluster](http://redisgate.kr/redis/cluster/redis-cli-cluster.php)

클러스터 자동 구성

[redis Cluster Configuration](http://redisgate.kr/redis/cluster/cluster_configuration.php)

redis 클러스터 수동 구성 방법

[redis Cluster Design 레디스 클러스터 설계](http://redisgate.kr/redis/cluster/cluster_design.php)

1. **redisinsight 설치**

[Redisinsight](https://www.notion.so/Redisinsight-9483935195ed4bd5baa3b9a88826626b)

1. **redisinsight 포트포워딩** 

포워딩 후 접근 및 redisinsight 설정

---

## Cluster - slave 수동설정하는방법

- **참고**

하나의 master는 여러개의 slave를 가질 수 있다.

slave는 replica라는 단어로 변경되었다.

하나의 slave는 여러개의 master를 가질 수 없다.

연결되어있는 master가 없어지면 , slave는 자동으로 다른 master를 찾아간다.

1. **slots 구성**

master가 될 cluster에 slots을 설정한다.

```bash
# 예시
$ redis-cli -p <port_number> -h <ip-addr> cluster addslots {0..16383}

# 사용 예
$ redis-cli -p 7000 cluster addslots {0..16383}
```

위 명령어는 16384개의 slots을 127.0.0.1:7000 redis에 설정하는 명령이다.

1. **meet**

master와 slave를 만나게 한다.

```bash
# 127.0.0.1:7001번 slave cluster에서 7000번 master를 등록
127.0.0.1:7001> cluster meet 127.0.0.1 7000
```

1. **replicate**

slave cluster를 등록한다.

slave cluster cli에 들어가서 작성해야 한다.

```bash
# 예시
cluster replicate <id값>

# 실제 사용 예
127.0.0.1:7001> cluster replicate b4ba8719b121756dc2f3a467962afbab6cfefe4c
```

위 id값을 갖고잇는 redis에게 7001번 cluster를 master로 등록한다.

---

## Cluster 삭제하는 방법

1. **cluster forget**

등록된 클러스터 정보를 제거한다.

redis-cli 명령어를 통해 대상 redis로 접속 후 명령어 실행한다.

```bash
127.0.0.1:7000> cluster forget <cluster-id>

# 사용 예
127.0.0.1:7000> cluster forget d79e78471992306d7ed415a6089467ac04fb6dfc
```

1. **slots 재할당 or 삭제**

master일 경우에만 slots을 다른 master에 재 할당 하거나 , 삭제한다.

master에 slots이 한개라도 있다면 , 삭제가 안돼기 때문이다.

```bash
# slots 재할당
$ redis-cli --cluster reshard <redis-ip>:<redis-port>

# 이후 재 할당할 양의 slots 개수를 입력하고, 
# 재 할당할 master cluster의 id값을 입력하면 
# 입력한 id값의 master로 slots이 이동한다.
# 아래는 사용 예

$ redis-cli --cluster reshard 127.0.0.1:7000
```

slots 삭제

[redis Cluster Delslots : 슬롯을 삭제하는 명령](http://redisgate.kr/redis/cluster/cluster_delslots.php)

1. **cluster delete** 

최종적으로 cluster를 삭제한다.

```bash
$ redis-cli --cluster del-node <redis-ip>:<redis-port> <cluster-id>

# 사용 예
$ redis-cli --cluster del-node 127.0.0.1:7003 7cf47abcb0d65b7f3ef1a81042b17ce1277ee4d4
```

**관련 포스팅** 

[Redis Cluster (node 추가 및 삭제)](https://blog.naver.com/PostView.nhn?blogId=theswice&logNo=221524069567&parentCategoryNo=&categoryNo=24&viewDate=&isShowPopularPosts=true&from=search)

[redis Cluster Delslots : 슬롯을 삭제하는 명령](http://redisgate.kr/redis/cluster/cluster_delslots.php)