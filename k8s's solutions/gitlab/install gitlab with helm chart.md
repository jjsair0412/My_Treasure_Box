# kubernetes gitlab 설치 - with helm chart
## 1. Prerequisites
- 아래 문서는 helm chart를 이용해 gitlab을 설치하는 방안에 대해 설명합니다.
## 2. install gitlab 
### 2.1 gitlab을 설치할  namespace 생성
```
$ kubectl create namespace gitlab
```

### 2.2 helm repo에 gitlab repo추가 및 pull
- helm repo에 gitlab을 add합니다.
```
$ helm repo add gitlab https://charts.gitlab.io
$ helm pull gitlab/gitlab-runner --untar
```

### 2.3 tls 인증서 생성
- gitlab은 tls 인증서 정보가 필요하기 때문에 , crt파일과 key값을 이용해서 tls 인증서를 생성해 줍니다.
```
kubectl create -n gitlab secret tls custom-ca --key eks.xxx.xyz.key --cert eks.xxx.xyz.crt
```

### 2.4 values.yaml 수정 ( values 파일 생성 )
- 구축 환경에서 , storageClass는 ceph를 사용하며 , RWX를 설정해야 하기 때문에 설치되는 솔루션에 해당 옵션을 부여합니다.
```
$ cat persistent-volume.yaml
grafana:
  persistence:
    storageClassName: ceph-filesystem
    accessModes:
      - ReadWriteMany

minio:
  persistence:
    accessMode: ReadWriteMany
    storageClass: ceph-filesystem

postgresql:
  persistence:
    accessModes:
      - ReadWriteMany
    storageClass: ceph-filesystem

prometheus:
  server:
    persistentVolume:
      storageClass: ceph-filesystem
      accessModes:
        - ReadWriteMany

pushgateway:
  persistentVolume:
    storageClassName: ceph-filesystem
    accessModes:
      - ReadWriteMany

alertmanager:
  persistentVolume:
    storageClassName: ceph-filesystem
    accessModes:
      - ReadWriteMany

redis:
  master:
    persistence:
      accessModes:
        - ReadWriteMany
      storageClass: ceph-filesystem
```
- private registry에서 이미지를 꺼내오는 경우 ( 폐쇄망 등 ... ) 아래 values파일의 속성값들을 모두 변경시켜주어야 합니다.
- 필요한 image리스트또한 작성되어있습니다.
```
# gitlab은 ingress가 자동생성됩니다. domain과 https & http 옵션을 global 구성에서 선택합니다.
global: 
  edition: ce # gitlab 버전 ( 무료 & 유료 선택 . 유료는 ee. 이미지가 다름 )
  hosts:
    domain: xxx.xxx.xyz # ingress host domain 주소 
    gitlab:
      name: gitlab.xxx.xxx.xyz 
      https: true
    registry:
      name: registry.xxx.xxx.xxx # registry host domain 주소
      https: true
    minio: 
      name: minio.xxx.xxx.xxx # minio domain 주소
      https: true
  ingress:
    configureCertmanager: false
    class: "nginx"
    tls:
      enable: false
  certificates:
    image:
      repository: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/alpine-certificates
      tag: 20191127-r2@sha256:56d3c0dbd1d425f24b21f38cb8d68864ca2dd1a3acc28b65d0be2c2197819a6a
      pullPolicy: IfNotPresent
  kubectl:
    image:
      repository: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/kubectl
      tag: 1.18.20@sha256:aebdfcf7bde7b80ad5eef7a472d1128542c977dc99b50c3e471fc98afbb9f52c
      pullPolicy: IfNotPresent
  busybox:
    image:
      repository: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/cloud-native/mirror/images/busybox
      tag: latest
      pullPolicy: IfNotPresent
  global:
    communityImages:
      migrations:
        repository: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/gitlab-toolbox-ce
      sidekiq:
        repository: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/gitlab-sidekiq-ce
      toolbox:
        repository: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/gitlab-toolbox-ce
      webservice:
        repository: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/gitlab-webservice-ce
      workhorse:
        repository: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/gitlab-workhorse-ce


cert-manager:
  image:
    repository: quay.io/jetstack/cert-manager-controller
  webhook:
    image:
      repository: quay.io/jetstack/cert-manager-webhook

# gitlab 차트 내부 gitlab에서 , ce와 ee 이미지 두가지로 나뉩니다. 
# ee는 엔터프라이즈 버전 ( 유료 ) gitlab을 사용햇을 경우 필요한 이미지이고 , 아래 예제의 ce 이미지는 무료 버전의 이미지입니다. ( image name example : gitlab/registry.gitlab.com/gitlab-org/build/cng/gitlab-sidekiq-ce )
# global 옵션에서 엔터프라이즈 버전과 무료버전을 선택할 수 있습니다. global.edition: ce 
# 해당 예시는 무료 버전인 ce 이미지를 사용합니다.
gitlab:
  prometheus:
    server:
      image:
        repository: harbor.xxx.xxx.xxx/gitlab/quay.io/prometheus/prometheus
        tag: v2.31.1
        pullPolicy: IfNotPresent
  kas:
    image:
      repository: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/gitlab-kas
      tag: v15.1.0
      pullPolicy: IfNotPresent
  gitlab-shell:
    image:
      repository: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/gitlab-shell
      pullPolicy: IfNotPresent
      tag: v14.7.4
  gitaly:
    image:
      repository: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/gitaly
      tag: v15.1.0
      pullPolicy: IfNotPresent
    init:
      image:
        repository: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/alpine-certificates
        tag: 20191127-r2
  gitlab-exporter:
    image:
      repository: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/gitlab-exporter
      tag: 11.16.0
      pullPolicy: IfNotPresent
  migrations:
    image:
      repository: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/gitlab-toolbox-ce
      tag: v15.0.3
      pullPolicy: IfNotPresent
    init:
      image:
        repository: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/alpine-certificates
        tag: 20191127-r2
  sidekiq:
    init:
      image:
        repository: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/gitlab-sidekiq-ce
        tag: v15.0.3
    image:
      repository: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/gitlab-sidekiq-ce
      tag: v15.0.3
  webservice:
    init:
      dependencies:
        image:
          repository: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/gitlab-webservice-ce
          tag: v15.0.3
    image:
      repository: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/gitlab-webservice-ce
      tag: v15.0.3
    workhorse:
      image: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/gitlab-workhorse-ce

gitlab-runner:
  gitlabUrl: http://gitlab.xxx.xxx.xxx # gitlab domain 주소
  image: harbor.xxx.xxx.xxx/gitlab/gitlab/gitlab-runner:alpine-v15.0.0

grafana:
  image:
    repository: grafana/grafana
    tag: 7.5.5

minio:
  image: harbor.xxx.xxx.xxx/gitlab/minio/minio
  imageTag: RELEASE.2017-12-28T01-21-00Z
  minioMc:
    image: harbor.xxx.xxx.xxx/gitlab/minio/mc
    tag: RELEASE.2018-07-13T00-53-22Z

postgresql:
  image:
    registry: harbor.xxx.xxx.xxx
    repository: gitlab/bitnami/postgresql
    tag: 12.11.0-debian-11-r13
  metrics:
    image:
      registry: harbor.xxx.xxx.xxx
      repository: gitlab/bitnami/postgres-exporter
      tag: 0.8.0-debian-10-r99
      pullPolicy: IfNotPresent

redis:
  image:
    registry: harbor.xxx.xxx.xxx
    repository: gitlab/bitnami/redis
    tag: 6.0.9-debian-10-r0
  metrics:
    image:
      registry: harbor.xxx.xxx.xxx
      repository: gitlab/bitnami/redis-exporter
      tag: 1.12.1-debian-10-r11
      pullPolicy: IfNotPresent

registry:
  image:
    repository: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/gitlab-container-registry
    tag: v3.48.0-gitlab

prometheus:
  server:
    image:
      repository: harbor.xxx.xxx.xxx/gitlab/quay.io/prometheus/prometheus
      tag: v2.31.1
      pullPolicy: IfNotPresent
  configmapReload:
    prometheus:
      image:
        repository: harbor.xxx.xxx.xxx/gitlab/jimmidyson/configmap-reload
        tag: v0.5.0
        pullPolicy: IfNotPresent



upgradeCheck:
  enabled: true
  image:
    repository:
    tag:
    pullPolicy: IfNotPresent

shared-secrets:
  selfsign:
    image:
      pullPolicy: IfNotPresent
      repository:
      tag:

# gitlab은 tls인증서가 필요합니다. 해당 예시에서는 custom-tls라는 secret을 ca 파일을 통해 미리 만들어주었기 때문에
# certmanager를 설치하지 않습니다.
certmanager:
  install: false

# nginx-ingress가 이미 있기때문에 false
nginx-ingress: 
  enabled: false
```

