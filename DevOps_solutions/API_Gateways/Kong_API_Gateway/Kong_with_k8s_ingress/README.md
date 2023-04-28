# Kong_with_k8s_ingress
해당 문서는 Kong gateway를 k8s의 ingress 로 사용하는 방안에 대해 기술합니다.

설치는 helm chart를 통해 진행합니다.
## Precondition
- 설치 환경

|name|version|
|--|--|
|os|ubuntu 22.04|
|k8s version|v1.27.1|
|container runtime|containerd://1.6.20|

- [kong kubernetes ingress 공식 docs](https://docs.konghq.com/kubernetes-ingress-controller/latest/#main)

## 1. 동작 방식
- [관련 정보 공식 docs](https://docs.konghq.com/kubernetes-ingress-controller/2.9.x/concepts/design/)

Kong의 kubernetes ingress controller는 핵심 두가지 요소로 동작합니다.
- 모든 트래픽 처리를 담당하는 **핵심 프록시 Kong**
- Kubernetes에서 Kong 구성을 동기화하는 일련의 프로세스인 **Controller Manager**

Kong Kubernetes ingress controller는 단순하게 Kubernetes cluster에 들어오는 트래픽을 프록시하는 기능에서 그치지 않고 , **Kong plugin을 구성**하고 , **로드벨런싱**하며 , **Health checking** 하는 등 Kong에 관련한 것을 독립적으로 관리할 수 있게끔 도와줍니다.

![Kong_ingress_controller][Kong_ingress_controller]

[Kong_ingress_controller]:./images/Kong_ingress_controller.png

## 2. Kong Helm 설치
Helm chart로 Kong을 설치합니다.
```bash
# or using Helm
$ helm repo add kong https://charts.konghq.com
$ helm repo update

# Helm 3
$ helm pull kong/kong --untar

# helm pull 결과 확인
$ ls
CHANGELOG.md  Chart.lock  Chart.yaml  FAQs.md  README.md  UPGRADE.md  charts  ci  crds  example-values  templates  values.yaml
```

