# ArgoCD Install Helm
## [](#prerequisites)1. Prerequisites

-  Helm chart를 이용한 argocd 설치 
- 
## 2. ArgoCD 설치
### 2.1 namespace 설정
- ArgoCD 배포용 namespace 설정
```
$ kubectl create namespace argo
```
### 2.2 Helm Install & ArgoCD 설정
- Helm ArgoCD 설치 Directory로 이동
```
$ cd 2.yaml/ArgoCD
```
- worker Node에 배포하기 위해 Affinity 설정
```
$ cat affinity-values.yaml
controller:
  affinity:
   nodeAffinity:
     requiredDuringSchedulingIgnoredDuringExecution:
       nodeSelectorTerms:
       - matchExpressions:
         - key: role
           operator: NotIn
           values:
           - "controlplane"
           - "router"
           - "infra"
  nodeSelector:
    role: "worker"


dex:
  affinity:
    nodeAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        nodeSelectorTerms:
        - matchExpressions:
          - key: role
            operator: NotIn
            values:
            - "controlplane"
            - "router"
            - "infra"
  nodeSelector:
    role: "worker"


repoServer:
  affinity:
    nodeAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        nodeSelectorTerms:
        - matchExpressions:
          - key: role
            operator: NotIn
            values:
            - "controlplane"
            - "router"
            - "infra"
  nodeSelector:
    role: "worker"


server:
  affinity:
    nodeAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        nodeSelectorTerms:
        - matchExpressions:
          - key: role
            operator: NotIn
            values:
            - "controlplane"
            - "router"
            - "infra"
  nodeSelector:
    role: "worker"


applicationSet:
  affinity:
    nodeAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        nodeSelectorTerms:
        - matchExpressions:
          - key: role
            operator: NotIn
            values:
            - "controlplane"
            - "router"
            - "infra"
  nodeSelector:
    role: "worker"


notifications:
  affinity:
    nodeAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        nodeSelectorTerms:
        - matchExpressions:
          - key: role
            operator: NotIn
            values:
            - "controlplane"
            - "router"
            - "infra"
  nodeSelector:
    role: "worker"



redis:
  affinity:
    nodeAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        nodeSelectorTerms:
        - matchExpressions:
          - key: role
            operator: NotIn
            values:
            - "controlplane"
            - "router"
            - "infra"
  nodeSelector:
    role: "worker"

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
