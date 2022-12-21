# Custom Resource ( CR ) , Custom Resource Definition ( CRD ) 개발
kubernetes custom resources를 개발하는 과정에 대해 기술한 폴더 입니다.

개발하기 위해서 CR과 CRD 그리고 Operator의 개념을 짚고 넘어가야 하기 때문에 , 이론의 관련한 내용은 여기에 작성해 두었습니다.

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


## 2. Custom Resource ( cr ) 이란 ?
cr은 , k8s의 특정 object 모음을 저장해 둔 endpoint 입니다.
- 예를 들어 , pod resource에는 pod object 모음이 포함되어 있습니다.

cr은 동적으로 생성되거나 사라질 수 있으며 , cr이 생성되면 사용자는 pod와 같은 resource들을 kubectl 명령어로 api server에 요청을 보내어 컨트롤할  수 있습니다.

## 3. Custom Resource Definitions ( crd ) 란 ?
cr을 생성하여 k8s object를 컨트롤하기 위해선 , cr을 etcd에 등록해야만 합니다.

k8s etcd에 CR을 등록하기 위해 , CRD나 AAA( API Aggregation ) 를 사용합니다.

CRD는 CR을 통해 kubectl로 api server에 요청을 보내어 object를 컨트롤하기 위해서 etcd에 cr을 등록시킬 때 사용하는 명세 입니다.

## 4. API Aggregation ( AAA ) 이란 ?
AAA 또한 CRD와 동일하게 CR을 etcd에 등록할 때 사용하는 명세 입니다.

## 5. CRD VS AAA
AAA는 GO 언어를 통해 바이너리 이미지를 따로 만들어 주어야 하지만 , CRD는 yml파일로 간편하게 작성할 수 있습니다.

## 6. controller 란?

## 7. custom controller 란?

## 8. operator란 ?