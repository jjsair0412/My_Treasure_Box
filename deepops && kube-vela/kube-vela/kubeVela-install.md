# kubeVela-install
해당 문서는 deepops로 k8s를 배포한 이후 kube-vela을 배포하는 방법에 대한 가이드 문서입니다.

## Prerequirement

**구축 환경**
| os | 사양 | k8s version | deepops version | container runtime | role | ip addr |
|--|--|--|--|--|--|--|
| ubuntu 20.04 | 4core 8GB | v1.20.7 | 21.09 | docker://19.3.12 | control plane | 10.0.0.2 |
| ubuntu 20.04 | 2core 4GB | v1.20.7 | 21.09 | docker://19.3.12 | worker | 10.0.0.3 |

**설치 목표대상 kubevela version**

- CLI Version: v1.6.5
- Core Version: 1.6.5
- GitRevision: git-18639cc
- GolangVersion: go1.19.4

**참고문서**
- [kubevela install docs](https://kubevela.io/docs/installation/kubernetes)
- [kubevela VelaUX docs](https://kubevela.io/docs/reference/addons/velauxs)

## Kubernetes Requirements
kubevela는 k8s version이 아래와 같아야만 설치가 가능합니다.
- Kubernetes cluster >= v1.19 && <= v1.24

또한 여러 k8s provider를 지원합니다.
- Alibaba Cloud ACK Service
- AWS EKS Service
- Azure AKS Service
- Google GKE Service
- Rancher K3s or RKE

kubevela는 vela core를 설치한 이후 , velaUX를 따로 설치해야만 UI로 접근할 수 있습니다.
- dashboard : velaUX
- kubevela : velaCore

## KubeVela 설치
### 1. Install KubeVela CLI
vela command를 사용하기 위해서 curl 명령어로 vela command를 설치 합니다.

```bash
$ curl -fsSl https://kubevela.net/script/install.sh | bash
```

vela를 설치하는 방안은 두가지로 나뉩니다.
1. vela command를 사용하는 방법
2. helm chart로 설치하는 방법

#### 1.1 vela command로 설치
vela command로 kubevela을 설치합니다.

```bash
$ vela install
```

#### 1.2 helm chart로 설치
helm chart로 kubevela를 배포합니다.

```bash
$ helm repo add kubevela https://charts.kubevela.net/core
$ helm repo update
$ helm install --create-namespace -n vela-system kubevela kubevela/vela-core --wait
```

### 2. Install VelaUX
KubeVela의 dashboard로 접속하기 위해서 VelaUX를 배포합니다.

Vela dashboard를 사용하고싶지 않다면 , 배포하지 않으면 됩니다.

#### 2.1 velaUX addon enable
vela 명령어를 통해 velaUX addon을 nodeport type으로 enable 시킵니다.

```bash
$ vela addon enable velaux serviceType=NodePort
```

LB type으로 enable 또한 가능합니다.

```bash
$ vela addon enable velaux serviceType=LoadBalancer
```

결과는 다음과 같습니다.
```bash
...
Addon velaux enabled successfully.
Please access addon-velaux from the following endpoints:
+---------+-----------+-------------------------------+----------------------------+-------+
| CLUSTER | COMPONENT |   REF(KIND/NAMESPACE/NAME)    |          ENDPOINT          | INNER |
+---------+-----------+-------------------------------+----------------------------+-------+
| local   | apiserver | Service/vela-system/apiserver | apiserver.vela-system:8000 | true  |
| local   | velaux    | Service/vela-system/velaux    | http://10.0.0.2:30000  | false |
+---------+-----------+-------------------------------+----------------------------+-------+
```

NodePort 30000번으로 dashboard에 접근합니다.

***velaUX의 default USER는 다음과 같습니다.***
- ID : admin
- PWD : VelaUX12345

