# Kubernetes RBAC
## Overview
해당 문서는 Kubernetes RBAC에 대해 복습하면서 작성한 문서 입니다.

## RBAC 이란 ?
kubernetes는 기본적으로 Kube-API-Server에 REST 요청을 보내서 특정 액션을 리소스에게 명령하며 관리하게 됩니다.

사용자는 REST 요청에 리소스 액션을 보낼 때, 자격증명(인증 토큰, 사용자 이름 암호 또는 인증서)을 포함시켜서 자신을 인증하게 됩니다.

Kube-API-Server로 보내는 REST 종류는 , 잘 알다싶이 다음과 같습니다.
- GET
- POST
- PUT
- DELETE
등..

또한 파드, 서비스, 시크릿 등의 리소스가 이러한 요청에 응답하고 사용자 요청에 맞는 액션을 취하게 됩니다.
- 파드를 생성한다, 파드를 조회한다, 서비스를 생성한다, 시크릿을 제거한다 등..

추가로 전체 리소스에 권한을 적용하는것 외에, RBAC 규칙을 특정 리소스 인스턴스에 적용할 수 있으며, 추가로 특정 URL 경로에도 권한을 정할 수 있습니다.
- 예를 들어 /api , /healthz 에 접근권한 부여...

### RBAC 이해
RBAC은 어떤 사용자가 어떤 액션을 수행할 수 있는지 여부를 결정하는 요소로 ***role*** 을 사용하게 됩니다.

액션을 명령하는 ***주체(사람, 서비스어카운트, 서비스어카운트 그룹)*** 는 하나 이상의 ***role*** 과 연계되어 있으며, 여러 ***role*** 이 할당됬다면, 롤에서 허용하는 모든작업을 수행할 수 있습니다.
- 예를들어 user role에 파드를 업데이트하는 권한이 없으면, ```API 서버는 사용자가 시크릿에 대해 PUT or PATCH 요청을 수행하지 못하게 합니다.```

### RBAC 리소스들
RBAC 리소스는 두 그룹으로 나뉠 수 있으며, 나뉘는 기준은 다음과 같습니다.
- Namespace에 종속적인가?

|수행할 수 있는 동사(액션) 지정|롤을 특정 사용자, 그룹, 서비스어카운트에 바운딩|
|--|--|
|Role|ClusterRole|
|RoleBinding|ClusterRoleBinding|
|Namespace에 종속적임|Namespace에 종속적이지 않음|

## User Account and Service Account
RBAC에 대해 실습해 보기 전에, User Account 와 Service Account 둘을 구분할 필요가 있습니다.

