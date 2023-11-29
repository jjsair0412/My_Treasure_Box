# ElasticSearch multi cluster deploy 방안
## 0. Overview
해당 문서는 ElasticSearch와 Kibana를 docker-compose를 통해 Mulit cluster 구성으로 배포하는 방안에 대해 기술합니다.

폴더 구조
```bash
$ tree
.
├── README.md
├── create-cert.yml
├── docker-compose.yml
└── instance.yml
```

해당 레포지토리에 있는 Dockerfile은, ES를 Nori analysis가 설치된 상태의 DockerImage를 생성하기 위한 Dockerfile 입니다.
- 사용하려면 빌드해서 Docker in Docker image를 생성한 이후, private registry에 push한 후 해당 image를 pull해와서 사용하면 됨.
- [nori_dockerfile](./Dockerfile)

## 1. 배포방안
### 1.0 .env 파일 생성
.env 파일에, docker-compose.yml 에 들어갈 세팅값을 설정합니다.
- project name, cert가 저장될 경로, elastic user password를 작성해서 생성합니다.

```bash
cat <<EOF> .env
    COMPOSE_PROJECT_NAME=es 
    CERTS_DIR=/usr/share/elasticsearch/config/certificates 
    ELASTIC_PASSWORD={elastic_user_password}
EOF
```


### 1.1 cert key 생성
ES에선 기본적으로 ElasticSearch끼리 클러스터링 시 tls 인증서를 사용해야 합니다.

따라서 create-cert.yml 파일과 instance.yml 파일을 통해 각 노드(es x3, kibana x1)들의 인증서를 발급받아 docker volume에 적제해 둡니다.

새로운 ES Node를 추가하기 위해선, instance.yml 파일에 추가합니다.
```yaml
instances:
  - name: es01
    dns:
      - es01 
      - localhost
    ip:
      - 127.0.0.1

  - name: es02
    dns:
      - es02
      - localhost
    ip:
      - 127.0.0.1

# 노드 추가할 땐, 아래 형식에 맞게 리스트를 하나 더 추가해 주면 됩니다.
  - name: es03
    dns:
      - es03
      - localhost
    ip:
      - 127.0.0.1

  - name: kib01
    dns:
      - kib01
      - localhost
```

docker compose 명령어를 통해 create-cert.yml을 실행하여 cert key를 발급받습니다.
- es_certs volume이 생성되고, 해당 볼륨에 key가 적재됩니다.

```bash
docker compose -f create-cert.yml run --rm create_certs
[+] Creating 1/0
 ✔ Volume "es_certs"  Created                                                                                                                                          0.0s 
Archive:  /certs/bundle.zip
   creating: /certs/ca/
  inflating: /certs/ca/ca.crt        
   creating: /certs/es01/
  inflating: /certs/es01/es01.crt    
  inflating: /certs/es01/es01.key    
   creating: /certs/es02/
  inflating: /certs/es02/es02.crt    
  inflating: /certs/es02/es02.key    
   creating: /certs/es03/
  inflating: /certs/es03/es03.crt    
  inflating: /certs/es03/es03.key    
   creating: /certs/kib01/
  inflating: /certs/kib01/kib01.crt  
  inflating: /certs/kib01/kib01.key 

docker volume ls
DRIVER    VOLUME NAME
local     es_certs
```


### 1.2 Elastic and Kibana deploy
docker compose 명령어로 배포한 뒤 , 포트번호로 접근하여 배포가 정상적으로 이루어졌는지 확인합니다.

```bash
docker compose up
```