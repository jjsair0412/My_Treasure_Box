# Apple Silicon M칩에서 DND로 Kubernetes 구축
## Overview
Intel 칩이 아닌 ARM 계열 CPU일 경우, VirtualBox + Vagrant 로 VM을 구축하는데 많은 어려움이 있음.

그에 따라 Docker Container로 Kubernetes를 구축하는것이 가장 편리하기에 , 아래와 같은 방법으로 Kubernetes를 구축함.

구축할때는 Kind 라는 Kubernetes 프로비저닝 툴을 사용함. 이는 docker container를 노드로 사용함.
- [공식 문서](https://kind.sigs.k8s.io/)

## 1. prerequired
먼저 아래 명령어로 어떤 칩을 사용중인지 확인
- 사용중 브렌드 정보
```bash
$ sysctl -a | grep -i brand
machdep.cpu.brand_string: Apple M2
```

- 사용중 CPU 아키텍쳐 정보
```bash
$ arch
arm64
```

또한 Kind를 사용하는 도커 엔진 리소스에는 , 최소 vCPU 4, Memory 8GB 이상을 할당하는것을 권고함.
- [관련 문서](https://kind.sigs.k8s.io/docs/user/quick-start/#settings-for-docker-desktop)

그리고 Docker Desktop이 설치된 환경에서 사용하는것이 좋음.
- CPU , Memory 설정이 편해서..

docker hub에서 각 아키텍처 별 컨테이너 실행 확인
- ubuntu 이미지를 사용함.
```bash
# arm64v8 실행 성공!
$ docker run --rm -it arm64v8/ubuntu arch
aarch64
```

다른 아키텍쳐의 ubuntu 이미지로 실행할 경우, 실행 실패
```bash
# arm64v8 실행 시도
$ docker run --rm -it amd64/ubuntu arch
WARNING: The requested image's platform (linux/amd64) does not match the detected host platform (linux/arm64/v8) and no specific platform was requested
x86_64
```

```bash
# riscv64 실행 시도
$ docker run --rm -it riscv64/ubuntu arch
WARNING: The requested image's platform (linux/riscv64) does not match the detected host platform (linux/arm64/v8) and no specific platform was requested
riscv64
```

kind의 경우 ARM 아키텍처 컨테이너 이미지를 제공하기 때문에,. Apple 실리콘 칩에서 Kind로 Kubernetes Cluster를 구축하는것이 가능함.

## 2. Kind 설치
먼저 Kind 툴을 brew로 설치함
```bash
$ brew install kind
...설치중...
$ kind --version
kind version 0.22.0

# Install kubectl
$ brew install kubernetes-cli
$ kubectl version --client=true

# Install Helm
$ brew install helm
$ helm version
```
 
## 3. Kind로 클러스터 배포 및 확인
클러스터 배포 전 , docker container 상태를 확인해 봅니다.
```bash
$ docker ps
```

클러스터를 배포합니다.
```bash
$ kind create cluster
```

배포된 Kubernetes 클러스터를 확인합니다.

```bash
# 클러스터 배포 확인
$ kind get clusters
kind

$ kind get nodes
kind-control-plane

$ kubectl cluster-info
Kubernetes control plane is running at https://127.0.0.1:59981
CoreDNS is running at https://127.0.0.1:59981/api/v1/namespaces/kube-system/services/kube-dns:dns/proxy

To further debug and diagnose cluster problems, use 'kubectl cluster-info dump'.
```

노드 정보도 확인해 봅니다.

```bash
$ kubectl get nodes -o wide
NAME                 STATUS   ROLES           AGE    VERSION   INTERNAL-IP   EXTERNAL-IP   OS-IMAGE                         KERNEL-VERSION        CONTAINER-RUNTIME
kind-control-plane   Ready    control-plane   113s   v1.29.2   172.18.0.2    <none>        Debian GNU/Linux 12 (bookworm)   5.15.49-linuxkit-pr   containerd://1.7.13
```

파드 정보를 확인해 봅니다.
- coredns , etcd , kube-apiserver , kube-proxy 등 정상적으로 동작하고잇는게 보입니다.

```bash
$ kubectl get pod -A
NAMESPACE            NAME                                         READY   STATUS    RESTARTS   AGE
kube-system          coredns-76f75df574-cwczf                     1/1     Running   0          2m5s
kube-system          coredns-76f75df574-xhmhp                     1/1     Running   0          2m5s
kube-system          etcd-kind-control-plane                      1/1     Running   0          2m22s
kube-system          kindnet-vt2cs                                1/1     Running   0          2m6s
kube-system          kube-apiserver-kind-control-plane            1/1     Running   0          2m21s
kube-system          kube-controller-manager-kind-control-plane   1/1     Running   0          2m21s
kube-system          kube-proxy-29w5v                             1/1     Running   0          2m6s
kube-system          kube-scheduler-kind-control-plane            1/1     Running   0          2m21s
local-path-storage   local-path-provisioner-7577fdbbfb-gl9hz      1/1     Running   0          2m5s
```

실제적으로 도커 컨테이너는 마스터노드 컨테이너 한대만 프로비저닝된것을 확인할 수 있습니다.
```bash
$ docker ps
CONTAINER ID   IMAGE                  COMMAND                   CREATED         STATUS         PORTS                       NAMES
e1234b7192d8   kindest/node:v1.29.2   "/usr/local/bin/entr…"   3 minutes ago   Up 3 minutes   127.0.0.1:59981->6443/tcp   kind-control-plane
```

Kube config파일을 확인해 보면 , Kind 클러스터의 Kube-apiserver로 연결되어있는것을 확인할 수 있습니다.
```bash
$ cat ~/.kube/config 
```

## 4. Kind 상세 정보 확인
Kind Cluster는 Docker In Docker로 Kubernetes를 프로비저닝 합니다.

또한 Kind는 별도의 docker network(bridge Type) 를 생성해서 사용합니다.
- network name : kind
```bash
$ docker network ls
NETWORK ID     NAME            DRIVER    SCOPE
1a7abd7c404c   host            host      local
63385dec931c   kind            bridge    local
...
```

또한 기본적으로 도커 컨테이너를 통해 Kubernetes를 프로비저닝 하기 때문에, 이름만 다르게 생성한다면 여러개의 클러스터를 프로비저닝할 수 있습니다.

```bash
# myk8s 이름의 클러스터 프로비저닝
$ kind create cluster --name myk8s
...

# 총 2개의 마스터노드 컨테이너가 생성된것을 확인할 수 있습니다.
$ docker ps
CONTAINER ID   IMAGE                  COMMAND                   CREATED              STATUS              PORTS                       NAMES
02940fa1b771   kindest/node:v1.29.2   "/usr/local/bin/entr…"   About a minute ago   Up About a minute   127.0.0.1:60045->6443/tcp   myk8s-control-plane
e1234b7192d8   kindest/node:v1.29.2   "/usr/local/bin/entr…"   8 minutes ago        Up 8 minutes        127.0.0.1:59981->6443/tcp   kind-control-plane
```

get nodes 명령어로 특정 클러스터를 지정할 수 있습니다.
```bash
$ kind get nodes --name myk8s
myk8s-control-plane
$ kind get nodes             
kind-control-plane
```

CRI와 CNI를 확인해보면, 각각 
- CRI : containerd
- CNI : kindnet 
을 사용하는것을 볼 수 있습니다.
```bash
# CRI 는 containerd 사용
$ kubectl get node -o wide

# CNI 는 kindnet 사용
$ kubectl get pod -A
```

스토리지는 local-path 라는 StorageClass가 설치됩니다.
```bash
$ kubectl get sc
NAME                 PROVISIONER             RECLAIMPOLICY   VOLUMEBINDINGMODE      ALLOWVOLUMEEXPANSION   AGE
standard (default)   rancher.io/local-path   Delete          WaitForFirstConsumer   false                  3m53s
```

## 5. 클러스터 삭제
아래 명령어로 클러스터를 간단하게 삭제할 수 있습니다.
```bash
# 클러스터 삭제
$ kind delete cluster --name myk8s
```

## 6. Kind 확장
### 6.1 워커노드 및 마스터노드 상세 설정
yaml template을 통해 Kind의 워커노드 개수나, 마스터노드 개수 또는 옵션을 변경할 수 있습니다.
- [관련 문서](https://kind.sigs.k8s.io/docs/user/quick-start/#configuring-your-kind-cluster)

아래 yaml template은, 워커노드 한대, 마스터노드 한대를 프로비저닝하고, 워커노드의 ```listenAddress``` 옵션으로 ```0.0.0.0``` 을 할당하면서 service에 access 할 수 있는 진입점을 열어주는 옵션입니다. ***이는 실제 Kubernetes Cluster의 WorkerNode에서 , NodePort를 열어주는것과 동일한 옵션입니다.***
- 차후 Kind의 Ingress 또는 LoadBalancer를 사용할 때 이용됩니다.
- default : ```0.0.0.0```

워커노드들의 ```hostPort``` 는 서로 상이해야 합니다.

```yaml
# two node (one workers) cluster config
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
networking:
  apiServerAddress: "0.0.0.0"
nodes:
- role: control-plane
- role: worker
  extraPortMappings:
  - containerPort: 31000
    hostPort: 31000
    listenAddress: "0.0.0.0" # Optional, defaults to "0.0.0.0"
    protocol: tcp # Optional, defaults to tcp
  - containerPort: 31001
    hostPort: 31001
```

만들어준 yaml Template으로 Kind Cluster를 프로비저닝합니다.
```bash
$ kind create cluster --config kind-2node.yaml --name myk8s
```

클러스터 배포를 확인합니다.
```bash
# docker 컨테이너 확인
$ docker ps
CONTAINER ID   IMAGE                  COMMAND                   CREATED              STATUS              PORTS                                  NAMES
ee4e8e322f5c   kindest/node:v1.29.2   "/usr/local/bin/entr…"   About a minute ago   Up About a minute   0.0.0.0:60090->6443/tcp                myk8s-control-plane
e6bdc7049c28   kindest/node:v1.29.2   "/usr/local/bin/entr…"   About a minute ago   Up About a minute   0.0.0.0:31000-31001->31000-31001/tcp   myk8s-worker

# 노드 확인
$ kubectl get nodes
NAME                  STATUS   ROLES           AGE   VERSION
myk8s-control-plane   Ready    control-plane   59s   v1.29.2
myk8s-worker          Ready    <none>          36s   v1.29.2
```

yaml template에서 워커노드에 ```listenAddress``` 로 열어준 port 매핑정보를 확인해보면, 아래와 같습니다.
- 포트포워드로 각 컨테이너의 포트와 연결된것을 확인할 수 있습니다.
- 따라서 ***Kubernetes Service를 생성할 때, NodePort를 31000 번으로 설정하면, 호스트 PC에서 접속이 가능***합니다.
```bash
$ docker port myk8s-worker
31000/tcp -> 0.0.0.0:31000
31001/tcp -> 0.0.0.0:31001
```

필요하다면 각 컨테이너(노드) 에 exec 해서 사용도 가능합니다.

```bash
# 컨테이너 내부 정보 확인 : 필요 시 각각의 노드(?)들에 bash로 접속하여 사용 가능
$ docker exec -it myk8s-control-plane ip -br -c -4 addr
$ docker exec -it myk8s-worker  ip -br -c -4 addr
```

실제로 deployment와 service를 만들어 테스트해봅니다.
```yaml
# 디플로이먼트와 서비스 배포
cat <<EOF | kubectl create -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: deploy-websrv
spec:
  replicas: 2
  selector:
    matchLabels:
      app: deploy-websrv
  template:
    metadata:
      labels:
        app: deploy-websrv
    spec:
      terminationGracePeriodSeconds: 0
      containers:
      - name: deploy-websrv
        image: nginx:alpine
        ports:
        - containerPort: 80
---
apiVersion: v1
kind: Service
metadata:
  name: deploy-websrv
spec:
  ports:
    - name: svc-webport
      port: 80
      targetPort: 80
      nodePort: 31000
  selector:
    app: deploy-websrv
  type: NodePort
EOF
```

도커 컨테이너 포트포워드 정보를 확인해보면 , 31000번과 31001 포트가 포트포워딩 된것을 확인할 수 있습니다.
```bash
$ docker ps
CONTAINER ID   IMAGE                  COMMAND                   CREATED         STATUS         PORTS                                  NAMES
e6bdc7049c28   kindest/node:v1.29.2   "/usr/local/bin/entr…"   9 minutes ago   Up 9 minutes   0.0.0.0:31000-31001->31000-31001/tcp   myk8s-worker
```

프로비저닝된 Nginx로 연결해봅니다.
```bash
$ curl 127.0.0.1:31000
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

***위의 Yaml Template으로 Kind 클러스터를 프로비저닝 한다면, 31000, 31001 번 포트만 노드포트로 지정이 가능하고, 특정 포트가 필요하다면 그때마다 추가해주어야 합니다.***

Kind도 LoadBalancer를 프로비저닝하여 사용이 가능합니다.
- [관련 문서](https://kind.sigs.k8s.io/docs/user/loadbalancer)

먼저 로컬 PC에 go install로 Kind 바이너리를 설치합니다.
```bash
$ go install sigs.k8s.io/cloud-provider-kind@latest
...
```

클라우드 제공업체 KIND는 호스트에서 독립 실행형 바이너리로 실행되고 KIND 클러스터에 연결하여 서비스에 대한 새로운 로드 밸런서 컨테이너를 프로비저닝합니다. 시스템에서 포트를 열고 컨테이너 런타임에 연결하려면 권한이 필요합니다.

설치가 완료되었으면 , 아래 yaml template으로 loadbalancer를 프로비저닝 합니다.

아래 yaml template은 nginx deployment를 프로비저닝하고, 이 파드로는 kind의 loadbalancer로 서비스 타입을 지정하여 접근하게끔 합니다.

그리고 마지막 LoadBalacner Type의 foo-service는 각 파드로 라우팅하는 로드벨런서 서비스를 생성합니다.
```bash
apiVersion: apps/v1
kind: Deployment
metadata:
  name: deploy-websrv
spec:
  replicas: 2
  selector:
    matchLabels:
      app: deploy-websrv
  template:
    metadata:
      labels:
        app: deploy-websrv
    spec:
      terminationGracePeriodSeconds: 0
      containers:
      - name: deploy-websrv
        image: nginx:alpine
        ports:
        - containerPort: 80
---
kind: Service
apiVersion: v1
metadata:
  name: foo-service
spec:
  type: LoadBalancer
  selector:
    app: deploy-websrv
  ports:
  # Default port used by the image
  - port: 5678
```

해당 yaml을 적용합니다.
```bash
$ kubectl apply -f kind-lb.yaml
```

    LoadBalancer Type프로비저닝 방안은 차후 진행 예정