Kubernetes 공식 문서에선 다음과 같이 기술합니다.
- [공식문서 (사용자 어카운트와 서비스 어카운트 비교) 링크](https://kubernetes.io/ko/docs/reference/access-authn-authz/service-accounts-admin/#%EC%82%AC%EC%9A%A9%EC%9E%90-%EC%96%B4%EC%B9%B4%EC%9A%B4%ED%8A%B8%EC%99%80-%EC%84%9C%EB%B9%84%EC%8A%A4-%EC%96%B4%EC%B9%B4%EC%9A%B4%ED%8A%B8-%EB%B9%84%EA%B5%90)


    - **사용자 어카운트는 사람을 위한 것이지만, 서비스 어카운트는 쿠버네티스의 경우 파드의 일부 컨테이너에서 실행되는 애플리케이션 프로세스** 를 위한 것이다.

    - 사용자 어카운트는 전역적으로 고려되기 때문에, 클러스터의 모든 네임스페이스에 걸쳐 이름이 고유해야 한다. 
    
        어떤 네임스페이스를 확인하든지 간에, 특정 사용자명은 해당 유저만을 나타낸다. 쿠버네티스에서 서비스 어카운트는 네임스페이스별로 구분된다. 두 개의 서로 다른 네임스페이스는 동일한 이름의 서비스어카운트를 각자 가질 수 있다.
    
    - 일반적으로 클러스터의 사용자 어카운트는 기업 데이터베이스로부터 동기화될 수 있으며, 여기서 새로운 사용자 어카운트를 생성하려면 특별한 권한이 필요하며 복잡한 비즈니스 프로세스에 연결된다. 
    
        반면에 서비스 어카운트를 생성하는 경우는, 클러스터 사용자가 최소 권한 원칙에 따라 특정 작업을 위한 서비스 어카운트를 만들 수 있도록 보다 가볍게 만들어졌다. 
        
        실 사용자를 온보딩하는 단계와 서비스어카운트를 생성하는 단계를 분리하는 것은, 워크로드가 최소 권한 원칙을 따르기 쉬워지게 한다.
    
    - 사람과 서비스 어카운트에 대한 감사 고려 사항은 다를 수 있다. 이 둘을 따로 관리함으로써 더욱 쉽게 감사를 수행할 수 있다.

***정리하면, User Account는 쿠버네티스에 할당된 유저를 의미하며, Service Account는 Kubernetes Resource가 Kube-Api-Server로 요청을 호출하기 위한 자격 증명 주체 입니다.***

### Service Account 생성
아래 yaml template으로 sample namespace에 service-account-example-sa Service Account를 생성해 봅니다.
```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: service-account-example-sa
  namespace: sample

$ kubectl apply -f sa.yaml
```

sample namesapce에 Service Account가 생성된것을 확인할 수 있습니다.
- default Service Account는 Kubernetes가 기본적으로 생성해주는 Service Account 입니다. 
- 만약 파드나 특정 리소스에 Service Account를 할당하지 않는다면, 해당 default Service Account가 할당됩니다.
```bash
$ kubectl get sa -n sample
NAME                         SECRETS   AGE
default                      0         8s
service-account-example-sa   0         7s
```

kubernetes v1.24 이전까지는 Service Account를 생성하게 되면, Kube-API Server에 인증할 수 있는 Token이 자동으로 생성되었습니다.

**그러나, v1.24 이상에서는 보안 강화를위해 자동생성해주지 않기 때문에, Service Account의 Token을 직접 생성해주어야 합니다.**

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: service-account-example-sa-secret # Secret 이름
  namespace: sample # 생성 대상 Service Account와 동일한 namespace에 위치해야 함
  annotations:
    kubernetes.io/service-account.name: service-account-example-sa # 대상 Service Account 이름 기입
type: kubernetes.io/service-account-token

$ kubectl apply -f sa-token.yaml 
secret/service-account-example-sa-secret created
```

위와 같이 생성하게되면, Service Account가 Kube-api Server에 인증하기 위한 Token을 획득할 수 있습니다.
```bash
$ kubectl get secret -n sample
NAME                                TYPE                                  DATA   AGE
service-account-example-sa-secret   kubernetes.io/service-account-token   3      6s
```

이제 해당 Service Account를 마운트하는 Nginx image Deployment를 하나 생성해봅니다.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: service-account-example
  namespace: sample
  labels:
    app: service-account-example
spec:
  replicas: 1
  selector:
    matchLabels:
      app: service-account-example
  template:
    metadata:
      labels:
        app: service-account-example
    spec:
      serviceAccountName: service-account-example-sa # SA 를 지정해줍니다
      containers:
        - name: nginx
          image: nginx:latest
          ports:
            - containerPort: 80


$ kubectl apply -f sa-deploy.yaml
deployment.apps/service-account-example created
```

해당 Deployment가 생성한 Pod를 Describe해보면, 어딘가에 Mount하고있는것을 확인할 수 있습니다.
```bash
$ kubectl describe pods -n sample
...
  nginx:
    Container ID:   docker://ca40321f3741a98a0075aceae83e441592d3085aca0e2db584a67d3791ce80da
    Image:          nginx:latest
    Image ID:       docker-pullable://nginx@sha256:86e53c4c16a6a276b204b0fd3a8143d86547c967dc8258b3d47c3a21bb68d3c6
    Port:           80/TCP
    Host Port:      0/TCP
    State:          Running
      Started:      Wed, 08 Nov 2023 00:23:34 +0900
    Ready:          True
    Restart Count:  0
    Environment:    <none>
    Mounts:
      /var/run/secrets/kubernetes.io/serviceaccount from kube-api-access-zlznx (ro) # ??
```

exec으로 직접 파드에 접근해서 해당 경로로 들어가보면, 방금 생성한 Service Account의 Token이 마운트되어있는것을 확인할 수 있습니다.
- 해당 Token으로 Kube-API Server와 통신할 때 인증을 요청할 수 있습니다.

```bash
$ kubectl exec -it service-account-example-67dcff6577-p2bhx -n sample -- /bin/bash 

root@service-account-example-67dcff6577-p2bhx:/var/run/secrets/kubernetes.io/serviceaccount# pwd
/var/run/secrets/kubernetes.io/serviceaccount

root@service-account-example-67dcff6577-p2bhx:/var/run/secrets/kubernetes.io/serviceaccount# ls -l
total 0
lrwxrwxrwx 1 root root 13 Nov  7 15:23 ca.crt -> ..data/ca.crt
lrwxrwxrwx 1 root root 16 Nov  7 15:23 namespace -> ..data/namespace
lrwxrwxrwx 1 root root 12 Nov  7 15:23 token -> ..data/token
```

해당 Token 값을 Bearer Token으로 설정해서 Kube-API 서버와 통신해 봅니다.
- ```https://$KUBERNETES_SERVICE_HOST/api/v1/namespaces/default/pods``` 경로는 , default namespace의 pods list를 출력하는 REST API 경로입니다.
- Kube-API Server도 결국 REST로 통신하기 때문에, 위와 같은 URL 구조로 요청을 보낼 수 있습니다.

```bash
$ TOKEN=$(cat token)

$ curl -X GET https://$KUBERNETES_SERVICE_HOST/api/v1/namespaces/default/pods --header "Authorization: Bearer $TOKEN" --insecure
{
  "kind": "Status",
  "apiVersion": "v1",
  "metadata": {},
  "status": "Failure",
  "message": "pods is forbidden: User \"system:serviceaccount:sample:service-account-example-sa\" cannot list resource \"pods\" in API group \"\" in the namespace \"default\"",
  "reason": "Forbidden",
  "details": {
    "kind": "pods"
  },
  "code": 403
}
```

통신 수행 결과, 401 (Unauthorized) 가 아닌 403 (Forbidden) 에러가 발생합니다.

그러한 이유는 , **생성한 Nginx Deployment에 할당된 Service Account인 service-account-example-sa 가, 자격 증명만 가능할 뿐 kubernetes 리소스에 대한 어떠한 액션(생성, 조회, 삭제, 수정) 권한도 없기 때문입니다.**

**이때 Role과 RoleBinding 혹은 ClusterRole과 ClusterRoleBinding을 권한 부여를 위해 사용하게 됩니다.**

## Role과 RoleBinding 사용
먼저 Namepsace에 종속적인 Role과 RoleBinding으로 권한을 부여해 봅니다.

### Role 생성
해당 yaml template은, roleTest namespace의 pod 리소스를 대상으로 모든 읽기 권한을 부여합니다.
```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  namespace: sample # namespace 지정, SA와 동일해야 함.
  name: sa-reader
rules:
- apiGroups: [""] # 권한을 부여할 resources가 위치한 api 그룹 지정, "" 으로 두면 core api group인것으로 간주됨
  resources: ["pods"] # 어떤 리소스에 대해 적용할것인지 나열
  verbs: ["get", "watch", "list"] # 허용할 Action 나열
```

해당 Role을 생성합니다.
```bash
$ kubectl apply -f role.yaml 
role.rbac.authorization.k8s.io/pod-reader created

# 생성결과 확인
$ kubectl get role -n sample
NAME         CREATED AT
pod-reader   2023-11-07T15:36:51Z
```

### RoleBinding 생성
이제, 방금 생성한 Role과 Service Account를 바인딩해줄 RoleBinding을 생성합니다.
```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: read-pods
  namespace: sample
subjects:
- kind: ServiceAccount # or User
  name: service-account-example-sa # Service Account Name or User Name 
  namespace: sample # User or SA가 생성된 Namespace 기입
roleRef:
  kind: Role 
  name: pod-reader # 바인딩할 Role Name
  apiGroup: rbac.authorization.k8s.io
```

RoleBinding을 생성해서 , Service Account와 Role을 바인딩 해 줍니다.

```bash
$ kubectl apply -f role-bind.yaml
rolebinding.rbac.authorization.k8s.io/read-pods created

# 생성결과 확인
$ kubectl get Rolebinding -n sample
NAME        ROLE              AGE
read-pods   Role/pod-reader   11s

# service account와 바인딩결과 확인
$ kubectl describe rolebinding -n sample
Name:         read-pods
Labels:       <none>
Annotations:  <none>
Role:
  Kind:  Role
  Name:  pod-reader
Subjects:
  Kind            Name                        Namespace
  ----            ----                        ---------
  ServiceAccount  service-account-example-sa  sample
```

### 결과 확인
이제 service-account-example-sa Service Account는 pod에 대해 ["get", "watch", "list"] 권한을 부여받았습니다.

다시 Pod에 exec 하여 아까와 동일한 API로 Kube-Api Server로 요청을 보내봅니다.
- 대상 : ```https://$KUBERNETES_SERVICE_HOST/api/v1/namespaces/default/pods``` 

```bash
curl -X GET https://$KUBERNETES_SERVICE_HOST/api/v1/namespaces/default/pods --header "Authorization: Bearer $TOKEN" --insecure
{
  "kind": "Status",
  "apiVersion": "v1",
  "metadata": {},
  "status": "Failure",
  "message": "pods is forbidden: User \"system:serviceaccount:sample:service-account-example-sa\" cannot list resource \"pods\" in API group \"\" in the namespace \"default\"",
  "reason": "Forbidden",
  "details": {
    "kind": "pods"
  },
  "code": 403
}
```

아직 403 에러가발생합니다.

**그 이유는, Role과 RoleBinding은 특정 Namespace 에 대해서만 작업 수행이 가능하기 때문입니다.***

현재 sample namespace로 설정해 두었기 때문에, sample namespace로 다시 보내봅니다.
```bash
curl -X GET https://$KUBERNETES_SERVICE_HOST/api/v1/namespaces/sample/pods --header "Authorization: Bearer $TOKEN" --insecure
{
  "kind": "PodList",
  "apiVersion": "v1",
  "metadata": {
    "resourceVersion": "3877012"
  },
  "items": [
    {
      "metadata": {
        "name": "service-account-example-67dcff6577-48fwc",
        "generateName": "service-account-example-67dcff6577-",
        "namespace": "sample",
        "uid": "01fa3da2-b50b-4494-8fdc-7ad3cc01b4b0",
        "resourceVersion": "3876948",
...
```

정상적으로 200 ok와 pod 조회가 가능한것을 확인할 수 있습니다.

- ETC , 다른 리소스에 요청보내면, 권한이 없다는것을 확인할 수 있습니다.

```bash
curl -X GET https://$KUBERNETES_SERVICE_HOST/api/v1/namespaces/sample/service --header "Authorization: Bearer $TOKEN" --insecure
{
  "kind": "Status",
  "apiVersion": "v1",
  "metadata": {},
  "status": "Failure",
  "message": "service is forbidden: User \"system:serviceaccount:sample:service-account-example-sa\" cannot list resource \"service\" in API group \"\" in the namespace \"sample\"",
  "reason": "Forbidden",
  "details": {
    "kind": "service"
  },
  "code": 403
}
```


## ClusterRole과 ClusterRoleBinding 사용
이제 Namespace에 구애받지 않는 ClusterRole과 ClusterRoleBinding을 통해 모든 리소스에대해 접근 권한을 허용해 보겠습니다.

### ClusterRole 생성
ClusterRole은 Role과 다르게, ```rbac.authorization.k8s.io/aggregate-to-monitoring: "true"``` label을 통해서 여러 ClusterRole을 하나로 묶어 집계할 수 있습니다.

작동 방식은, 모든 ClusterRole을 집계할 공통 ClusterRole에 ```rbac.authorization.k8s.io/aggregate-to-monitoring: "true"``` matchLabels을 설정해 두어 모니터링합니다.

그리고, 집계 대상이될 ClusterRole은 ```rbac.authorization.k8s.io/aggregate-to-monitoring: "true"``` label을 설정해서, 집계할 ClusteRole에 집계될 수 있도록 합니다.

**집계된 ClusterRole을 사용해서, 집계대상 ClusterRole의 권한을 모두 부여받을 수 있습니다.**
- 해당 예제에선 aggregate-monitoring-role를 Service Account와 바인딩시키면, pod와 service, endpoint에 대해 조회권한을 부여받을 수 있습니다.

먼저, 집계될 ClusterRole을 아래와 같은 yaml template을 통해 생성합니다.

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: aggregate-monitoring-role # 집계될 clusterrole 이름
aggregationRule:
  clusterRoleSelectors:
  - matchLabels:
      # 집계하기 위해 해당 label을 설정합니다. 
      # rbac.example.com/aggregate-to-monitoring: "true" label이 존재하는 clusterRole을 대상으로 집계한다는 의미
      rbac.example.com/aggregate-to-monitoring: "true" 
rules: [] # control plane이 자동으로 clusterrole의 rule들을 여기에 집계합니다.

$ kubectl apply -f all-clusterrole.yaml 
clusterrole.rbac.authorization.k8s.io/aggregate-monitoring-role created
```

집계 대상이 될 ClusterRole들을 생성합니다.

1. pod에 대한 조회권한이 있는 ClusterRole
```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: pod-reader
  labels: 
    rbac.example.com/aggregate-to-monitoring: "true"
rules:
- apiGroups: [""] # 권한을 부여할 resources가 위치한 api 그룹 지정, "" 으로 두면 core api group인것으로 간주됨
  resources: ["pods"] # resources 지정
  verbs: ["get", "list", "watch"] # action 지정


$ kubectl apply -f pod-clusterrole.yaml 
clusterrole.rbac.authorization.k8s.io/pod-reader created
```

집계가 정상적으로 작동했는지 , ```aggregate-monitoring-role``` 을 describe 해 봅니다.

```bash
# pod의 조회권한이 할당된것을 확인할 수 있습니다.
$ kubectl describe clusterrole aggregate-monitoring-role
Name:         aggregate-monitoring-role
Labels:       <none>
Annotations:  <none>
PolicyRule:
  Resources  Non-Resource URLs  Resource Names  Verbs
  ---------  -----------------  --------------  -----
  pods       []                 []              [get list watch]
```

2. service와 endpoint에 대한 조회권한이 있는 ClusterRole
```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: service-reader
  labels: 
    rbac.example.com/aggregate-to-monitoring: "true" # 이 어노테이션은 이 Role을 aggregate-monitoring-role과 집계되게 합니다.
rules:
- apiGroups: ["","discovery.k8s.io"] # endpointslices 는 core api group에 있는게아니라, discovery.k8s.io api group에 있기 때문에 추가해주어야 함
  resources: ["services", "endpointslices"] # resources 지정
  verbs: ["get", "list", "watch"] # action 지정

$ kubectl apply -f service-clusterrole.yaml 
clusterrole.rbac.authorization.k8s.io/service-reader created
```

집계가 정상적으로 작동했는지 , ```aggregate-monitoring-role``` 을 describe 해 봅니다.

```bash
# service와 endpoints에 대한 조회권한이 할당된것을 확인할 수 있습니다.
$ kubectl describe clusterrole  aggregate-monitoring-role
Name:         aggregate-monitoring-role
Labels:       <none>
Annotations:  <none>
PolicyRule:
  Resources                        Non-Resource URLs  Resource Names  Verbs
  ---------                        -----------------  --------------  -----
  endpointslices                   []                 []              [get list watch]
  pods                             []                 []              [get list watch]
  services                         []                 []              [get list watch]
  endpointslices.discovery.k8s.io  []                 []              [get list watch]
  services.discovery.k8s.io        []                 []              [get list watch]
```

### ClusterRoleBinding 생성
이제 생성한 ClusterRole들을 ClusterRoleBinding으로 Service Account와 바인딩 시켜줍니다.
- 그전에 role과 rolebinding을 모두 제거하여 모든권한이 없는 service-account-example-sa 유저로 바꿔둡니다.

```aggregate-monitoring-role``` ClusterRole 에 생성된 service, pod, endpoint 조회권한 ClusterRole 을 집계해 두었으니, ```aggregate-monitoring-role``` 을 바인딩하여 모든 권한(pod,service,endpoint 조회) 을 부여받습니다.
- 따로따로 clusterrole을 부여하는것 또한 가능합니다.

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: read-pod-service-global
subjects:
- kind: ServiceAccount
  name: service-account-example-sa # service account 이름 
  namespace: sample # 바인딩 대상 service account의 namespace 
roleRef:
  kind: ClusterRole
  name: aggregate-monitoring-role # 집계 clusterrole 지정
  apiGroup: rbac.authorization.k8s.io

$ kubectl apply -f clusterrolebinding.yaml 
clusterrolebinding.rbac.authorization.k8s.io/read-pod-service-global created
```

생성 결과를 확인합니다.
```bash
$ kubectl describe clusterrolebinding read-pod-service-global
Name:         read-pod-service-global
Labels:       <none>
Annotations:  <none>
Role:
  Kind:  ClusterRole
  Name:  aggregate-monitoring-role
Subjects:
  Kind            Name                        Namespace
  ----            ----                        ---------
  ServiceAccount  service-account-example-sa  sample
```

### 결과 확인
이제 service-account-example-sa Service Account는 모든 Namespace에 pod, service, endpoints에 대한 조회 권한을 부여받았습니다.

pod에 exec 하여 테스트 해봅니다.

동일하게 마운트 정보를 확인합니다.
```bash
$ kubectl exec -it service-account-example-67dcff6577-kmbll -n sample -- /bin/bash

# 파드내부
cd /var/run/secrets/kubernetes.io/serviceaccount/

ls -l
total 0
lrwxrwxrwx 1 root root 13 Nov  7 16:59 ca.crt -> ..data/ca.crt
lrwxrwxrwx 1 root root 16 Nov  7 16:59 namespace -> ..data/namespace
lrwxrwxrwx 1 root root 12 Nov  7 16:59 token -> ..data/token
```

테스트 케이스들은 다음과 같습니다.
- 테스트 케이스 1 : ```https://$KUBERNETES_SERVICE_HOST/api/v1/namespaces/default/pods``` 
- 테스트 케이스 2 : ```https://$KUBERNETES_SERVICE_HOST/api/v1/namespaces/sample/pods``` 
- 테스트 케이스 3 : ```https://$KUBERNETES_SERVICE_HOST/api/v1/namespaces/default/service``` 
- 테스트 케이스 4 : ```https://$KUBERNETES_SERVICE_HOST/api/v1/namespaces/sample/service``` 
- 테스트 케이스 5 : ```https://$KUBERNETES_SERVICE_HOST/api/v1/namespaces/default/endpoints``` 
- 테스트 케이스 6 : ```https://$KUBERNETES_SERVICE_HOST/api/v1/namespaces/sample/endpoints``` 

#### 1번 테스트케이스
default namespace에 모든 pods의 조회를 요청합니다.
- 정상 수행
```bash
$ TOKEN=$(cat token)

$ curl -X GET https://$KUBERNETES_SERVICE_HOST/api/v1/namespaces/default/pods --header "Authorization: Bearer $TOKEN" --insecure
{
  "kind": "PodList",
  "apiVersion": "v1",
  "metadata": {
    "resourceVersion": "3881541"
  },
  "items": []
}
```

#### 2번 테스트케이스
sample namespace에 모든 pods의 조회를 요청합니다.
- 정상 수행
```bash
$ TOKEN=$(cat token)

$ curl -X GET https://$KUBERNETES_SERVICE_HOST/api/v1/namespaces/sample/pods --header "Authorization: Bearer $TOKEN" --insecure
{
  "kind": "PodList",
  "apiVersion": "v1",
  "metadata": {
    "resourceVersion": "3881615"
  },
  "items": [
    {
      "metadata": {
        "name": "service-account-example-67dcff6577-r88d6",
        "generateName": "service-account-example-67dcff6577-",
        "namespace": "sample",
        "uid": "aed063bb-6450-41ec-b8de-cf103779e592",
        "resourceVersion": "3881182",
        "creationTimestamp": "2023-11-07T16:39:42Z",
        "labels": {
...
```


#### 3번 테스트케이스
default namespace에 모든 service의 조회를 요청합니다.
- 정상 수행
```bash
$ TOKEN=$(cat token)

$ curl -X GET https://$KUBERNETES_SERVICE_HOST/api/v1/namespaces/default/services --header "Authorization: Bearer $TOKEN" --insecure
{
  "kind": "ServiceList",
  "apiVersion": "v1",
  "metadata": {
    "resourceVersion": "3881693"
  },
  "items": [
    {
      "metadata": {
        "name": "kubernetes",
        "namespace": "default",
        "uid": "ab7f3303-e0df-49e5-bd45-d5d2c8c28c12",
        "resourceVersion": "230",
        "creationTimestamp": "2023-07-25T14:58:21Z",
        "labels": {
          "component": "apiserver",
          "provider": "kubernetes"
        },
...
```

#### 4번 테스트케이스
sample namespace에 모든 service의 조회를 요청합니다.
- 정상 수행
```bash
$ TOKEN=$(cat token)

$ curl -X GET https://$KUBERNETES_SERVICE_HOST/api/v1/namespaces/sample/services --header "Authorization: Bearer $TOKEN" --insecure
{
  "kind": "ServiceList",
  "apiVersion": "v1",
  "metadata": {
    "resourceVersion": "3881762"
  },
  "items": []
}
```

#### 5번 테스트케이스
default namespace에 모든 endpointslices의 조회를 요청합니다.
- 정상 수행
```bash
$ TOKEN=$(cat token)

$ curl -X GET https://$KUBERNETES_SERVICE_HOST/apis/discovery.k8s.io/v1/namespaces/default/endpointslices --header "Authorization: Bearer $TOKEN" --insecure
{
  "kind": "EndpointSliceList",
  "apiVersion": "discovery.k8s.io/v1",
  "metadata": {
    "resourceVersion": "3882941"
  },
  "items": [
    {
      "metadata": {
        "name": "kubernetes",
        "namespace": "default",
        "uid": "7c1f4867-021f-4b04-880d-afde14b1a4fd",
        "resourceVersion": "233",
        "generation": 1,
        "creationTimestamp": "2023-07-25T14:58:21Z",
        "labels": {
          "kubernetes.io/service-name": "kubernetes"
        },
        "managedFields": [
          {
            "manager": "kube-apiserver",
            "operation": "Update",
...
```

#### 6번 테스트케이스
sample namespace에 모든 endpointslices의 조회를 요청합니다.
- 정상 수행
```bash
$ TOKEN=$(cat token)

$ curl -X GET https://$KUBERNETES_SERVICE_HOST/apis/discovery.k8s.io/v1/namespaces/sample/endpointslices --header "Authorization: Bearer $TOKEN" --insecure
{
  "kind": "EndpointSliceList",
  "apiVersion": "discovery.k8s.io/v1",
  "metadata": {
    "resourceVersion": "3882994"
  },
  "items": []
}
...
```

clusterRole을 통해 권한을 부여한 리소스들 조회가 정상적으로 작동하는것을 확인할 수 있습니다.

## TroubleShooting
ClusterRole 과 Role을 번갈아가면서 테스트 하기 때문에, Service Account와 바인딩된 상태에서 Role이 제대로 구성이 안되어있을 때, 재 구성한다면 아래와같은 현상이 발생할 수 있습니다.
- service Account를 인식하지 못하고 ```system:anonymous```로 출력되는 에러발생
```bash
$ curl -X GET https://$KUBERNETES_SERVICE_HOST/api/v1/namespaces/default/endpoints --header "Authorization: Bearer $TOKEN" --insecure
{
  "kind": "Status",
  "apiVersion": "v1",
  "metadata": {},
  "status": "Failure",
  "message": "endpoints is forbidden: User \"system:anonymous\" cannot list resource \"endpoints\" in API group \"\" in the namespace \"default\"",
  "reason": "Forbidden",
  "details": {
    "kind": "endpoints"
  },
  "code": 403
}
```

**이럴땐 파드 제거했다가 재 배포하면 해결됩니다.**

## ETC - 권한 확인여부 명령어
- 참고, 아래 명령어로 특정 유저나 Service Account가 특정 리소스에 접근할 수 있는지 여부를 no, yes로 나타내줍니다.

```bash
# service-account-example-sa SA가 endpointslices에 대해 list권한이 있는지 여부 확인
$ kubectl auth can-i list endpointslices --as=system:serviceaccount:sample:service-account-example-sa --namespace=sample
yes
```
