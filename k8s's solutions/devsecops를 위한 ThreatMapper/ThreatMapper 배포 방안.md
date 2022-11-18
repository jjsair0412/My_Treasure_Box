# ThreatMapper 배포 방안
deepfence의 ThreatMapper를 배포하는 방안에 대해 기술합니다.
- 공식 git : https://github.com/deepfence/ThreatMapper

## 0. what is ThreatMapper ?
ThreatMapper는 프로덕션 플랫폼에서 위협을 찾고 악용 위험을 기준으로 이러한 위협의 순위를 지정합니다. 

취약한 소프트웨어 구성 요소, 노출된 비밀 및 우수한 보안 관행에서 벗어난 부분을 찾아냅니다. 
ThreatMapper는 에이전트 기반 검사와 에이전트 없는 모니터링의 조합을 사용하여 위협을 탐지할 수 있는 가장 광범위한 범위를 제공합니다.

ThreatMapper의 **ThreatGraph** 시각화를 사용하면 애플리케이션 보안에 가장 큰 위험을 초래하는 문제를 식별하고 계획된 보호 또는 치료를 위해 우선 순위를 지정할 수 있습니다.

출처 : https://github.com/deepfence/ThreatMapper
ThreatMapper docs : https://community.deepfence.io/docs/threatmapper/

## 0. why using ThreatMapper ?
ThreatMapper는 개발 파이프라인에서 이미 사용하고 있는 우수한 '시프트 레프트' 보안 관행을 수행합니다. 새로운 소프트웨어 취약성에 대해 실행 중인 애플리케이션을 계속 모니터링하고 업계 전문가 벤치마크에 대해 호스트 및 클라우드 구성을 모니터링합니다.

ThreatMapper를 사용하여 클라우드, kubernetes, 서버리스(Fargate) 및 온프레미스 플랫폼에서 프로덕션 워크로드 및 인프라에 대한 보안 관찰 가능성을 제공합니다.

## 1. install precondition
최소 사양은 다음과 같습니다.

deepfence-console의 최소 사양이긴 하지만 , 아래와 같은 사양을 맞춰주어야 pod가 정상적으로 올라갑니다.
### 1.1 System Requirements

The Management Console may be installed on a single Docker host or in a dedicated Kubernetes cluster:

  * A Docker Host is suitable for small-scale deployments, managing up to several hundred production nodes
  * A Kubernetes Cluster is suitable for small and large-scale deployments 

| Feature                                   | Requirements (Docker)                                                                                                                  | Requirements (Kubernetes)           | 
|-------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------|
| CPU: No of cores                          | 4                                                                                                                                      | 3 nodes, 4 cores each               |
| RAM                                       | 16 GB                                                                                                                                  | 3 nodes, 8 GB each                  |
| Telemetry and data from Deepfence Sensors | Port 443 (configurable), firewalled                                                                                                    | Port 443 (configurable), firewalled |
| Administrative and API access             | Port 443 (configurable), firewalled                                                                                                    | Port 443 (configurable), firewalled |
| Docker                                    | *Version 20.10.18 (minimum version 18.06.0)                                                                                            |
| Tuning                                    | `sysctl -w vm.max_map_count=262144` # [details](https://www.elastic.co/guide/en/elasticsearch/reference/current/vm-max-map-count.html) |

Larger deployments, managing 250 or more production nodes, will require additional CPU and RAM resources.  For enterprise-scale deployments, managing 1000+ production nodes, the ThreatMapper Console should be deployed on a Kubernetes cluster of 3 or more nodes.

출처 : https://github.com/deepfence/ThreatMapper/blob/master/docs/docs/threatmapper/console/requirements.md#system-requirements

## 2. deploy ThreatMapper
ThreatMapper는 두 part로 분리하여 배포합니다.
1. ThreatMapper management console
	- 단일 도커 호스트 또는 Kubernetes 클러스터에 배포할 수 있는 컨테이너 기반 애플리케이션입니다.
2. ThreatMapper agent 
	- ThreatMapper는 에이전트 없는 **Cloud Scanner** 작업 및 에이전트 기반 **센서 에이전트 를 사용하여 실행 중인 인프라를 모니터링합니다.**

