# Elastic TroubleShooting
ElasticSearch를 TroubleShooting 하는 방법에 대해 기술합니다.

## 문서 색인
- [Elastic 트러블슈팅 순서](#elastic-트러블슈팅-순서)
    - ES 트러블슈팅의 순서
- [ES의 문제 상황들](#es-문제-상황-사례들)
    - [클러스터 status가 Green이 아닐 경우](#사례-1-클러스터의-status가-green이-아닐-경우)
    - [doc 색인할때 403 에러나 , 클러스터에 색인이 되지 않을경우](#사례-2-클러스터에-문서가-색인이-되지-않을-경우--클라이언트에서-403-error-발생)
    - [간헐적으로 색인 과정에서 일부 문서가 누락될 경우](#사례-3-간헐적으로-색인-과정에서-일부-문서가-누락됨)
    - [샤드 배치가 되지 않을경우](#사례-4-샤드-배치가-되지-않을때)
    - [CMS 환경에서 너무 잦은 Old GC가 발생하는 경우](#사례-5-cms-gc-환경에서-너무-잦은-old-gc가-발생하는-경우)

## Elastic 트러블슈팅 순서
1. 모니터링 알람 확인
2. 클러스터 상태 확인
3. 각 노드에서의 에러 로그 확인
4. 클라이언트에서 에러 로그 확인

추가로 원인 분석을 위해선 , 주요 지표를 확인하고 에러로그를 분석합니다.


## ***ES 문제 상황 사례들***

## 사례 1. 클러스터의 status가 Green이 아닐 경우
아래 경로로 가서 클러스터 status가 나타내는것이 무엇을 의미하는지 먼저 확인합니다.
- [status 상태의 의미](./Elastic_Monitoring.md/#cat-health-상태-체크)

`Yellow` 혹은 `Red` 상태에 따른 영향도를 파악합니다.
- 중요하지 않은 인덱스가 Red , Yellow 일 수도 있기 때문에 먼저 어떤 인덱스가 `Red` , `Yellow` 인지 파악합니다.
```bash
GET /_cat/indices?v
```


그 다음 `Yellow` , `Red` 인덱스중 어떤 샤드에게 문제가 생겼는지 파악합니다.
- 장애 원인 분석
```bash
GET /_cat/shards?h=idx,sh,pr,st,docs,unassigned.reason
```

## 사례 2. 클러스터에 문서가 색인이 되지 않을 경우 && 클라이언트에서 403 ERROR 발생
**ES는 디스크 사용량이 100% 되면 , 노드 운영체제 또한 문제가 생기기 때문에 ES는 디스크 사용량이 일정 수준 이상 되면 더이상 색인작업이 일어나지 않습니다.**

아래 ES 설정값들이 위의 사례에 대한 설정값입니다.

|||
|--|--|
|이름|의미|
|cluster.routing.allocation.disk.thredhold_enabled|보호장치를 사용할 것인지 아닌지를 설정|
|cluster.routing.allocation.disk.watermark.low|기본값은 85% , 이 값보다 높아지면 더이상 샤드를 배치하지 않음|
|cluster.routing.allocation.disk.watermark.high|기본값은 90% , 이 값보다 높아지면 샤드들을 다른 데이터 노드로 옮기기 시작함|
|cluster.routing.allocation.disk.watermark.flood_stage|기본값은 95% , 이 값보다 높아지면 더이상 색인 작업을 하지 않음|

데이터노드를 증설하거나 불필요 인덱스를 삭제해서 디스크 공간을 확보해야 함..

**디스크 공간을 확보한 후 , Read-Only 상태의 인덱스를 명시적으로 풀어주어야 합니다.**
- 403 에러가 발생하는것은 , 위에 보호장치가 작동해서 Read-Only 상태로 바꿔버리기에 403이 납니다.

```bash
# * 부분에 특정 인덱스를 지정할 수 있음 , * 는 모든 인덱스를 의미함.
PUT /*/setting
{
    "index.blocks.read_only_allow_delete": null
}
```

## 사례 3. 간헐적으로 색인 과정에서 일부 문서가 누락됨
노드에서 간헐적으로 Rejected 에러가 발생함 ,, 

ES 에는 색인/검색 요청을 처리하는 스레드가 존재합니다.

그리고 스레드가 모두 요청을 처리하고 있을 때를 대비해서 큐도 존재합니다.

**만약 모든 큐가 꽉차 있다면 ? Rejected 에러가 발생합니다 !**
- ES 모니터링에서 꼭 확인해야할 지표 : Rejected 에러
- 노드가 감당할 수 있는 큐보다 더 많이 document가 들어올때 Rejected 에러가 발생할 수 있습니다.

### 해결방법
1. 데이터 노드 증설
- 클러스터가 처리 가능한 처리량을 높히기
- 샤드의 적절한 분배가 먼저 진행되어야 함

2. 큐 증설
- 여러가지 이슈사항이 있습니다. 그러나 제한적 상황에서 효과를 발휘할 수 있습니다.
    - 평상시 처리량은 괜찮은데 , 가끔씩 많은양의 요청이 인입되는 경우에 효과적일 수 있습니다.

**아래 conf를 수정해서 처리할 수 있는 큐 사이즈를 늘립니다.**
```conf
thread_pool.bulk.queue_size: 1000
```

## 사례 4. 샤드 배치가 되지 않을때
노드들이 갖고 잇는 전체 샤드의 개수에는 제한이 존재합니다.

그리고 해당 제한을 넘어간다면 , 더이상 샤드가 생성되지 않고 인덱스 생성또한 불가능해 집니다.

아래 conf가 해당 설정값인데 , default는 1000개 입니다.
```conf 
cluster.max_shards_per_nodes (Defaults : 1000)
```

만약 매일생성되는 인덱스설정이 다음과 같을 때 , 매일 총 10개의 샤드가 생성됩니다.
```json
{
    "index.number_of_shards": 5,
    "index.number_of_replicas": 1
}
```

1년간 로그를 유지할 경우 , 총 3650개 샤드가 생성됩니다.

이때 데이터 노드개수가 3개라면 1년만에 노드당 기본적으로 가질 수 있는 샤드 개수를 넘어서기에 샤드가 생성되지 않습니다.

### 해결방법
1. 데이터 노드 증설
    - 나오는 샤드 개수를 계산해서 데이터노드를 증설합니다.
2. 인덱스 설정 변경
    - 만드는 샤드개수를 조절합니다.
3. `cluster.max_shards_per_node` 변경
    - 샤드를 갖고있을수 있는 총 개수를 생성되는 샤드 개수에 맞게끔 늘려줍니다.
    - 그러나 요 값을 바꾸면 , 노드들에게 너무 많은 샤드가 배치됨으로 CPU사용량이 올라가거나 디스크 사용량 , 힙메모리 사용량 등이 올라가기 때문에 권고되는 설정은 아닙니다.
```bash
PUT /_cluster/settings
{
    "persistent":{
        "cluster.max_shards_per_node": "3000"
    }
}
```

## 사례 5. CMS GC 환경에서 너무 잦은 Old GC가 발생하는 경우
- 먼저읽기 
    - [GC 관련 문서](https://12bme.tistory.com/57) 
    - [STW 상태관련 문서](https://swjeong.tistory.com/88)

Old Gc는 CMS GC를 사용할 때 , Old 영역에서 발생하는 GC를 의미합니다.

이때 stop the world 상태 (잠시 멈춤) 현상이 나오기 때문에 , 성능에 큰 영향을 미칩니다.

불필요하게 많은 객체들이 Old 메모리 영역으로 이동하기 때문에 , 잦은 Old GC가 발생합니다.
- ES 문제라기 보단 ES가 사용하는 JVM의 문제 입니다.

**GC의 Survivor 영역이 이미 가득차서 Eden에서 Survivor 영역으로 이동할 수 없다면 , Survivor 영역을 거치지 않고 Old 영역으로 바로이동합니다.**
- 만약 GC에 들어가는 객체가 Survivor 영역보다 더 크다면 , Eden에서 Old 영역으로 바로갑니다.
    - JVM VisualGC를 통해 분석해서 봐야합니다.
    
### 해결방법
***Survivor 영역을 늘려주는것으로 해결할 수 있습니다.***

**Survivor 영역은 NewRatio와 SurvivorRatio JVM 튜닝을 통해 적용할 수 있습니다.**

Heap Memory를 튜닝하지 않았을 경우 ,, 아래 비율대로 GC가 설정됩니다.
- You튜닝전ng : Old = 1 : 9
- Eden : SO : S1 = 8 : 1 : 1

![튜닝전][튜닝전]

[튜닝전]:./images/튜닝전.png

만약 Heap Memory를 30GB로 튜닝했을 경우 ,, 아래 옵션값으로 튜닝합니다.

```conf
-XX:NewRatio=2
-XX:SurvivorRatio=6
-XX:CMSInitiatingOccupancyFraction=80 # Default : 75
```
    - CMSInitiatingOccupancyFraction 설정은 , CMS에서 Old 영역에 메모리가 어느정도까지 차면 비워주는 메모리 양을 설정할 수 있습니다. 
    - 기본은 75% .. 75%까지 찻을 때 Old로 가게끔 설정. (default)

그럼 비율이 다음과 같이 달라집니다.
- Young : Old = 1 : 2
- Eden : SO : S1 = 6 : 1 : 1

![튜닝후][튜닝후]

[튜닝후]:./images/튜닝후.png