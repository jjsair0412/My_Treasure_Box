# Redis-cluster
## 1. Prerequisites
- helm chart를 이용한 redis 설치
- [redis helm](https://github.com/bitnami/charts/tree/master/bitnami/redis-cluster)
- 상황마다 helm values.yaml파일은 변경될 수 있습니다. 
## 2. Redis-cluster 설치
### 2.1 namespace 설정
- redis 관리용 namespace 설정
```
$ kubectl create ns redis
```
### 2.2 Helm install && Redis 설정
- Helm repo 추가 및 helm values 파일들 pull 과정
```
$ helm repo add bitnami https://charts.bitnami.com/bitnami

$ helm pull bitnami/redis-cluster

$ tar xvfz redis-cluster... 
```
- 특정 node에 배포하기 위해 affinity 설정
```
$ cat affinity-values.yaml
redis:
  affinity:
    ...

updateJob:
  affinity:
    ...
```
- prometheus 연동을 위해 values.yaml파일 생성
```
$ cat config-values.yaml
metrics:
  enabled: true
  serviceMonitor:
    namespace: monitoring
    enabled: true
    interval: 30s
    labels:
      release: prometheus
```

- redis Probe를 custom해서 작성. 
  - Probe에서 에러 발생시 아래 custom-values.yaml 사용
```yaml
cluster:
  init: true
  nodes: 6
  replicas: 1

redis:
  customLivenessProbe:
    exec:
      command:
        - sh
        - -c
        - redis-cli -h localhost -p $REDIS_PORT_NUMBER ping
  customReadinessProbe:
    exec:
      command:
        - sh
        - -c
        - redis-cli -h localhost -p $REDIS_PORT_NUMBER ping

usePassword: false
password: ''
```

### 2.3 Helm redis 설치
```
$ helm upgrade --install redis-cluster . -f values.yaml,affinity-values.yaml,config-values.yaml -n redis
```
### 2.4 Pod 상태 확인
```
$ kubectl get pods -n redis
NAME                  READY   STATUS    RESTARTS   AGE
pod/redis-cluster-0   2/2     Running   0          24m
pod/redis-cluster-1   2/2     Running   0          24m
pod/redis-cluster-2   2/2     Running   0          24m
pod/redis-cluster-3   2/2     Running   0          24m
pod/redis-cluster-4   2/2     Running   0          24m
pod/redis-cluster-5   2/2     Running   0          24m
```