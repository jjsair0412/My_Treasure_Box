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
>Logstash pipeline과 conf 파일을 수정하기 위해 . 각 디렉터리와 마운트 시켜줍니다.
>
>볼륨 마운트할 경로에 위 두가지 파일 생성합니다.
- /usr/share/logstash/pipeline/logstash.conf : logstash pipeline 관리
```bash
input {
  beats {
    port => 5044
  }
}

output {
  stdout {
    codec => rubydebug
  }
}
```

- /usr/share/logstash/config/logstash.yml : logstash 전체 conf 파일
```bash
http.host: "0.0.0.0"
xpack.monitoring.elasticsearch.hosts: [ "http://elasticsearch:9200" ]
```


도커로 실행해 줍니다.
>실행하기전 pipeline이나 conf를 수정하고 실행해도 무관합니다.
```bash
$ docker run -d --name  logstash -p 5044:5044  -v ~/logstash/logstash.conf:/usr/share/logstash/pipeline/logstash.conf -v ~/logstash/logstash.yml:/usr/share/logstash/config/logstash.yml --net elastic docker.elastic.co/logstash/logstash:version

# 실 사용 명령어 : Elastic Version이 7.10.0 이기 때문에 , logstash도 7.10.0으로 맞춰줍니다.
$ docker run -d --name  logstash -p 5044:5044 -v ~/logstash/logstash.conf:/usr/share/logstash/pipeline/logstash.conf -v ~/logstash/logstash.yml:/usr/share/logstash/config/logstash.yml --net elastic docker.elastic.co/logstash/logstash:7.17.13
```
