# deployment
## revisionHistoryLimit
deployment가 rollout 될 때 , kubernetes는 새로운 옵션의 파드를 생성하고 , Old파드는 새로운 파드가 Running이 된다면 이전 파드가 제거됩니다.

**그러나 K8S는 기본적으로 , Old Pod가 제거되더라도 ```.spec.revisionHistoryLimit``` 옵션이 default로 10개로 잡히기 때문에 , 예전에 사용중이었던 Replicaset의 개수가 ```.spec.revisionHistoryLimit``` 개수만큼 유지됩니다!!**

그렇게 하는 이유는 , Rollout된 Deployment가 RollBack 될 때 , 저장하고있는 예전 Replicaset을 통해서 RollBack되기 때문입니다.
>따라서 default 옵션(10개) 라면 , -10 Revision까지만 롤백이 가능하다는 이야기가 됩니다.

- [revisionHistoryLimit 관련 공식문서설명](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#revision-history-limit)

## rolling update 파드 교체 개수 지정전력
deployment의 rolling update 중에 한번에 몇개의 파드를 교체할지 결정할 수 있습니다.

### 1. maxSurge
```.spec.strategy.rollingUpdate.maxSurge``` 는 , deployment가 의도하는 replica 개수보다 얼마나 많은 파드 인스턴스를 허용할 지를 지정합니다.

해당 값은 백분률로 개산되며 , 절대값은 올림해서 백분률이 됩니다.
- default : 25% 

만약 default 설정이라면 , 의도한 개수보다 최대 25% 더 많은 인스턴스가 잇을 수 있습니다.
- [공식문서](https://kubernetes.io/ko/docs/concepts/workloads/controllers/deployment/#%EC%B5%9C%EB%8C%80-%EC%84%9C%EC%A7%80-max-surge)


### 2. Max Unavailable
```.spec.progressDeadlineSeconds``` 는 , 업데이트 중에 의도하는 레플리카 수를 기준으로 사용할 수 없는 파드 인스턴스 수를 결정합니다.

해당 값은 백분률로 개산되며 , 절대값은 내림해서 백분률이 됩니다.
- default : 25% 

만약 의도하는 레플리카 수가 4이고, default 설정이라면 사용할 수 없는 파드 개수는 1개 입니다.
- [공식문서](https://kubernetes.io/ko/docs/concepts/workloads/controllers/deployment/#%EC%B5%9C%EB%8C%80-%EB%B6%88%EA%B0%80-max-unavailable)

## 카나리아 배포 - rollout 일시정지
카나리아 배포 방식을 kubectl rollout으로 진행할 수 있습니다.

    카나리아 배포는 , 새 버전을 모든 파드에 대해 배포하는것이 아니라, 
    
    일부만 바꿔서 정상적인지 체크하고, 
    
    정상적이라면 전부를 바꾸고 , 
    
    정상적이지 않다면 다시 롤백하는 기술 입니다.

***rollout resume 명령어로 rollout을 일시 중지시켜서 수행합니다.***

```bash
$ kubectl rollout resume deployment myDep
```

## 잘못된 버전의 rollout 방지기술 - minReadySeconds

```.spec.minReadySeconds``` 필드를 사용하면 , 파드를 사용 가능한것 (1/1) 로 간주하기 전 , 새로 만든 파드를 준비할 시간을 줍니다.
- 프로브랑 비슷 ?

default 값은 0이며 , 해당 값을 잘 사용한다면 롤아웃 프로세스 속도를 늦춰 문제가 있는 버전이 프로덕션 레벨에 흘러들어가도 , 문제를 대처할 수 있는 시간을 만들어줍니다.

### 테스트
정상작동 deployment를 생성합니다.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: jinseong
spec:
  replicas: 3
  selector:
    matchLabels:
      app: jinseong
  template:
    metadata:
      labels:
        app: jinseong
    spec:
      containers:
      - image: jinseong/jinseong:v2
        name: nodejs
```

실제로 문제가있는 버전으로 생성했던 deployment를 rollout 합니다.
- 이때 readinessProbe 를 1초로 둡니다.

```yaml
kind: Deployment
metadata:
  name: jinseong
spec:
  replicas: 3
  minReadySeconds: 10
  strategy:
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
    type: RollingUpdate
  replicas: 3
  selector:
    matchLabels:
      app: jinseong
  template:
    metadata:
      name: jinseong
      labels:
        app: jinseong
    spec:
      containers:
      - image: jinseong/jinseong:v3
        name: nodejs
        readinessProbe:
          periodSeconds: 1
          httpGet:
            path: /
            port: 8080
```


```bash
$ kubectl apply -f problem.yaml
```

따라서 파드는 Probe가 성공할 때 까지 STATUS를 0/1로 두고 기다립니다.
```bash
NAME                     READY   STATUS    RESTARTS   AGE
kubia-684686f868-wp5l9   0/1     Running   0          5m23s
kubia-6979c5f8b9-ckbb9   1/1     Running   0          7m10s
kubia-6979c5f8b9-rz5lf   1/1     Running   0          7m10s
kubia-6979c5f8b9-t4fpv   1/1     Running   0          7m10s
```

그런데 해당 deployment는 rollout 프로세스를 겪고 있는데, 저상태에서 멈춰 있습니다.

**그 이유는 , minReadySeconds를 10초로 두었기 때문입니다.**

***minReadySeconds가 10초이기 때문에 , 파드가 10초동안 Running이여야만 사용 가능한 상태라고 확인하고 , rollout 프로세스가 진행됩니다.***

    만약 minReadySeconds를 설정해두지 않았다면, 비정상적인 파드로 모두 교체되었을 것이기에 서비스 중단이라는 치명적인 오류가 발생했을것.

## rollout history 확인
rollout의 CHANGE-CAUSE 및 rollback을 위해 history를 확인할 수 있습니다.

rollout 수행 시 , ```--record``` 명령어를 같이 줌으로써 기록할 수 있습니다.

```bash
kubectl edit deploy kubia --record

kubectl apply -f new_app.yaml --record

kubectl set image deployment kubia nodejs=jinseong/jinseong:v1 --record
...등
```

```bash
kubectl rollout history deployment jinseong                  
deployment.apps/jinseong 
REVISION  CHANGE-CAUSE
2         <none>
3         <none>
5         kubectl edit deploy jinseong --record=true
6         kubectl set image deployment jinseong nodejs=jinseong/jinseong:v1 --record=true
```


## rollout 데드라인 설정
rollout이 너무 오래걸릴 경우, 실패할것으로 간주합니다.
- default 10분

deployment의 ```spec.ProgressDeadlineExceeded``` 속성으로 설정할 수 있으며 , deployment describe 명령어로 확인할 수 있습니다.

## rollout 중단
롤아웃을 중단시킬 수 있습니다.

아래 명령어로 진행합니다.
```bash
$ kubectl rollout undo deployment jinseong
deployment.apps/jinseong rolled back
```