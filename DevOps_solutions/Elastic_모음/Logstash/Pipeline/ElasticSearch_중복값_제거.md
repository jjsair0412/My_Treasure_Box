# ElasticSearch 중복값 제거
## OverView
input 값을 ElasticSearch에 색인된 데이터를 받고, 필터에서 색인된 데이터 중 중복된 데이터를 제거한 이후, output으로 ElasticSearch index에 색인하는 logstash pipeline 입니다.

## 전재 조건
### ElasticSearch 구축 정보
- FQDN : http:elasticsearch:9200
- username : test
- password : test1234
- 구축 환경 : docker
    - docker network : elastic

위 구축 정보를 기반으로 , Pipeline과 logstash 설정 파일을 변경합니다.
## 1. logstash.yml 수정

```yml
# logstash에 인바운드 할 수 있는 IP 대역을 모두 업니다.
http.host: "0.0.0.0" 

# logstash monitoring 대상인 ElasticSearch FQDN을 입력합니다.
xpack.monitoring.elasticsearch.hosts: [ "http://elasticsearch:9200" ] 

# ElasticSearch의 master user 계정 명을 입력합니다.
xpack.monitoring.elasticsearch.username: test

# ElasticSearch의 master user 계정의 비밀번호를 입력합니다.
xpack.monitoring.elasticsearch.password: test1234
```

## 2. logstash.conf 수정
중복값을 제거하기 위해 ,  fiter에서 ```fingerprint``` 플러그인을 사용합니다.

    
>Logstash의 fingerprint 플러그인이 고유 ID값을 생성하고 그 값을 문서의 documentID로 사용됩니다. 
>
>ElasticSearch는 documentID값이 동일한 문서는 덮어쓰기 때문에 , 중복된 문서들을 하나로 합칠 수 있습니다.


```conf
input {
    # match_all로 모든 doc 읽어옴
    elasticsearch {
        hosts   => ["http://elasticsearch:9200"]
        index   => "info_index"
        query   => '{"query": {"match_all": {}}}' # info_index 인덱스에서 모든 문서를 읽어옵니다.
        user => "test" # 해당 index를 접근할 수 있는 username 및 password를 입력합니다.
        password => "test1234"
        docinfo => true
    }
}

filter {
  fingerprint { # fingerprint 플러그인으로 각 문서에 고유한 ID (fingerprint) 를 생성합니다.
    source => "firstInfoId"
    target => "[@metadata][fingerprint]"
    method => "SHA1"
  }
}

output {
  elasticsearch {
    hosts => ["http://elasticsearch:9200"] 
    ; index => "%{+YYYY.DD.dd}_full_indexing_test_three"
    index => "info_index"
    user => "test"  
    password => "test1234"
    document_id => "%{[@metadata][fingerprint]}" # 생성한 고유 ID (fingerprint) 를 각 문서의 document_id로 지정합니다. 
  }
}
```




