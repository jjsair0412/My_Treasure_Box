# Metric Server
Kubernetes HPA를 구현하기 위해 Metric Server를 구축함.

쿠버네티스 Metrics Server는 클러스터의 kubelet으로부터 리소스 메트릭을 수집하고, 수집한 메트릭을 쿠버네티스 API를 통해 노출시키며, 메트릭 수치를 나타내는 새로운 종류의 리소스를 추가하기 위해 APIService를 사용할 수 있다.
- [HPA 관련 공식문서](https://kubernetes.io/ko/docs/tasks/run-application/horizontal-pod-autoscale-walkthrough/)

## 1. Metric Server 설치
아래 github url에 위치한 yaml파일로 간단히 설치할 수 있습니다.

```bash
$ wget https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

**만약 EKS와 같은 관리형 Kubernetes Cluster이거나, Custom CNI를 사용중이라면, metrics-server Deployment에 다음 옵션값들을 추가해주어야 합니다. !**
- [관련 StackOverflow issue](https://stackoverflow.com/questions/74616394/how-to-resolve-failing-or-missing-response-address-is-not-allowed-from-custom)

```bash
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    k8s-app: metrics-server
  name: metrics-server-jinseong
  namespace: kube-system
spec:
  selector:
    matchLabels:
      k8s-app: metrics-server
  strategy:
    rollingUpdate:
      maxUnavailable: 0
  template:
    metadata:
      labels:
        k8s-app: metrics-server
    spec:
      hostNetwork: true # 추가
      dnsPolicy: ClusterFirstWithHostNet # 추가
      containers:
      - args:
        - --cert-dir=/tmp
        - --secure-port=4443
        - --kubelet-preferred-address-types=InternalIP,ExternalIP,Hostname
        - --kubelet-use-node-status-port
        - --metric-resolution=15s
        - --kubelet-insecure-tls # 추가
        - --kubelet-preferred-address-types=InternalIP
        image: registry.k8s.io/metrics-server/metrics-server:v0.6.4
...
```

kubectl apply 명령어로 배포합니다.

```bash
$ kubectl apply -f components.yaml
```

top 명령어를 통해 metric이 정상 수집되는지 확인해 봅니다.

```bash
$ kubectl top nodes
```