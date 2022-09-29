# rabbitmq helm install
## 1. Prerequisites
- 해당 문서는 rabbitmq cluster를 helm chart로 설치하여 구성하는 방안에 대해 기술합니다.
- 기본적으로 설치된 rabbitmq cluster들은 5672번 포트를 통해 통신합니다.
- rabbitmq를 설치하는 과정은 크게 두 단계로 나뉘게 됩니다.
	- 1. rabbitmq-cluster-operator 설치
		- rabbitmq-cluster-operator를 helm chart로 설치하여 배포하게 되면 , CRD가 생성됩니다.
	- 2. CRD로 rabbitmq-cluster 설치
		- operator가 설치되며 생성된 CRD를 통해서 rabbitmq-cluster를 설정하고 kubectl apply , create 명령어로 rabbitmq-cluster를 구성합니다.
## 2. rabbitmq-cluster-operator helm install
### 2.1 namespace 생성
- 먼저 rabbitmq-cluster-operator를 관리할 namespace를 생성합니다.
```bash
$ kubectl create namespace rabbitmq-cluster-operator
```
### 2.2 helm pull & values.yaml 수정
- helm chart를 pull합니다.
```bash
# helm repo add rabbitmq
helm repo add bitnami https://charts.bitnami.com/bitnami

# rabbitmq-cluster-operator pull
helm pull rabbitmq-cluster-operator bitnami/rabbitmq-cluster-operator --untar
```
- 가져온 chart의 values.yaml파일을 수정합니다.
```yaml
# 폐쇄망 설치의 경우 , 아래와같은 이미지값을 변경시켜 private registry를 바라보게끔 변경합니다.
$ cat setting-values.yaml
global:
  imageRegistry: {private_image_registry}
  imagePullSecrets: {pull_secrets}
  storageClass: {global_storageClass}

rabbitmqImage:
  repository: bitnami/rabbitmq
  tag: 3.10.7-debian-11-r3

credentialUpdaterImage:
  repository: bitnami/rmq-default-credential-updater
  tag: 1.0.2-scratch-r6

clusterOperator:
  image:
    repository:  bitnami/rabbitmq-cluster-operator
    tag: 1.14.0-scratch-r5

msgTopologyOperator:
  image:
    repository: bitnami/rmq-messaging-topology-operator
    tag: 1.7.1-scratch-r3
```
### 2.3 helm chart install
- helm chart로 rabbitmq를 install 합니다.
```bash
$ helm upgrade --install rabbitmq-cluster-operator -n rabbitmq-cluster-operator . -f values.yaml,setting-values.yaml
```
### 2.4 설치 결과 확인
- rabbitmq-cluster-operator 설치 결과를 kubectl 명령어로 확인합니다.
  모든 pod가 running인지 확인합니다.
```bash
$ kubectl get all -n rabbitmq-cluster-operator
```
- rabbitmq-cluster-operator가 CRD를 정상 생성하였는지 확인합니다.
```bash
$ kubectl get CRD https://customresourcedefinitions.apiextensions.k8s.io | grep rabbitmqclusters.rabbitmq.com
```
## 3. rabbitmq-cluster 설치
### 3.1 namespace 생성
- rabbitmq-cluster를 관리할 namespace를 생성합니다.
- rabbitmq-cluster-operator와 동일한 namespace에 생성 해도 무관하지만 , 다른 namespace를 생성해서 cluster를 관리하는것이 가독성이 더 좋기때문에 , namespace를 생성합니다.
```bash
$ kubectl create namespace rabbitmq-cluster
```
### 3.2 rabbitmq-cluster yaml파일 구성
- rabbitmq-cluster-operator를 설치하면서 생성된 CRD로 rabbitmq-cluster를 구축합니다.
- rabbitmq-cluster는 기본적으로 HA구성을 가질 수 있으며 , 클러스터끼리는 5672번 포트로 통신하게 됩니다.
- rabbitmq-cluster.yaml파일을 통해서 tls secret , cluster 개수 등의 cluster 구성값들을 설정할 수 있습니다.
- 아래 공식 문서를 참고하여 작성합니다.
[rabbitmq-cluster setting value](https://www.rabbitmq.com/kubernetes/operator/using-operator.html)
- rabbitmq-cluster yaml파일을 다음과 같이 구성합니다.
```yaml
$ cat rabbitmq-cluster.yaml
apiVersion: rabbitmq.com/v1beta1
kind: RabbitmqCluster
metadata:
  name: rabbitmqcluster-sample
  namespace: rabbitmq-cluster
spec:
  replicas: 3 # 3개의 cluster로 HA구성
  service:
    type: NodePort # rabbitmq ui 접속할 service type. NodePort, or LoadBalancer 가 올 수 있음
  TerminationGracePeriodSeconds: 80 
  persistence:
    storageClassName: fast # storageClass
    storage: 20Gi # storage size
  tls: 
    secretName: {tls-secret} # https통신을 위하여 tls 
  resources:
    requests:
      cpu: 1000m
      memory: 2Gi
    limits:
      cpu: 1000m
      memory: 2Gi
```
- TerminationGracePeriodSeconds ?
	- default 값은 604800 ( 1주일 ) 
	-  rabbitmq-cluster pod가 정상적으로 종료하기 위해 기다리는 시간입니다.
- resource 값 세팅 방안
	- RabbitMQ 및 Erlang이 사용 가능한 총 메모리를 일시적으로 초과하여 즉각적인 OOM 종료를 유발할 수 있습니다. 
	- 이를 방지하기 위해 클러스터 운영자는 total_memory_available_override_value 를 구성하여 메모리 헤드룸을 20%(최대값은 2GB)로 설정합니다 . 
	- 이는 RabbitMQ에 설정된 실제 메모리 제한이 지정된 리소스 요구 사항보다 20% 적음을 의미합니다.
### 3.4 rabbitmq-cluster 생성
- kubectl 명령어를 통해 rabbitmq-cluster를 생성합니다.
```bash
$ kubectl apply -f rabbitmq-cluster.yaml
```
- kubectl get all 명령어로 cluster 설치 결과를 확인합니다.
```bash
$ kubectl get all -n rabbitmq-cluster
```
## 4.  rabbitmq-cluster 정상 수행 확인
### 4.1 ingress 생성
- ingress를 생성합니다. rabbitmq-cluster가 생성되어있는 namespace에 생성 합니다.
- rabbitmq-cluster-operator는 CRD만 생성하고 , 실제 rabbitmq-cluster가 올라가있는 곳은 rabbitmq-cluster.yaml파일로 생성한 곳에 서비스가 수행중이라고 생각하면 됩니다.
```yaml
$ cat rabbitmq-ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: rabbitmq-cluster-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    kubernetes.io/ingress.class= "nginx"
spec:
  rules:
  - host: "rabbitmq.jinseong.com" 
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: rabbitmq-cluster
            port:
              number: 15672
  tls:
  - hosts:
	- rabbitmq.jinseong.com
	secretName: {tls secret}
```
- kubectl 명령어로 ingress를 생성 합니다.
```bash
$ kubectl apply -f rabbitmq-ingress.yaml
```
### 4.2 접근 확인
- 아래 명령어로 username과 pwd를 확인하여 로그인 합니다.
```bash
# Username
kubectl get secret rabbitmq-cluster-default-user -o jsonpath="{.data.username}" -n rabbitmq | base64 -d

# Password
kubectl get secret rabbitmq-cluster-default-user -n rabbitmq -o jsonpath="{.data.password}" | base64 -d
```