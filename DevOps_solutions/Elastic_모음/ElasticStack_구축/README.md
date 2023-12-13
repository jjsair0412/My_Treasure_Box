# ElasticStack 구축 모음
Elastic 관련 구축방안들을 모아놓은 Repository 입니다.

## ElasticSearch Single Node 구성
test용 single node를 구성하기 위해 간단히 docker로 ES를 구축하는 방안을 정리해두었습니다.
- [singlenode_구축방안](./elastic_singleNode_구축방안.md)

## ElasticSearch Production Ready 구성
ElasticSearch를 Production Level로 구성하기 위해 Node간 tls 통신 등을 허용하여 구축하는 방안입니다.
### Docker compose
- [Docker-compose 설치방안](./elastic_kibana_multiNode_구축방안.md/)
### Helm chart - in kubernetes
- [helm-chart 설치방안](./elastic_prod_helm_install/)