# rabbitmq user 변경 방안 - default user 변경 및 user 추가 방안
## 1. Prerequisites
- 해당문서는 rabbitmq의  default user 정보를 변경하거나 user를 추가할 수 있는 방안에 대해 기술합니다.
- 관련 문서는 아래를 참조하였습니다.
- user 및 
## 2. rabbitmq default user info 변경 방안
- 먼저 user 정보를 가진 secret을 생성합니다. 
	- user 정보는 ID 및 pwd를 의미합니다.
	- 예시로 들은 yaml 파일 ( 다음의  yaml )과 동일한 구성이 되어야 하며 , cluster 이름에 따라 ( rabbitmq-cluster-operator를 설치하며 생성한 rabbitmq-cluster.yaml 파일을 통해 만든 rabbitmq-cluster의 이름 ) secret을 다르게 주어야 합니다.
	- 만약 cluster 이름이 jinseong-rabbitmq라면 , secret의 이름은 jinseong-rabbitmq-default-user 이어야 합니다.
	- 해당 문서에서는 cluster 이름을 jinseong-rabbitmq로 두고 진행합니다.
### 2.1 secret 생성
- rabbitmq-cluster의 secret을 생성합니다.
  설정한 default user 정보는 아래와 같습니다
  - ID : jinseong
  - PWD : pwd
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: my-rabbit-creds
type: Opaque
stringData:
  username: a-user # has to be an existing user
  password: a-secure-password
  default_user.conf: |
    default_user=jinseong
    default_pass=pwd
```
### 2.2. rabbitmq.com/v1beta1 CRD의 user생성
- CRD를 통해 생성해두었던 secret 기반으로 user를 생성합니다.
- 생성할 때 , tags 값을 통해서 권한을 부여할 수 있습니다.
  부여할 수 있는 권한은 총 네가지가 있으며 , 다음과 같습니다.
	-	administrator
	-	management
	-	policymaker
	-	monitoring
-	default user를 생성해야 하기 때문에 , 권한은 administrator로 지정 합니다.
```yaml
cat rabbitmq-cluster-user.yaml
apiVersion: rabbitmq.com/v1beta1
kind: User
metadata:
  name: user-example
  namespace: rabbitmq-system
spec:
  tags:
  - administrator
  rabbitmqClusterReference:
    name: example-rabbitmq
  importCredentailsSecret: # 생성해두었던 secret 이름이 들어갑니다.
    name: my-rabbit-creds 
```
### 2.3 rabbitmq-cluster 재 배포
- rabbitmq-cluster.yaml파일을 재 배포합니다.
```
$ kubectl apply -f rabbitmq-cluster-user.yaml
```
- 만약 user정보가 바뀌지 않는다면 , rabbitmq-cluster pvc를 모두 제거한 뒤 재 배포합니다.