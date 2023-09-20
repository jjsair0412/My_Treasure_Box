# Logstash Docker Install 
LogStash를 도커로 구축한 이후 , 로컬 도커로 구축된 ElasticSearch와 연동하는 방안에 대해 기술합니다.

먼저. ElasticSearch를 아래 정보에 맞춰 docker로 구축합니다.
- docker network : elastic
- Elastic username : test
- Elastic password : test1234

아래 문서를 참고하여 docker로 구축합니다.
- [ElasticSearch docker 구축방안](../elastic_기본설치방안.md)

## LogStash docker run
LogStash를 도커로 실행합니다.

```bash
$ docker run -d --name elasticsearch -p 9200:9200 -p 9300:9300 --net elastic docker.elastic.co/logstash/logstash:8.2.3
```
