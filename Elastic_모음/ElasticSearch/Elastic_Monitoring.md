# Elastic Monitoring
## 문서 색인

- [Cat API 활용](#cat-api-활용하기)
    - [ES_클러스터_상태확인](#cat-health)

    - [ES_노드_상태확인](#cat-nodes)
    - [index_상태_확인](#cat-indices)
    - [shard_상태_확인](#cat-shards)

- [ES의 주요 모니터링 지표](#es의-주요-모니터링-지표)
    - ES를 통해 APM을 구축할 때 , 모니터링 지표 구분과 Best Practice 정보

## cat API 활용하기
cat API는 클러스터 정보를 읽기 편한 형태로 출력하기 위해 만들어진 REST API.

cat API로 ES 클러스터 모니터링을 빠르게 할 수 있습니다.
- [cat API 사용가능 REST List](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat.html)

### cat health
ES 클러스터의 전반적인 상태를 확인할 수 있습니다.
- v는 verbose 의 약자
- 아래처럼 ES 클러스터의 전반적인 상태를 확인 가능합니다.
    - init : 만들어지고있는 shard
    - relo : 재 배치되고있는 shard 개수
    - unassign : shard들 중 노드에 배치되지 않은 shard들의 개수

```bash
# request
GET /_cat/health?v

# response
epoch      timestamp cluster        status node.total node.data shards pri relo init unassign pending_tasks max_task_wait_time active_shards_percent
1688469913 11:25:13  docker-cluster green           1         1      8   8    0    0        0             0                  -                100.0%
```

### cat health 상태 체크
cat health를 통해 체크할 수 있는 상태는 총 3가지 입니다.
- 위 response를 보면 green인것을 확인할 수 있음.

|||
|--|--|
|**상태**|**의미**|
|green|primary shard , replica shard 모두 정상적으로 각 노드에 배치된 상태|
|yellow|primary shard는 정상적으로 동작하지만 , 일부 replica shard가 정상적으로 배치되지 않은 상태 , 색인 성능에는 이상이 없지만 검색 성능에는 영향을 줄 수 있음|
|red|일부 primary shard와 replica shard가 정상적으로 배치되지 않은 상태 , 색인 성능 , 검색 성능 모두에 영향을 줄 수 있으며 문서 유실이 발생할 가능성이 있음|

### cat nodes
node들의 전반적인 상태를 확인할 수 있습니다.
- v는 verbose 의 약자
- 아래처럼 ES node의 전반적인 상태를 확인 가능합니다.
    - heap.percent : heap memory 사용량
    - ram.percent : node memory 사용량
    - node.role : 노드 role 확인가능
        - `*` 이 달려있는 노드가 master node
        - [공식문서](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-nodes.html) 참고해서 role 해석 가능 . 단축해서 써놨음, ex) m : master
 
```bash
# request
GET /_cat/nodes?v

# response
ip         heap.percent ram.percent cpu load_1m load_5m load_15m node.role   master name
172.18.0.2           15          81   3    1.25    0.74     0.53 cdfhilmrstw *      61021934dd41

```

`help` 로 node 또다른 세부적 정보를 볼 수 있는 RESTAPI 를 확인할 수 있습니다.
- 공식문서에서 자세한 정보 찾기 가능

```bash
# request
GET /_cat/nodes?help

# response
id                                 | id,nodeId                                   | unique node id                                                                                                   
pid                                | p                                           | process id                                                                                                       
ip                                 | i                                           | ip address                                                                                                       
port                               | po                                          | bound transport port                                                                                             
http_address                       | http                                        | bound http address                                                                                               
version                            | v         
...
```

`h` 명령어로 위의 help 값을 하나씩 넣어서 내가 원하는정보만 꺼내볼 수 있습니다.
```bash
# request
GET /_cat/nodes?h=ip,port,name,heap.percent,disk.avail&v

# response 
ip         port name         heap.percent disk.avail
172.18.0.2 9300 61021934dd41           41    200.8gb
```

### cat indices
`cat indeices` 는 index들의 상태를 확인할 수 있는 REST API 입니다.
- index 2개를 미리 `PUT` 명령어로 생성해 둔 상태 입니다.
- health : cat health에서 나왔던 health와 동일
    - ES 클러스터의 상태는 index의 health와 동일합니다.
        - 1개 이상의 index가 yellow면 ES 클러스터도 yellow
        - 1개 이상의 index가 red면 ES 클러스터도 red

```bash
# request
GET /_cat/indices?v

# response
health status index uuid                   pri rep docs.count docs.deleted store.size pri.store.size
yellow open   book  k53EfQRoR6CST25F7w_hBA   1   1          0            0       225b           225b
yellow open   movie h-QFhCYNQ16q2G8bjZIGaQ   1   1          1            0      4.4kb          4.4kb
```

`cat indices` 또한 help 파라미터를 사용할 수 있습니다.
```bash
# help 목록확인
GET /_cat/indices?help
...

# help 파라미터로 상세정보 확인
GET /_cat/indices?h=~~
```

### cat shards
`cat shards` 는 shards 상태를 확인할 수 있습니다.


```bash
# request
GET /_cat/shards?v

# response
index                                                         shard prirep state      docs store ip         node
.ds-.logs-deprecation.elasticsearch-default-2023.07.04-000001 0     p      STARTED               172.18.0.2 61021934dd41
.kibana-event-log-8.2.3-000001                                0     p      STARTED               172.18.0.2 61021934dd41
.geoip_databases                                              0     p      STARTED               172.18.0.2 61021934dd41
.ds-ilm-history-5-2023.07.04-000001                           0     p      STARTED               172.18.0.2 61021934dd41
.kibana_task_manager_8.2.3_001                                0     p      STARTED               172.18.0.2 61021934dd41
.kibana_8.2.3_001                                             0     p      STARTED               172.18.0.2 61021934dd41
.apm-agent-configuration                                      0     p      STARTED               172.18.0.2 61021934dd41
book                                                          0     p      STARTED       0  225b 172.18.0.2 61021934dd41
book                                                          0     r      UNASSIGNED                       
movie                                                         0     p      STARTED       2 4.4kb 172.18.0.2 61021934dd41
movie                                                         0     r      UNASSIGNED                       
.apm-custom-link                                              0     p      STARTED               172.18.0.2 61021934dd41
```

`cat shards` 또한 help Parameter를 사용할 수 있습니다.
```bash
# help 목록 확인
GET /_cat/shards?help

# h 파라미터로 더 상세한정보 확인
GET /_cat/shards?h=index,shard,prirep,state,docs,unassigned.reason&v
index                                                         shard prirep state      docs unassigned.reason
.ds-.logs-deprecation.elasticsearch-default-2023.07.04-000001 0     p      STARTED         
.kibana-event-log-8.2.3-000001                                0     p      STARTED         
.geoip_databases                                              0     p      STARTED         
.ds-ilm-history-5-2023.07.04-000001                           0     p      STARTED         
.kibana_task_manager_8.2.3_001                                0     p      STARTED         
.kibana_8.2.3_001                                             0     p      STARTED         
.apm-agent-configuration                                      0     p      STARTED         
book                                                          0     p      STARTED       0 
book                                                          0     r      UNASSIGNED      INDEX_CREATED
movie                                                         0     p      STARTED       2 
movie                                                         0     r      UNASSIGNED      INDEX_CREATED
.apm-custom-link                                              0     p      STARTED         
```

## ES의 주요 모니터링 지표
kibana의 `Stack Monitoring`을 선호
- prometheus , cloud watch 등 APM 툴은 굉장히 많음
- 그러나 모든 지표가 다 중요하진 않기 때문에 , 지표들의 모니터링 기준을 잡는것이 중요 합니다.

### 모니터링 지표 구분
모니터링 지표들은 크게 두가지로 나뉩니다.

**1. 임계치를 통해 해당 임계값을 넘어가거나 적다면 알람 발생용 지표**
- 대표적으로 아래 5가지

||||
|--|--|--|
|이름|의미|임계치 설정 (예시) |
|CPU Usage|노드가 CPU를 얼마나 사용중인가|50% 이상|
|Disk Usage|노드가 Disk를 얼마나 사용중인가 . 노드가 얼마나 많은 document를 저장중인가|70% 이상|
|Load|노드가 얼마나 많은 CPU / 디스크 연산을 처리하고 있는가|CPU 개수에 따라 상이 (CPU 개수가 2라면 2 이상)|
|JVM Heap|노드의 JVM이 얼마나 많은 메모리를 사용하는가|85% 이상|
|Threads|처리량을 넘어서는 색인/검색 요청이 있는가|Reject Threads가 1 이상|



**2. 문제 원인을 파악하기위한 분석용 지표**
- 대표적으로 아래 5가지
    - [GC_관련_문서-꼭읽어보기-GC영역의 깊은 이해를 도우는 좋은 글](https://12bme.tistory.com/57)

|||
|--|--|
|이름|의미|
|Memory Usage|노드의 메모리 사용량 . JVM 사용량과는 다름|
|Disk I/O|디스크에서 발생하는 I/O 지연 시간|
|GC Rate|Young GC, Old GC의 발생 주기|
|GC Duration|Young GC, Old GC에 소요되는 시간|
|Latency|색인, 검색에 소요되는 시간|
|Rate|색인, 검색 요청이 인입되는 양|