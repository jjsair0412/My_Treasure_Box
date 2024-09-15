# Kubernetes Pod 스케줄링
해당 문서는 Kubernetes의 Pod가 어떻게 스케줄링되고, 어떻게 조절할 수 있는지에 대해 기술합니다.

## 0. Repository Map
- [Taint와 Toleration](#1-node-taint--node-toleration)
  - 특정 노드에 파드를 허용하는 방법
- [nodeAffinity](#2-node-affinity)
  - 파드가 label을 기반으로 파드를 스케줄링할 노드를 선택하는 방법
- [podAffinity](#3-podaffinity)
  - 파드가 label을 기반으로 label을 가진 파드가 스케줄링된 동일한 노드에 스케줄링하는 방법
- [podAntiAffinity](#4-podantiaffinity--파드를-서로-떨어트리기)
  - 파드가 label을 기반으로 label을 가진 파드가 스케줄링된 노드가 아닌 다른 노드에 스케줄링하는 방법

## 1. Node Taint , Node Toleration
어떤 파드가 특정 노드를 사용할 수 있는지를 제한하고자 사용됩니다.

```kubectl describe node``` 명령어로 , 각 노드에 걸린 Taints를 확인할 수 있습니다.

예를들어 master Node에는 아래와같은 Taints가 작성되어 있습니다.

```bash
...
Taints:  node-role.kubernetes.io/control-plane:NoSchedule 
```

Taint는 아래 구조를 가집니다.
- 키, 값, 효과 
- ```<key>=<value>:<effect>```
    - 위의 예제의 구조는 아래와 같습니다.
    - key : node-role.kubernetes.io/master
    - value : null
    - effect : NoSchedule

**해당 테인트를 가진 노드에, 해당 테인트를 허용하는 톨러레이션이 없는 파드를 스케줄링 하지 못하게 막는 효과를 가지고있습니다.**
- 있다면 허용

마스터노드에 허용되는 파드들의 종류로는, System 파드들이 허용되며, 해당 파드의 Tolerations은 다음과 같습니다.
- 아래 예에서 볼 수 있듯, 첫번째 Tolerations에서 control-plane의 taint와 일치하기에, 허용되는 Pod라고 알 수 있습니다.
    - 추가로 다른 2가지 Tolerations들은 준비되지 않았거나(not-ready) 도달할 수 없는(unreachable) 노드에서 파드를 얼마나 오래 실행할 수 있는지 정의합니다.
```bash
...
Tolerations:                 node-role.kubernetes.io/control-plane:NoSchedule
                             node.kubernetes.io/not-ready:NoExecute op=Exists for 300s
                             node.kubernetes.io/unreachable:NoExecute op=Exists for 300s
```

***노드는 하나 이상의 Taint를 가질 수 있으며, 파드는 하나 이상의 Tolerations를 가질 수 있습니다.***

### 1.1 Taint Effect의 종류
각 테인트는 3가지 effects를 가질 수 있습니다.

#### 1. ```NoSchedule```
파드가 테인트를 허용하는 톨러레이션이 없다면, 노드에 스케줄링되지 않습니다.

#### 2. ```PreferNoSchedule```
```NoSchedule``` 보단 soft한 버전입니다. 스케줄러가 파드를 노드에 스케줄링하지 않으려 하지만, 다른곳에 스케줄링 할 수 없다면 다른 노드에 스케줄링 됩니다.

#### 3. ```NoExecute```
스케줄링에만 영향을 주는 ```NoSchedule```, ```PreferNoSchedule``` 과 다르게 이미 실행중인 파드에도 영향을 줍니다.

- 노드에 부여된 특정 태인트를 허용하는 톨러레이션이 없는 파드는 그 노드에서 즉시 제거된다는 의미입니다.

- 특정 태인트를 허용하는 톨러레이션이 있는데, 특정 용인 시간(tolerationSeconds) 을 설정하지 않은 파드는 해당 노드에 계속 남아있게 됩니다.

- 특정 용인 시간(tolerationSeconds)을 명시하여 태인트를 용인하는 파드는 지정된 시간 동안 노드에 연결됩니다. 
    
    그 시간이 지나면, 노드 라이프사이클 컨트롤러는 파드를 노드에서 추방합니다. 이는 특정 기간 동안만 태인트를 용인하도록 설정된 파드는 그 기간이 끝나면 노드에서 제거된다는 것을 의미합니다.


### 1.2 Taint , Tolerations 사용
kubectl 명령어로 노드에 Taint를 할당할 수 있습니다.

```bash
# example
$ kubectl taint node <Node_Name> <Taint>

# taint 할당 usecase
$ kubectl taint node docker-desktop node-type=production:NoSchedule
node/docker-desktop tainted

# taint 제거 usecase
# 제거는 할당 명령어와 동일하게 작성하고, 맨뒤에 - 만 추가
$ kubectl taint node docker-desktop node-type=production:NoSchedule-
node/docker-desktop untainted
```

아래 명령어로 pod를 실행해보면, 해당 노드에 스케줄링되지 못하고 pending상태로 있는것을 확인할 수 있습니다.

```bash
$ kubectl create deployment test --image busybox --replicas 5 -- sleep 999999
deployment.apps/test created

$ kubectl get pods
NAME                    READY   STATUS    RESTARTS   AGE
test-5bcbcd9767-5f64g   0/1     Pending   0          5s
test-5bcbcd9767-mkc97   0/1     Pending   0          5s
test-5bcbcd9767-wszzw   0/1     Pending   0          5s
test-5bcbcd9767-xf4jl   0/1     Pending   0          5s
test-5bcbcd9767-zvt62   0/1     Pending   0          5s
```

이것을 허용하려면 , 아까 추가한 ```node-type=production:NoSchedule``` Taint를 허용하는 Tolerations를 deployment에 추가해 주면 됩니다.
- 해당 Taint의 key, value, effect 구조에 맞춰서 작성해줍니다.
    - key : node-type
    - value : production
    - effect : NoSchedule

#### 주의
Taint는 value가 null일 수 있으며, value를 가집니다.

따라서 Pod에 Tolerations를 추가할 때, ```Operator``` 옵션을 주어야합니다.

```Operator``` 가 가질 수 있는 값은 다음과 같습니다.
- ```Equal``` : default, value가 같을 때
- ```Exists``` : value가 존재할 때 . 따라서 Exists로 특정 테인트 키에 여러값을 허용할 수 있음.

Tolerations를 추가한 deploymet 생성
```yaml
cat <<EOF> test.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: prod
spec:
  replicas: 5
  selector:
    matchLabels:
      app: busybox
  template:
    metadata:
      labels:
        app: busybox
    spec:
      containers:
      - args:
        - sleep
        - "99999"
        image: busybox
        name: main
      tolerations: # node-type=production:NoSchedule Taint 허용
      - key: node-type 
        operator: Equal
        value: production
        effect: NoSchedule
EOF
```

파드가 Running인것을 확인할 수 있습니다.
```bash
$ kubectl get pods -o wide
NAME                    READY   STATUS    RESTARTS   AGE   IP           NODE             NOMINATED NODE   READINESS GATES
prod-64fd476dfb-26xnt   1/1     Running   0          15s   10.1.0.109   docker-desktop   <none>           <none>
prod-64fd476dfb-6cqxl   1/1     Running   0          15s   10.1.0.110   docker-desktop   <none>           <none>
prod-64fd476dfb-86zw5   1/1     Running   0          15s   10.1.0.112   docker-desktop   <none>           <none>
prod-64fd476dfb-n6ggg   1/1     Running   0          15s   10.1.0.111   docker-desktop   <none>           <none>
prod-64fd476dfb-wfqjp   1/1     Running   0          15s   10.1.0.113   docker-desktop   <none>           <none>
```

### 1.3 tolerationSeconds
파드를 실행중인 노드가 준비되지 않거나(not-ready) 도달할 수 없는 경우(unreachable) ```NoExecute ```Effect와 함께 ```tolerationSeconds``` 를 정의하여 대기하는 시간을 지정할 수 있습니다.
- 초단위 설정
- Default : 300 (5분)
```yaml
...
    - effect: NoExecute
      key: node.kubernetes.io/not-ready
      operator: Exists
      tolerationSeconds: 300
    - effect: NoExecute
      key: node.kubernetes.io/unreachable
      operator: Exists
      tolerationSeconds: 300
```

위 예제는 ```tolerationSeconds``` 시간을 정의하지 않고 그냥 생성했을 경우의 pod template 입니다.

지연시간 5분이 길다면, 위 두개 ```tolerations``` 를 추가한 뒤 ```tolerationSeconds``` 를 따로 설정해 주면 됩니다.

## 2. nodeAffinity
각 파드는 고유한 nodeAffinity를 지정할 수 있습니다.

이를 통해서 꼭 지켜야할 필수 요구사항이나, 선호도를 지정할 수 있습니다.

Kubernetes에게 **어떤 노드** 가 **특정 파드** 를 원한다는것을 알려주면, Kubernetes는 **해당 노드중 하나에게 파드를 스케줄링하려고 시도하고, 스케줄링할 수 없다면 다른 노드를 선택합니다.**

nodeAffinity는 Node Selector와 동일한 방식인 Label을 기반으로 동작합니다.

### 2.1 사용 예
nodeAffinity는 아래와 같이 사용하게 됩니다.

해당 예제는, ```gpu=true``` Label이 존재하는 노드에만 파드를 스케줄링하게끔 작성되어 있습니다.
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: jinseong-gpu
spec:
  affinity:
    nodeAffinity:
      requiredDuringSchedulingIgnoredDuringExecution: # 필수 요구사항
        nodeSelectorTerms:
        - matchExpressions: # label이 gpu=true 인 node에 스케줄링
          - key: gpu
            operator: In
            values:
            - "true"
  containers:
  - image: nginx
    name: jinseong
```

만약 ```gpu=true``` Label이 존재하는 노드가 하나도없다면, 아래와 같은 에러가 발생합니다.

```bash
...
Node-Selectors:              <none>
Tolerations:                 node.kubernetes.io/not-ready:NoExecute op=Exists for 300s
                             node.kubernetes.io/unreachable:NoExecute op=Exists for 300s
Events:
  Type     Reason            Age   From               Message
  ----     ------            ----  ----               -------
  Warning  FailedScheduling  6s    default-scheduler  0/1 nodes are available: 1 node(s) didn't match Pod's node affinity/selector. preemption: 0/1 nodes are available: 1 Preemption is not helpful for scheduling..
```

아래 명령어로 노드에 label을 지정하면, pod가 정상작동합니다.

```bash
# label 지정 명령어
$ kubectl label nodes <Node_Name> <label_Key>=<label_Value>

# usecase
$ kubectl label nodes docker-desktop gpu=true

# label 삭제 명령어
$ kubectl label nodes <Node_Name> <label_Key>-

# 정상적으로 스케줄링 됨. (gpu=true label이 있는 노드에 스케줄링)
$ kubectl get pods
NAME           READY   STATUS    RESTARTS   AGE
jinseong-gpu   1/1     Running   0          5m33s
```

### 2.2 주의 사항
Affinity는 노드에 스케줄링하는것 까지만 관여합니다.

***만약 label이 지정되어 있어서 파드가 정상 스케줄링된 이후에, 스케줄링된 노드에 label이 제거되었더라도, 스케줄링된 Pod는 해당 노드에서 삭제되지 않고 계속 Runing상태로 남아있게 됩니다.***

또한 바로위 예시에선 , ```requiredDuringSchedulingIgnoredDuringExecution``` 를 통해 affinity를 구성하였는데, ***이는 필수 요구사항을 의미하고, 선호도별로 affinity를 나눌 수도 있습니다.***
- 바로아래 2.1.3에서 선호도 나옴

### 2.3 스케줄링 시점의 우선순위 지정
Affinity의 큰 장점으론, 파드가 스케줄링되는 시점에, 어떤 노드에 가장먼저 스케줄링해라 라는 우선순위를 지정할 수 있다는 점에 있습니다.

선호도를 사용하는 경우는, 만약 1번노드부터 순차적으로 10번노드까지 있을 때, ***1번노드에 1차적으로 스케줄링하는데, 1번노드에 자리가 없거나 스케줄링하지 못한다면 2번 노드에 스케줄링되어야 하는 파드라면, 선호도를 부여하여 해당 요구사항을 충족시킬 수 있습니다.***

해당 선호도는 Template에서 ```preferredduringschedulingignoredduringexecution``` 의 ```weight```값으로 조절할 수 있습니다.

- 예시는 다음과 같습니다.
  - ```availability-zone=mainzone``` affinity가, ```share-type=dedicated``` affinity보다 4배 더 선호도가 높게 작동하며 스케줄링 됩니다.
    - ```availability-zone``` weight : 80
    - ```share-type``` weight : 20
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: pref
spec:
  replicas: 5
  selector:
    matchLabels:
      app: busybox
  template:
    metadata:
      labels:
        app: busybox
    spec:
      containers:
      - args:
        - sleep
        - "99999"
        image: busybox
        name: jinseong
      affinity:
        nodeAffinity:
          preferredDuringSchedulingIgnoredDuringExecution: # 선호도 요구사항
          - weight: 80 # 선호도 가중치 부여
            preference:
              matchExpressions:
              - key: availability-zone
                operator: In
                values:
                - mainzone
          - weight: 20 # 선호도 가중치 부여
            preference:
              matchExpressions:
              - key: share-type
                operator: In
                values:
                - dedicated
```

만약 노드가 4개 있으며, 각 노드의 label 구성이 다음과 같다고 생각할 때,
- 노드1 label : 
  - availability-zone=mainzone 
  - share-type=dedicated
- 노드2 label : 
  - availability-zone=mainzone 
  - share-type=share
- 노드3 label : 
  - availability-zone=testzone
  - share-type=dedicated
- 노드4 label :
  - availability-zone=testzone
  - share-type=share

바로 위 예제 Deployment를 생성하면, 스케줄링 우선순위는 다음과 같습니다.
- 우선순위 1 : 노드1
- 우선순위 2 : 노드2
- 우선순위 3 : 노드3
- 우선수위 4 : 노드4

## 3. podAffinity
Pod들이 서로 가까이 배치될 필요가 있을때 사용하는 스케줄링 기법

예를들어 백엔드 파드와 프론트엔드 파드가 있다고 가정할 때, 각 파드들이 가까이 배치되는것은 대기시간이 줄어들고 Application 성능이 향상되는 이점을 가질 수 있습니다.

따라서 ```podAffinity```를 지정하여, 각 파드들이 가까이 배치될 수 있도록 조절할 수 있습니다.

### 3.1 사용 예시들
podAffinity 또한 Affinity기 때문에, ```requiredDuringSchedulingIgnoredDuringExecution```(필수 요구사항) 과 ```preferredduringschedulingignoredduringexecution```(선호도 요구사항) 둘중 하나를 선택해서 사용할 수 있습니다.

#### 3.1.1  ```requiredDuringSchedulingIgnoredDuringExecution``` 사용 예 (필수 요구사항)
아래 yaml template으로 Deployment를 생성하면, ```app=backend``` label이 붙은 pod가 스케줄링되는 노드에만 스케줄링됩니다.
- ```requiredDuringSchedulingIgnoredDuringExecution```(필수 요구사항) 사용

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend
spec:
  replicas: 5
  selector:
    matchLabels:
      app: frontend
  template:
    metadata:
      labels:
        app: frontend
    spec:
      containers:
      - name: main
        image: busybox
        args:
        - sleep
        - "99999"
      affinity:
        podAffinity:
          requiredDuringSchedulingIgnoredDuringExecution: # 필수 요구사항 , preferredduringschedulingignoredduringexecution 으로 선호도 요구사항또한 가능(weight 가중치)
          - topologyKey: kubernetes.io/hostname
            labelSelector:
              matchLabels: # matchExpressions 필드 또한 사용가능
                app: backend
```

아래 명령어로 ```app=backend``` label이 붙은 pod를 생성하면, 해당 Pod가 스케줄링된 노드에만 frontend Pod가 스케줄링되는것을 확인할 수 있습니다.

```bash
$ kubectl create deployment backend --replicas 5 --image busybox -- sleep 999999 -l app=backend
```

#### 3.1.2 ```preferredduringschedulingignoredduringexecution``` 사용 예
Affinity 를 선언할 때, ```preferredduringschedulingignoredduringexecution```(선호도 요구사항) 을 통해서 해당 label이 붙은 파드가 없다면 다른곳에 스케줄링되도 무방하다고 알릴 수 있습니다.

사용 예는 다음과 같습니다.
- nodeAffintiy때와 동일하게, affinity별로 weight(가중치) 를 부여하여 노드 스케줄링 선호도를 조절할 수 있습니다.
  - 1번노드가 가중치 제일 높다면 1번에 먼저, 만약 1번 노드에 스케줄링 할 수 없다면 그다음 가중치 affinity로

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend
spec:
  replicas: 5
  selector:
    matchLabels:
      app: frontend
  template:
    metadata:
      labels:
        app: frontend
    spec:
      containers:
      - name: main
        image: busybox
        args:
        - sleep
        - "99999"
      affinity:
        podAffinity:
          preferredDuringSchedulingIgnoredDuringExecution: # 선호도
          - weight: 80 # 가중치부여, 1순위
            podAffinityTerm:
              topologyKey: kubernetes.io/hostname
              labelSelector:
                matchLabels:
                  app: backend
          - weight: 20 # 가중치부여, 2순위. app=backend보다 4배 덜 주요하게 작동
            podAffinityTerm:
              topologyKey: kubernetes.io/hostname
              labelSelector:
                matchLabels:
                  app: backend-two
```


### 3.2 podAffinity 사용 시 주의점 (특이한점)
위처럼 Backend의 label을 갖고있는 파드어피니티 Frontend가 배포되어 있을 때, ***만약 podAffinity 규칙을 정의하지 않은 Pod(Backend Pod) 가 삭제되고 다시 배포되더라도, 스케줄러가 정의하지 않은 Pod(Backend Pod)를 또 podAffinity를 정의한 Frontend Pod와 동일한 노드에 스케줄링합니다.***

만약 실수로 에러가나서 다른노드로 스케줄링되면, podAffinity 규칙이 깨져버리기 때문에 이렇게 작동합니다.

## 4. podAntiAffinity , 파드를 서로 떨어트리기
서로다른 Pod를 같은 노드에 스케줄링하는것도 필요하지만, 성능 및 고가용성을 확보하기 위해 각 파드가 같은 노드에 스케줄링되는것을 막을 필요도 있을 수 있습니다.

### 4.1 사용 예시
- DB Cluster라면, EKS와 같은 Cloud Kubernetes Service를 사용할 경우에 **각 DB Cluster Pod를 다른 리전, 다른 AZ의 노드에 위치시켜서 고 가용성을 확보할 수 있습니다.**
- 모든 파드를 서로다른 노드에 분산 스케줄링하여, 한 노드에 장애가 발생하더라도 서비스가 중단되지 않도록 구성할 수 있습니다.

### 4.2 사용 방안
```podAffinity``` 와 동일한데, ```podAffinity``` 를 ```podAntiAffinity``` 속성으로 바꿔주기만하면 됩니다.

```podAntiAffinity``` 또한 Affinity기 때문에, ```requiredDuringSchedulingIgnoredDuringExecution```(필수 요구사항) 과 ```preferredduringschedulingignoredduringexecution```(선호도 요구사항) 둘중 하나를 선택해서 사용할 수 있습니다.
- 그런데 ```preferredduringschedulingignoredduringexecution```(선호도 요구사항) 을 podAntiAffinity에서 사용하는것은 조심스러워야 합니다. 고 가용성이나 재해복구를 위해 사용한다면, 선호도를 사용해서 같은 노드에 스케줄링되는 확률을 열어놓기보다는, ```requiredDuringSchedulingIgnoredDuringExecution```(필수 요구사항) 을 사용하여 확실한 목적(고 가용성, 재해복구) 에 맞춰서 사용하는것이 좋기 때문입니다.

사용 예제는 다음과 같습니다.

#### 4.2.1 ```requiredDuringSchedulingIgnoredDuringExecution```(필수 요구사항) 일 경우
- pod frontend는 pod backend와 같은 노드에 스케줄링되지 않습니다.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend
spec:
  replicas: 5
  selector:
    matchLabels:
      app: frontend
  template:
    metadata:
      labels:
        app: frontend
    spec:
      containers:
      - name: main
        image: busybox
        args:
        - sleep
        - "99999"
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution: # 필수 요구사항 , preferredduringschedulingignoredduringexecution 으로 선호도 요구사항또한 가능(weight 가중치)
          - topologyKey: kubernetes.io/hostname
            labelSelector:
              matchLabels: # matchExpressions 필드 또한 사용가능
                app: backend
```
#### 4.2.2 ```preferredduringschedulingignoredduringexecution```(선호도 요구사항) 일 경우
- pod frontend는 pod backend와 같은 노드에 스케줄링되지 않지만, 만약 backend가 스케줄링되지 않은 노드에 스케줄링될 수 없다면 그다음 선호도 affinity를 따릅니다.(backend-two 가 스케줄링되지 않은 노드에 스케줄링됨)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend
spec:
  replicas: 5
  selector:
    matchLabels:
      app: frontend
  template:
    metadata:
      labels:
        app: frontend
    spec:
      containers:
      - name: main
        image: busybox
        args:
        - sleep
        - "99999"
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution: # 선호도
          - weight: 80 # 가중치부여, 1순위
            podAffinityTerm:
              topologyKey: kubernetes.io/hostname
              labelSelector:
                matchLabels:
                  app: backend
          - weight: 20 # 가중치부여, 2순위. app=backend보다 4배 덜 주요하게 작동
            podAffinityTerm:
              topologyKey: kubernetes.io/hostname
              labelSelector:
                matchLabels:
                  app: backend-two
```
