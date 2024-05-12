# Kind Cluster With Ingress Controller
## Overview
Kind Cluster를 프로비저닝할 때 , Ingress와 같이 설치되게끔 합니다.

## 1. Kind Cluster 생성
먼저 Kind Cluster를 생성합니다.

```extraPortMappings``` , ```node-labels``` 옵션을 아래와 같이 설정해야 합니다.
- 로컬 호스트가 80, 443 포트로 Ingress Controller에 요청해야 하기 때문에, ```extraPortMappings``` Allow
- Ingress-Controller가 특정 노드에서만 실행되도록 ```node-labels``` 설정

```kubeadmConfigPatches``` 설정으로 MasterNode에 node-labels의 ```ingress-ready=true``` 설정을 꼭 넣어주어야 합니다.
- 만약 설정을 빼놓고 클러스터를 프로비저닝 했다면, ```kubectl label ...``` 명령어로 적용해도 무관합니다.
- [관련 issues](https://github.com/kubernetes-sigs/kind/issues/3226)

```bash
kubectl label nodes kind-control-plane ingress-ready=true
```

```yaml
cat <<EOF | kind create cluster --config=-
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
- role: control-plane
  kubeadmConfigPatches:
  - |
    kind: InitConfiguration
    nodeRegistration:
      kubeletExtraArgs:
        node-labels: "ingress-ready=true"
  extraPortMappings:
  - containerPort: 80
    hostPort: 80
    listenAddress: "0.0.0.0" # Optional, defaults to "0.0.0.0"
    protocol: TCP
  - containerPort: 443
    hostPort: 443
    listenAddress: "0.0.0.0" # Optional, defaults to "0.0.0.0"
    protocol: TCP
- role: worker
EOF
```

## 2. Ingress Nginx 설치 및 확인

이후 Nginx Ingress Controller를 설치합니다.
```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
```

모든 파드가 정상작동중인지 확인합니다.
```bash
$ kubectl get all -n ingress-nginx
NAME                                            READY   STATUS      RESTARTS   AGE
pod/ingress-nginx-admission-create-xc64w        0/1     Completed   0          102s
pod/ingress-nginx-admission-patch-5q98r         0/1     Completed   0          102s
pod/ingress-nginx-controller-7b76f68b64-pc9nw   1/1     Running     0          102s

NAME                                         TYPE        CLUSTER-IP     EXTERNAL-IP   PORT(S)                      AGE
service/ingress-nginx-controller             NodePort    10.96.198.91   <none>        80:32137/TCP,443:31484/TCP   102s
service/ingress-nginx-controller-admission   ClusterIP   10.96.97.172   <none>        443/TCP                      102s

NAME                                       READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/ingress-nginx-controller   1/1     1            1           102s

NAME                                                  DESIRED   CURRENT   READY   AGE
replicaset.apps/ingress-nginx-controller-7b76f68b64   1         1         1       102s

NAME                                       COMPLETIONS   DURATION   AGE
job.batch/ingress-nginx-admission-create   1/1           15s        102s
job.batch/ingress-nginx-admission-patch    1/1           15s        102s
```

ingressclass 확인
```bash
$ kubectl get ingressclass
NAME    CONTROLLER             PARAMETERS   AGE
nginx   k8s.io/ingress-nginx   <none>       3m19s
```

## 3. Ingress 사용
간단하게 Nginx를 Ingress로 프로비저닝하는 yaml template을 배포해봄으로써 정상 작동하는지 확인합니다.

```bash
apiVersion: apps/v1
kind: Deployment
metadata:
  name: deploy-ingress
spec:
  replicas: 2
  selector:
    matchLabels:
      app: deploy-ingress
  template:
    metadata:
      labels:
        app: deploy-ingress
    spec:
      terminationGracePeriodSeconds: 0
      containers:
      - name: deploy-ingress
        image: nginx:alpine
        ports:
        - containerPort: 80
---
kind: Service
apiVersion: v1
metadata:
  name: deploy-ingress
spec:
  type: ClusterIP
  selector:
    app: deploy-ingress
  ports:
  - port: 80
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: deploy-ingress
spec:
  ingressClassName: nginx
  rules:
  - host: jinseong.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: deploy-ingress
            port:
              number: 80
```

```/etc/hosts``` 에 설정한 host 경로를 넣어줍니다.

```bash
sudo vi /etc/hosts
...
127.0.0.1 jinseong.com
...
```

curl 요청으로 nginx에 정상 접근 되는지 확인 합니다.
```bash
$ curl jinseong.com
<!DOCTYPE html>
<html>
<head>
<title>Welcome to nginx!</title>
<style>
html { color-scheme: light dark; }
body { width: 35em; margin: 0 auto;
font-family: Tahoma, Verdana, Arial, sans-serif; }
</style>
</head>
<body>
<h1>Welcome to nginx!</h1>
<p>If you see this page, the nginx web server is successfully installed and
working. Further configuration is required.</p>

<p>For online documentation and support please refer to
<a href="http://nginx.org/">nginx.org</a>.<br/>
Commercial support is available at
<a href="http://nginx.com/">nginx.com</a>.</p>

<p><em>Thank you for using nginx.</em></p>
</body>
</html>
```

## 4. Kind cluster 정리
아래 명령어로 프로비저닝한 Kind Cluster를 제거합니다.
```bash
kind delete cluster
```