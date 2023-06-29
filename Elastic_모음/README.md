# elastic_친구들
해당 폴더는 Elastic에 관련된 것들이 모여있습니다.

## local elastic docker test
elasticsearch와 kibana을 엮어줄 docker network를 생성합니다.
```bash
$ docker network create elastic
```

elasticsearch를 local에서 간단히 테스트해보기 위해 아래 명령어를 사용합니다.
- 아래 두 옵션이 필요합니다.
    - ssl disable
    - elasticSearch single node enable 
```bash
$ docker run -d --name elasticsearch -p 9200:9200 -p 9300:9300 --restart=always -e "xpack.security.enabled=false" -e "discovery.type=single-node" --net elastic docker.elastic.co/elasticsearch/elasticsearch:8.7.0
```

kibana dev tool을 사용하기위해서 kibana도 설치합니다.
```bash
$ docker run --name kib-01 --net elastic -p 5601:5601 docker.elastic.co/kibana/kibana:8.8.2
```