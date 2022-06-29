
# ArgoCD Install Helm
## [](#prerequisites)1. Prerequisites

-   Helm chart를 이용한 argocd 설치
-   참고 링크
    -   [argocd docs](https://argo-cd.readthedocs.io/en/stable/)
    -   [argocd Helm Chart](https://github.com/argoproj/argo-helm/tree/master/charts/argo-cd)
   
## 2. ArgoCD 설치
### 2.1 namespace 설정
- ArgoCD 배포용 namespace 설정
```
$ kubectl create namespace argo
```

-   Agrocd Helm Repo 추가 및 동기화

```
$ helm repo add argo https://argoproj.github.io/argo-helm
$ helm repo update
```

-   Agrocd 설치 용 Helm Download

```
$ helm pull argo/argo-cd --untar
```
- 특정 node에 배포하기 위해 affinity 설정
- 원하는 옵션으로 바꿔서 설정해주면 된다.
```
$ cat affinity-values.yaml
controller:
  affinity:
    ...
  nodeSelector:
    ...


dex:
  affinity:
    ...
  nodeSelector:
    ...

repoServer:
  affinity:
    ...
  nodeSelector:
    ...

server:
  affinity:
    ...
  nodeSelector:
    ...


applicationSet:
  affinity:
    ...
  nodeSelector:
    ... 

notifications:
  affinity:
    ...
  nodeSelector:
    ...


redis:
  affinity:
    ...
  nodeSelector:
    ... 

```
- 폐쇄망 구성 및 private registry에서 image를 받아올 경우 , 아래 속성들을 변경시켜 준다.
```
$ cat private-values.yaml
global:
  image:
    repository: 10.xxx.xxx:5000
    tag: v2.4.0

dex:
  initImage:
    repository: 10.xxx.xxx:5000/quay.io/argoproj/argocd
    tag: v2.4.0
  image:
    repository: 10.xxx.xxx:5000/ghcr.io/dexidp/dex
    tag: v2.30.2

controller:
  image:
    repository: 10.xxx.xxx:5000/quay.io/argoproj/argocd
    tag: v2.4.0

applicationSet:
  image:
    repository: 10.xxx.xxx:5000/quay.io/argoproj/argocd
    tag: v2.4.0

repoServer:
  image:
    repository: 10.xxx.xxx:5000/quay.io/argoproj/argocd
    tag: v2.4.0

server:
  image:
    repository: 10.xxx.xxx:5000/quay.io/argoproj/argocd
    tag: v2.4.0

redis:
  image:
    repository: 10.xxx.xxx:5000/redis
    tag: 7.0.0-alpine

notifications:
  image:
    repository: 10.xxx.xxx:5000/quay.io/argoproj/argocd
    tag: v2.4.0
```
### 2.3 Helm ArgoCD Chart 설치
```
$ helm upgrade --install argocd . \
--namespace=argo \
--set controller.logLevel="info" \
--set server.logLevel="info" \
--set repoServer.logLevel="info" \
--set server.replicas=2 \
--set server.ingress.https=true \
--set repoServer.replicas=2 \
--set controller.enableStatefulSet=true \
--set installCRDs=false \
-f values.yaml,affinity-values.yaml
```

### 2.4 ArgoCD ingress 설정
-   Https Ingress 용 TLS 인증서 생성
- TLS 인증서를 생성하지 않아도 무관하다. -> 실습할때만

```
$ kubectl create -n argocd secret tls argocd-tls --key {key.file.name} --cert {cert.file.name}
```

-   Argo CD runs both a gRPC server (used by the CLI), as well as a HTTP/HTTPS server 용 Ingress 2개를 생성
-   주의 사항은 Insecure Mode를 활성화 하지 않을 경우 nginx.ingress.kubernetes.io/backend-protocol: "HTTPS"를 명시해줘야 Redirection Loop Error가 발생 하지 않음
```
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: argocd-server-http-ingress
  namespace: argo
  annotations:
    kubernetes.io/ingress.class: "nginx"
    ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
    nginx.ingress.kubernetes.io/backend-protocol: "HTTPS"
spec:
  rules:
  - host: "jinseong.xxx.xxx.net"
    http:
      paths:
      - pathType: Prefix
        path: /
        backend:
          service:
            name: argocd-server
            port:
              number: 80
  tls: # TLS를 생성하지 않았다면 , 해당 key와 value들 제거
  - hosts:
    - jinseong.xxx.xxx.net
    secretName: # tls secret name

---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: argocd-server-grpc-ingress
  namespace: argo
  annotations:
    kubernetes.io/ingress.class: "nginx"
    nginx.ingress.kubernetes.io/backend-protocol: "GRPC"
spec:
  rules:
  - host: "dev-jinseong.xxx.xxx.net"
    http:
      paths:
      - pathType: Prefix
        path: /
        backend:
          service:
            name: argocd-server
            port:
              number: 80
  tls: # TLS를 생성하지 않았다면 , 해당 key와 value들 제거
  - hosts:
    - dev-jinseong.xxx.xxx.net
    secretName: # tls secret name
```
## 3. 초기 UI 확인
- 초기 Password Get
- 초기 id 또한 admin이다.
```
$ kubectl -n argo get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d && echo
```
-  ArgoCD UI 확인
![argocd-ui-1][argo-ui-1]

  

[argo-ui-1]:./images/argo-ui-1.PNG