### 2.5 helm install
- gitlab-gitaly의 storageClass와 accessMode는 values파일에서 관리하지 않고 , set 옵션을 이용해 모드를 변경해줍니다.
- gitlab에서 cicd 작업을 하기 위해 gitlab-runner를 설치해 줍니다.
[gitlab set 옵션 목록](https://docs.gitlab.com/charts/installation/command-line-options.html#rbac-settings)
```
helm upgrade --install gitlab . --namespace gitlab  \
--set gitlab-runner.install=true \ # gitlab-runner 설치
--set certmanager.install=false \
--set global.ingress.configureCertmanager=false \
--set nginx-ingress.enabled=false \
--set global.ingress.tls.secretName=custom-tls \
--set global.certificates.customCAs[0].secret=custom-tls \
-f values.yaml,setting-values.yaml
```
### 2.6 gitlab ingress 생성
```
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: gitlab-ingress
  namespace: gitlab
  annotations:
    kubernetes.io/ingress.class: "nginx"
spec:
  rules:
  - host: "gitlab.xxx.xyz"
    http:
      paths:
      - pathType: Prefix
        path: /
        backend:
          service:
            name: gitlab-webservice-default
            port:
              number: 8181

  tls:
  - hosts:
    - gitlab.xxx.xyz
    secretName: custom-ca

```



## 3. 파드 상태 확인
### 3.1 gitlab 상태 확인
```
kubectl get all -n gitlab
```
### 3.2 gitlab 접속을 위한 초기 비밀번호 생성
```
kubectl get secret gitlab-gitlab-initial-root-password -n gitlab -ojsonpath='{.data.password}' | base64 --decode ; echo
```
## 4. Troubleshooting
### 4.1 Gitlab Runner 설정 변경
- Cert Manager로 생성 된 인증서가 아닌 Self Sign 인증서를 사용 할 경우 Gitlab Runner에서 아래와 같은 Error Meassge가 발생
```
Couldn't execute POST against https://xxx.com/api/v4/jobs/request: Post https://hostname.tld/api/v4/jobs/request: x509: certificate signed by unknown authority
```
- 위와 같은 에러 발생 시 Runner 아래 설정을 추가
- CA 파일 Secret 생성 (이전 Harbor 설치 시 사용한 CA)
```
$ kubectl create secret generic gitlab-runner-certs \
--from-file=ca.crt
```
- Runner Deployment Yaml File에 gitlab-runner-certs 인증서 Mount와 환경 변수 설정 후 Redeploy
```
volumeMounts:
- mountPath: /etc/gitlab-runner/certs
  name: gitlab-runner-certs
volumes:
- name: gitlab-runner-certs
   secret:
     defaultMode: 438
     secretName: gitlab-runner-certs
env:
- name: CI_SERVER_TLS_CA_FILE
  value: /etc/gitlab-runner/certs/xxx.xxx.leedh.cloud
```
- 위 설정 완료 후 Runner 실행 시 /etc/gitlab-runner/certs/xxx.xxx.leedh.cloud 파일을 읽어 오다 Permission Denie 에러가 발생 할 경우 아래 설정을 0, 0으로 변경하여 Pod Security 변경
```
securityContext:
  fsGroup: 0
  runAsUser: 0
```
### 4.1.1 rke2 환경일 경우
- rke2 환경에서 아래와 같은 에러가 발생한다면 , /etc/rancher/rke2/registries.yaml 설정의 mirror와 , gitlab의 인증정보들을 넣어준 후 rke2를 재 시작 해주어야 합니다.
```
# cat registries.yaml
mirrors:
  docker.io:
    endpoint:
      - "http://10.xxx.xxx.xxx:5000"
  10.xxx.xxx.xxx:5000:
    endpoint:
      - "http://10.xxx.xxx.xxx:5000"
  harbor.xxx.xxx.xxx:
    endpoint:
      - "http://harbor.xxx.xxx.xxx"
  gitlab.xxx.xxx.xxx: # gitlab 정보 mirrors에 추가
    endpoint:
      - "http://gitlab.xxx.xxx.xxx"


configs:
  "harbor.xxx.xxx.xxx":
    auth:
      username: admin
      password: Harbor12345
    tls:
      insecure_skip_verify: true
  "gitlab.xxx.xxx.xxx": # 추가된 mirror에대한 config 추가 ( 인증 정보 및 .. ) 
    auth:
      username: root # gitlab login 정보
      password: pgAxVAi... # gitlab password
    tls:
      insecure_skip_verify: true # https 무시 ( ssl 인증서 없을경우 추가 )
```