배포할 모든 솔루션은 동일 namespace에 존재 해야 하기 때문에 , 배포용 ns부터 생성합니다.
```bash
$ kubectl create ns deepfence
```

### 2.1 Deploy management console 
ThreatMapper의 management console을 배포합니다.

#### 2.1.1 deploy metric server
**가장 먼저 metric server를 배포해야 합니다.**
get 명령으로 metric server가 설치 대상 cluster에 존재 하는지를 확인한 뒤 , 없다면 metric server를 배포합니다.
```bash
# 존재 하는지 확인
$ kubectl get -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# 없다면 배포
$ kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```
#### 2.1.2 Deploy ThreatMapper console
helm chart를 통해 배포합니다.
```bash
helm repo add deepfence https://deepfence-helm-charts.s3.amazonaws.com/threatmapper

helm pull deepfence/deepfence-console --untar
```

console의 values값을 일부 변경해야 합니다.

1. container runtime
	- k8s cluster에서 사용중인 container runtime을 맞춰 주어야 합니다. 
	  docker를 사용중이기에 dockerSock 을 true로 설정합니다.
2. storageClass setting
	- default로 openebs storageclass가 등록되어 있기 때문에 , 사용중인 sc로 변경합니다.
3. clusterDomain setting
	- k8s cluster domain을 setting 합니다 . 
	- default가 cluster.local이기에 kubeconfig파일이 변화하지 않는다면 default로 사용합니다.
4. modify deepfence ui expose service type
	- ui 배포 타입을 지정합니다. default는 ClusterIP인데 .. default로 사용합니다.
	- 실제 ui 접근은 deepfence-route로 진행합니다.
```yaml
$ cat setting-values.yaml
# container runtime setting
# default = using containerd 
mountContainerRuntimeSocket:
  dockerSock: true
  # Change if socket path is not the following
  dockerSockPath: "/var/run/docker.sock"
  containerdSock: false
  # Change if socket path is not the following
  containerdSockPath: "/run/containerd/containerd.sock"
  crioSock: false
  # Change if socket path is not the following
  crioSockPath: "/var/run/crio/crio.sock"


# storageClass setting
# default = openebs-hostpath
volume:
  storageClass: local 


# clusterDomain setting 
# default = cluster.local
clusterDomain: "cluster.local"

# modify deepfence ui expose service type
# default = ClusterIP
router:
  service:
    type: ClusterIP
```

helm install 진행합니다.
```bash
$ helm upgrade --install deepfence-console . -n deepfence -f values.yaml,setting-values.yaml
```

정상적으로 설치 되었는지 확인합니다.
설치에는 2~5분정도 시간이 소요됩니다.
```bash
$ kubectl get pods -n deepfence
```

### 2.2 Deploy deepfence-router
ui에 접근하기 위한 service를 만드는 router를 배포합니다.

동일하게 helm chart로 배포합니다.
```bash
helm pull deepfence/deepfence-router --untar
```

values.yaml의 세팅값을 변경합니다.

expose할 type을 지정합니다.
해당 문서는 ingress를 따로 만들어줄 것이기에 , ClusterIP type으로 생성합니다.
```yaml
service:
  name: deepfence-router
  # Select the type of service to be used. 
  # When exposing the service in an on premisses Kubernetes cluster, select NodePort as type
  # Also, possible to use Ingress as type when ingress controller is installed
  type: ClusterIP # LoadBalancer/NodePort/Ingress/ClusterIP
```

helm install 진행합니다.
```bash
$ helm upgrade --install deepfence-router . -n deepfence -f values.yaml,setting-values.yaml
```

pod와 service가 정상적으로 배포되었는지 확인합니다.
```bash
$ kubectl get pods -n deepfence | grep route
deepfence-internal-router-575d7f7fff-6xshc        1/1     Running   0             63m
deepfence-router-bd995cff5-fdvd9                  1/1     Running   0             63m


$ kubectl get service -n deepfence | grep route
deepfence-internal-router                ClusterIP   10.233.6.167    <none>        443/TCP                      64m
deepfence-router                         ClusterIP   10.233.13.110   <none>        443/TCP,80/TCP               57m
```

