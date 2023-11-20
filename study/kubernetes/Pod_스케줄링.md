# Kubernetes Pod 스케줄링
해당 문서는 Kubernetes의 Pod가 어떻게 스케줄링되고, 어떻게 조절할 수 있는지에 대해 기술합니다.

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