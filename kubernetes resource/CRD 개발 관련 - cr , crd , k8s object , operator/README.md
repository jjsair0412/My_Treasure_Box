# Custom Resource ( CR ) , Custom Resource Definition ( CRD ) 개발
kubernetes custom resources를 개발하는 과정에 대해 기술한 폴더 입니다.

개발하기 위해서 CR과 CRD 그리고 Operator의 개념을 짚고 넘어가야 하기 때문에 , 이론의 관련한 내용은 여기에 작성해 두었습니다.

또한 CRD를 해당 문서처럼 개발하기 위해선 , **k8s version 1.7** 이상이여야만 합니다.
그 이전 버전에서는 CRD가 베타버전이거나 없습니다.

## INDEX
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
- [devjh님의 블로그](https://frozenpond.tistory.com/111)


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