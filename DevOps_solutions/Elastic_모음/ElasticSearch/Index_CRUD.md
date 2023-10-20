# Index CRUD Command
Elastic Search에 색인된 문서를 대상으로 , Create, Read, Update, Delete 명령어들을 정리해 두었습니다.

## 사전 지식
해당 문서에서 CRUD를 진행할 때 , ElasticSearch의 Update Script를 사용하였습니다.
- [관련 공식문서](https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-scripting-fields.html#_update_scripts)


### 종류

#### 1. ctx._source
    - Access to the document _source field.

#### 2. ctx.op
    - The operation that should be applied to the document: index or delete.

#### 3. ctx._index etc
    - Access to document metadata fields, some of which may be read-only.

## Overview
예시에 활용된 doc의 모습

```bash
GET /info_index/_doc/WGmXtooBzIqvL9Pvja71?pretty=true
{
  "_index" : "info_index",
  "_type" : "_doc",
  "_id" : "WGmXtooBzIqvL9Pvja71",
  "_version" : 14,
  "_seq_no" : 548,
  "_primary_term" : 4,
  "found" : true,
  "_source" : {
    "firstInfoId" : 2,
    "name" : "2_영상",
    "age" : 7000,
    "keyword" : [
      "초록",
      "힐링이다",
      "mood"
    ],
    "categories" : [
      {
        "main_category" : "영상",
        "sub_category" : "전문가"
      }
    ]
  }
}
```

## Update
### 기존 정보 수정
```bash
curl -X POST -u "test:test1234" "http://localhost:9200/index_name/_update/{_id_value}_" -H "Content-Type: application/json" -d '{
    "doc" : {
       "target": "new_value"
    }
}'

# usecase
curl -X POST -u "test:test1234" "http://localhost:9200/info_index/_update/WGmXtooBzIqvL9Pvja71" -H "Content-Type: application/json" -d '{
    "doc" : {
       "age": 7000
    }
}'
```

### 새로운 값 추가
- 한개 추가
```bash
curl -X POST -u "test:test1234" "http://localhost:9200/info_index/_update/WGmXtooBzIqvL9Pvja71" -H "Content-Type: application/json" -d '{
  "script": {
    "source": "if (!ctx._source.keyword.contains(params.new_keyword)) { ctx._source.keyword.add(params.new_keyword); }",
    "lang": "painless",
    "params": {
      "new_keyword": "안녕하세요잇"
    }
  }
}'
```

- 여러개 추가
```bash
curl -X POST -u "test:test1234" "http://localhost:9200/info_index/_update/WGmXtooBzIqvL9Pvja71" -H "Content-Type: application/json" -d '{
  "script": {
    "source": "if (!ctx._source.keyword.contains(params.keywords_to_add)) { ctx._source.keyword.addAll(params.keywords_to_add); }",
    "lang": "painless",
    "params": {
      "keywords_to_add": ["으악셋","으악둘"]
    }
  }
}'
```

## Delete
### 특정 태그 삭제
- 한개만 삭제
```bash
curl -X POST -u "test:test1234" "http://localhost:9200/info_index/_update/WGmXtooBzIqvL9Pvja71" -H "Content-Type: application/json" -d '{
  "script": {
    "source": "if (ctx._source.keyword.contains(params.delete_target_keyword)) { ctx._source.keyword.removeIf(e -> e.equals(params.delete_target_keyword)); }",
    "lang": "painless",
    "params": {
      "delete_target_keyword": "안녕하세요잇"
    }
  }
}'
```

- 여러개 삭제
```bash
curl -X POST -u "test:test1234" "http://localhost:9200/info_index/_update/WGmXtooBzIqvL9Pvja71" -H "Content-Type: application/json" -d '{
  "script": {
    "source": "for(int i =0; i< params.keywords_to_remove.size(); i++) {if (ctx._source.keyword.contains(params.keywords_to_remove[i])) {ctx._source.keyword.removeIf(e -> e.equals(params.keywords_to_remove[i]));}}",
    "lang": "painless",
    "params": {
      "keywords_to_remove": ["으악하나","으악둘"]
    }
  }
}'
```

- index내부 모든 doc 삭제
```bash
curl -X POST -u "test:test1234" "localhost:9200/info_index/_delete_by_query" -H "Content-Type: application/json" -d'
{
  "query": {
    "match_all": {}
  }
}'
```