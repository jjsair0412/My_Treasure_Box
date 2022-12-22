# Custom Resource ( CR ) , Custom Resource Definition ( CRD ) 개발
kubernetes custom resources를 개발하는 과정에 대해 기술한 폴더 입니다.

개발하기 위해서 CR과 CRD 그리고 Operator의 개념을 짚고 넘어가야 하기 때문에 , 이론의 관련한 내용은 여기에 작성해 두었습니다.

또한 CRD를 해당 문서처럼 개발하기 위해선 , **k8s version 1.7** 이상이여야만 합니다.
그 이전 버전에서는 CRD가 베타버전이거나 없습니다.

## INDEX
0. [결론](#결론)
1. [Kubernetes Object란 ?](#1-kubernetes-object란)
2. [Custom Resource란 ?](#2-custom-resource--cr--이란)
3. [Custom Resource Definition 이란 ?](#3-custom-resource-definitions--crd--란)
4. [AAA ( API Aggregation ) 란 ?](#4-api-aggregation--aaa--이란)
5. [CRD VS AAA](#5-crd-vs-aaa)
6. [controller 란 ?](#6-controller-란)
7. [custom controller란 ?](#7-custom-controller-란)
8. [operator란 ?](#8-operator란)

해당 문서의 개발 방안이나 실습 과정 등은 , 아래의 참고 문서들을 참고하였습니다.

## 0. 참고 문서
- [k8s object에 관련한 docs](https://kubernetes.io/ko/docs/concepts/overview/working-with-objects/kubernetes-objects/)
- [Custom resource 란 ?](https://kubernetes.io/ko/docs/concepts/extend-kubernetes/api-extension/custom-resources/)
- [controller 란 ?](https://kubernetes.io/ko/docs/concepts/architecture/controller/)
- [k8s operator](https://kubernetes.io/ko/docs/concepts/extend-kubernetes/operator/#writing-operator)
- [devjh님의 블로그 - cr ,crd 개발 프로세스 학습](https://frozenpond.tistory.com/111)
- [Kubernetes의 확장인 CRD Custom Resource Definition 와 CR Custom Resource 에 대한 개념 정리](https://ccambo.tistory.com/m/entry/Kubernetes-%ED%99%95%EC%9E%A5%EC%9D%B8-CRD%EC%99%80-CR-%EC%97%90-%EB%8C%80%ED%95%9C-%EA%B0%9C%EB%85%90-%EC%A0%95%EB%A6%AC)


# 결론
먼저 , k8s cr과 crd의 관계를 결론지은 후 각 요소들에 대한 설명을 읽어보면 더 좋기 때문에 , 결론부터 작성합니다.

### **kubernetes은 상태 관리 시스템입니다.**

만약 deployment의 replicas 개수가 3개에서, 2개로 변경되었다고 생각해 봅시다.
이때 생성되어있는 pod는 3개에서 2개로 변경 되어야 할 것입니다.

이러한 k8s object ( pod , deployment ,,, ) 의 상태를 변경시키는것이 controller 입니다.
controller의 role을 정리하면 다음과 같습니다.
- Kubernetes는 Resource들의 변경을 감시하고 있다.
- 변경이 감지되면 Kubernetes는 관련된 이벤트를 발생시킨다.
- 발생된 이벤트는 Controller의 Reconcile 함수로 전달된다.
- Reconcile 함수에서 전달된 이벤트 데이터에 따라 Current State를 Desired State로 맞추기 위한 작업을 진행한다.

### **kubernetes의 모든 resource는 GVR로 식별됩니다.**

GVR이란 , Group , Version , Resource 의 앞 글자의 조합 입니다.
예를들어 deployment spec을 보면 , GVR의 구조를 가진것을 알 수 있습니다.
- apiVersion: apps/v1 <<- Group이 apps, Version이 V1 kind: Deployment <<- Resource가 Deployment ...

pod나 deployment 모두 kubernetes의 resource로 controller의 관리 대상이 됩니다.


### ***그렇다면 CR과 CRD는 무엇일까 ?***
kubernetes에서는 실제 application이 동작하거나 운영할 때 필요한 기본적인 resource들만을 제공하고 있습니다.
(Pod , Deployment , service)

***prometheus를 기준으로 CRD 개념을 설명해 보면 ,***

만약 k8s의 기본 resource만을 기준으로 메트릭을 수집하게 된다면 , 수집대상 정보나 규칙이 많아질수록 prometheus의 설정이 복잡해질 것입니다.
또한 대상 시스템에 따라 , 하나 이상의 promehteus를 실행하여 연계하거나 , 각각 메트릭 수집을 분리하여 운영하는 등의 작업이 필요할 것 입니다.

운영자 입장에선 , 너무 불편합니다.
이런 작업들이 자동화 되면 좋겟는데 ..
그러나 k8s는 상태관리 시스템이기에 , 자동화를 위한 상태 정보를 관리할 대상이 없기 때문에 k8s는 이런 작업을 할 수 없습니다.

#### ***그래서 CRD는 .. ?***
그래서 , k8s는 CRD를 사용합니다.

개발 언어 (여기서는 Go) 관점에서 풀어보면 구조체 Structure 를 오브젝트 데이터 관리용으로 사용합니다. 구조체에서 관리할 데이터 항목과 형식을 지정하며, 관리할 데이터를 정의 합니다.

```go
type struct HelloSpec {
  Message string    `json:"message"`
  ...
}
```

operator를 통해 해당 object를 생성해 주엇다 하더라도 , k8s는 이 object를 인식하지 못하기에 GVR 형식으로 K8S에 등록 시켜 주어야 하는데 , 즉 !

**Kubernetes에 사용자가 정의한 오브젝트(Kubernetes에서는 리소스)에 대한 이름과 형식과 사이즈등의 데이터 관리 정보를 GVR 기준으로 정의한 것이 CRD** 입니다.


위의 go struct를 바탕으로 CRD를 생성해 보면 , 아래와 같습니다.

```yaml
apiVersion: apiextensions.k8s.io/v1        # Kubernetes에서 제공하는 CRD용 Group과 Version
kind: CustomResourceDefinition                # Kubernetes에서 제공하는 Resource
metadata:
  name: hellos.examples.com              # CRD 식별명
spec:
  group: examples.com                # Group
  versions: 
    - name: v1alpha1        # Version
      served: true
      storage: true
      schema:
        openAPIV3Schema:
          type: object
          properties:
            spec:
              type: object
              properties:
                message:        # 필드명 ! 위의 struct에서 정의한 필드에 대응된다.
                  type: string  # 필드 형식
                replicas:
                  type: integer
                  minimum: 1
                  maximum: 10
  names:
    kind: Hello                            # Resource
    plural: hellos                    # List 등으로 표현할 Resource의 복수형
  scope: Namespaced                    # Namespace 범위로 한정
```

그러나 , CRD만을 생성했다 해서 , 할수있는건 아무것도 없습니다.

#### ***그래서 CR은 .. ?***

**따라서 , 명세서 (CRD) 를 작성했으니 kubernetes가 해당 명세의 실제 상태정보를 관리할 수 있는 object가 바로 CR 입니다.**

개발 언어 면으로 이해한다면 , CRD는 class이고 , CRD라는 class로 만들어낸 객체가 바로 CR 입니다.

위 CRD를 바탕으로 생성해낸 CR은 다음과 같아질 것입니다.

```yaml
apiVersion: examples.com/v1alpha1        # CRD에서 지정한 Group/Version
kind: Hello                                                    # CRD에서 지정한 Resource Kind
metadata:
  name: my-new-hello-object                    # CR 식별명 (오브젝트 인스턴스 식별)
spec:
  message: "hi crd!!"                                # 필드에 저장할 값
```

그러나 , 여기까지 만들어주엇다 해서 k8s 기본 리소스인 pod나 deployment와 함께 연계해서 동작하진 않습니다.

### **중요**
위에서 , k8s는 상태 관리 시스템이라 하였습니다.

controller가 object의 상태변화를 감시하다가 , 변화되면 k8s resource와 연계하여 동작 ( replica 개수 맞추기 ) 합니다.

따라서 , 해당 CR에 상태 변화를 감시하고 , 상태가 변화하면 k8s resource와 연계하여 작동하는 custom controller가 필요하게 됩니다.

이때 상태가 변화한다는 이야기는 , 위에 예를 빗대어 설명하자면 ```yaml sepc.message``` 필드의 값이 "hi crd!!" 에서 , "hello world" 값으로 변경되는것 을 이야기 합니다.
- replica의 갯수가 변경되는것과 동일

## 1. Kubernetes Object란 ?
k8s object는 k8s에서 영 속성을 가진 요소입니다.

cluster 상태를 나타내기 위해서 사용되며 , 다음과 같이 설명할 수 있습니다.
- 어떤 컨테이너화된 애플리케이션이 동작 중인지 (그리고 어느 노드에서 동작 중인지)
- 그 애플리케이션이 이용할 수 있는 리소스
- 그 애플리케이션이 어떻게 재구동 정책, 업그레이드, 그리고 내고장성과 같은 것에 동작해야 하는지에 대한 정책

object는 "어떤 의도를 가진 레코드" 라 할 수 있으며 , k8s system은 해당 object 생성을 보장하기 위해서 지속적으로 작동합니다.

object를 생성함으로써 , cluster workroad를 어떤 형태로 보이고자 하는 지에 대해 효과적으로 k8s 시스템에 전달 합니다.
- 따라서 k8s 사용자가 의도한 상태가 됩니다.

**또한 k8s 사용자는 object를 컨트롤하기 위해 k8s api server에게 요청을 보내게 됩니다.**

### 1.1 object spec ( 명세 ) 와 state ( 상태 )
거의 모든 k8s  object들은 spec과 state 필드를 포함합니다.

1. state
    - 쿠버네티스 시스템과 컴포넌트에 의해 제공되고 업데이트된 오브젝트의 현재 상태 를 설명합니다.
2. spec
    - object의 포멧을 결정짓습니다.

### 1.2 k8s object의 종류
pod , deployment , daemonset .. 등 spec과 state를 가지는 것들이 있습니다.


## 2. Custom Resource ( cr ) 란 ?
cr은 , k8s의 특정 object 모음을 저장해 둔 endpoint 입니다.
- 예를 들어 , pod resource에는 pod object 모음이 포함되어 있습니다.

cr은 동적으로 생성되거나 사라질 수 있으며 , cr이 생성되면 사용자는 pod와 같은 resource들을 kubectl 명령어로 api server에 요청을 보내어 컨트롤할  수 있습니다.

## 3. Custom Resource Definitions ( crd ) 이란 ?
cr을 생성하여 k8s object를 컨트롤하기 위해선 , cr을 etcd에 등록해야만 합니다.

k8s etcd에 CR을 등록하기 위해 , CRD나 AAA( API Aggregation ) 를 사용합니다.

CRD는 CR을 통해 kubectl로 api server에 요청을 보내어 object를 컨트롤하기 위해서 etcd에 cr을 등록시킬 때 사용하는 명세 입니다.

## 4. API Aggregation ( AAA ) 란 ?
AAA 또한 CRD와 동일하게 CR을 etcd에 등록할 때 사용하는 명세 입니다.

## 5. CRD VS AAA
AAA는 GO 언어를 통해 바이너리 이미지를 따로 만들어 주어야 하지만 , CRD는 yml파일로 간편하게 작성할 수 있습니다.

## 6. controller 란?
software 계층에서 controller는 쌓여진 요청을 처리하는 장치를 의미합니다. ( spring에서의 controller , device controller 등 ..)

k8s controller는 etcd를 감시하여 선언된 object api에 맞게 의도하는 상태로 맞추어주려 노력하는 컴포넌트 입니다.

예를 들어 , pod를 생성하여 etcd에 넣어 놓으면 , 해당 변경 사항을 controller가 감시하여 컨테이너를 띄웁니다.

또한 controller는 object의 설정을 업데이트 합니다.
- job이 종료되면 controller가 job object가 finished로 표시되도록 업데이트

## 7. custom controller 란?
일반 controller 는 pod와 같이 api object를 컨트롤 합니다.

그러나 custom controller는 custom resource ( cr ) 을 컨트롤 하는 컴포넌트 입니다.

cr과 crd를 이용하여 생성한 cr은 , etcd에 등록되는 구조화된 데이터 명세일 뿐이고 , cr을 통해 object을 컨트롤하기 위해선 custom controller가 필요합니다.

cr을 이용하여 사용자의 의도인 object 상태 (state)를 선언 (etcd에 등록) 하면 , custom controller가 그 상태를 맞추어 주기 위해 동작합니다.
- object의 설정을 업데이트 및 object 컨트롤

## 8. operator 란 ?
- CR의 컨트롤러 역할을 할 수 있는 쿠버네티스 API 서버의 클라이언트(개발 패턴, 익스텐션을 칭하기도합니다.)
- k8s 컨트롤러 개념을 통해 쿠버네티스 코드를 수정하지않고 클러스터의 동작을 확장합니다.
- 컨트롤러의 역할을 할 뿐 아니라 쿠버네티스 운영에 필요한 모든것을 포함합니다.

k8s는 operator를 제공하는데 , k8s operator는의 클라이언트를 붙여 서버 동작을 제어할 수 있는 컴포넌트 입니다.

쿠버네티스의 소스를 몰라도 operator를 만들어놓으면 cr과 crd에 의한 etcd의 변경을 감지하고 쿠버네티스에 원하는 동작을 하게 할 수 있습니다.

### 8.1 operator 개발 방법
[operator 종류](https://kubernetes.io/ko/docs/concepts/extend-kubernetes/operator/#writing-operator)

위 링크를 타고가서 각 언어별 ( ruster , java , python , go 등 ...) operator를 확장 개발할 수 있습니다.
제공되는 sdk 및 프레임워크를 커스텀하여 개발합니다.