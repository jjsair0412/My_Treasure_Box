# CRD 개발 - golang
해당 폴더에서는 실제 cr , crd를 yaml파일로 정의한 뒤 , ( etcd에 등록 ) 

golang용 operator를 커스터마이징 하여 해당 cr를 통해서 k8s object를 컨트롤하는 실습 입니다.

## 0. prerequisite
참고 문서
- [CRD 생성관련 공식문서](https://kubernetes.io/docs/tasks/extend-kubernetes/custom-resources/custom-resource-definitions/)
- [by devjh의 블로그](https://frozenpond.tistory.com/111)

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
helloworld kind를 가진 resource를 생성하면

1. nginx image를 가진 deployment 배포
2. 해당 deployment를 expose하는 서비스 생성
- service type은 NodePort 사용
3. log로 "hello world i'm jinseong" 출력

를 한번에 할 수 있게끔 하는 custom resource를 개발합니다.

해당 CRD는 namespace에 종속적이게끔 개발합니다.

kubectl로 상태를 확인할 때 , "hw" 라는 단축키로 kube-api server에 요청을 보낼 수 있게 끔 개발합니다.

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

structural schema 검사에 관련한 문서는 한번 읽어봅시다.
- URL : https://kubernetes.io/docs/tasks/extend-kubernetes/custom-resources/custom-resource-definitions/#specifying-a-structural-schema

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

***이제 helloworld라는 kind로 CR을 생성할 수 있습니다.***


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
```


kubectl apply로 CR을 etcd에 정의 합니다.

```bash
$ kubectl apply -f cr.yaml 
helloworld.jjsair0412.example.com/hello-sample created
```

kubectl get 명령어로 pod나 deploy와 같은 k8s object 처럼 CR을 관리할 수 있습니다.

describe와 같은 명령어도 작동 합니다.

```bash
$ kubectl get hw
NAME           AGE
hello-sample   10m

$ kubectl describe hw hello-sample
```

CR을 kubectl 명령어로 조회나 삭제 , 수정 등록 등은 가능하지만 , etcd에 미리 등록되어있는 pod나 deploy와 같은 k8s resource를 조작할 수는 없습니다.

따라서 operator를 custom 합니다.

## 3. operator 사용
git 명령어로 operator 가져옵니다.

```bash
$ git clone https://github.com/operator-framework/operator-sdk
```

operator로 이동한 뒤 