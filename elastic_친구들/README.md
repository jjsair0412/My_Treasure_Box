# elastic_친구들
해당 폴더는 Elastic에 관련된 것들이 모여있습니다.

## local docker test
elasticsearch를 local에서 간단히 테스트해보기 위해 아래 명령어를 사용합니다.
- 아래 두 옵션이 필요합니다.
    - ssl disable
    - elasticSearch single node enable 
```bash
$ docker run -d --name elasticsearch -p 9200:9200 -p 9300:9300 --restart=always -e "xpack.security.enabled=false" -e "discovery.type=single-node"  docker.elastic.co/elasticsearch/elasticsearch:8.7.0
```