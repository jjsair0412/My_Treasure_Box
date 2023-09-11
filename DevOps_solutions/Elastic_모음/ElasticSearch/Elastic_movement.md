# Elastic_movement
ElasticSearch의 기본 동작과정에 대해 기술합니다.
## 문서 색인
- [elasticsearch의-색인-과정?](#elasticsearch의-색인-과정)
    - elasticsearch의 색인 과정
- [색인의-성능](#etc---색인의-성능)
    - elsaticsearch의 색인 성능 최적화의 과정
- [ES_검색과정의_이해](#elasticsearch의-검색-과정)
    - elasticsearch의 검색 과정
- [검색의_성능](#etc---검색-성능-최적화)
    - elasticsearch의 검색 성능 최적화의 과정
- [text_and_keyword_type](#text-and-keyword-type)
    - Text type과 keyword type의 차이에 따른 색인 및 검색 차이
- [text_and+keyword_성능](#etc---text-vs-keyword-속도-최적화)
    - Text type과 keyword type의 성능 최적화

## ElasticSearch의 색인 과정
`색인` 이란 ? 
- 문서를 분석하고 저장하는 과정을 색인이라 합니다.

ES 내부에 문서를 입력할 때 , 문서를 분석하고 storage에 저장하는 일련의 과정을 `색인`이라 합니다.

- 아래 사진은 elasticsearch의 색인 과정의 다이어그램 입니다.

![색인_과정][색인_과정]

[색인_과정]:./images/색인_과정.png


### ETC - 색인의 성능
적절한 수의 샤드 개수를 설정하는것이 ES 색인 성능에 큰 영향을 미칩니다.

![색인_성능][색인_성능]

[색인_성능]:./images/색인_성능.png

만약 위와같이 3대의 데이터 노드가 있는 상태에서 , 아래 명령어로 document를 put 요청했다고 생각해 봅시다.
- 또한 미리 1번 ID를 가진 document를 PUT 요청했기에 primary shard가 (초록색 네모) 있고 replica shard (노란색 네모) 또한 생성되어있다 생각해 봅시다.
- `number_of_shards` 기본값은 1이기에 따로 설정하지 않았다고 생각

```bash
PUT /books/_doc/2
```

이때 2번 ID를 가진 document가 PUT요청 되었을 때 , 이미 primary shard 및 replica shard가 1번 , 2번 데이터 노드에 있기에 존재하는 shard에 저장됩니다.
- primary shard가 1개이기 때문에 , 색인이 하나의 데이터 노드에서만 일어납니다.
- 3대지만 1대의 성능.. 

이때 , ES 는 클러스터링을 통한 성능 향상이 이점임에도 3번 데이터노드는 사용하지 못하는 상황이 됩니다.

### > **따라서 , ES의 성능을 향상시키기 위해선 각 노드의 컴퓨팅 리소스를 수직 확장하는것 또한 방법이겠지만 , 샤드의 개수를 적절하게 설정해주는것이 성능에 큰 영향을 미치며, 이것이바로 ES의 key point 입니다 !**

- 만약 위와같은 상황에 index template을 아래와 같이 적용하면 , !

```bash
PUT _index_template/base_template
{
    "index_patterns": ["books"],
    "template": {
        "setting": {
            "number_of_shards": 3,
            "number_of_replicas": 1
        }
    }
}
```

프라이머리 샤드가 3개이기 때문에 , 3대의 데이터 노드 모두 색인에 참여하게 됩니다,

![색인_성능_UP][색인_성능_UP]

[색인_성능_UP]:./images/색인_성능_UP.png


> **만약 이 상황에서 데이터 노드를 늘리면 , 아래와 같이 샤드 개수가 고르게 분배되지 않기 때문에 용량 불균형이 일어날 수 있습니다.**

만약 각 샤드 용량이 10G 라면 , 각각 10G , 20G , 10G , 20G 로 용량 불균형이 일어나게 됩니다.

![색인_성능_DOWN][색인_성능_DOWN]

[색인_성능_DOWN]:./images/색인_성능_DOWN.png

**그렇다면 적절한 샤드의 개수는 어떻게 계산 ?**
- 샤드배치 계획은 상황마다 바뀝니다. 따라서 모니터링하면서 샤드개수를 맞추는것이 중요

## ElasticSearch의 검색 과정
ES의 검색 과정은 다음과같은 다이어그램을 따릅니다.
- 가장 중요한 과정은 , `inverted index` 과정이 가장 중요합니다.

![검색_과정][검색_과정]

[검색_과정]:./images/검색_과정.png


### inverted index ? 
문자열을 분석한 결과를 저장하고있는 구조체 라 볼 수 있습니다,

만약 아래와 같은 document 2개가 색인되었다 생각해봅니다.
```bash
1
{
    "type": "book",
    "title": "k8s",
    "author": "jinseong",
}

2
{
    "type": "book",
    "title": "elasticsearch",
    "author": "jinseong",
}
```

그럼 inverted index가 아래와 같이 Token과 Document라는 2행을 가진 표가 생성된다고 이해하면 됩니다.
- Token은 특정 단어이며 , Documents는 몇번째 Documents에 특정 단어가 있는지를 판단해서 채워넣습니다.

|||
|--|--|
|Tokens|Documents|
|book|1,2|
|k8s|1|
|elsaticsearch|2|
|jinseong|1,2|

### 애널라이저 (Analyzer)
inverted index를 만들기 위한 과정에서 , 애널라이저라는 과정이 꼭 필요합니다.

    문자열 -> character fileter -> tokenizer -> token filter -> tokens 

위 단계를 거치며 문자열을 분석해서 inverted index 구성을 위해 토큰을 만들어 내는 과정을 의미합니다.

character fileter , tokenizer , token filter를 잘 조합해서 custom analyzer를 만들 수 있습니다.
- 예시
  - ```character fileter``` 에서 특수문자를 제거
  - ```tokenizer``` 에서 공백을 제거
  - ```token filter``` 에서 소문자로 변경

## 검색 과정 최종 아키텍쳐
따라서 , 검색 과정은 아래와 같은 최종적인 아키텍처를 가집니다, 

>검색어 분석 과정에서 에널라이저를 적용하여 , 해당 애널라이저로 토큰을 생성한 뒤 검색하는것을 의미합니다.

우측편이 실제로 어떤 작업이 일어나는지에대해 기술되어 있습니다.

![실제_검색_과정][실제_검색_과정]

[실제_검색_과정]:./images/실제_검색_과정.png

### ETC - 검색 성능 최적화
검색 요청은 primary shard와 replica shard 모두 처리할 수 있습니다.
- 따라서 색인 성능은 충분한데 , 검색 요청의 성능을 높히기 위해선 , `number_of_replica` 를 늘리면서 검색 요청성능을 높힐 수 있습니다.
- `number_of_replica` 개수는 동적이기에 운영시에 언제든 개수를 늘릴 수 있습니다.
    - `number_of_shards` 개수는 라우팅 정책이 바뀌기에 바꿀수 없습니다.

## text and keyword type
둘다 문자열을 나타냅니다.

그러나 검색 방식에 차이가 있습니다,

![text_vs_keyword][text_vs_keyword]

[text_vs_keyword]:./images/text_vs_keyword.png

**1. text type**
- 전문 검색 (Full text search) 를 위해 토큰이 생성됨

**2. keyword type**
- Exact Matching 을 위해 토큰이 생성됨.
- 정확하게 일치하는값을 찾기 위해 토큰이 생성됨.,

### Text Type
analyze api를 통해 `I am a boy` 라는 문자열을 standard analyze 로 보내면 , 아래와 같은 응답을 확인할 수 있습니다.

따라서 I 나 am 등 의 값을 검색하면 , 저장된 document중 가장 높은 점수를 받은 document인 `I am a boy` 가 검색됩니다.
```bash
GET _analyze
{
  "analyzer": "standard",
  "text": "I am a boy"
}
```

response
```bash
{
  "tokens" : [
    {
      "token" : "i",
      "start_offset" : 0,
      "end_offset" : 1,
      "type" : "<ALPHANUM>",
      "position" : 0
    },
    {
      "token" : "am",
      "start_offset" : 2,
      "end_offset" : 4,
      "type" : "<ALPHANUM>",
      "position" : 1
    },
    {
      "token" : "a",
      "start_offset" : 5,
      "end_offset" : 6,
      "type" : "<ALPHANUM>",
      "position" : 2
    },
    {
      "token" : "boy",
      "start_offset" : 7,
      "end_offset" : 10,
      "type" : "<ALPHANUM>",
      "position" : 3
    }
  ]
}
```

### keyword Type
analyze api를 통해 `I am a boy` 라는 문자열을 keyword analyze 로 보내면 , 아래와 같은 응답을 확인할 수 있습니다.

`I am a boy` 라는 전체문자 그대로가 Token으로 갖는 token만 생성됩니다.

따라서 keyword analyzer로 `I am a boy` 가 저장되었을 경우에 해당 document를 찾으려면 `I am a boy` 전문을 검색해야 합니다.
```bash
GET _analyze
{
  "analyzer": "keyword",
  "text": "I am a boy"
}
```

response
```bash
{
  "tokens" : [
    {
      "token" : "I am a boy",
      "start_offset" : 0,
      "end_offset" : 10,
      "type" : "word",
      "position" : 0
    }
  ]
}
```

### ETC - text vs keyword 속도 최적화
keyword type이 token이 덜 생성되기 때문에 cpu 리소스를 덜 잡아먹어서 , 색인 속도가 더 빠릅니다.

또한 문자열 필드가 동적 매핑된다면 , text와 keyword type 모두 생성됩니다.
- 문자열이 들어오면 ES는 둘다 만듭니다.

> 따라서 문자열 특성에 따라 text와 keyword를 정적 매핑해 준다면 , 성능에 도움이 됩니다.

### ETC text vs keyword 필드의 예
- 상대적일 수 있지만 , 필드 타입이 지정되어있어야 한다면 keyword가 좋고 , 미리 몇개만쳐도 결과가 나와야한다면 text가 좋다.

|||
|--|--|
|text로 정의되면 좋은 필드|keyword로 정의되면 좋은 필드|
|주소|성별|
|이름|물품 카테고리|
...