### 2.3 ingress 생성
ui에 접근할 ingress를 아래와 같은 yaml로 구성하여 apply 합니다.

아래 annotation중 proxy body size는 required option이며 , cert-manager를 통해 생성된 tls 인증서를 사용하기에 cluster-issuer 어노테이션을 추가합니다.
```yaml
metadata:
  annotations:
    nginx.ingress.kubernetes.io/backend-protocol: HTTPS
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: 200m
```

전체 deepfence-ingress.yaml 파일
```yaml
$ cat deepfence-ingress.yaml

apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: deepfence-ingress
  namespace: deepfence
  annotations:
    kubernetes.io/ingress.class: "nginx"
    nginx.ingress.kubernetes.io/rewrite-target: /
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/backend-protocol: HTTPS
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: 200m
spec:
  rules:
  - host: "deepfence.jinseong.xyz"
    http:
      paths:
      - pathType: Prefix
        path: /
        backend:
          service:
            name: deepfence-router
            port:
              number: 443 # ssl 인증 안한다면 80
  tls:
  - hosts:
    - deepfence.jinseong.xyz
    secretName: deepfence-tls # tls secret name
```

ingress apply 진행합니다.
```bash
$ kubectl apply -f deepfence-ingress.yaml
```

ui에 접근하여 확인합니다.

login 페이지 확인 후 , Register로 회원가입 진행한 뒤 로그인 합니다.

- login 페이지 확인
![deepfence1][deepfence1]

[deepfence1]:./images/deepfence1.PNG

- 회원가입
![deepfence2][deepfence2]

[deepfence2]:./images/deepfence2.PNG

- 가입한 ID로 로그인 한 뒤 main page 확인
![deepfence3][deepfence3]

[deepfence3]:./images/deepfence3.PNG


### 2.4 deploy deepfence-agent 
메트릭을 수집하기 위한 agent를 배포합니다.

deamonset type이 배포되어 모든 노드에 파드가 생성되고 , 메트릭을 수집하여 컨테이너 현황 , 접근 모니터링 등을 수행합니다.
주의할 점은 , console과 버전이 일치해야 합니다.
- **최상의 호환성을 위해 센서의 버전 번호가 관리 콘솔의 버전과 최대한 일치하는지 확인해야 합니다.**

### 2.4.1 key 확인
설치하기 위해 key를 확인합니다.

1. 이전에 배포해두었던 deepfence ui로 접근 한 뒤 , login 합니다.
2. 그리고 우측 상단의 user 모형을 클릭
3. Setting 클릭

Agent Setting 페이지로 넘어가게 되는데 , 여기에 나온 helm 명령어로 그대로 배포하여도 무관하지만 , 형상 관리및 버전 관리를 용이하게 하기 위하여 helm chart를 pull한 뒤 설치 합니다.

deepfenceKey 값을 복사하여 저장해 둡니다.

### 2.4.2 deploy agent
helm chart로 설치합니다.
```bash
helm repo add deepfence https://deepfence-helm-charts.s3.amazonaws.com/threatmapper
helm pull deepfence/deepfence-agent --untar
```

values의 값을 일부 변경합니다. 
1. managementConsoleUrl
	- console url을 작성합니다. 
	- ingress로 배포했기에 , ingress url 작성합니다.
2. deepfenceKey
	- console로 login한 user setting에서 Key값을 확인합니다.
	- 위에서 저장한 key값을 사용합니다.
3. image
	- console과 image version을 맞춥니다.
	- 해당 문서는 1.4.1 version으로 진행합니다.
4. clusterName
	- clusterName을 임의로 지정합니다.
5. mountContainerRuntimeSocket
	- k8s cluster에서 사용중인 container runtime을 선택합니다.
	- default sock 파일 경로는 console chart의 values값에 적혀 있습니다.

