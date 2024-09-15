# SearchAPI
해당 모듈은 , RDB에서 ROW를 select 이후 elasticsearch에 색인이 완료된 쿼리를 검색하는 기능을 가진 API 입니다.

## Ref 문서
- [ElasticSearch 공식 Java API 참고문서](https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/java-rest-high-search.html)
- [기능구현에 있어 참고한 블로그](https://coding-start.tistory.com/172)
  - deprecated된 코드가 많기 때문에 유의해서 구성해야함

## OverView
elasticSearch에 검색하려면 , 두가지 방안이 있습니다.

1. Low Level Rest Client


    ElasticSearch의 Query DSL 문을 그대로 보내는방법

2. High Level Rest Client
    

    ElasticSearch의 Query DSL 문을 자바 객체로 사용하는 방법

해당 문서는 2번 High Level Rest Client로 진행합니다.

elasticSearch에 index가 생성되었고 , doc 들도 [색인 Batch 모듈](../indexInitBatch) 로 색인되었다는 가정 하에 코드를 작성하기에 , 검색 기능만 존재합니다.

## index template
검색 대상 index는 아래와 같습니다.
- [indexInitBatch](../indexInitBatch) job을 돌린 후 색인된 index를 대상으로 검색합니다.

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

## 검색 요구사항
검색의 기본 요구사항은 다음과 같습니다.

    1. 카테고리 별 검색이 가능해야 함
    2. 이름 별 검색이 가능해야 함
    
검색의 상세 요구사항은 다음과 같습니다.

    1. 여러 합성어가 같이 검색되더라도 , 나뉘어서 결과가 추출되어야 함
    - ex) 감자깡과 새우깡 doc을 검색한다 했을 경우, 아래처럼 검색해도 감자깡, 새우깡 doc 이 추출되어야 함.
    검색 예시 : '감자깡새우깡', '감자 새우', '감자새우' 
    
    2. 어미만 따로 검색되더라도 , 해당 어미를 가진 doc이 추출되어야 함.
    - ex) category가 과자 인 doc을 검색한다 했을 때, 과자 를 검색하면 과자인 doc이 모두 검색되어야 함. 카테고리는 여러개를 한번에 검색될 수 있어야 함
    검색 예시 : '과자', '과자입니다', '과자자', '과자스마트워치', '과자 스마트워치'. '과자스마트워치자연','과자 스마트워치 자연'

## dependency
아래 의존성 추가합니다.

```gradle
// ElasticSearch HighLevel API dependency
// https://mvnrepository.com/artifact/org.elasticsearch.client/elasticsearch-rest-high-level-client
implementation 'org.elasticsearch.client:elasticsearch-rest-high-level-client:7.17.12'

// return을 Gson으로 받기 때문에 Gson 의존성추가
// https://mvnrepository.com/artifact/com.google.code.gson/gson
implementation 'com.google.code.gson:gson:2.10'
```

## 코드 작성
### RestClient로 ElasticSearch와 연결
ElasticSearch에 검색 쿼리를 날리기 위해서 , RestClient로 연결합니다.
```java
    private RestHighLevelClient createConnection() {
        // elasticSearch 로그인정보 기입
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials("test", "test1234"));

        // elasticSearch Host 주소와 위에 생성한 로그인정보 파라미터로 넣어서 RestHighLevelClient 객체 새성
        return new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("127.0.0.1", 9200, "http")
                ).setHttpClientConfigCallback(httpClientBuilder -> 
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
        );
    }
```
### matchAll
먼저 검색대상 index 내부 모든 문서를 가져오는 matchAll 코드를 작성합니다.
>코드 설명은 주석처리해두었습니다.
- [실제 서비스코드](./src/main/java/com/example/searchapi/Service/SearchServiceImpl.java)

## ETC 쿼리 예시
- 문서중 이름이 ```애플워치``` 이면서 카테고리가 ```스마트워치```인 문서 추출 
```bash
POST info_index/_search
{
    "query": {
        "bool": {
            "should": [
                {
                    "match_phrase": {
                        "name": {
                            "query": "애플워치"
                        }
                    }
                },
            ],
            "must": [
                {
                    "match":{
                        "category": "스마트워치"
                    }
                }
            ]
        }
    }
}
```

- 이름이 ```애플워치``` 인 문서 또는 카테고리가 ```과자``` 인 문서 추출
>should 구문으로 애플워치 인 문서가 최상단에 노출
```bash
POST info_index/_search
{
    "query": {
        "bool": {
            "should": [
                {
                    "match_phrase": {
                        "name": {
                            "query": "애플워치"
                        }
                    }
                },
                {
                    "match":{
                        "category": "스마트워치"
                    }
                }   
            ]
        }
    }
}
```
