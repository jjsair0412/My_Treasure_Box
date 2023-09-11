# elastic_친구들
해당 폴더는 Elastic에 관련된 것들이 모여있습니다.

## local elastic docker test
elasticsearch와 kibana을 엮어줄 docker network를 생성합니다.
```bash
$ docker network create elastic
```

elasticsearch를 local에서 간단히 테스트해보기 위해 docker를 사용해서 테스트 합니다.
- 아래 두 옵션이 필요합니다.
    - ssl disable
    - elasticSearch single node enable 
```bash
$ docker run -d --name elasticsearch -p 9200:9200 -p 9300:9300 --restart=always -e "xpack.security.enabled=false" -e "discovery.type=single-node" --net elastic docker.elastic.co/elasticsearch/elasticsearch:8.2.3
```

kibana dev tool을 사용하기위해서 kibana도 docker run 합니다..
```bash
$ docker run --name kib-01 -d --net elastic -p 5601:5601 docker.elastic.co/kibana/kibana:8.2.3
```

## production ElasticSearch config
elasticsearch를 prod 환경에서 사용하기 위해선 , id , password를 설정하고 TLS setting을 해주어야 합니다.

## elasticsearch ID , Password 생성 - 로컬 환경
elasticsearch를 실행할 때 , id , password를 설정하려면 아래 도커 명령어를 사용합니다.
- xpack.security.enabled : true
- xpack.security.transport.ssl.enabled : false
>로컬에서 single node elasticsearch를 실행하는데 , ID, password만 enable 하는 경우
>

아래 명령어를 수행하면 , **```elastic``` user에 password를 세팅할 수 있습니다.**
- userName : ```elastic```
- password : ```my_password```

```bash
$ docker run -d --name elasticsearch -p 9200:9200 -p 9300:9300 --restart=always \
-e "xpack.security.enabled=true" \
-e "xpack.security.transport.ssl.enabled=false" \
-e "discovery.type=single-node" \
-e "ELASTIC_PASSWORD=my_password" \
--net elastic docker.elastic.co/elasticsearch/elasticsearch:8.2.3
```

컨테이너 접속하여 새로운 유저를 생성합니다.
- elastic user는 kibnan에서 접근할 수 없습니다.
- 아래와같은 에러발생함
>elastic user는 super user이기 때문에 , kibana에서 사용할 수 없으며 , 사용하려면 elastic user token을 발급받아서 접근해야 합니다.
>
>발급된 elastic user token의 expired time은 1200초
- [elasticserach 토큰인증 관련 블로그](https://techexpert.tips/ko/elasticsearch-ko/elasticsearch-%ED%86%A0%ED%81%B0%EC%9D%84-%EC%82%AC%EC%9A%A9%ED%95%9C-%EC%9D%B8%EC%A6%9D/)

```bash
[FATAL][root] Error: [config validation of [elasticsearch].username]: value of "elastic" is forbidden. This is a superuser account that cannot write to system indices that Kibana needs to function. Use a service account token instead. Learn more: https://www.elastic.co/guide/en/elasticsearch/reference/8.0/service-accounts.html
```

컨테이너 접근
```bash
docker exec -it elasticsearch /bin/bash
```

사용자 생성
- ***elastic:my_password*** : my_password에 elastic 유저 페스워드 기입
    - ex) 비밀번호가 test일 경우 , elastic:test
- ***new_user*** : 원하는 user이름 기입
- ***password*** : 원하는 password 생성
    - ***password 제약조건*** : passwords must be at least [6] characters long
- ***roles*** : 해당 유저 권한부여
    - 아래 예제에선 superuser 권한을 부여함
- ***full_name*** : 해당 유저를 설명하는 식별자 - 관리목적
```bash
# usecase
curl -X POST -u "elastic:my_password" "http://localhost:9200/_security/user/new_user" -H "Content-Type: application/json" -d '{
  "password": "new_user_password",
  "roles": ["superuser"],
  "full_name": "New Superuser"
}'



# 실 사용 명령어
curl -X POST -u "elastic:test" "http://localhost:9200/_security/user/test" -H "Content-Type: application/json" -d '{
  "password": "test1234",
  "roles": ["superuser", "kibana_admin", "create_index", "manage", "all"],
  "full_name": "New Superuser"
}'
{"created":true}
```


### elasticsearch id , password kibana 등록

kibana.yml에 elastic user 정보를 기입합니다.
- 또는 그냥 도커명령어에 추가
>운영 환경이라면 kibana.yml파일 수정하는것이 관리에 있어서 좋음.
```yml
elasticsearch.username: "my_username" <-- 생성한 아이디 이름
elasticsearch.password: "my_password" <-- 설정한 비밀번호
```

kibana 재 시작
```bash
$ docker run --name kib-01 -d --net elastic -p 5601:5601 \
-e "ELASTICSEARCH_HOSTS=http://elasticsearch:9200" \
-e "ELASTICSEARCH_USERNAME=test" \
-e "ELASTICSEARCH_PASSWORD=test1234" \
docker.elastic.co/kibana/kibana:8.2.3

# 실 사용 명령어
$ docker run --name kib-01 -d --net elastic -p 5601:5601 \
-e "ELASTICSEARCH_HOSTS=http://elasticsearch:9200" \
-e "ELASTICSEARCH_USERNAME=test" \
-e "ELASTICSEARCH_PASSWORD=test1234" \
docker.elastic.co/kibana/kibana:8.2.3
```


## elasticsearch ID , Password 생성 - 운영 환경
elasticsearch를 실행할 때 , id , password를 설정하려면 아래 옵션들을 넣어둡니다.
- xpack.security.enabled: true
- xpack.security.authc.token.enabled: true
- xpack.security.http.ssl.enabled: true
- xpack.security.http.ssl.keystore.path: "http.p12"

>운영환경에서 사용할 경우
>
>singleNode로 실행하는것이 아닌 , 클러스터링 필요


## ETC 
### 검색
elasticSearch에 과자 카테고리를 검색하려면 아래처럼 요청보내면 됨.
>
```bash
curl -X GET -u "elastic:test" "http://localhost:9200/info_index/_search" -H "Content-Type: application/json" -d '{
  "query": {
    "match": {
      "category": "과자"
    }
  }
}'
```