```yaml
$ cat setting-values.yaml
managementConsoleUrl: deepfence.jinseong.xyz

deepfenceKey: asdfasdf-asdfasdf-asdfasdf-asdfasdf 
image:
  tag: 1.4.1
  clusterAgentImageTag: 1.4.1

clusterName: prod-cluster

# container runtime setting ( i'm now using docker )
mountContainerRuntimeSocket:
  containerdSock: false
  dockerSock: true
  crioSock: false
  containerdSockPath: "/var/run/docker.sock"
```
helm 배포 진행합니다.
```bash
$ helm upgrade --install deepfence-agent . -n deepfence -f values.yaml,setting-values.yaml
```

## 3. 설치 결과 확인
파드 및 모든 서비스가 모두 정상 동작하는지 확인합니다.
```bash
$ kubectl get all -n deepfence
NAME                                                  READY   STATUS    RESTARTS      AGE
pod/deepfence-agent-7bq7l                             1/1     Running   0             60m
pod/deepfence-agent-blzw9                             1/1     Running   0             60m
pod/deepfence-agent-gskhr                             1/1     Running   0             60m
pod/deepfence-api-6b65fdcb88-85wh9                    1/1     Running   0             83m
pod/deepfence-backend-bd6ff4776-kn5hg                 1/1     Running   0             83m
pod/deepfence-celery-c76bb5b9f-f48z7                  1/1     Running   0             83m
pod/deepfence-cluster-agent-848c7b987c-f4mjm          1/1     Running   0             60m
pod/deepfence-diagnosis-7cc844d596-lqqsm              1/1     Running   0             83m
pod/deepfence-discovery-5lxm8                         1/1     Running   0             83m
pod/deepfence-discovery-8drtz                         1/1     Running   0             83m
pod/deepfence-discovery-blxg4                         1/1     Running   0             83m
pod/deepfence-es-0                                    1/1     Running   0             83m
pod/deepfence-es-1                                    1/1     Running   0             83m
pod/deepfence-es-2                                    1/1     Running   0             83m
pod/deepfence-fetcher-796c5ff9dd-mc7q6                1/1     Running   0             83m
pod/deepfence-internal-router-575d7f7fff-6xshc        1/1     Running   0             83m
pod/deepfence-package-scanner-59dc64b66f-jl62v        1/1     Running   0             83m
pod/deepfence-postgres-pgpool-bbb5fb8bb-x7bq7         1/1     Running   0             83m
pod/deepfence-postgres-postgresql-0                   1/1     Running   0             83m
pod/deepfence-postgres-postgresql-1                   1/1     Running   0             83m
pod/deepfence-postgres-postgresql-2                   1/1     Running   1 (82m ago)   83m
pod/deepfence-redis-77958475f7-k87vx                  1/1     Running   0             83m
pod/deepfence-router-bd995cff5-fdvd9                  1/1     Running   0             83m
pod/deepfence-secret-scaner-85968ff69f-gmt9c          1/1     Running   0             83m
pod/deepfence-topology-78484ccdd6-2jg5x               1/1     Running   0             83m
pod/deepfence-ui-69887bbc47-bvzl9                     1/1     Running   0             83m
pod/deepfence-vulnerability-mapper-7bbd76499c-qtdz8   1/1     Running   0             83m

NAME                                             TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)                      AGE
service/deepfence-api                            ClusterIP   10.xxx.xxx.xxx  <none>        9998/TCP,9997/TCP            83m
service/deepfence-backend                        ClusterIP   10.xxx.xxx.xxx  <none>        4041/TCP                     83m
service/deepfence-diagnosis                      ClusterIP   10.xxx.xxx.xxx  <none>        8009/TCP                     83m
service/deepfence-es                             ClusterIP   None            <none>        9200/TCP,9300/TCP            83m
service/deepfence-fetcher                        ClusterIP   10.xxx.Xx.xxx   <none>        8001/TCP,8002/TCP,8006/TCP   83m
service/deepfence-internal-router                ClusterIP   10.xxx.xx.xxx   <none>        443/TCP                      83m
service/deepfence-package-scanner                ClusterIP   10.xxx.xx.xxx   <none>        8005/TCP                     83m
service/deepfence-postgres                       ClusterIP   10.xxx.xx.xx    <none>        5432/TCP                     83m
service/deepfence-postgres-pgpool                ClusterIP   10.xxx.xxx.xxx  <none>        5432/TCP                     83m
service/deepfence-postgres-postgresql-headless   ClusterIP   None            <none>        5432/TCP                     83m
service/deepfence-redis                          ClusterIP   10.xxx.xxx.xxx  <none>        6379/TCP                     83m
service/deepfence-router                         ClusterIP   10.xxx.xxx.xxx  <none>        443/TCP,80/TCP               77m
service/deepfence-secret-scanner                 ClusterIP   10.xxx.xx.xxx   <none>        8011/TCP                     83m
service/deepfence-topology                       ClusterIP   10.xxx.xx.xxx   <none>        8004/TCP                     83m
service/deepfence-ui                             ClusterIP   10.xxx.xx.xx    <none>        4042/TCP                     83m
service/deepfence-vulnerability-mapper           ClusterIP   10.xxx.xx.xx    <none>        8001/TCP                     83m

NAME                                 DESIRED   CURRENT   READY   UP-TO-DATE   AVAILABLE   NODE SELECTOR            AGE
daemonset.apps/deepfence-agent       3         3         3       3            3           kubernetes.io/os=linux   60m
daemonset.apps/deepfence-discovery   3         3         3       3            3           <none>                   83m

NAME                                             READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/deepfence-api                    1/1     1            1           83m
deployment.apps/deepfence-backend                1/1     1            1           83m
deployment.apps/deepfence-celery                 1/1     1            1           83m
deployment.apps/deepfence-cluster-agent          1/1     1            1           60m
deployment.apps/deepfence-diagnosis              1/1     1            1           83m
deployment.apps/deepfence-fetcher                1/1     1            1           83m
deployment.apps/deepfence-internal-router        1/1     1            1           83m
deployment.apps/deepfence-package-scanner        1/1     1            1           83m
deployment.apps/deepfence-postgres-pgpool        1/1     1            1           83m
deployment.apps/deepfence-redis                  1/1     1            1           83m
deployment.apps/deepfence-router                 1/1     1            1           83m
deployment.apps/deepfence-secret-scaner          1/1     1            1           83m
deployment.apps/deepfence-topology               1/1     1            1           83m
deployment.apps/deepfence-ui                     1/1     1            1           83m
deployment.apps/deepfence-vulnerability-mapper   1/1     1            1           83m

NAME                                                        DESIRED   CURRENT   READY   AGE
replicaset.apps/deepfence-api-6b65fdcb88                    1         1         1       83m
replicaset.apps/deepfence-backend-bd6ff4776                 1         1         1       83m
replicaset.apps/deepfence-celery-c76bb5b9f                  1         1         1       83m
replicaset.apps/deepfence-cluster-agent-848c7b987c          1         1         1       60m
replicaset.apps/deepfence-diagnosis-7cc844d596              1         1         1       83m
replicaset.apps/deepfence-fetcher-796c5ff9dd                1         1         1       83m
replicaset.apps/deepfence-internal-router-575d7f7fff        1         1         1       83m
replicaset.apps/deepfence-package-scanner-59dc64b66f        1         1         1       83m
replicaset.apps/deepfence-postgres-pgpool-bbb5fb8bb         1         1         1       83m
replicaset.apps/deepfence-redis-77958475f7                  1         1         1       83m
replicaset.apps/deepfence-router-bd995cff5                  1         1         1       83m
replicaset.apps/deepfence-secret-scaner-85968ff69f          1         1         1       83m
replicaset.apps/deepfence-topology-78484ccdd6               1         1         1       83m
replicaset.apps/deepfence-ui-69887bbc47                     1         1         1       83m
replicaset.apps/deepfence-vulnerability-mapper-7bbd76499c   1         1         1       83m

NAME                                             READY   AGE
statefulset.apps/deepfence-es                    3/3     83m
statefulset.apps/deepfence-postgres-postgresql   3/3     83m
```

ui에 접근하여 파드 현황 및 컨테이너 보안 스캔을 진행하여 테스트 합니다.
