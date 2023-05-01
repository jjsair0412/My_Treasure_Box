# Kong_with_k8s_ingress
해당 문서는 Kong gateway를 k8s의 ingress 로 사용하는 방안에 대해 기술합니다.

설치는 helm chart를 통해 진행합니다.

Kong ingress 테스트 내용은 다음 Docs를 참조하였습니다.
- [Get_Start_Kong_ingress_controller](https://docs.konghq.com/kubernetes-ingress-controller/2.9.x/guides/getting-started/)
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

proxy의 service type을 NodePort로 변경하는것 또한 가능합니다.
```bash
# Specify Kong proxy service configuration
proxy:
  # Enable creating a Kubernetes service for the proxy
  enabled: true
  type: NodePort # default로 LoadBalancer 등록되어있음
  loadBalancerClass
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

배포가 완료되면 , 아래와 같은 출력결과를 확인할 수 있습니다.

Kong의 LB proxy IP 및 포트를 다음 명령어들로 지정해줌으로써 외부 LB와 연동합니다.
- 현재는 베어메탈 환경이기 때문에 , 그냥 놔두고 NodePort처럼 사용합니다.
```bash
...
To connect to Kong, please execute the following commands:

HOST=$(kubectl get svc --namespace kong kong-kong-proxy -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
PORT=$(kubectl get svc --namespace kong kong-kong-proxy -o jsonpath='{.spec.ports[0].port}')
export PROXY_IP=${HOST}:${PORT}
curl $PROXY_IP
...
```

80:30682/TCP , 443:30431/TCP 포트로 curl 명령어를 날려보면 , Kong에 아직 route가 등록되지 않아서 request를 proxy할 경로를 모르기에 다음과 같은 response를 확인할 수 있습니다.
```bash
$ curl 127.0.0.1:30682
{"message":"no Route matched with those values"}
```

## 3. API Gateway 사용해보기
### 3.1 Test Pod Deploy
Kong Docs에서 있는 간단한 HTTP APP으로 테스트 합니다.

그대로 echo 명령어를 복사하여 배포합니다.
App 전문 yaml
```bash
echo "
apiVersion: v1
kind: Service
metadata:
  labels:
    app: echo
  name: echo
spec:
  ports:
  - port: 1025
    name: tcp
    protocol: TCP
    targetPort: 1025
  - port: 1026
    name: udp
    protocol: TCP
    targetPort: 1026
  - port: 1027
    name: http
    protocol: TCP
    targetPort: 1027
  selector:
    app: echo
---
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app: echo
  name: echo
spec:
  replicas: 1
  selector:
    matchLabels:
      app: echo
  strategy: {}
  template:
    metadata:
      creationTimestamp: null
      labels:
        app: echo
    spec:
      containers:
      - image: kong/go-echo:latest
        name: echo
        ports:
        - containerPort: 1027
        env:
          - name: NODE_NAME
            valueFrom:
              fieldRef:
                fieldPath: spec.nodeName
          - name: POD_NAME
            valueFrom:
              fieldRef:
                fieldPath: metadata.name
          - name: POD_NAMESPACE
            valueFrom:
              fieldRef:
                fieldPath: metadata.namespace
          - name: POD_IP
            valueFrom:
              fieldRef:
                fieldPath: status.podIP
        resources: {}
" | kubectl apply -f -
```

### 3.2 구성 그룹 만들기
ingress와 Gateway API controller는 경로구성을 알 수 있도록 가르키는 구성 집합이 필요합니다.

이를 통해서 하나의 클러스터에서 여러 컨트롤러가 있을 수 있기 때문에 , 각 서비스별 경로를 생성하기 전, **해당 경로들을 묶을 class configuration을 생성해야 합니다.**

그러나 helm chart로 Kong을 배포하였을 때는 기본적으로 같이 배포되기 때문에 , 이 과정을 생략할 수 있습니다.
```bash
# helm chart로 배포시 ingressClass를 확인해보면 잇는것을 확인할 수 있음.
$ kubectl get ingressClass
NAME   CONTROLLER                            PARAMETERS   AGE
kong   ingress-controllers.konghq.com/kong   <none>       11m

# yaml로 따로따로 배포했을 경우 , 아래 명령어로 ingressClass를 만들어 주어야 한다.
echo "
apiVersion: networking.k8s.io/v1
kind: IngressClass
metadata:
  name: kong
spec:
  controller: ingress-controllers.konghq.com/kong
" | kubectl apply -f 
```

### 3.3 Ingress Controller 생성하기
Kubernetes ingress controller는 기본적으로 IngressClass 및 GatewayClass를 인식하기 때문에 , Ingress를 생성할 때 ingressClassName을 명시해주면 해당 Ingress를 사용하게 됩니다.

만약 SSL 인증서를 등록하기 위해선 , nginx ingress와 동일하게 spec.tls 칸에 추가해주면 됩니다.

Kong에서 지원하는 Annotation 종류는 다음 문서에 자세히 작성되어 있습니다.
- [공식 Docs Anntation List](https://docs.konghq.com/kubernetes-ingress-controller/latest/references/annotations/)
아래는 test ingress 전문
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: echo
  annotations:
    konghq.com/strip-path: 'true'  # Ingress 리소스에 정의된 경로를 제거한 다음 요청을 업스트림 서비스로 전달
spec:
  ingressClassName: kong # ingressClassName , Kong
  rules:
  - host: kong.example # Domain Name
    http:
      paths:
      - path: /echo # route
        pathType: ImplementationSpecific
        backend:
          service:
            name: echo # service_name
            port:
              number: 1027 # service_port
  tls:
  - hosts: 
    - Kong.example
    secretName : tls_secret_name

# apply 
kubectl apply -f -
```

그대로 복사해서 배포합니다.
```bash
$  kubectl get ing -A
NAMESPACE   NAME   CLASS   HOSTS          ADDRESS       PORTS   AGE
default     echo   kong    kong.example   10.104.73.3   80      7s
```

etc/hosts파일에 proxyIP kong.example을 등록한 뒤 , proxy port를 달아서 /echo route로 들어가봅니다.
```bash
$ curl kong.example:30682/echo
Welcome, you are connected to node worker-1.
Running on Pod echo-74d47cc5d9-qfpwv.
In namespace default.
With IP address 192.168.226.66.
```