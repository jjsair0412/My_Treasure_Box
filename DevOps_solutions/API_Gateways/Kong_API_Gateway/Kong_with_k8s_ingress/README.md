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

kong api gateway 관리용 namespace를 생성합니다.
```bash
$ kubectl create ns kong 
```

helm upgrade 명령어로 kong api gateway를 설치합니다.

kong의 values.yaml에 아무런 설정을 두지 않았기 때문에 , LoadBalancer type으로 설치됩니다.
- LB type은 NodePort의 확장이기에 , 클라우드 환경이 아니라 앞단 LB가 프로비저닝되지 않더라도 크게 문제되지 않습니다.
```bash
$ helm upgrade --install kong . -n kong

# 설치결과 확인
$ kubectl get all -n kong
NAME                             READY   STATUS    RESTARTS   AGE
pod/kong-kong-5cf79c5854-2nfbv   2/2     Running   0          3h1m

NAME                                   TYPE           CLUSTER-IP      EXTERNAL-IP   PORT(S)                      AGE
service/kong-kong-proxy                LoadBalancer   10.111.68.160   <pending>     80:30682/TCP,443:30431/TCP   3h1m
service/kong-kong-validation-webhook   ClusterIP      10.97.213.43    <none>        443/TCP                      3h1m


NAME                        READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/kong-kong   1/1     1            1           3h1m

NAME                                   DESIRED   CURRENT   READY   AGE
replicaset.apps/kong-kong-5cf79c5854   1         1         1       3h1m
```

80:30682/TCP , 443:30431/TCP 포트로 curl 명령어를 날려보면 , Kong에 아직 route가 등록되지 않아 다음과 같은 출력을 확인할 수 있습니다.
```bash
$ curl 127.0.0.1:30682
{"message":"no Route matched with those values"}
```