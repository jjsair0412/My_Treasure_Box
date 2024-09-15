# redis command
- 해당 문서는 redis 명령어에 대한 문서입니다.

아래 명령어중 cli 접속 후 라는 말은 , master 에 접속한다는말이다.

1. **redis cluster 모드(다른 노드에 있는 키/값 보는 법) , 각 노드에 접속하는 방법**

```bash
# 아래 명령어는 각 노드에 접속하는 명령어이다.
$ redis-cli –c –p port
# 노드 접속방법 사용 예 :

$ redis-cli -c -p 7001
# 위처럼 사용하면 , default ip주소인 127.0.0.1:7001 에 접속하게 된다..
# ip주소를 다른것으로 변경하려면 , -h 옵션 사용하면 된다.

$ redis-cli -c -h <ip주소> -p <port_번호>
 ex ) redis-cli -c -h 192.168.x.x -p 1000

$ get key

사용 예 ) get 1
```

-c 옵션 중요하다. port부분에 해당 노드 포트번호 들어감

1. **cluster 내 모든 키 검색**

```bash
redis-cli --cluster call IP:port keys "*"
ex)redis-cli –cluster call 127.0.0.1:6301 keys “*”
```

1. **Redis 데이터 입력 , 수정 , 삭제 , 조회**

[Redis 데이터 입력, 수정, 삭제, 조회](https://sungwookkang.com/1313)

1. **슬레이브 노드를 마스터 노드로 역할을 바꾸어 주는 명령 (슬레이브 노드에서만 사용 가능)**

```bash
redis-cli cluster failover [FORCE|TAKEOVER]
```

No option: 마스터가 살아있을 때 사용한다.
FORCE: 마스터가 다운되었을 때 사용한다.

TAKEOVER: 마스터가 다운되었고, 마스터 개수가 2개 이하일 때 사용한다.

1. **cluster에 참여하고 있는 노드의 정보를 보는 명령어**

redis-cli 접속 후 ,

```bash
cluster nodes
```

1. **cluster 정보 조회 명령어**

redis-cli 접속 후

```bash
cluster info
```

1. **cluster 노드 추가/삭제**

redis-cli 접속 후

```bash
redis-cli --cluster add-node IP:PORT --cluster-slave: 슬레이브로 추가 시

 사용 예 ) redis-cli –cluster add-node 127.0.0.1:6301 –cluster-slave

redis-cli --cluster add-node IP:PORT: 마스터로 추가 시

 사용 예 ) redis-cli –cluster add-nodel 127.0.0.1:6301

redis-cli --cluster del-node IP:PORT 삭제방법

 사용 예 ) redis-cli –cluster del-node 127.0.0.1:6301
```

1. **cluster 키/값 저장 명령어**

```bash
redis-cli -c -p port set key "value"
- mset은 여러개를 저장함: mset key1 "value1" key2 "value2"

 ex) redis-cli –c –p port set 1 “hello”
```

1. **cluster 키에 해당하는 값 가져오는 명령어**

redis-cli  접속 후

```bash
get key

- mget은 여러개를 가져옴: mget key1 key2 key3
```

1. **노드 내에 있는 모든 키에 해당하는 값 가져오는 명령어**

redis-cli  접속 후

```bash
keys "*"
```

1. **키와 해당하는 값을 삭제하는 명령어**

redis-cli 접속 후

```bash
del key
```

1. **현재 데이터를 모두 저장한다(서버 종료시에 save시점으로부터 데이터 복구)**

redis-cli 접속 후

```bash
save
```

1. **각 Redis 상태 보는 명령어** 

master인지 slave인지 ,  master라면 연결되어잇는 slave는 몇개인지 등의 정보 출력됌.

```bash
# 정보를 보고싶은 redis 접속 후,

$ info Replilcation

# 사용 예
$ redis-cli -c -p 7000
# 127.0.0.1:7000 redis 접속
$ info Replication

# Replication
role:master
connected_slaves:0
master_failover_state:no-failover
master_replid:6b3421a5f83f173720716524c446c657f43ff83a
master_replid2:0000000000000000000000000000000000000000
master_repl_offset:0
second_repl_offset:-1
repl_backlog_active:0
repl_backlog_size:1048576
repl_backlog_first_byte_offset:0
repl_backlog_histlen:0
```

1.  **클러스터 삭제하는 방법**

[[Redis] 클러스터 마스터, 슬레이브 노드를 제거하는 방법](https://mozi.tistory.com/385)

1. **각 노드 slots 위치 변경하는 방법**

[[Redis] 클러스터 노드간 슬롯을 변경(이동)하는 방법](https://mozi.tistory.com/384)