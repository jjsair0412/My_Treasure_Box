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
- 폐쇄망 환경에서 gitlab을 구성할 때 , 아래설정값들을 모두 변경시켜주어야 합니다.
```
# required option
global:
  hosts:
    domain: gitlab.xxx.xxx.xyz
	externalIP: http://gitlab.xxx.xxx.xxx

minio:
  image: harbor.xxx.xxx.xyz/gitlab/minio/minio
  imageTag: RELEASE.2017-12-28T01-21-00Z

gitlab-runner:
  image: harbor.xxx.xxx.xyz/gitlab/gitlab/gitlab-runner

global:
  busybox:
    image:
	  repository: harbor.xxx.xxx.xyz/gitlab/registry.gitlab.com/gitlab-org/cloud-native/mirror/images/busybox
	  tag: latest

gitlab:
  gitaly:
    image: 
	  repository: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/gitaly
      tag: v15.1.0
  gitlab-shell:  
    image
	  repository: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/gitlab-shell
	  tag: v14.7.4
  migrations:
    image:
	  repository: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/gitlab-toolbox-ee
	  tag: v15.1.0
  sidekiq:
    image:
	  repository: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/gitlab-sidekiq-ee
	  tag: v15.1.0
  toolbox:
    image:
	  repository: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/gitlab-toolbox-ee
	  tag: v15.1.0
  webservice:
    image:
	  repository: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/gitlab-webservice-ee
	  tag: v15.1.0
    workhorse: 
	  image: harbor.xxx.xxx.xxx/gitlab/registry.gitlab.com/gitlab-org/build/cng/gitlab-workhorse-ee
```

### 2.5 helm install
- gitlab-gitaly의 storageClass와 accessMode는 values파일에서 관리하지 않고 , set 옵션을 이용해 모드를 변경해줍니다.
- gitlab에서 cicd 작업을 하기 위해 gitlab-runner를 설치해 줍니다.
[gitlab set 옵션 목록](https://docs.gitlab.com/charts/installation/command-line-options.html#rbac-settings)
```
helm upgrade --install gitlab gitlab/gitlab \
--namespace=gitlab \
--set gitlab-runner.install=true \
--set certmanager.install=false \
--set nginx-ingress.enabled=false \
--set global.ingress.configureCertmanager=false \
--set global.ingress.tls.secretName=custom-ca \
--set gitlab.gitlab-runner.certsSecretName="gitlab-runner-certs" \
--set gitlab-runner.certsSecretName="gitlab-runner-certs" \
--set gitlab-runner.runners.cache.cacheShared=true \
--set gitlab-runner.runners.cache.secretName=gitlab-minio-secret \
--set gitlab-runner.runners.cache.s3CachePath=runner-cache \
--set gitlab.gitlab-runner.certsSecretName="gitlab-runner-cert" \
--set gitlab.gitaly.persistence.storageClass=ceph-filesystem \
--set gitlab.gitaly.persistence.accessMode="ReadWriteMany" \
--set global.certificates.customCAs[0].secret=custom-ca \
--set prometheus.server.persistentVolume.storageClass=ceph-filesystem \
-f values.yaml,persistent-volume.yaml
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