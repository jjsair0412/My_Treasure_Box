# EKS Autoscale with Karpenter
## Overview
EKS Cluster를 Auto Scaling 하기 위한 방법은 하기 3가지로 나뉨.

1. [HPA (Horizontal Pod Autoscaler)](#1-hpa-horizontal-pod-autoscaler)
    - Pod Scale In / Out
    - Pod 수평 확장
2. [VPA (Vertical Pod Autoscaler)](#2-vpa-vertical-pod-autoscaler)
    - Pod Scale Up / Down
    - Pod 수직 확장
3. [CA (Cluster Autoscaler)](#3-ca-cluster-autoscaler)
    - 노드 오토스케일링
4. Karpenter
    - 노드 오토스케일링

위 3가지 방법들을 각기 테스트 해보고, 각자의 장단점을 구분하기 위한 문서.

아래모든 실습은 아래와 같은 ClusterConfig EKS yaml파일을 통해 진행하였음.
```yaml
apiVersion: eksctl.io/v1alpha5
kind: ClusterConfig
metadata:
  name: eks-cluster # 생성할 EKS 클러스터명
  region: ap-northeast-2 # 클러스터를 생성할 리젼

iam:
  withOIDC: true # AWS IAM은 EKS에게 외부 인증서버이기 때문에, OIDC를 true로 설정하여 AWS Resource를 관리할 수 있게끔 구성함.

vpc:
  cidr: "172.31.0.0/16" # 클러스터에서 사용할 VPC의 CIDR
nodeGroups:
  - name: eks-cluster-ng # 클러스터의 노드 그룹명
    instanceType: t2.medium # 클러스터 워커 노드의 인스턴스 타입
    desiredCapacity: 1 # 클러스터 워커 노드의 갯수
    volumeSize: 20  # 클러스터 워커 노드의 EBS 용량 (단위: GiB)
    iam:
      withAddonPolicies:
        ImageBuilder: true # AWS ECR에 대한 권한 추가
        albIngress: true  # alb ingress에 대한 권한 추가
    ssh:
      allow: true # 워커 노드에 SSH 접속 허용
      publicKeyName: myKeyPair # 워커 노드에 SSH 접속을 위해 사용할 pem키 명(aws key pairs에 등록되어 있어야함. .pem 확장자 제외한 이름)
  - name: spot-ng
    minSize: 1
    maxSize: 5
    tags:
      k8s.io/cluster-autoscaler/eks-cluster: "true" # 해당 노드그룹만 CA에서 스케일링 대상.
      k8s.io/cluster-autoscaler/enabled: "true"
    instancesDistribution:
      maxPrice: 0.2
      instanceTypes: ["t2.small", "t3.small"]
      onDemandBaseCapacity: 0
      onDemandPercentageAboveBaseCapacity: 50
    ssh: 
      allow: true # 워커 노드에 SSH 접속 허용
      publicKeyName: myKeyPair
```
## 1. HPA (Horizontal Pod Autoscaler)
- [HPA_문서](https://kubernetes.io/ko/docs/tasks/run-application/horizontal-pod-autoscale/#horizontalpodautoscaler%EB%8A%94-%EC%96%B4%EB%96%BB%EA%B2%8C-%EC%9E%91%EB%8F%99%ED%95%98%EB%8A%94%EA%B0%80)

HPA는 파드 단위의 오토스케일링 기능입니다.

HPA는 Deployment 및 Deployment의 replicaset의 개수를 조절하여, 다음과 같은 알고리즘을 적용하여 적절한 Pod 개수를 유지합니다.

1. HPA 컨트롤러는 대상 파드의 API를 통해 리소스 메트릭을 가져옵니다.
2. 이후 목표 사용률 값이 설정되면, 컨트롤러는 각 파드 컨트롤러에 설정된 request & limit 깂을 기준으로 사용률을 계산합니다.
3. 이후 하기 알고리즘을 사용해서, HPA 컨트롤러는 원하는(desired) 메트릭 값과, 현재(current) 메트릭 값 사이의 비율로 작동합니다.

```
원하는 레플리카 수 = ceil[현재 레플리카 수 * ( 현재 메트릭 값 / 원하는 메트릭 값 )]
```

예를들어 , 현재 메트릭 값이 200m 이고 , 원하는 값이 100m 이며 replica 갯수가 1 이라면, 아래 공식이 성립됩니다.

```bash
# ceil 함수는 만약 결과가 소수라면 반올림해서 정수로 변환함.
원하는_레플리카_수 = ceil[1 * ( 200 / 100 )]
``` 

따라서 원하는 레플리카의 수는 ```2``` 가 됩니다.

***HPA 컨트롤러는 15초마다 각 Kubernetes 리소스 사용률을 질의합니다. 따라서 새로고침 주기가 15초***

### 1.1 HPA 실습
#### 0. Metrics Server 설치
HPA Controller는 대상 리소스에 대해 메트릭값을 일정 주기로 풀링하여 CPU나 메모리 사용률을 감시합니다.

그에 따라 Metrics Server를 Kubernetes Cluster에 배포해야 합니다.

Amazon EKS Version 1.29 에서 아래 명령어로 Metrics Server를 배포하였습니다.

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

Metric Server deployment 가 잘 배포되었는지 확인합니다.

```bash
$ kubectl get deployment metrics-server -n kube-system
NAME             READY   UP-TO-DATE   AVAILABLE   AGE
metrics-server   1/1     1            1           33s
```

top 명령어로 실제로 메트릭이 수집되는지 확인합니다.

```bash
$ kubectl top nodes                                   
NAME                                               CPU(cores)   CPU%   MEMORY(bytes)   MEMORY%   
ip-172-31-23-101.ap-northeast-2.compute.internal   60m          3%     579Mi           8%   
```

#### 1. HPA 구현
실제로 Pod가 의도한대로 동작하는지 확인합니다.

HPA를 구현하기 위해선, Deployment를 생성하고, 대상 Deployment의 메트릭값을 풀링하는 HPA 리소스가 필요합니다.
- 타 리소스에 대해서도 HPA로 관리가 가능합니다. replicas 개수를 관리하는 리소스라면 가능합니다.
    - replicas 개수를 관리한다는것은 Kube-api-server의 ```scale``` API를 지원하는것입니다.
    - HPA 는 이러한 ```scale``` API를 컨트롤하여 파드를 Scale In / Out 합니다.

먼저 [test_apache](./HPA/test_apache.yml) 를 배포합니다.

hpa-example 이미지를 사용하여 컨테이너를 실행하는 디플로이먼트를 시작하고, 다음의 매니페스트를 사용하여 디플로이먼트를 서비스로 노출합니다.

```bash
$ kubectl apply -f test_apache.yml 

# 결과확인
$ kubectl get all
NAME                              READY   STATUS    RESTARTS   AGE
pod/php-apache-598b474864-kw69k   1/1     Running   0          18s

NAME                 TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)   AGE
service/kubernetes   ClusterIP   10.100.0.1       <none>        443/TCP   38m
service/php-apache   ClusterIP   10.100.252.253   <none>        80/TCP    18s

NAME                         READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/php-apache   1/1     1            1           19s

NAME                                    DESIRED   CURRENT   READY   AGE
replicaset.apps/php-apache-598b474864   1         1         1       19s
```

이후 해당 deployment 에 대해 HPA 리소스를 생성해야 합니다.

HPA 리소스는 kubectl 명령어로 생성할 수 있는데, 다음과 같습니다.
- [HPA_생성_공식문서](https://kubernetes.io/ko/docs/tasks/run-application/horizontal-pod-autoscale-walkthrough/#create-horizontal-pod-autoscaler)

아래 설정은, HPA 컨트롤러가 php-apache deployment 리소스의 파드 cpu 사용률을 50%로 제한합니다.

50%가 초과하면 scale out 되는데, 최소 파드 1개부터 10개까지 늘어나게 됩니다.

```bash
# php-apache deployment에 대해 HPA 리소스 생성
kubectl autoscale deployment php-apache --cpu-percent=50 --min=1 --max=10
horizontalpodautoscaler.autoscaling/php-apache autoscaled
```

dry-run 명령어로 yaml template을 확인해보면, 다음과 같습니다.
```yaml
$ kubectl autoscale deployment php-apache --cpu-percent=50 --min=1 --max=10 -o yaml --dry-run
apiVersion: autoscaling/v1
kind: HorizontalPodAutoscaler
metadata:
  creationTimestamp: null
  name: php-apache
spec:
  maxReplicas: 10
  minReplicas: 1
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: php-apache
  targetCPUUtilizationPercentage: 50
status:
  currentReplicas: 0
  desiredReplicas: 0
```

hpa 리소스에 대해 get 요청으로 확인합니다.
```bash
$ kubectl get hpa
NAME         REFERENCE               TARGETS         MINPODS   MAXPODS   REPLICAS   AGE
php-apache   Deployment/php-apache   <unknown>/50%   1         10        1          48s
```

describe로 해당 hpa의 event message를 확인해보면, 정상 작동하여 최소값 replicas 개수인 1개를 유지하는것으로 확인됩니다.

또한 cpu 사용률이 50%가 넘지 않았기 때문에, 최소개수의 replica count를 가진다는 메시지도 확인할 수 있습니다.

```bash
kubectl describe hpa
Name:                                                  php-apache
Namespace:                                             default
Labels:                                                <none>
Annotations:                                           <none>
CreationTimestamp:                                     Thu, 30 May 2024 00:12:28 +0900
Reference:                                             Deployment/php-apache
Metrics:                                               ( current / target )
  resource cpu on pods  (as a percentage of request):  0% (1m) / 50%
Min replicas:                                          1
Max replicas:                                          10
Deployment pods:                                       1 current / 1 desired
Conditions:
  Type            Status  Reason            Message
  ----            ------  ------            -------
  AbleToScale     True    ReadyForNewScale  recommended size matches current size
  ScalingActive   True    ValidMetricFound  the HPA was able to successfully calculate a replica count from cpu resource utilization (percentage of request)
  ScalingLimited  True    TooFewReplicas    the desired replica count is less than the minimum replica count
Events:           <none>
```

이제 부하를 발생시켜 보겠습니다.

새로운 터미널을 열어서, 아래 명령어로 php-apache service에 쿼리를 무한정 보내는 루프를 실행합니다.

```bash
kubectl run -i --tty load-generator --rm --image=busybox:1.28 --restart=Never -- /bin/sh -c "while sleep 0.01; do wget -q -O- http://php-apache; done"
```

hpa를 확인해보면, deployment에 실제로 CPU 부하가 증가되었으며, 그에 따라 replicas 개수가 수정되고, HPA의 CPU 사용률이 낮아지는것을 확인할 수 있습니다.

- 부하 증가 (242%)
```bash
$ kubectl get hpa
NAME         REFERENCE               TARGETS    MINPODS   MAXPODS   REPLICAS   AGE
php-apache   Deployment/php-apache   242%/50%   1         10        3          3m33s
```

- replicas 개수 변화
```bash
$ kubectl get pods
NAME                          READY   STATUS    RESTARTS   AGE
load-generator                1/1     Running   0          2m9s
php-apache-598b474864-2c98x   1/1     Running   0          80s
php-apache-598b474864-8bff8   1/1     Running   0          50s
php-apache-598b474864-kw69k   1/1     Running   0          13m
php-apache-598b474864-lmjhc   1/1     Running   0          95s
php-apache-598b474864-qzz87   1/1     Running   0          95s
php-apache-598b474864-v7hz6   1/1     Running   0          80s
php-apache-598b474864-xvm9w   1/1     Running   0          35s
```

- CPU 부하 정상화
```bash
$ kubectl get hpa 
NAME         REFERENCE               TARGETS   MINPODS   MAXPODS   REPLICAS   AGE
php-apache   Deployment/php-apache   35%/50%   1         10        7          5m20s
```

해당 무한루프 쿼리에 CPU 사용률을 50% 미만으로 유지하기 위해선, replicas 개수가 7개정도면 충분하다는것을 알 수 있습니다.

지속적으로 hpa를 확인해 보면, replicas 7개 상태에서 CPU 부하는 대략 38% ~ 46% 정도라는것을 알 수 있습니다.

```bash
$ kubectl get hpa
NAME         REFERENCE               TARGETS   MINPODS   MAXPODS   REPLICAS   AGE
php-apache   Deployment/php-apache   38%/50%   1         10        7          7m34s
...
$ kubectl get hpa
NAME         REFERENCE               TARGETS   MINPODS   MAXPODS   REPLICAS   AGE
php-apache   Deployment/php-apache   46%/50%   1         10        7          7m16s
...
```

이제 무한루프 쿼리를 멈추면, CPU 사용량이 다시 0%로 복구하면서 Pod의 Replicas 개수가 1로 변화하는것을 볼 수 있습니다.

- CPU 사용량 낮아짐
```bash
$ kubectl get hpa
NAME         REFERENCE               TARGETS   MINPODS   MAXPODS   REPLICAS   AGE
php-apache   Deployment/php-apache   0%/50%    1         10        7          9m17s
```

- 그에 따라 Pod 개수가 1개씩 terminat 되며 최소값으로 변화합니다. 이러한 과정은 시간이 소요됩니다.
```bash
$ kubectl get pods
NAME                          READY   STATUS        RESTARTS   AGE
php-apache-598b474864-2c98x   1/1     Running       0          8m31s
php-apache-598b474864-8bff8   1/1     Running       0          8m1s
php-apache-598b474864-kw69k   1/1     Running       0          20m
php-apache-598b474864-lmjhc   0/1     Terminating   0          8m46s
php-apache-598b474864-qzz87   1/1     Running       0          8m46s
php-apache-598b474864-v7hz6   1/1     Running       0          8m31s
php-apache-598b474864-xvm9w   1/1     Running       0          7m46s

....
$ kubectl get pods
NAME                          READY   STATUS    RESTARTS   AGE
php-apache-598b474864-kw69k   1/1     Running   0          23m

# replicas 개수 1로 변화
$ kubectl get hpa 
NAME         REFERENCE               TARGETS   MINPODS   MAXPODS   REPLICAS   AGE
php-apache   Deployment/php-apache   0%/50%    1         10        1          15m
```

### 1.2 HPA 결론
HPA의 감시 대상 메트릭값은 CPU 뿐만아니라 다양한 메트릭을 대상으로 진행할 수 있습니다.

나아가 메트릭을 사용자 커스터마이징 하여 세팅할 수도 있습니다.
- [다양한 메트릭 및 사용자 정의 메트릭을 기초로한 오토스케일링 공식문서](https://kubernetes.io/ko/docs/tasks/run-application/horizontal-pod-autoscale-walkthrough/#%EB%8B%A4%EC%96%91%ED%95%9C-%EB%A9%94%ED%8A%B8%EB%A6%AD-%EB%B0%8F-%EC%82%AC%EC%9A%A9%EC%9E%90-%EC%A0%95%EC%9D%98-%EB%A9%94%ED%8A%B8%EB%A6%AD%EC%9D%84-%EA%B8%B0%EC%B4%88%EB%A1%9C%ED%95%9C-%EC%98%A4%ED%86%A0%EC%8A%A4%EC%BC%80%EC%9D%BC%EB%A7%81)

## 2. (VPA Vertical Pod Autoscaler)
VPA 또한 HPA와 동일하게 파드의 scaling 작업에 필요합니다.

***VPA는 Pod의 Pod Scale Up / Down , 즉 수직 확장을 도와줍니다.***

VPA는 각 Pod들에 대해 cpu와 memory의 request limits와 request를 , 클러스터 리소스 사용률을 계산하여 자동으로 할당합니다.

VPA 또한 노드의 CPU, Memory 사용량을 모니터링 하기 때문에, Metric Server가 필요한데, 해당문서에서는 실습의 편의를 위해 한꺼번에 설치합니다.

### 2.1 VPA의 작동방식
VPA의 작동 프로세스는 다음과 같습니다.

#### 2.1.1 Clster 모니터링
1. VPA는 Kubernetes Cluster에서 실행중인 파드의 CPU, Memory 사용량을 모니터링 합니다.
2. 모니터링된 데이터를 기반으로 파드에 필요한 리소스를 계산합니다.
3. 계산된 리소스를 기반으로 파드의 'request' , 'limit' 값을 조정합니다.

### 2.2 VPA 실습
#### 2.2.1 VPA 배포
VPA 사용하기 위해선 먼저 Vertical Pod Autoscaler 를 배포해야 합니다.

먼저 VPA Repository를 clone할 디렉토리로 이동하여, VPA Git을 Clone 합니다.
```bash
# ~/util 경로 사용 예정
$ cd ~/util

# vpa clone
$ git clone https://github.com/kubernetes/autoscaler.git
```

이후 clone 한 repository의 vertical-pod-autoscaler 경로로 이동합니다.
```bash
$ cd autoscaler/vertical-pod-autoscaler/

$ pwd
/Users/jujinseong/util/autoscaler/vertical-pod-autoscaler
```

- ***만약 다른 버전의 Vertical Pod Autoscaler를 이미 배포했다면 다음 명령을 사용하여 제거합니다.***
```bash
./hack/vpa-down.sh
```

다음 명령을 사용하여 Vertical Pod Autoscaler를 배포합니다.
```bash
./hack/vpa-up.sh
...
```

VPA 배포가 정상적으로 진행되었는지 확인합니다.

```bash
$ kubectl get pods -n kube-system
NAME                                        READY   STATUS    RESTARTS   AGE
...
metrics-server-6d94bc8694-xwmp7             1/1     Running   0          31m
vpa-admission-controller-548b84c8c8-fqdpt   1/1     Running   0          16s
vpa-recommender-8f4b9d68c-hgkhp             1/1     Running   0          17s
vpa-updater-5c88cd9cb6-rrhcl                1/1     Running   0          17s
```

#### 2.2.2 VPA 사용
VPA repository에 저장되어있는 Sample Application을 사용하여 VPA 가 정상 작동중인지 체크합니다.

```bash
# Test 대상 yaml
cat examples/hamster.yaml
```

해당 Application의 yaml 파일을 확인해보면, container의 cpu, memory requests가 각각 100m, 50Mi 로 세팅되어있는것을 확인할 수 있습니다.

또 다른 특이사항으로, 해당 Pod에 배포될 Application은 deployment resource yaml에서 100millicpu, 50Mibyte memory를 할당받았는데, 실제로 동작하기 위해선 그보다 더 많은 리소스가 필요합니다.
***VPA는 이러한 상황을 모니터링하고, request limit를 직접 수정하여 Pod가 정상동작하도록 자동 변화시켜 도와줍니다.***
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: hamster
spec:
  selector:
    matchLabels:
      app: hamster
  replicas: 2
  template:
    metadata:
      labels:
        app: hamster
    spec:
      securityContext:
        runAsNonRoot: true
        runAsUser: 65534 # nobody
      containers:
        - name: hamster
          image: registry.k8s.io/ubuntu-slim:0.1
          resources:
            requests: # requests 설정부분
              cpu: 100m
              memory: 50Mi
          command: ["/bin/sh"]
          args:
            - "-c"
            - "while true; do timeout 0.5s yes >/dev/null; sleep 0.5s; done"
```

또한 VPA 리소스도 보이는데, 해당 VPA yaml template에서는 hamster 이름을 가진 Deploymet를 대상으로 cpu, memory에 대해 최소 허용값과 최대 허용값을 설정합니다.
```yaml
apiVersion: "autoscaling.k8s.io/v1"
kind: VerticalPodAutoscaler
metadata:
  name: hamster-vpa
spec:
  # recommenders field can be unset when using the default recommender.
  # When using an alternative recommender, the alternative recommender's name
  # can be specified as the following in a list.
  # recommenders: 
  #   - name: 'alternative'
  targetRef:
    apiVersion: "apps/v1"
    kind: Deployment
    name: hamster
  resourcePolicy:
    containerPolicies:
      - containerName: '*' # 해당 deployment (hamster) 의 container 특정할 수 있음.
        minAllowed: # 최소허용값
          cpu: 100m
          memory: 50Mi
        maxAllowed: # 최대허용값
          cpu: 1
          memory: 500Mi
        controlledResources: ["cpu", "memory"]
```

다음 명령어로 VPA 대상 예시 ```hamster.yaml``` Template을 배포합니다.

```bash
$ kubectl get all
NAME                          READY   STATUS    RESTARTS   AGE
pod/hamster-c6967774f-df7vn   1/1     Running   0          13s
pod/hamster-c6967774f-q9fth   1/1     Running   0          13s

NAME                 TYPE        CLUSTER-IP   EXTERNAL-IP   PORT(S)   AGE
service/kubernetes   ClusterIP   10.100.0.1   <none>        443/TCP   78m

NAME                      READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/hamster   2/2     2            2           13s

NAME                                DESIRED   CURRENT   READY   AGE
replicaset.apps/hamster-c6967774f   2         2         2       13s
```

Pod를 describe해보면, Requests가 cpu, memory 각각 100m, 50Mi로 실제 Application이 필요한 양보다 적게 할당된것을 볼 수 있습니다.
```bash
$ kubectl describe pods hamster-c6967774f-hr7c2
...
    Image ID:      sha256:6f59484c23b20a132b654eaf7fb2856d29cefdbcef39b2fa8e0a971a20fa38f1
    Port:          <none>
    Host Port:     <none>
    Command:
      /bin/sh
    Args:
      -c
      while true; do timeout 0.5s yes >/dev/null; sleep 0.5s; done
    State:          Running
      Started:      Thu, 30 May 2024 00:53:43 +0900
    Ready:          True
    Restart Count:  0
    Requests:
      cpu:        100m
      memory:     50Mi
...
```

이때 VPA 상태를 확인해보면, 대상 Deployment에 필요한 리소스를 모니터링하여 CPU , MEM 옵션을 설정한것을 볼 수 있습니다.
```bash
$ kubectl get vpa
NAME          MODE   CPU    MEM       PROVIDED   AGE
hamster-vpa   Auto   587m   262144k   True       76s
```

**VPA는 계산이 완료된 이후 Pod의 limit request를 직접 변화시키는데, 이때 Pod가 제거되었다가 재 실행됩니다.**
```bash
$ kubectl get pods        
NAME                      READY   STATUS    RESTARTS   AGE
hamster-c6967774f-rl2tt   1/1     Running   0          2m11s
hamster-c6967774f-twgs9   1/1     Running   0          71s
```

describe 명령어로 실제 limit request가 변화하였는지 확인해 봅니다.
```bash
$ kubectl describe pods 
...
    Container ID:  containerd://1c4d7867e2dab04e5ae0553d155d771897094bcedd55f06c2bb8264e546e59db
    Image:         registry.k8s.io/ubuntu-slim:0.1
    Image ID:      sha256:6f59484c23b20a132b654eaf7fb2856d29cefdbcef39b2fa8e0a971a20fa38f1
    Port:          <none>
    Host Port:     <none>
    Command:
      /bin/sh
    Args:
      -c
      while true; do timeout 0.5s yes >/dev/null; sleep 0.5s; done
    State:          Running
      Started:      Thu, 30 May 2024 00:55:36 +0900
    Ready:          True
    Restart Count:  0
    Requests: 
      cpu:        587m # 변화됨
      memory:     262144k # 변화됨
...
```

## 3. CA (Cluster Autoscaler)
- [Cluster Autoscaler docs](https://github.com/kubernetes/autoscaler/blob/master/cluster-autoscaler/cloudprovider/aws/README.md)
- [Cluster Autoscaler FAQ](https://github.com/kubernetes/autoscaler/blob/master/cluster-autoscaler/FAQ.md#when-does-cluster-autoscaler-change-the-size-of-a-cluster)

Cluster Autoscaler는 EKS에서 지원하는 노드 오토스케일링 기능입니다.

Pending되어있는 파드가 Cluster에 존재할 경우, 노드를 수평확장하여 확장된 노드에 Pending Pod를 프로비저닝 합니다.
- VPA , HPA를 통해 파드를 수직, 수평확장 하였는데도 Pod가 프로비저닝되지 못한다면, 노드리소스가 부족한 경우가 대다수일 것이며, CA는 해당 상황에 도움이 됩니다.

***기본적으로 Auto Scaling Group을 통해서 Node Group을 오토스케일링 하게 됩니다.***

또한 Cluster Autoscaler는 특정 시간을 간격으로 스케일 인/스케일 아웃 을 수행합니다. 그리고 AWS에서는 ***AutoScalingGroup(ASG)*** 를 사용하여 Cluster AutoScaler를 적용합니다.

### 3.1 CA 실습
#### 3.1.1 Role 생성
EKS Cluster가 AWS 리소스(NodeGroup , EC2 등 ...) 에 접근하기 위한 Iam Role을 생성해야 합니다.

ClusterAutoscaler를 사용하기 위한 Policy를 하나 생성합니다.
- []()
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "autoscaling:DescribeAutoScalingGroups",
        "autoscaling:DescribeAutoScalingInstances",
        "autoscaling:DescribeLaunchConfigurations",
        "autoscaling:DescribeScalingActivities",
        "autoscaling:DescribeTags",
        "ec2:DescribeImages",
        "ec2:DescribeInstanceTypes",
        "ec2:DescribeLaunchTemplateVersions",
        "ec2:GetInstanceTypesFromInstanceRequirements",
        "eks:DescribeNodegroup"
      ],
      "Resource": ["*"]
    },
    {
      "Effect": "Allow",
      "Action": [
        "autoscaling:SetDesiredCapacity",
        "autoscaling:TerminateInstanceInAutoScalingGroup"
      ],
      "Resource": ["*"]
    }
  ]
}
```

aws command를 이용해서 policy를 하나 생성합니다.
- **생성한 뒤 발생한 ARN을 통해 Role을 생성해야 하니, 기억해둡니다.***
```bash
aws iam create-policy \
    --policy-name AmazonEKSClusterAutoscalerPolicy \
    --policy-document file://cluster-autoscaler-policy.json
```

생성한 Policy로 IAM Role을 생성합니다.

```bash
eksctl create iamserviceaccount \
  --cluster=eks-cluster \
  --namespace=kube-system \
  --name=cluster-autoscaler \
  --attach-policy-arn=<위에서 생성한 policy의 arn> \
  --override-existing-serviceaccounts \
  --approve
```

eksctl 명령어를 사용하지 않고, ServiceAccount와 Role을 수동으로 생성할수도 있습니다.

```bash                    
apiVersion: v1
kind: ServiceAccount
metadata:
  name: cluster-autoscaler
  namespace: kube-system
  labels:
    app.kubernetes.io/managed-by: eksctl 
  annotations:
    eks.amazonaws.com/role-arn: <Role_ARN>
```

service Account가 생성된것을 확인할 수 있습니다.
```bash
$ kubectl get sa -n kube-system | grep cluster-autoscaler
cluster-autoscaler                     0         88s

$ kubectl describe sa cluster-autoscaler -n kube-system               
Name:                cluster-autoscaler
Namespace:           kube-system
Labels:              app.kubernetes.io/managed-by=eksctl
Annotations:         eks.amazonaws.com/role-arn: <iam:ARN>
Image pull secrets:  <none>
Mountable secrets:   <none>
Tokens:              <none>
Events:              <none>
```

실제 AWS Console에서 확인해 보면, ***eksctl-eks-cluster-addon-iamserviceaccount-...*** 이름을 가진 Role이 하나 생성된것을 볼 수 있습니다.

#### ETC . EKS Cluster OIDC 관리
- [관련_내용_참고블로그](https://aws-diary.tistory.com/129)
EKS에서 AWS 리소스들을 관리하기 위해서는 , 위처럼 특정 Role을 생성한 뒤, 해당 Role의 이름을 annotation으로 등록해서 생성한 serviceAccount를 kubernetes 안에 생성하여 권한을 부여합니다.

예를들어 LoadBalacner를 생성하기 위해서 , LB 생성권한이 있는 Role을 생성하고, 해당 Role의 이름을 가진 Service Account를 생성하여 사용합니다.

신뢰관계는 AWS EKS Cluster의 OIDC를 대상으로 합니다.
- Role 신뢰관계 예
```bash
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Federated": "<EKS_OIDC_ARN>"
            },
            "Action": "sts:AssumeRoleWithWebIdentity",
            "Condition": {
                "StringEquals": {
                    "oidc.eks.ap-northeast-2.amazonaws.com/id/<EKS_OIDC_ARN>:aud": "sts.amazonaws.com"
                }
            }
        }
    ]
}
```

여기에서 "StringEquals" 조건은 OIDC 토큰의 aud (대상) 클레임을 검사합니다. sts.amazonaws.com으로 설정된 경우에만 IAM 역할에 대한 웹 신원 연결이 수락됩니다. 이는 클러스터 내의 파드 또는 서비스 계정이 AWS 자격 증명을 통해 AWS API에 액세스할 때 요청된 대상이 AWS STS 서비스인지 확인합니다.

#### 3.1.2 Cluster Autoscaler 배포
먼저 cluster autoscaler deployment 파일을 다운로드 합니다.

```bash
curl -o cluster-autoscaler-autodiscover.yaml https://raw.githubusercontent.com/kubernetes/autoscaler/master/cluster-autoscaler/cloudprovider/aws/examples/cluster-autoscaler-autodiscover.yaml
```

이후 <YOUR CLUSTER NAME> 부분을 찾아서 나의 EkS Cluster 이름으로 수정해야 합니다.
```bash
# Mac 일 경우
sed -i '' 's/<YOUR CLUSTER NAME>/eks-cluster/g' cluster-autoscaler-autodiscover.yaml

# Linux 일 경우
sed -i '' 's/<YOUR CLUSTER NAME>/eks-cluster/g' cluster-autoscaler-autodiscover.yaml
```

CA가 노드를 스케일업, 스케일다운 하기 위해서 EKS Cluster의 NodeGroup을 식별해야 합니다.

따라서 cluster-autoscaler Deployment의 command를 확인해 보면, 아래 명령어가 있습니다.
```bash
  - --node-group-auto-discovery=asg:tag=k8s.io/cluster-autoscaler/enabled,k8s.io/cluster-autoscaler/eks-cluster
```

위처럼 설정하게 되면, NodeGroup에 ```tag=k8s.io/cluster-autoscaler/enabled``` 또는  ```tag=k8s.io/cluster-autoscaler/eks-cluster``` 태그가 정의되어 있다면, ClusterAutoscaler는 해당 노드가 eks-cluster에 위치한다고 인식합니다.

아래처럼 수동으로 넣어줄수도 있습니다.
- ***1:10, 1:3 은 스케일링 될 최소, 최대 노드 개수를 의미합니다.***
```bash
  - --nodes=1:10:eks-cluster-ng
  - --nodes=1:3:spot-ng
```

또한 먼저 cluster-autosclaer 이름의 serviceaccount를 생성했기 때문에,해당 섹션을 yaml파일에서 지워줍니다.
```bash
---
apiVersion: v1
kind: ServiceAccount
metadata:
  labels:
    k8s-addon: cluster-autoscaler.addons.k8s.io
    k8s-app: cluster-autoscaler
  name: cluster-autoscaler
  namespace: kube-system
---
...
```


변경된 파일을 배포합니다.
```bash
$ kubectl apply -f cluster-autoscaler-autodiscover.yaml
```

이후 cluster-autoscaler Deployment에 ```cluster-autoscaler.kubernetes.io.safe-to-evict``` Annotation을 하나 추가해 주어야 합니다.
- 해당 Annotation은 Cluster Autocaler가 파드를 안전하게 종료할 수 있는지에 대한 Annotation 입니다. 
  true로 설정된 파드는 CA에 의해 다른 노드로 옮겨가거나, 종료될 수 있지만, false로 설정된 파드는 주요 작업을 수행중이거나 옮겨지면 안되는 경우로 간주되어 종료되지 않습니다.

patch 명령어로 추가해 줍니다.
```bash
$ kubectl patch deployment cluster-autoscaler \
   -n kube-system \
   -p '{"spec":{"template":{"metadata":{"annotations":{"cluster-autoscaler.kubernetes.io/safe-to-evict": "false"}}}}}'
```

그리고 CA의 최신 릴리즈 버전명을 GitHub에서 확인한 뒤[CA_Git](https://github.com/kubernetes/autoscaler/releases) deployment의 Image 버전을 수정합니다.
- 2024/06/09 일자 latest version : 1.30.1
```bash
$ kubectl set image deployment cluster-autoscaler -n kube-system cluster-autoscaler=k8s.gcr.io/autoscaling/cluster-autoscaler:v1.30.1
```

#### 3.1.3 정상 설치 확인
cluster autocaler 로그를 확인하여 정상 작동중인지 확인합니다.
```bash
$ kubectl -n kube-system logs -f deployment.apps/cluster-autoscaler
```

### 3.2 AutoScaling Test
아래 deployment를 사용하여 테스트 해봅니다.
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-test
spec:
  selector:
    matchLabels:
      run: nginx-test
  replicas: 10
  template:
    metadata:
      labels:
        run: nginx-test
    spec:
      containers:
      - name: nginx-test
        image: nginx:latest
        ports:
        - containerPort: 80
        resources:
          limits:
            cpu: 500m
            memory: 512Mi
          requests:
            cpu: 500m
            memory: 512Mi
```

resource.request 에서 요구한 메모리나 cpu 보다 node 스팩이 부족하기 때문에 스케일링되는것을 확인할 수 있습니다.

```bash
$ kubectl apply -f test_deployment.yaml
```

초기에는 Pending 파드를 확인할 수 있습니다.

```bash                                                     
NAME                              READY   STATUS    RESTARTS   AGE
pod/nginx-test-596c69dbc5-2j92v   0/1     Pending   0          13s
pod/nginx-test-596c69dbc5-47jx9   1/1     Running   0          13s
pod/nginx-test-596c69dbc5-7ddcm   1/1     Running   0          13s
pod/nginx-test-596c69dbc5-8lq87   1/1     Running   0          13s
pod/nginx-test-596c69dbc5-c7nq5   0/1     Pending   0          13s
pod/nginx-test-596c69dbc5-dcvg6   0/1     Pending   0          13s
pod/nginx-test-596c69dbc5-j6p8w   1/1     Running   0          13s
pod/nginx-test-596c69dbc5-lk9mq   0/1     Pending   0          13s
pod/nginx-test-596c69dbc5-nl8h2   0/1     Pending   0          13s
pod/nginx-test-596c69dbc5-pzrtt   0/1     Pending   0          13s
```

CA가 Pending 파드를 확인하고, 노드를 추가합니다.
```bash 
$ kubectl get nodes
NAME                                               STATUS     ROLES    AGE    VERSION
ip-172-31-20-132.ap-northeast-2.compute.internal   Ready      <none>   100m   v1.29.3-eks-ae9a62a
ip-172-31-29-145.ap-northeast-2.compute.internal   Ready      <none>   88m    v1.29.3-eks-ae9a62a
ip-172-31-47-82.ap-northeast-2.compute.internal    NotReady   <none>   1s     v1.29.3-eks-ae9a62a

# 시간경과
...
NAME                                               STATUS     ROLES    AGE    VERSION
ip-172-31-20-132.ap-northeast-2.compute.internal   Ready      <none>   101m   v1.29.3-eks-ae9a62a
ip-172-31-20-99.ap-northeast-2.compute.internal    NotReady   <none>   65s    v1.29.3-eks-ae9a62a
ip-172-31-29-145.ap-northeast-2.compute.internal   Ready      <none>   89m    v1.29.3-eks-ae9a62a
ip-172-31-42-143.ap-northeast-2.compute.internal   NotReady   <none>   65s    v1.29.3-eks-ae9a62a
ip-172-31-47-82.ap-northeast-2.compute.internal    NotReady   <none>   77s    v1.29.3-eks-ae9a62a
ip-172-31-49-250.ap-northeast-2.compute.internal   NotReady   <none>   70s    v1.29.3-eks-ae9a62a
```

eks를 생성할 때, spot-ng 노드그룹만 CA 대상이었기 때문에, Spot instance가 생성되어 스케일링된것을 확인할 수 있습니다.

### 3.3 장단점
#### 3.3.1 장점
CA는 기본적으로 자동 스케일링을 지원합니다.

파드 resource, 리소스가 부족할때, (Pending) 노드가 확장하며 , 리소스가 충분히 사용되지 않는 경우, 불필요한 파드를 자동으로 제거합니다.

또한 다양한 스케일링 설정을 지원하기 때문에, 클러스터 동작을 맞춤형으로 바꿀 수 있습니다. 예를들어 특정 조건에서만 확장/축소 하도록 설정할 수 있습니다.

#### 3.3.2 단점
CA는 하나의 자원에 대해 여러군대에서 관리하다 보니, 관리정보가 서로 동기화되지 않아 다양한 문제가 발생합니다.

예를들어 ***EKS에서 노드를 제거하더라도, 실제로 노드를 프로비저닝시킨곳은 ASG이기 때문에 인스턴스는 제거되지 않습니다.***

또한 스케일링 속도가 매우 느립니다.

그리고 가장큰 함정이 있는데, CA는 언뜻 보기에 클러스터 전체의 CPU, Memory 부하가 높아졋을 때 확장하는것처럼 보이지만, 아닙니다.

***CA는 Pending 상태의 파드가 생기는 타이밍에 CA가 동작합니다.***
- 이는 Request , Limits를 적절히 설정하지 않은 상태에서는, 실제 노드부하가 낮더라도 잘못된 Resource 할당으로 인해 Pending Pod가 발생할 수 있습니다. 이말은 ***부하 평균이 낮은데도 스케일링시킬수 있고, 부하 평균이 높은데도 스케일 아웃이 되지 않을수 있다는 의미가 됩니다.(부하는 높은데 Pending 파드가 없을때)***

## 4. Karpenter
- [참고_블로그](https://techblog.gccompany.co.kr/karpenter-7170ae9fb677)
- [karpenter_공식문서](https://karpenter.sh/docs/getting-started/migrating-from-cas/)

CA의 단점들을 극복하기 위해 Karpenter를 사용할 수 있습니다.

Karpenter는 으폰스소로 , 노드의 수명주기를 관리할 수 있는 솔루션입니다.

### 4.1 Karpenter 사용
먼저 Karpenter 또한 AWS 리소스를 사용해야하기 때문에 , CA 사용과 동일하게 OIDC를 통해 인증처리를 해주어야 합니다.

#### 4.1.1 IAM 권한부여 수행
Karpenter는 Karpenter Node와 Karpenter Controller가 각기 다른 Policy와 신뢰관계를 가지고 작동합니다.

먼저 **Karpenter Node**에 부여할 신뢰 관계를 다음과 같이 생성합니다.
```bash
echo '{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Service": "ec2.amazonaws.com"
            },
            "Action": "sts:AssumeRole"
        }
    ]
}' > node-trust-policy.json

# Role 생성
aws iam create-role --role-name "KarpenterNodeRole" \
    --assume-role-policy-document file://KarpenterNodePolicy.json
```

이후 아래 4개의 AWS 관리형 Policy를 생성한 Karpenter Node Role에 부여합니다.
- arn은 환경에 맞게 조절합니다.
```bash
aws iam attach-role-policy --role-name "KarpenterNodeRole" \
    --policy-arn "arn:${AWS_PARTITION}:iam::aws:policy/AmazonEKSWorkerNodePolicy"

aws iam attach-role-policy --role-name "KarpenterNodeRole" \
    --policy-arn "arn:${AWS_PARTITION}:iam::aws:policy/AmazonEKS_CNI_Policy"

aws iam attach-role-policy --role-name "KarpenterNodeRole" \
    --policy-arn "arn:${AWS_PARTITION}:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"

aws iam attach-role-policy --role-name "KarpenterNodeRole" \
    --policy-arn "arn:${AWS_PARTITION}:iam::aws:policy/AmazonSSMManagedInstanceCore"
```

그리고 Karpenter Controller의 Role을 생성해주는데, 아래와 같은 신뢰관계를 가지고 생성합니다.
- OIDC Endpoint는 환경에 맞게끔 구성합니다.
- sub의 sa는 차후 karpenter용 service account 정보를 넣어주게됩니다.
  - 아래 예시의 ServiceAccount 정보 
    - namespace : ${KARPENTER_NAMESPACE}
    - ServiceAccount : karpenter
```json
cat << EOF > controller-trust-policy.json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Federated": "arn:${AWS_PARTITION}:iam::${AWS_ACCOUNT_ID}:oidc-provider/${OIDC_ENDPOINT#*//}"
            },
            "Action": "sts:AssumeRoleWithWebIdentity",
            "Condition": {
                "StringEquals": {
                    "${OIDC_ENDPOINT#*//}:aud": "sts.amazonaws.com",
                    "${OIDC_ENDPOINT#*//}:sub": "system:serviceaccount:${KARPENTER_NAMESPACE}:karpenter"
                }
            }
        }
    ]
}
EOF

// Role 생성
aws iam create-role --role-name "KarpenterControllerRole-${CLUSTER_NAME}" \
    --assume-role-policy-document file://controller-trust-policy.json
```

KarpenterController Role이 사용할 Policy 생성해 줍니다.
- Policy에 작성할 Resources들은 환경에 맞게끔 ARN을 부여해서 생성합니다.
```json
# role에 부여할 policy 생성
cat << EOF > controller-policy.json
{
    "Statement": [
        {
            "Action": [
                "ssm:GetParameter",
                "ec2:DescribeImages",
                "ec2:RunInstances",
                "ec2:DescribeSubnets",
                "ec2:DescribeSecurityGroups",
                "ec2:DescribeLaunchTemplates",
                "ec2:DescribeInstances",
                "ec2:DescribeInstanceTypes",
                "ec2:DescribeInstanceTypeOfferings",
                "ec2:DescribeAvailabilityZones",
                "ec2:DeleteLaunchTemplate",
                "ec2:CreateTags",
                "ec2:CreateLaunchTemplate",
                "ec2:CreateFleet",
                "ec2:DescribeSpotPriceHistory",
                "pricing:GetProducts"
            ],
            "Effect": "Allow",
            "Resource": "*",
            "Sid": "Karpenter"
        },
        {
            "Action": "ec2:TerminateInstances",
            "Condition": {
                "StringLike": {
                    "ec2:ResourceTag/karpenter.sh/nodepool": "*"
                }
            },
            "Effect": "Allow",
            "Resource": "*",
            "Sid": "ConditionalEC2Termination"
        },
        {
            "Effect": "Allow",
            "Action": "iam:PassRole",
            "Resource": "arn:${AWS_PARTITION}:iam::${AWS_ACCOUNT_ID}:role/KarpenterNodeRole-${CLUSTER_NAME}",
            "Sid": "PassNodeIAMRole"
        },
        {
            "Effect": "Allow",
            "Action": "eks:DescribeCluster",
            "Resource": "arn:${AWS_PARTITION}:eks:${AWS_REGION}:${AWS_ACCOUNT_ID}:cluster/${CLUSTER_NAME}",
            "Sid": "EKSClusterEndpointLookup"
        },
        {
            "Sid": "AllowScopedInstanceProfileCreationActions",
            "Effect": "Allow",
            "Resource": "*",
            "Action": [
            "iam:CreateInstanceProfile"
            ],
            "Condition": {
            "StringEquals": {
                "aws:RequestTag/kubernetes.io/cluster/${CLUSTER_NAME}": "owned",
                "aws:RequestTag/topology.kubernetes.io/region": "${AWS_REGION}"
            },
            "StringLike": {
                "aws:RequestTag/karpenter.k8s.aws/ec2nodeclass": "*"
            }
            }
        },
        {
            "Sid": "AllowScopedInstanceProfileTagActions",
            "Effect": "Allow",
            "Resource": "*",
            "Action": [
            "iam:TagInstanceProfile"
            ],
            "Condition": {
            "StringEquals": {
                "aws:ResourceTag/kubernetes.io/cluster/${CLUSTER_NAME}": "owned",
                "aws:ResourceTag/topology.kubernetes.io/region": "${AWS_REGION}",
                "aws:RequestTag/kubernetes.io/cluster/${CLUSTER_NAME}": "owned",
                "aws:RequestTag/topology.kubernetes.io/region": "${AWS_REGION}"
            },
            "StringLike": {
                "aws:ResourceTag/karpenter.k8s.aws/ec2nodeclass": "*",
                "aws:RequestTag/karpenter.k8s.aws/ec2nodeclass": "*"
            }
            }
        },
        {
            "Sid": "AllowScopedInstanceProfileActions",
            "Effect": "Allow",
            "Resource": "*",
            "Action": [
            "iam:AddRoleToInstanceProfile",
            "iam:RemoveRoleFromInstanceProfile",
            "iam:DeleteInstanceProfile"
            ],
            "Condition": {
            "StringEquals": {
                "aws:ResourceTag/kubernetes.io/cluster/${CLUSTER_NAME}": "owned",
                "aws:ResourceTag/topology.kubernetes.io/region": "${AWS_REGION}"
            },
            "StringLike": {
                "aws:ResourceTag/karpenter.k8s.aws/ec2nodeclass": "*"
            }
            }
        },
        {
            "Sid": "AllowInstanceProfileReadActions",
            "Effect": "Allow",
            "Resource": "*",
            "Action": "iam:GetInstanceProfile"
        }
    ],
    "Version": "2012-10-17"
}
EOF

// Policy KarpenterController Role에 부여
aws iam put-role-policy --role-name "KarpenterControllerRole-${CLUSTER_NAME}" \
    --policy-name "KarpenterControllerPolicy-${CLUSTER_NAME}" \
    --policy-document file://controller-policy.json
```

#### 4.1.2 Karpenter Tag 정보 구성
다음으로 tag 정보를 구성해야 합니다.

Karpenter는 WorkerNode를 생성할 때 , AWSNodeTemplate에 구성되어있는 tag를 기반으로 동작합니다.

Karpenter가 사용할 , 혹은 사용하고 있는 Subnet과 Security Group에 Tag 작업을 진행합니다.

```bash
for NODEGROUP in $(aws eks list-nodegroups --cluster-name "${CLUSTER_NAME}" --query 'nodegroups' --output text); do
    aws ec2 create-tags \
        --tags "Key=karpenter.sh/discovery,Value=${CLUSTER_NAME}" \
        --resources $(aws eks describe-nodegroup --cluster-name "${CLUSTER_NAME}" \
        --nodegroup-name "${NODEGROUP}" --query 'nodegroup.subnets' --output text )
done```


Add tags to our security groups.
This command only tags the security groups for the first nodegroup in the cluster.
If you have multiple nodegroups or multiple security groups you will need to decide which one Karpenter should use.


```bash
# eks cluster 이름 환경변수 등록
export CLUSTER_NAME=$(eksctl get cluster --output json | jq -r '.[].Name')

# Karpenter 대상 Node Group 지정
# 해당 예제에서는 0번째 NodeGroup을 지정함
NODEGROUP=$(eksctl get nodegroup --cluster "${CLUSTER_NAME}" -o json | jq -r '.[0].Name')

# NodeGroup에 Launch Template이 등록된 경우만 수행
LAUNCH_TEMPLATE=$(aws eks describe-nodegroup --cluster-name "${CLUSTER_NAME}" \
    --nodegroup-name "${NODEGROUP}" --query 'nodegroup.launchTemplate.{id:id,version:version}' \
    --output text | tr -s "\t" ",")

# Karpenter worker node Cluster에 부여할 security Group
SECURITY_GROUPS=$(aws eks describe-cluster --name "${CLUSTER_NAME}" | jq -r '.cluster.resourcesVpcConfig.securityGroupIds.[0]')

# Managed Node Group의 Launch Template에 보안 그룹을 사용하는 설정이 되어 있는 경우, Launch Template에서 보안 그룹 ID를 가져오기
SECURITY_GROUPS="$(aws ec2 describe-launch-template-versions \
    --launch-template-id "${LAUNCH_TEMPLATE%,*}" --versions "${LAUNCH_TEMPLATE#*,}" \
    --query 'LaunchTemplateVersions[0].LaunchTemplateData.[NetworkInterfaces[0].Groups||SecurityGroupIds]' \
    --output text)"

# Tag 부여
aws ec2 create-tags \
    --tags "Key=karpenter.sh/discovery,Value=${CLUSTER_NAME}" \
    --resources "${SECURITY_GROUPS}"
```

