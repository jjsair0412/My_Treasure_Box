# ElasticSearch 검색 엔진 개발
ElasticSearch를 통해 검색엔진을 개발하기 위해서, 한글 형태소 분석이 가능해야 합니다.


## 쿼리문법은 아래 문서를 참고
- [ElasticSearch 가이드북 검색 쿼리문법](https://esbook.kimjmin.net/05-search)


따라서 **nori 한글 형태소 분석기를 사용합니다.**

테스트 환경은 ElasticSearch 노드 개수가 1대이기떄문에 , 샤드 및 레플리카 개수 지정 없이 index 패턴만 설정합니다.

### nori 설치
엘라스틱서치에 nori 플러그인을 설치합니다.
>elasticsearch 홈 디렉토리에서 빈폴더안에 실행파일들 있음
>도커도 동일 : 컨테이너 내부 접근하여 설치하면 됨
```bash
// 설치
$ bin/elasticsearch-plugin install analysis-nori

// 제거
$ bin/elasticsearch-plugin remove analysis-nori
```

### kubernetes에 배포될 ElasticSearch라면 ..
만약 Kubernetes에 배포될 ElasticSearch에 특정 플러그인을 설치해야 한다면 , 특정 버전의 ElasticSearch image를 Dockerfile의 baseImage로 등록하여 custom image를 빌드한 이후, 해당 이미지를 사용합니다.

```Dockerfile
# 버전은 필요에 따라 변경
FROM docker.elastic.co/elasticsearch/elasticsearch:7.10.0 

# Nori Plugin 설치
RUN bin/elasticsearch-plugin install analysis-nori
```

nori 형태소분석기가 잘 설치되었는지 엘라스틱서치에 쿼리를 보내서 확인합니다.

일반 standard tokenizer를 사용하면 , 공백만 자를 수 있습니다.
```bash
GET _analyze
{
  "tokenizer": "standard",
  "text": [
    "동해물과 백두산이"
  ]
}

# response
{
  "tokens" : [
    {
      "token" : "동해물과",
      "start_offset" : 0,
      "end_offset" : 4,
      "type" : "<HANGUL>",
      "position" : 0
    },
    {
      "token" : "백두산이",
      "start_offset" : 5,
      "end_offset" : 9,
      "type" : "<HANGUL>",
      "position" : 1
    }
  ]
}
```

nori 형태소분석기를 tokenizer로 사용해서 테스트하면 , 한국어 사전 정보를 통해 어근 , 합성어등을 분리시킬 수 있습니다.

nori_tokenizer에는 다음과 같은 옵션이 있습니다.
- ***user_dictionary :*** 사용자 사전이 저장된 파일의 경로를 입력합니다.
- ***user_dictionary_rules :*** 사용자 정의 사전을 배열로 입력합니다.
- ***decompound_mode :*** 합성어의 저장 방식을 결정합니다. 다음 3개의 값을 사용 가능합니다.
  - ***none :*** 어근을 분리하지 않고 완성된 합성어만 저장합니다.
  - ***discard (디폴트) :*** 합성어를 분리하여 각 어근만 저장합니다.
  - ***mixed :*** 어근과 합성어를 모두 저장합니다.

```bash
GET _analyze
{
  "tokenizer": "nori_tokenizer",
  "text": [
    "동해물과 백두산이"
  ]
}

# response
{
  "tokens" : [
    {
      "token" : "동해",
      "start_offset" : 0,
      "end_offset" : 2,
      "type" : "word",
      "position" : 0
    },
    {
      "token" : "물",
      "start_offset" : 2,
      "end_offset" : 3,
      "type" : "word",
      "position" : 1
    },
    {
      "token" : "과",
      "start_offset" : 3,
      "end_offset" : 4,
      "type" : "word",
      "position" : 2
    },
    {
      "token" : "백두",
      "start_offset" : 5,
      "end_offset" : 7,
      "type" : "word",
      "position" : 3
    },
    {
      "token" : "산",
      "start_offset" : 7,
      "end_offset" : 8,
      "type" : "word",
      "position" : 4
    },
    {
      "token" : "이",
      "start_offset" : 8,
      "end_offset" : 9,
      "type" : "word",
      "position" : 5
    }
  ]
}
```

### index 생성
nori tokenizer를 사용하는 index를 생성합니다.
- 인덱스 명 : info_index

각 파라미터별 설명은 다음과 같습니다.
- analysis의 analyzer를 custom type으로 지정 , tokenizer nori_tokenizer 지정,
- nori_tokenizer decompound_mode를 mixed로 지정
- name , age , category 검색이 가능
  - category가 과자 라면 , 과자입니다 검색했을경우에 과자 추출
  - name이 주진성 이라면 , 주진성2, 주진성이다. 주진성입니다 검색했을 경우 주진성 추출
  - 과자 주진성 모두 검색하면 , 과자 카테고리 및 주진성 이름 추출

```bash
PUT info_index
{
  "settings": {
    "analysis": {
      "analyzer": {
        "korean": {
          "type": "custom",
          "tokenizer": "nori_tokenizer"
        }
      },
      "tokenizer": {
        "nori_tokenizer": {
          "type": "nori_tokenizer",
          "decompound_mode": "mixed"
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "category": {
        "type": "text",
        "analyzer": "korean"
      },
      "name": {
        "type": "text",
        "analyzer": "korean"
      },
      "age":{
        "type": "integer"
      }
    }
  }
}
```

### 검색결과 확인
생성한 index에 대해 검색쿼리를 날려보면 , 요구사항에 부합하게 잘 작동하는것을 확인할 수 있습니다.
>아래쿼리 날리면 , 카테고리가 과자 또는 자연인 doc을 모두 검색해옵니다.
```bash
curl info_index/_search
{
  "query": {
    "match": {
      "category": "과자자자연"
    }
  }
}
```

>아래쿼리 날리면, 카테고리가 과자인 doc들과 ,  이름이 겔럭시워치 인 doc을 모두 검색해옵니다.
```bash
curl info_index/_search
{
    "query": {
        "bool": {
            "should": [  # should 로 쿼리 수행결과 가중치 높힘
                {
                    "match_phrase": { # match_phrase 로 쿼리가 정확히 맞는 doc만 추출
                        "name": {
                            "query": "겔럭시워치"
                        }
                    }
                },
                {
                    "match":{
                        "category": "과자"
                    }
                }
            ]
        }
    }
}
```
