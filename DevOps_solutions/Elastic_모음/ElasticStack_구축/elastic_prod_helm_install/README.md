# ElasticSearch Production Ready Version - helm chart install
해당 문서는 ElasticSearch를 Kubernetes Cluster에 설치하는 방안에 대해 기술합니다.

ElasticSearch Cluster Node들끼리 통신할 때, TLS 통신을 enable 하고, user login을 설정함으로써 Production 환경에 적합한 설치 구성입니다.
- [참고 문서](https://thomasdecaux.medium.com/configure-transport-layer-security-tls-ssl-for-an-elasticsearch-cluster-deployed-with-helm-on-fbbac2325a00)

## 0. 설치대상 Version
- ElasticSearch 7.10.0

7.10.0 버전 이후 8버전부터는 ElasticSearch가 유료화되엇기 떄문에 , 7.10.0 버전으로 설치합니다.


## 1. helm chart import
먼저 ElasticSearch Helm chart를 import 합니다.
- [Helm_Chart](./elasticsearch/)

```bash

helm repo add elastic https://helm.elastic.co

helm pull elastic/elasticsearch --version 7.10.0 --untar 
```

## 2. 구축 준비
### 2.1 이론
ElasticSearch를 Production환경에 배포하기 위해선, ElasticSearch의 최소 보안규정을 준수해야 합니다.

xpack.security setting을 해두어야 하며, 해당 문서의 설치는 ingress-nginx가 앞단에 위치하여 ElasticSearch에 접근 시 TLS 인증이 한번 이루어진다는 가정 하에 작성합니다.

만약 ElasticSearch Cluster에 클라이언트가 접근할 때 Https 인증이 필요하다면, esConfig를 아래와 같이 설정해두어야 합니다.
```yaml
esConfig:
  elasticsearch.yml: |
    xpack.security.enabled: true
    xpack.security.transport.ssl.enabled: true
    xpack.security.transport.ssl.verification_mode: certificate
    xpack.security.transport.ssl.key: /usr/share/elasticsearch/config/certs/tls.key
    xpack.security.transport.ssl.certificate: /usr/share/elasticsearch/config/certs/tls.crt
    xpack.security.http.ssl.enabled: true
    xpack.security.http.ssl.key: /usr/share/elasticsearch/config/certs/tls.key
    xpack.security.http.ssl.certificate: /usr/share/elasticsearch/config/certs/tls.crt
```

하지만 해당 문서는, ***ingress-nginx가 앞단에 위치하여 ElasticSearch에 접근 시 TLS 인증이 한번 이루어진다는 가정***이기 때문에, http 설정은 제외하고 ElasticSearch Node끼리 통신할 때만 TLS 인증을 진행하도록 아래와 같이 세팅합니다.
```yaml
esConfig:
  elasticsearch.yml: |
    xpack.security.enabled: true
    xpack.security.transport.ssl.enabled: true
    xpack.security.transport.ssl.verification_mode: certificate
    xpack.security.transport.ssl.client_authentication: required
    xpack.security.transport.ssl.key: /usr/share/elasticsearch/config/certs/tls.key
    xpack.security.transport.ssl.certificate: /usr/share/elasticsearch/config/certs/tls.crt
```

각 xpack.security setting에 대한 설명은 다음과 같습니다.

1. ```xpack.security.http.ssl.enabled: true```
    - 이 설정은 Elasticsearch의 HTTP 계층에 SSL/TLS 보안을 활성화합니다. 이것은 Elasticsearch에 대한 모든 HTTP 통신이 암호화되어야 함을 의미합니다.
2. ```xpack.security.http.ssl.key: /usr/share/elasticsearch/config/certs/tls.key```
    - 이 경로는 Elasticsearch 서버의 SSL 키 파일의 위치를 지정합니다. 이 키 파일은 서버의 ID를 안전하게 인증하고 SSL 연결을 설정하는 데 사용됩니다.
3. ```xpack.security.http.ssl.certificate: /usr/share/elasticsearch/config/certs/tls.crt``` 
    - 이 경로는 서버의 SSL 인증서 파일의 위치를 지정합니다. 클라이언트는 이 인증서를 사용하여 서버의 신원을 검증하고, 보안된 연결을 확립합니다.
4. ```xpack.security.enabled: true``` 
    - 이 설정은 Elasticsearch의 보안 기능을 활성화합니다. 이는 사용자 인증, 역할 기반 접근 제어, 암호화된 통신 등 다양한 보안 기능을 포함합니다.
5. ```xpack.security.transport.ssl.enabled: true``` 
    - 이 설정은 Elasticsearch 클러스터 내의 노드 간 통신에 SSL/TLS 보안을 활성화합니다. 이것은 클러스터 내 노드 사이의 모든 데이터 전송이 암호화되어야 함을 의미합니다.
6. ```xpack.security.transport.ssl.verification_mode: certificate``` 
    - 이 설정은 SSL/TLS 인증서 기반의 검증 모드를 지정합니다. 클러스터 내의 각 노드는 다른 노드의 인증서를 검증하여 통신의 신뢰성을 확보합니다. certificate 모드는 인증서 자체만을 검증합니다. ***즉, 이 설정은 클러스터의 각 노드가 서로 통신할 때 상대방 노드의 인증서가 유효한지만 확인하고, 인증서가 신뢰할 수 있는 발급 기관에 의해 서명되었는지는 확인하지 않습니다.***
        - openssl 등의 인증서를 먹여도 괜찮습니다.
7. ```xpack.security.transport.ssl.key: /usr/share/elasticsearch/config/certs/tls.key``` 
    - 이 경로는 각 Elasticsearch 노드의 SSL 키 파일 위치를 지정합니다. 이 키는 노드 간 통신의 인증 및 암호화에 사용됩니다.
8. ```xpack.security.transport.ssl.certificate: /usr/share/elasticsearch/config/certs/tls.crt``` 
    - 이 경로는 각 노드의 SSL 인증서 파일 위치를 지정합니다. 다른 노드는 이 인증서를 통해 해당 노드의 신원을 확인합니다.
9. ```xpack.security.transport.ssl.client_authentication: required``` 
    - 이 설정은 클러스터 내의 노드 간 통신에서 클라이언트 인증이 필요함을 명시합니다. required로 설정되면 모든 노드 간 통신에서 SSL/TLS 인증서를 통한 상호 인증이 필수적입니다. 즉, 한 노드가 다른 노드와 통신을 시작할 때, 두 노드 모두 서로의 인증서를 검증해야 합니다. 이렇게 하면 클러스터 내의 모든 노드가 서로를 식별하고 검증하는 과정을 거쳐야 하므로, 보안성이 강화됩니다.

### 2.2 구축 과정
전체적인 구축 과정은 다음과 같습니다.

0. ElasticSearch 계정 생성
    - **username : elastic 고정**
1. openssl 또는 cert-bot, 신뢰할수 있는 TLS 인증서 등을 통해 ```ca.key``` , ```ca.crt``` 키를 발급
2. 발급받은 ```ca.key``` , ```ca.crt``` 를 통해 ElasticSearch InitContainer에서 ```ca.p12``` 키를 발급
3. ```elastic-cert.p12``` 를 ElasticSearch Node간 TLS 통신 시 인증서로 사용하도록 적용


## 3. 배포
### 3.0 tls 인증서 생성
먼저 ElasticSearch Cluster끼리 통신에 필요한 TLS 인증서를 발급받아야 합니다.
- openssl로 발급받아도 되고, [참고 문서](https://thomasdecaux.medium.com/configure-transport-layer-security-tls-ssl-for-an-elasticsearch-cluster-deployed-with-helm-on-fbbac2325a00) 처럼 cert-manager나 cert-bot을 통해 신뢰할 수 있는 인증서를 발급받아도 무관합니다.
- 인증서 유효 기간 : 3650일(약 10년)
```bash
# ca.key 발급
openssl genrsa -out ca.key 4096

# ca.cert 발급
openssl req -new -x509 -days 3650 -key ca.key -out ca.crt
```

발급받은 ca.key, ca.crt를 통해 secret을 생성합니다.
- **중요 !!! : tls type으로 생성해야 합니다.**

```bash
kubectl create secret  -n default tls jinseong-tls-gen --key ca.key --cert ca.crt
```


### 3.1 ElasticSearch 계정 생성
ElasticSearch 및 Kibana Login에 사용할 계정을 생성합니다.
- **중요 !!! : 해당 user는 username을 elastic 으로 고정해야만 합니다.**

```bash
kubectl create secret generic elastic-jinseong-credentials --namespace default --from-literal=password=jinseongPWD123 --from-literal=username=elastic --dry-run=client -o yaml > elastic-credentials.yaml
```

위 명령어 수행하면, 다음과 같은 secret이 생성됩니다.
- password, username은 주석에 작성해둔 문장,숫자를 base64로 인코딩한 결과입니다. (Kibana 설치 시 재활용 가능)
```yaml
apiVersion: v1
data:
  password: amluc2VvbmdQV0QxMjM= # jinseongPWD123
  username: ZWxhc3RpYw== # elastic
kind: Secret
metadata:
  creationTimestamp: null
  name: elastic-jinseong-credentials
  namespace: default
```

kubectl 명령어로 secret 먼저 생성합니다.

```bash
$ kubectl apply -f elastic-credentials.yaml 
secret/elastic-jinseong-credentials created

# 결과확인
$ kubectl get secret
NAME                           TYPE     DATA   AGE
elastic-jinseong-credentials   Opaque   2      8s
```

### 3.2 custom-values.yaml 설정
- [custom-values.yaml 전문](./elasticsearch/custom-values.yaml)

***ingress-nginx가 앞단에 위치하여 ElasticSearch에 접근 시 TLS 인증이 한번 이루어진다는 가정***이기 때문에, http 설정은 제외하고 ElasticSearch Node끼리 통신할 때만 TLS 인증을 진행하도록 세팅합니다.

custom-values.yaml을 분할해서 확인

#### 3.2.1 affinity
ElasticSearch Pod affinity를 설정하는 부분

```yaml
# Affinity 설정
nodeAffinity: {}
```

#### 3.2.2 user 설정
상단 [elasticsearch 계정 생성](#31-elasticsearch-계정-생성) 에서 만들어준 secret을 참조하여 ElasticSearch의 계정을 설정합니다.
- 각각
    - username : elastic 
        - elastic 고정!!!
    - password : jinseongPWD123

```yaml
# user 설정
extraEnvs:
  - name: ELASTIC_PASSWORD # Password
    valueFrom:
      secretKeyRef:
        name: elastic-jinseong-credentials
        key: password
  - name: ELASTIC_USERNAME # Username
    valueFrom:
      secretKeyRef:
        name: elastic-jinseong-credentials
        key: username
```

#### 3.2.3 InitContainer
가장 주요한 부분인 InitContainer 부분입니다.

먼저, 이전에 [TLS 통신을 위해 생성해둔 secret](#30-tls-인증서-생성)을 mount로 StatefulSet으로 생성될 ElasticSearch의 ```/usr/share/elasticsearch/config/certs``` 경로와 마운트 합니다.

```yaml
# tls secret mount
secretMounts:
- name: jinseong-tls
  secretName: jinseong-tls-gen
  path: /usr/share/elasticsearch/config/certs
```

InitContainer와 Main Container가 생성될 *.p12 키를 공유할 emptyDIR 하나를 선언해둡니다.

```yaml
extraVolumes:
- name: elastic-certificates
  emptyDir: {}
```

총 생성된 두개의 volume을 main Container Pod의 경로와 mount시켜줍니다.
- ```/usr/share/elasticsearch/config/certs``` : 미리 생성해둔 ca.crt, ca.key 가 들어감
- ```/usr/share/elasticsearch/config/certs-gen``` : InitContainer가 생성할 elastic-cert.p12 key가 들어감

```yaml
extraVolumeMounts:
- name: jinseong-tls
  mountPath: /usr/share/elasticsearch/config/certs
- name: elastic-certificates
  mountPath: /usr/share/elasticsearch/config/certs-gen
```

```esConfig.elasticsearch.yml``` 을 사용하여 http 설정은 제외하고 ElasticSearch Node끼리 통신할 때만 TLS 인증을 진행하도록 세팅합니다.

```yaml
esConfig:
  elasticsearch.yml: |
    xpack.security.enabled: true
    xpack.security.transport.ssl.enabled: true
    xpack.security.transport.ssl.verification_mode: certificate
    xpack.security.transport.ssl.client_authentication: required
    xpack.security.transport.ssl.keystore.path: /usr/share/elasticsearch/config/certs-gen/elastic-cert.p12
    xpack.security.transport.ssl.truststore.path: /usr/share/elasticsearch/config/certs-gen/elastic-cert.p12
```

InitContainer를 선언합니다.
- Container Image는 Main Pod Container Image와 동일하게 구성합니다.
- command 부분에서 ElasticSearch의 기본 binary file 중 ```./bin/elasticsearch-certutil``` 를 사용하여 ```ca.crt```와 ```ca.key```를 통해 ```elastic-cert.p12```를 생성합니다.

    ```set -euo pipefail``` 으로 아래 bash코드가 에러발생한다면 즉시 작동을 중지하게끔 구성합니다. **만약 해당 구문때문에 에러발생한다면 set -euo pipefail 제거**
- bash if 구문으로 만약 ```elastic-cert.p12``` 가 생성되어 있다면 ```elastic-cert.p12``` 를 생성하지 않도록 막습니다.
- 위에서 미리 설정해둔 볼륨들과 voluemMounts를 진행하여 ```ca.crt```와 ```ca.key```를 secret에서 얻고, 생성될 ```elastic-cert.p12``` 를 각 노드(파드)와 emptyDir로 공유합니다.
    - [참고 : ElasticSearch Binary : elasticsearch-certutil 공식문서](https://www.elastic.co/guide/en/elasticsearch/reference/7.17/certutil.html)
    - 위 링크 타고들어가서 좌측 리스트를 확인해보면, ElasticSearch의 /bin 폴더에 위치하고있는 바이너리파일들을 모두 확인할 수 있습니다.


```./bin/elasticsearch-certutil``` 명령어를 아래처럼 작성해야 합니다.
- dns, IP 안에있는 환경변수들을 InitContainer env에서 받아와 등록합니다.
- 아래 명령어로 cert 와 key를 발급받습니다.
```bash
elasticsearch-certutil cert \
      --name master-0 \
      --days 1000 \
      --ip ${POD_IP} \
      --dns master-0,master-svc,master-svc-headless,master-0.master-svc \
      --ca-cert /usr/share/elasticsearch/config/certs/tls.crt \
      --ca-key /usr/share/elasticsearch/config/certs/tls.key  \
      --ca-pass "" \
      --pass "" \
      --out /usr/share/elasticsearch/config/certs-gen/keystore.p12
```

InitContainer 전체 yaml 코드는 다음과 같습니다.
```yaml
extraInitContainers:
- name: setup-tls-cert
  image: "docker.elastic.co/elasticsearch/elasticsearch:7.10.0"
  command:
  - sh
  - -c
  - |
    #!/usr/bin/env bash
    set -euo pipefail

    if [[ ! -f /usr/share/elasticsearch/config/certs-gen/elastic-cert.p12 ]]; then
      ./bin/elasticsearch-certutil cert \
        --name ${NODE_NAME} \
        --days 1000 \
        --ip ${POD_IP} \
        --dns ${NODE_NAME},${POD_SERVICE_NAME},${POD_SERVICE_NAME_HEADLESS},${NODE_NAME}.${POD_SERVICE_NAME},${NODE_NAME}.${POD_SERVICE_NAME_HEADLESS} \
        --ca-cert /usr/share/elasticsearch/config/certs/tls.crt \
        --ca-key /usr/share/elasticsearch/config/certs/tls.key  \
        --ca-pass "" \
        --pass "" \
        --out /usr/share/elasticsearch/config/certs-gen/elastic-cert.p12
    fi;

  env:
  - name: NODE_NAME
    valueFrom:
      fieldRef:
        fieldPath: metadata.name
  - name: POD_IP
    valueFrom:
      fieldRef:
        fieldPath: status.podIP
  - name: POD_SERVICE_NAME
    value: "elasticsearch-master"
  - name: POD_SERVICE_NAME_HEADLESS
    value: "elasticsearch-master-headless"
  volumeMounts:
  - name: jinseong-tls 
    mountPath: /usr/share/elasticsearch/config/certs
  - name: elastic-certificates
    mountPath: /usr/share/elasticsearch/config/certs-gen/es-certs
```


### 3.3 배포
helm upgarde 명령어를 통해 ElasticSearch를 Kubernetes Cluster에 배포합니다.
```yaml
helm upgrade --install es . -n default -f values.yaml -f setting-values.yaml
```

### 3.4 Ingress 구성
Ingress용 tls 인증서 구성합니다.

```bash
kubectl create secret tls elastic-tls --key {key.pem} --cert {cert.pem} -n default --save-config
```

ElasticSearch Ingress를 생성합니다.

```yaml
cat <<EOF> elastic-ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: elastic-ingress
  namespace: default
  annotations:
    kubernetes.io/ingress.class: nginx
    ingress.kubernetes.io/ssl-redirect: "true"
    kubernetes.io/ingress.class: "nginx"
    nginx.ingress.kubernetes.io/proxy-body-size: "1tb"
spec:
  tls:
  - hosts:
    - elastic.jinseong.xxx
    secretName: elastic-tls
  rules:
  - host: "elastic.jinseong.xxx"
    http:
      paths:
      - pathType: Prefix
        path: /
        backend:
          service:
            name: elasticsearch-master
            port:
              number: 9200
EOF
```

## 4. 결과 확인
ElasticSearch Ingress에 접근하여 **로그인창이 출력되는지 확인하고, 로그인이 정상수행되는지 확인합니다.**

## 5. ETC - Kibana
추가로, Kibana를 설치할 땐 동일하게 **kibana helm chart version 7.10.x** 으로 설치합니다.
- 7.10.2 까지 정상작동하는것 확인

다른 점으로, values.yaml에 아래와 같이 Kubernetes Cluster 내부의 ElasticSearch 진입점을 작성해주고 단순 helm 설치하면 됩니다.
- 둘다필요

```yaml
kibanaConfig: 
  kibana.yml: |
    elasticsearch.hosts: [http://elasticsearch-master.default.svc.cluster.local:9200]

elasticsearchHosts: "http://elasticsearch-master.default.svc.cluster.local:9200"
```