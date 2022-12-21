# CRD 개발 - golang
해당 폴더에서는 실제 cr , crd를 yaml파일로 정의한 뒤 , ( etcd에 등록 ) 

golang용 operator를 커스터마이징 하여 해당 cr를 통해서 k8s object를 컨트롤하는 실습 입니다.

## 0. prerequisite
참고 문서
- [CRD 생성관련 공식문서](https://kubernetes.io/docs/tasks/extend-kubernetes/custom-resources/custom-resource-definitions/)
- [by devjh의 블로그](https://frozenpond.tistory.com/111)
- [operator SDK 개발 방안 공식문서](https://sdk.operatorframework.io/docs/building-operators/golang/tutorial/)

실제 코드 위치
- [CR , CRD yaml manifest]
- [golang code]()

## 0.1 개발 환경
- macOS m1
- IDE Tool : vscode
- Golang : version go1.19.3 darwin/amd64
- operator version : [operator sdk](https://github.com/operator-framework/operator-sdk)
- kubernetes version : v1.25.2

## 0.2 개발 목표
helloworld kind를 가지는 cr을 개발합니다.

기능은 다음과 같습니다.
helloworld kind를 가진 cr을 생성 후 , message를 변경한다면 

1. nginx image를 가진 deployment 배포
2. 해당 deployment를 expose하는 서비스 생성
- service type은 NodePort 사용
3. log로 "hello world i'm jinseong" 출력

의 순서로 배포 작동하게끔하는 cr을 개발합니다.

또한 **replicas 개수가 변경되면 , 파드 개수가 늘어나거나 줄어들게끔 개발**합니다.

해당 CRD는 **namespace에 종속적**이게끔 개발합니다.

kubectl로 상태를 확인할 때 , **"hw" 라는 단축키로 kube-api server에 요청을 보낼 수 있게 끔** 개발합니다.

crd의 **fild 값은 message , appId , replicas , language , image** 를 받습니다.
- 이때 language는 enum 변수를 통해서 java, go , python만 가능하게끔 한정합니다.
- replicas의 최소는 1개 , 최대는 3개로 한정 합니다.

**appId , replicas , message,image는 필수 옵션**으로 한정합니다.

## 0.3 개발 과정
1. CRD yaml파일로 etcd에 정의
2. 정의된 CRD를 kind로 하는 CR yaml로 etcd에 정의
3. CR로 k8s object controller 하기 위해 operator-sdk custom하여 개발
- control 해야 할 object 목록
    - deployment
    - service

## 1. CRD 생성
CRD를 아래와 같이 생성합니다.

각 필드의 대한 설명은 주석으로 대체 합니다.

필드에 들어갈 수 있는 값들은 , 아래 공식 문서를 참고하여 필요한것들을 찾아서 작성합니다.
- [CRD 유효성 스키마 검사 방안](https://kubernetes.io/docs/tasks/extend-kubernetes/custom-resources/custom-resource-definitions/#specifying-a-structural-schema)
- [OpenAPI v3.0 validation schema](https://kubernetes.io/docs/tasks/extend-kubernetes/custom-resources/custom-resource-definitions/#validation)

```yaml
# apiextensions.k8s.io/v1을 지정하여 open API v3.0을 통해 구조를 검사하도록 정의
# structural schema 문서 읽어보기
apiVersion: apiextensions.k8s.io/v1
# kind는 CustomResourceDefinition
kind: CustomResourceDefinition
metadata:
  # name must match the spec fields below, and be in the form: <plural>.<group>
  name: helloworlds.jjsair0412.example.com
spec:
  # group name to use for REST API: /apis/<group>/<version>
  group: jjsair0412.example.com
  # list of versions supported by this CustomResourceDefinition
  versions:
    # version 명시
    - name: v1
      # Each version can be enabled/disabled by Served flag.
      served: true
      # One and only one version must be marked as the storage version.
      storage: true
      schema:
        # openAPIV3Schema는 required option 
        openAPIV3Schema:
          # k8s object type
          type: object
          # spec 필드 또한 필수 옵션.
          required: ["spec"]
          properties:
            spec:
              type: object
              required: ["appId", "replicas", "message","image"] # appId , replicas , message, image는 필수 옵션
              properties:
                message: # go struct 및 cr에 message를 입력하게끔 정의.
                  type: string # message의 data type 지정
                image: #go struct 및 cr에 image를 입력하게끔 정의.
                  type: string # image의 data type 지정
                appId: # go struct 및 cr에 appId를 입력하게끔 정의.
                  type: string # appId data type 지정
                replicas: #  go struct 및 cr에 replicas를 입력하게끔 정의.
                  type: integer # replicas의 data type 지정
                  minimum: 1 # 최소 1개~ 최대 3개
                  maximum: 3
                language: # go struct 및 cr에 language를 입력하게끔 정의.
                  type: string # language data type 지정
                  enum: # enum을 통해 데이터 미리 정해놓고, 셋중 하나가 아니라면 에러터지게끔 생성
                  - java
                  - go
                  - python
  # either Namespaced or Cluster , namespace에 종속적이게끔 생성
  scope: Namespaced
  names:
    # plural name to be used in the URL: /apis/<group>/<version>/<plural>
    plural: helloworlds
    # kubectl CLI 출력 결과 출력될 이름
    # singular name to be used as an alias on the CLI and for display
    singular: helloworld
    # CR의 Kind가 될 이름
    # kind is normally the CamelCased singular type. Your resource manifests use this.
    kind: helloworld
    # hw로 kubectl 명령어 날릴 수 있도록 생성
    # shortNames allow shorter string to match your resource on the CLI
    shortNames:
    - hw
```

apply하여 etcd에 CRD를 등록 합니다.

```bash
$ kubectl apply -f mycrd.yaml
customresourcedefinition.apiextensions.k8s.io/helloworld.jjsair0412.example.com created
```

get crd 명령어로 생성한 crd의 Name과 생성 시간을 확인할 수 있습니다.

```bash
$ kubectl get crd
NAME                                 CREATED AT
helloworlds.jjsair0412.example.com   2022-12-21T04:18:45Z
...
```

kubectl explain 명령어를 통해 crd의 대한 정보를 확인할 수 있습니다.

```bash
$ kubectl explain helloworlds
```

***이제 helloworld라는 kind를 가진 CR을 생성할 수 있습니다.***

## 2. CR 생성
CR을 아래와 같이 생성합니다.

각 필드의 대한 설명은 주석으로 대체 합니다.

```yaml
# CRD yaml에서 정의한 group 명/CRD yaml에서 정의한 version
# spec.group/spec.versions.name
apiVersion: "jjsair0412.example.com/v1"
# CRD yaml에서 정의한 kind 명
# spec.names.kind
kind: helloworld
metadata:
  name: hello-sample
# spec 은 필수 옵션
spec:
  # appId , replicas , message, image는 필수 옵션
  appId: "hello-world"
  replicas: 3
  message: "hi"
  image: nginx
  language: java
```

만약, 필수 옵션인 appId, replicas , message , image중 하나를 지정하지 않고 CR을 생성하면 다음과 같은 에러가 발생합니다.

1. 필수옵션 누락의 경우

```yaml
apiVersion: "jjsair0412.example.com/v1"
kind: helloworld
metadata:
  name: hello-sample
spec:
  # appId: "hello-world"
  replicas: 3
  message: "hi"
  image: nginx
  language: java
```

appId 누락되어 아래와 같은 에러 발생
```bash
The helloworld "hello-sample" is invalid: spec.appId: Required value
```

2. replicas 개수제한 안맞을 경우
replicas의 개수를 1~3개로 제한했기에 , 3개를 넘어가거나 1개보다 작으면 아래와 같은 에러가 발생합니다.

```yaml
apiVersion: "jjsair0412.example.com/v1"
kind: helloworld
metadata:
  name: hello-sample
spec:
  appId: "hello-world"
  replicas: 5
  message: "hi"
  image: nginx
  language: java
```

replicas가 5개라 아래와 같은 에러 발생

```bash
The helloworld "hello-sample" is invalid: spec.replicas: Invalid value: 5: spec.replicas in body should be less than or equal to 3
```

3. enum 에 값이 포함되지 않을경우
language 필드의 enum값에 지정한 값이 아닌 다른 값을 넣엇을 경우 , 에러가 발생합니다.

```yaml
apiVersion: "jjsair0412.example.com/v1"
kind: helloworld
metadata:
  name: hello-sample
spec:
  appId: "hello-world"
  replicas: 3
  message: "hi"
  image: nginx
  language: h
```

java , go , python중 한가지만 사용 가능
```bash
The helloworld "hello-sample" is invalid: spec.language: Unsupported value: "h": supported values: "java", "go", "python"
```

또한 기본적인 metadata를 변경시켜서 namespace별로 CR을 격리하거나 , 이름을 변경할 수 있습니다.

```yaml
apiVersion: "jjsair0412.example.com/v1"
kind: helloworld
metadata:
  name: hello-sample
  namespace: test
# required option
spec:
  # required option
  appId: "hello-world"
  replicas: 3
  message: "hi"
  image: nginx
  language: java
```

test namespace에 hello-sample CR이 생성되는것을 확인할 수 있습니다.

```bash
$ kubectl get hw -n test
NAME           AGE
hello-sample   6s
```
## 3. Custom controller 생성
이제 만들어진 CR이 k8s 기본 리소스인 Pod , Deployment , service 등과 연계해서 동작할 수 있도록 custom controller를 생성해야 합니다.

해당 문서에서는 golang을 통해 operator SDK로 개발합니다.

### 3.1 operator 설치
[operator SDK 공식 문서](https://sdk.operatorframework.io/docs/building-operators/golang/installation/)

#### **설치 필수 조건**
- git 설치
- go version 1.18 이상
- docker version 17.03+.
- kubectl and access to a Kubernetes cluster of a compatible version.

아래 공식 문서에서 나온 방안으로 설치합니다.
- [operator-sdk 설치 방안](https://sdk.operatorframework.io/docs/installation/)

macOS를 사용중이기 때문에 brew 명령어로 설치합니다.

```
$ brew install operator-sdk
```

아래 명령어로 설치결과 확인합니다.
```
$ operator-sdk version 
operator-sdk version: "v1.26.0", commit: "cbeec475e4612e19f1047ff7014342afe93f60d2", kubernetes version: "v1.25.0", go version: "go1.19.4", GOOS: "darwin", GOARCH: "arm64"
```

### 3.2 resource 정의
#### **필수 조건**
- cluster-admin 권한이 user에게 부여 되어 있어야 합니다.
- 다양한 운영자 이미지(예: hub.docker.com , quay.io )에 대한 액세스 가능한 이미지 레지스트리 및 명령줄 환경에 로그인됩니다.
  - example.com이 예제에서는 레지스트리 Docker Hub 네임스페이스로 사용됩니다. 다른 레지스트리 또는 네임스페이스를 사용하는 경우 다른 값으로 바꿉니다.

#### 3.2.1
프로젝트의 프로젝트 디렉터리를 만들고 프로젝트를 초기화합니다.

```bash
$ mkdir custom-operator-code
$ cd custom-operator-code
```


#### 3.2.2 api 생성
api를 create 합니다.

api를 operator-sdk로 생성합니다.
- 이전에 생성한 CRD yaml manifest를 확인하여 각 필드값에 맞는 값을 넣어줍니다.

```bash
# 사용 예
$ operator-sdk create api --version v1 --kind Hello --group mygroup

# 실제 수행 명령어
$ operator-sdk create api --version v1 --kind helloworld --group jjsair0412.example.com
```bash