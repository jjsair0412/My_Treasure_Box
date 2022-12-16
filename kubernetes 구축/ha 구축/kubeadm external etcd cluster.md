# kubernetes ha구성 - 외부 etcd와 연결
## Prerequirement
해당 문서는 kubeadm으로 kubernetes를 구축합니다.

그러나 etcd를 master node에 내장하지 않고 , 외부 etcd cluster와 연결하여 kubernetes cluster를 구축합니다.

etcd를 외부로 뺏기 때문에 , 고 가용성을 확보할 수는 있지만 , 그만큼 인프라 리소스가 더 필요하다는 단점이 잇습니다.

최소 3개의 마스터노드와 , 클러스터링된 3개의 etcd node가 필요하지만 해당 문서는 test의 목적으로 한개의 master와 한개의 etcd node로 구성합니다.

[고가용성 토폴로지 선택](https://kubernetes.io/ko/docs/setup/production-environment/tools/kubeadm/ha-topology/)

### 구축 환경
- OS : ubuntu 20.04
- etcd : 2 vcore , 4GB , 2EA
- master : 4 vCore , 8GB 1EA
- container runtime : conatinerd

### 참고 문서
- [kubeadm으로 고가용성 클러스터 생성](https://kubernetes.io/docs/setup/production-environment/tools/kubeadm/high-availability/#before-you-begin)
- [kubeadm으로 고가용성 etcd 클러스터 설정](https://kubernetes.io/docs/setup/production-environment/tools/kubeadm/setup-ha-etcd-with-kubeadm/)
- [Kubernetes 및 etcd 고가용성 클러스터 구축하기 - ysyukr님의 블로그](https://ysyu.kr/2019/10/how-to-ha-cluster-kubernetes-with-etcd/)
- [How to install kubernetes with external Etcd manually? - netgo](https://createnetech.tistory.com/35)

### 작업 전 수행과정
작업 전 update 진행합니다.
```bash
sudo apt-get update
```

모든 노드 ( master , worker , etcd ) 의 swap memory를 disable 합니다.
```
swapoff -a
```

## 1. network 설정
### 1.1 hosts 파일 설정
master node와 etcd node에 서로 DNS를 리졸빙 할 수 있게끔 etc/hosts파일을 구성합니다.
모든 노드에게 적용합니다.

DNS 정보는 아래와 같습니다.
- master : m.k8s.jjs
- etcd 1 : e.k8s.jjs
- etcd 2 : ee.k8s.jjs

아래와 같이 변경합니다.

```bash
$ sudo vi /etc/hosts
10.0.0.2  m.k8s.jjs
10.0.0.3  e.k8s.jjs
10.0.0.4  ee.k8s.jjs
```

Domain으로 ping을 보내서 서로 통신되는지 확인합니다.

```bash
ping m.k8s.jjs
ping e.k8s.jjs
ping ee.k8s.jjs
```

## 2. container runtime 설정
해당 문서는 k8s container runtime을 containerd로 사용합니다.
만약 docker를 사용하려면 , 아래의 모든 노드에 docker를 설치합니다.

***둘중 한가지만 설치합니다 ! 하나 설치가 완료됐다면 3번 ( iptables설정 ) 으로 넘어갑니다.***
### 2.1 containerd 설치
master , etcd , worker 등 모든 노드에 containerd를 설치합니다.

- 참고 URL : https://kubernetes.io/docs/setup/production-environment/container-runtimes/#containerd

```bash
$ cat <<EOF | sudo tee /etc/modules-load.d/containerd.conf
overlay
br_netfilter
EOF

$ sudo modprobe overlay
$ sudo modprobe br_netfilter

# 필요한 sysctl 파라미터를 정의합니다.
# 이 파라미터는 재시작하더라도 그대로 유지됩니다.
$ cat <<EOF | sudo tee /etc/sysctl.d/99-kubernetes-cri.conf
net.bridge.bridge-nf-call-iptables  = 1
net.ipv4.ip_forward                 = 1
net.bridge.bridge-nf-call-ip6tables = 1
EOF

# 시스템을 재시작하지 않고 sysctl 파라미터를 반영하기 위한 작업입니다.
$ sudo sysctl --system

$ sudo apt-get update
$ sudo apt-get install \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg \
    lsb-release

# Docker GPG key 추가
$ curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

# Docker를 **stable** 버전으로 설치하기 위해 아래의 명령을 내립니다.
$ echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 새로운 저장소가 추가되었으므로, 업데이트를 합니다.
$ sudo apt-get update

# containerd.io 설치
$ sudo apt-get install containerd.io
```

containerd 기본 설정 작업을 진행합니다.
containerd의 설정 파일을 config.toml값에 저장합니다.

```bash
$ sudo mkdir -p /etc/containerd
$ containerd config default | sudo tee /etc/containerd/config.toml
```

### 2.1.2 cgroup 변경
containerd는 cgroup을 변경하기 위해선 config.toml 내용을 변경해야 합니다.
cgroup 관련 설정 ( SystemdCgroup = true ) 이 만약 없다면 , 직접 타이핑하여 넣습니다.

systemd를 cgroup driver로 사용합니다.

```t
$ vi /etc/containerd/config.toml
[plugins]

  [plugins."io.containerd.grpc.v1.cri".containerd.runtimes.runc]
    ...
    [plugins."io.containerd.grpc.v1.cri".containerd.runtimes.runc.options]
      SystemdCgroup = true 
```

containerd를 재 시작 합니다.
```bash
$ sudo systemctl restart containerd

$ sudo systemctl status containerd
```

toml파일이 제대로 변경되지 않았다면 containerd가 시작되지 않습니다.

### 2.2 docker 설치
master , etcd , worker 등 모든 노드에 docker를 설치합니다.


kubernetes에서 지원하는 Docker 버전을 설치해야하기 때문에 지원 버전을 확인합니다.
- URL: https://kubernetes.io/docs/setup/release/notes/

해당 문서에서는 docker를 22.12.15일자로 latest 버전인 20.10.21로 설치합니다.

```bash
$ sudo apt-get update

$ sudo apt-get install \
     ca-certificates \
     curl \
     gnupg \
     lsb-release


$ sudo mkdir -p /etc/apt/keyrings

$ curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

$ echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
    $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

$ sudo apt-get update

$ sudo apt-get install docker-ce docker-ce-cli containerd.io docker-compose-plugin
```

### 2.2.1 cgroup 변경
docker의 cgroup driver를 systemd로 변경합니다.

```bash
$ mkdir /etc/docker
$ sudo su

$ cat > /etc/docker/daemon.json << EOF
{
  "exec-opts": ["native.cgroupdriver=systemd"],
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "100m"
  },
  "storage-driver": "overlay2",
  "storage-opts": [
    "overlay2.override_kernel_check=true"
  ]
}
EOF

$ mkdir -p /etc/systemd/system/docker.service.d

$ systemctl daemon-reload

$ systemctl enable docker
```

## 3. iptables 설정
kube-proxy가 이용할 iptable을 설정합니다.

```bash
$ cat << EOF > /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-ip6tables = 1
net.bridge.bridge-nf-call-iptables = 1
EOF

$ sysctl --system

$ modprobe br_netfilter # 리부팅 이후에도 해당 모듈이 내려가 있으면, rc.local을 이용하자
```

## 4. kubeadm 설치 진행
kubelet , kubeadm , kubectl을 모든 노드 [ etcd , k8s ( worker , master ) ] 에 설치합니다.

해당 문서는 특정 버전을 설치하지 않고 , latest 버전으로 설치합니다. 
```bash
#방화벽 disable
systemctl stop firewalld
systemctl disable firewalld

# 여기까지 설치전 환경설정

# kubeadm, kubelet, kubectl 설치

# Update the apt package index and install packages needed to use the Kubernetes apt repository:
sudo apt-get update
sudo apt-get install -y apt-transport-https ca-certificates curl

# Download the Google Cloud public signing key:
sudo curl -fsSLo /etc/apt/keyrings/kubernetes-archive-keyring.gpg https://packages.cloud.google.com/apt/doc/apt-key.gpg

# 만약 GPG key를 body size 에러때문에 가져오지 못하면 , 아래와같은 방식 시도
$ mkdir /etc/apt/keyrings/
$ cd /etc/apt/keyrings
$ wget https://packages.cloud.google.com/apt/doc/apt-key.gpg
$ mv apt-key.gpg kubernetes-archive-keyring.gpg

# Add the Kubernetes apt repository:
echo "deb [signed-by=/etc/apt/keyrings/kubernetes-archive-keyring.gpg] https://apt.kubernetes.io/ kubernetes-xenial main" | sudo tee /etc/apt/sources.list.d/kubernetes.list


# kubeadm , kubelet , kubectl 설치
sudo apt-get update
sudo apt-get install -y kubelet kubeadm kubectl # 이걸로 진행

# 특정 버전 설치 ( 1.21.0 설치 방안 )
sudo apt-get install -y kubelet=1.21.0-00 kubeadm=1.21.0-00 kubectl=1.21.0-00 # 해당문서는 사용 안했음.

# version hold
sudo apt-mark hold kubelet kubeadm kubectl
```

### 4.1 kubelet conf의 cgroup 변경
 "/etc/systemd/system/kubelet.service.d/10-kubeadm.conf" 파일에 kubelet 설정파일이 위치합니다.
 아래와 같은 명령어를 추가하여 kubelet의 cgroup driver를 systemd로 설정합니다.


```bash
Environment="KUBELET_EXTRA_ARGS=--cgroup-driver=systemd"
```

```bash
$ vi /etc/systemd/system/kubelet.service.d/10-kubeadm.conf
[Service]
...
Environment="KUBELET_KUBECONFIG_ARGS=--bootstrap-kubeconfig=/etc/kubernetes/bootstrap-kubelet.conf --kubeconfig=/etc/kubernetes/kubelet.conf"
Environment="KUBELET_CONFIG_ARGS=--config=/var/lib/kubelet/config.yaml"
Environment="KUBELET_EXTRA_ARGS=--cgroup-driver=systemd" # 해당 라인 추가
...
```

kubelet과 daemon을 restart , reload 합니다.

주의할 점은 daemon부터 reload 해야 합니다.

```bash
$ systemctl daemon-reload
$ systemctl restart kubelet
```

status로 kubelet의 상태를 확인해보면 , 아직 실행되지 않은것을 확인할 수 있습니다.
그 이유는 kubelet의 config.yaml파일이 없기 때문입니다. 

실행되지않은게 정상.

## 5. kubeadm으로 ETCD 설치
kubeadm을 이용해서 etcd 클러스터를 구성합니다.

***작업대상 노드는 모든 ETCD node에서만 진행합니다 !***

### Prerequirement
- ETCD 노드들은 기본적으로 2379 , 2380 포트로 통신이 가능해야 합니다.
- 각 ETCD 호스트 들에게는 systemd , bash shell이 설치되어 있어야 합니다.
- 각 호스트 들에게는 kubeadm, kubelet , container runtime ( 문서에는 docker ) 가 설치되어 있어야 합니다.
- 각 호스트 들에게는 k8s container image registry인 registry.k8s.io에 접근할 수 있어야 합니다. 
  만약 접근할 수 없다면 , ```kubeadm config images list/pull``` 명령어로 image를 미리 가지고와야 합니다.
- 호스트 간에 파일복사가 가능해야 합니다. ( scp , ssh )

### 5.1 kubelet 설정
먼저 kubelet을 설정해야 합니다.

기본적으로 kubelet은 ```/etc/systemd/system/kubelet.service.d/10-etcd-service-manager.conf``` 파일의 세팅값을 바탕으로 실행되게 됩니다.
conf파일 맨 앞쪽 숫자가 높을수록 파일 우선순위가 높기 때문에 , 20-etcd-service-manager.conf 파일을 아래 명령어로 만들어서
runtime을 세팅하거나 cgroup 세팅값을 변경합니다.

바로 위 작업처럼 10.conf파일을 전부다 바꿔서 그대로 사용해도 무관하지만 , 어렵기때문에 지금까지 설치한 설정파일을 바탕으로 20-etcd-service-manager.conf파일을 만드는게 좋습니다.

```bash
$ cat << EOF > /etc/systemd/system/kubelet.service.d/20-etcd-service-manager.conf
[Service]
ExecStart=
# container runtime의 cgroup driver를 systemd로 변경합니다. default값은 cgroupfs.
# "--container-runtime-endpoint" 세팅값을 이전에 설치한 container runtime으로 변경합니다. 해당 문서는 containerd를 설치 했기에 sock파일을 containerd로 하면 되고 , 
# docker로 runtime을 설정했다면 docker.sock파일의 경로를 넣어주면 됩니다.
ExecStart=/usr/bin/kubelet --address=127.0.0.1 --pod-manifest-path=/etc/kubernetes/manifests --cgroup-driver=systemd --container-runtime=remote --container-runtime-endpoint=unix:///var/run/containerd/containerd.sock
Restart=always
EOF


# kubelet에서 swap memory를 지원하지 않기 떄문에 , swap memory를 disalbe 합니다 !
$ swapoff -a

$ systemctl daemon-reload

$ systemctl restart kubelet


kubelet이 드디어 active상태인것을 확인 하고 넘어갑니다 .
active가 아니라면 journalctl -xe나 status를 보고 troubleshooting 진행합니다. 
```
$ systemctl status kubelet
```

kubeadm에 대한 구성 파일을 생성합니다.
다음 스크립트를 사용하여 etcd 구성원이 실행될 각 호스트에 대해 하나의 kubeadm 구성 파일을 생성합니다.


```bash
# Update HOST0, HOST1 and HOST2 with the IPs of your hosts
export HOST0=10.0.0.6
export HOST1=10.0.0.7
export HOST2=10.0.0.8

# Update NAME0, NAME1 and NAME2 with the hostnames of your hosts
export NAME0="infra0"
export NAME1="infra1"
export NAME2="infra2"

# Create temp directories to store files that will end up on other hosts
mkdir -p /tmp/${HOST0}/ /tmp/${HOST1}/ /tmp/${HOST2}/

HOSTS=(${HOST0} ${HOST1} ${HOST2})
NAMES=(${NAME0} ${NAME1} ${NAME2})

for i in "${!HOSTS[@]}"; do
HOST=${HOSTS[$i]}
NAME=${NAMES[$i]}
cat << EOF > /tmp/${HOST}/kubeadmcfg.yaml
---
apiVersion: "kubeadm.k8s.io/v1beta3"
kind: InitConfiguration
nodeRegistration:
    name: ${NAME}
localAPIEndpoint:
    advertiseAddress: ${HOST}
---
apiVersion: "kubeadm.k8s.io/v1beta3"
kind: ClusterConfiguration
etcd:
    local:
        serverCertSANs:
        - "${HOST}"
        peerCertSANs:
        - "${HOST}"
        extraArgs:
            initial-cluster: ${NAMES[0]}=https://${HOSTS[0]}:2380,${NAMES[1]}=https://${HOSTS[1]}:2380,${NAMES[2]}=https://${HOSTS[2]}:2380
            initial-cluster-state: new
            name: ${NAME}
            listen-peer-urls: https://${HOST}:2380
            listen-client-urls: https://${HOST}:2379
            advertise-client-urls: https://${HOST}:2379
            initial-advertise-peer-urls: https://${HOST}:2380
EOF
done
```

해당 문서에서는 etcd 노드가 2기이기 때문에 아래 스크립트로 진행합니다.

HOST에는 각 etcd 노드의 ip가 들어가고 , NAME에는 etc/hosts에 등록시킨 etcd host node에 domain이 들어갑니다.
```bash
#!/bin/bash
export HOST0=10.0.0.3
export HOST1=10.0.0.4 

export NAME0="e.k8s.jjs"
export NAME1="ee.k8s.jjs"

mkdir -p /tmp/${HOST0}/ /tmp/${HOST1}/

HOSTS=(${HOST0} ${HOST1})
NAMES=(${NAME0} ${NAME1})

for i in "${!HOSTS[@]}"; do
HOST=${HOSTS[$i]}
NAME=${NAMES[$i]}
cat << EOF > /tmp/${HOST}/kubeadmcfg.yaml
---
apiVersion: "kubeadm.k8s.io/v1beta3"
kind: InitConfiguration
nodeRegistration:
    name: ${NAME}
localAPIEndpoint:
    advertiseAddress: ${HOST}
---
apiVersion: "kubeadm.k8s.io/v1beta3"
kind: ClusterConfiguration
etcd:
    local:
        serverCertSANs:
        - "${HOST}"
        peerCertSANs:
        - "${HOST}"
        extraArgs:
            initial-cluster: ${NAMES[0]}=https://${HOSTS[0]}:2380,${NAMES[1]}=https://${HOSTS[1]}:2380,${NAMES[2]}=https://${HOSTS[2]}:2380
            initial-cluster-state: new
            name: ${NAME}
            listen-peer-urls: https://${HOST}:2380
            listen-client-urls: https://${HOST}:2379
            advertise-client-urls: https://${HOST}:2379
            initial-advertise-peer-urls: https://${HOST}:2380
EOF
done
```

스크립트를 아래 명령어로 수행합니다.
```bash
$ sudo bash etcd.sh
```

### 5.2 인증서 생성
kubeadm을 통해 etcd 인증서를 생성합니다.

CA가 이미 있는 경우 CA crt와 key파일을 /etc/kubernetes/pki/etcd/ca.crt및 /etc/kubernetes/pki/etcd/ca.key에 복사하기만 하면 됩니다. 
없는경우 해당 방안을 수행합니다.

etcd ${HOST0} 에서 인증서를 생성하고 , 다른 etcd host들의 cert 파일들을 생성한뒤
scp로 다른 호스트 ( ${HOST1} ${HOST2} )로 복사하는 과정입니다.

```bash
$ sudo kubeadm init phase certs etcd-ca
```

위 명령어를 수행하면 두 파일이 생성되게 됩니다.
- /etc/kubernetes/pki/etcd/ca.crt
- /etc/kubernetes/pki/etcd/ca.key

### 5.3 인증서 생성 및 복사
하나의 ETCD 마스터에서 다른 ETCD 클러스터의 인증서를 생성하는 과정입니다.

생성이후에 , 각 호스트들의 인증서를 scp 명령어로 이동합니다.

#### 5.4 인증서 생성
각 구성원에 대한 인증서를 생성합니다.

```bash
# ETCD 3번
kubeadm init phase certs etcd-server --config=/tmp/${HOST2}/kubeadmcfg.yaml
kubeadm init phase certs etcd-peer --config=/tmp/${HOST2}/kubeadmcfg.yaml
kubeadm init phase certs etcd-healthcheck-client --config=/tmp/${HOST2}/kubeadmcfg.yaml
kubeadm init phase certs apiserver-etcd-client --config=/tmp/${HOST2}/kubeadmcfg.yaml
cp -R /etc/kubernetes/pki /tmp/${HOST2}/
# cleanup non-reusable certificates
find /etc/kubernetes/pki -not -name ca.crt -not -name ca.key -type f -delete

# ETCD 2번
kubeadm init phase certs etcd-server --config=/tmp/${HOST1}/kubeadmcfg.yaml
kubeadm init phase certs etcd-peer --config=/tmp/${HOST1}/kubeadmcfg.yaml
kubeadm init phase certs etcd-healthcheck-client --config=/tmp/${HOST1}/kubeadmcfg.yaml
kubeadm init phase certs apiserver-etcd-client --config=/tmp/${HOST1}/kubeadmcfg.yaml
cp -R /etc/kubernetes/pki /tmp/${HOST1}/
find /etc/kubernetes/pki -not -name ca.crt -not -name ca.key -type f -delete

# ETCD 1번
kubeadm init phase certs etcd-server --config=/tmp/${HOST0}/kubeadmcfg.yaml
kubeadm init phase certs etcd-peer --config=/tmp/${HOST0}/kubeadmcfg.yaml
kubeadm init phase certs etcd-healthcheck-client --config=/tmp/${HOST0}/kubeadmcfg.yaml
kubeadm init phase certs apiserver-etcd-client --config=/tmp/${HOST0}/kubeadmcfg.yaml
# HOST 1번에서 HOST0번의 파일을 생성했기 때문에 파일 이동과정은 필요가 없습니다.

# clean up certs that should not be copied off this host
find /tmp/${HOST2} -name ca.key -type f -delete
find /tmp/${HOST1} -name ca.key -type f -delete
```

테스트시 실제 수행한 명령어들은 다음과 같습니다.

노드가 두개이기 때문에 , 아래와 같이 수행합니다.
HOST 부분에는 이전에 스트립트파일에서 사용한 호스트와 동일한 변수 입니다.
```bash
kubeadm init phase certs etcd-server --config=/tmp/10.0.0.4/kubeadmcfg.yaml
kubeadm init phase certs etcd-peer --config=/tmp/10.0.0.4/kubeadmcfg.yaml
kubeadm init phase certs etcd-healthcheck-client --config=/tmp/10.0.0.4/kubeadmcfg.yaml
kubeadm init phase certs apiserver-etcd-client --config=/tmp/10.0.0.4/kubeadmcfg.yaml
cp -R /etc/kubernetes/pki /tmp/10.0.0.4/
find /etc/kubernetes/pki -not -name ca.crt -not -name ca.key -type f -delete

kubeadm init phase certs etcd-server --config=/tmp/10.0.0.3/kubeadmcfg.yaml
kubeadm init phase certs etcd-peer --config=/tmp/10.0.0.3/kubeadmcfg.yaml
kubeadm init phase certs etcd-healthcheck-client --config=/tmp/10.0.0.3/kubeadmcfg.yaml
kubeadm init phase certs apiserver-etcd-client --config=/tmp/10.0.0.3/kubeadmcfg.yaml
# No need to move the certs because they are for HOST0

# clean up certs that should not be copied off this host
find /tmp/10.0.0.4 -name ca.key -type f -delete


sudo scp -i kisti.pem -r /tmp/10.0.0.4/* ubuntu@10.0.0.4:/home/ubuntu
```

#### 5.5 인증서 복사
인증서가 생성되었기 때문에 , 해당 호스트로 이동하여 복사합니다.

```bash
USER=ubuntu
HOST=${HOST1}
scp -r /tmp/${HOST}/* ${USER}@${HOST}:
ssh ${USER}@${HOST}
USER@HOST $ sudo -Es
root@HOST $ chown -R root:root pki
root@HOST $ mv pki /etc/kubernetes/
```

실제 수행한 명령어는 다음과 같습니다.
```bash
# 키값 생성한 호스트에서 작업
$ sudo scp -i kisti.pem -r /tmp/10.0.0.4/* ubuntu@10.0.0.4:/home/ubuntu

# 10.0.0.4 호스트로 ssh 접속이후 해당 호스트에서 작업
$ cd /home/ubuntu
$ ls
etcd.sh  kisti.pem  kubeadmcfg.yaml  pki
$ mv pki /etc/kubernetes/
```

필요한 인증키 데이터들이 모두 쌓였는지 tree명령어로 확인합니다.

***HOST1번 ETCD ( 10.0.0.3 )***
```bash
/tmp/${HOST0}
└── kubeadmcfg.yaml
---
/etc/kubernetes/pki
├── apiserver-etcd-client.crt
├── apiserver-etcd-client.key
└── etcd
    ├── ca.crt
    ├── ca.key
    ├── healthcheck-client.crt
    ├── healthcheck-client.key
    ├── peer.crt
    ├── peer.key
    ├── server.crt
    └── server.key
```

***HOST2번 ETCD ( 10.0.0.4 )***
```bash
$HOME
└── kubeadmcfg.yaml
---
/etc/kubernetes/pki
├── apiserver-etcd-client.crt
├── apiserver-etcd-client.key
└── etcd
    ├── ca.crt
    ├── healthcheck-client.crt
    ├── healthcheck-client.key
    ├── peer.crt
    ├── peer.key
    ├── server.crt
    └── server.key
```

만약 ETCD HOST3번이 있다면 , 결과는 아래와 같아야 합니다.
***HOST2번 ETCD ( 10.0.0.4 )***
```bash
$HOME
└── kubeadmcfg.yaml
---
/etc/kubernetes/pki
├── apiserver-etcd-client.crt
├── apiserver-etcd-client.key
└── etcd
    ├── ca.crt
    ├── healthcheck-client.crt
    ├── healthcheck-client.key
    ├── peer.crt
    ├── peer.key
    ├── server.crt
    └── server.key
```

#### 5.6 static pod manifest 생성
인증서와 구성이 준비되었기 때문에 , 파드 매니페스트를 생성해야 합니다.
각 ETCD HOST에서 kubeadm 명령어로 etcd를 위한 static manifest 를 생성합니다 !

***모든 ETCD NODE에서 수행 합니다***
```bash
root@HOST0 $ kubeadm init phase etcd local --config=/tmp/${HOST0}/kubeadmcfg.yaml
root@HOST1 $ kubeadm init phase etcd local --config=$HOME/kubeadmcfg.yaml
root@HOST2 $ kubeadm init phase etcd local --config=$HOME/kubeadmcfg.yaml
```

실 수행한 명령어는 다음과 같습니다.
```bash
root@10.0.0.3 $ kubeadm init phase etcd local --config=/tmp/10.0.0.3/kubeadmcfg.yaml
root@10.0.0.4 $ kubeadm init phase etcd local --config=$HOME/kubeadmcfg.yaml
```

결과는 다음이 출력됩니다.
```bash
[etcd] Creating static Pod manifest for local etcd in "/etc/kubernetes/manifests"
```

etcd.yaml파일이 생성되고 , 해당 파일을 "/etc/kubernetes/manifests" 경로에서 확인할 수 있습니다.


#### 5.7 ETCD Cluster 상태 확인 ( option )
생성된 etcd cluster의 health를 확인합니다.

```bash
docker run --rm -it \
--net host \
-v /etc/kubernetes:/etc/kubernetes registry.k8s.io/etcd:${ETCD_TAG} etcdctl \
--cert /etc/kubernetes/pki/etcd/peer.crt \
--key /etc/kubernetes/pki/etcd/peer.key \
--cacert /etc/kubernetes/pki/etcd/ca.crt \
--endpoints https://${HOST0}:2379 endpoint health --cluster
...
https://[HOST0 IP]:2379 is healthy: successfully committed proposal: took = 16.283339ms
https://[HOST1 IP]:2379 is healthy: successfully committed proposal: took = 19.44402ms
https://[HOST2 IP]:2379 is healthy: successfully committed proposal: took = 35.926451ms
```

### 6. controlPlan 설정
k8s master node를 설정합니다.

#### 6.1 crt , key 파일 이동
ETCD 노드에서 생성한 인증서 파일들을 , 모든 master node로 scp하여 전송해줍니다.
3개 파일 scp로 전송합니다.

1. ca.crt
2. apiserver-etcd-client.crt
3. apiserver-etcd-client.key

ETCD HOST0번에서 수행합니다.

문서 작성 환경에는 master가 한대뿐이기 때문에 , 10.0.0.2번에만 전송하지만 
ha구성을 위해 마스터가 3대 이상의 홀수라면 모든 마스터에게 전송해주어야 합니다.
```bash
scp /etc/kubernetes/pki/etcd/ca.crt ubuntu@10.0.0.2:/home/ubuntu
scp /etc/kubernetes/pki/apiserver-etcd-client.crt ubuntu@10.0.0.2:/home/ubuntu
scp /etc/kubernetes/pki/apiserver-etcd-client.key ubuntu@10.0.0.2:/home/ubuntu
```

모든 마스터 노드에서 받아온 crt파일들을 pki 폴더를 생성하고 그곳에 복사 합니다.

```bash 
$ mkdir -p /etc/kubernetes/pki/etcd/ 
$ cp /home/ubuntu/ca.crt /etc/kubernetes/pki/etcd/
$ cp /home/ubuntu/apiserver-etcd-client.crt /etc/kubernetes/pki/
$ cp /home/ubuntu/apiserver-etcd-client.key /etc/kubernetes/pki/
```

#### 6.2 첫번째 master node 생성
먼저 kubeadm-config.yaml 파일을 생성합니다.

첫번째 master node가 작업 대상입니다 !! 
문서에서는 10.0.0.2번 ip를 가진 노드입니다.

아래처럼 yaml파일을 생성합니다.
```yaml
cat << EOF > /root/kubeadm-config.yaml
apiVersion: kubeadm.k8s.io/v1beta3
kind: ClusterConfiguration
kubernetesVersion: stable
controlPlaneEndpoint: "LOAD_BALANCER_DNS:LOAD_BALANCER_PORT" # change this (see below)
etcd:
  external:
    endpoints:
      - https://ETCD_0_IP:2379 # change ETCD_0_IP appropriately
      - https://ETCD_1_IP:2379 # change ETCD_1_IP appropriately
      - https://ETCD_2_IP:2379 # change ETCD_2_IP appropriately
    caFile: /etc/kubernetes/pki/etcd/ca.crt
    certFile: /etc/kubernetes/pki/apiserver-etcd-client.crt
    keyFile: /etc/kubernetes/pki/apiserver-etcd-client.key
EOF
```

구성 템플릿의 다음 변수를 클러스터에 적합한 값으로 바꿉니다.

- LOAD_BALANCER_DNS : LB DNS 정보
- LOAD_BALANCER_PORT : LB PORT
  - 현재 테스트 환경은 LB가 등록되어있지 않고 , master 3중화 및 앞단 LB구성이 없기 때문에 master1번으로 endpoint를 선택합니다.
- ETCD_0_IP : ETCD 1번 IP
- ETCD_1_IP : ETCD 2번 IP
- ETCD_2_IP : ETCD 3번 IP

실제 작업시 수행한 yaml은 다음과 같습니다.
( ETCD 노드가 2개이기때문 )

```yaml
cat << EOF > /root/kubeadm-config.yaml
apiVersion: kubeadm.k8s.io/v1beta3
kind: ClusterConfiguration
kubernetesVersion: stable
controlPlaneEndpoint: "m.k8s.jjs:6443" 
etcd:
    external:
        endpoints:
          - https://10.0.0.3:2379 # change ETCD_0_IP appropriately
          - https://10.0.0.4:2379 # change ETCD_1_IP appropriately
        caFile: /etc/kubernetes/pki/etcd/ca.crt
        certFile: /etc/kubernetes/pki/apiserver-etcd-client.crt
        keyFile: /etc/kubernetes/pki/apiserver-etcd-client.key
EOF
```

#### 6.3 K8S cluster bootstrap
k8s 부트스트래핑 진행합니다.

아래 명령어로 직전 생성해두었던 kubeadm-config파일을 바탕으로 부트스트랩 합니다.

```bash
$ sudo kubeadm init --config kubeadm-config.yaml --upload-certs
```

### 7. k8s 설치 결과 확인 및 setting
부트스트랩이 성공적으로 완료되면 아래와 같이 결과가 출력 됩니다.

```bash
$ mkdir -p $HOME/.kube
$ sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
$ sudo chown $(id -u):$(id -g) $HOME/.kube/config
```

1. user 계정에서 kubectl 명령어 허용

```bash
$ export KUBECONFIG=/etc/kubernetes/admin.conf
```

2. cni 배포 명령어
```bash
$ kubectl apply -f [podnetwork].yaml
```

3. 다른 master node k8s cluster에 join시키는 명령어

```bash
  kubeadm join m.k8s.jjs:6443 --token ufhdg9.ayvscautn5l5a4qj \
        --discovery-token-ca-cert-hash sha256:01c5059648197e4699002d41944d8234961d413a671807bb9079dd4e1d5c6758 \
        --control-plane --certificate-key 889bfd2da196c5a961e1f43b82431f14f191924b90dcaf8f2e1cf5412015bb46
```

Please note that the certificate-key gives access to cluster sensitive data, keep it secret!
As a safeguard, uploaded-certs will be deleted in two hours; If necessary, you can use
"kubeadm init phase upload-certs --upload-certs" to reload certs afterward.

4. certs파일 수동 업데이트 명령어

```bash
kubeadm init phase upload-certs --upload-certs
```

5. worker node join 명령어

```bash
kubeadm join m.k8s.jjs:6443 --token ufhdg9.ayvscautn5l5a4qj \
        --discovery-token-ca-cert-hash sha256:01c5059648197e4699002d41944d8234961d413a671807bb9079dd4e1d5c6758 
```

kubectl get nodes 명령어를 통해 노드가 정상적으로 작동하고 있는지 확인합니다.
```bash
$ root@jjs:~# kubectl get nodes
NAME        STATUS     ROLES           AGE     VERSION
jjs         NotReady   control-plane   4m15s   v1.26.0
```

not ready 상태인 이유는 , cni network plugin이 설치되지 않았기 때문입니다.

cni를 아래 명령어중 하나 선택하여 설치합니다.
해당 문서에서는 calico를 설치합니다.

```bash
# calico 설치 
$ kubectl apply -f https://docs.projectcalico.org/manifests/calico.yaml

# weavenet 설치
$ kubectl apply -f "<https://cloud.weave.works/k8s/net?k8s-version=$>(kubectl version | base64 | tr -d '\\n')"
```

node가 Ready상태인지 확인합니다.

```bash
root@jjs:~# kubectl get nodes -o wide
NAME        STATUS   ROLES           AGE   VERSION   INTERNAL-IP    EXTERNAL-IP   OS-IMAGE             KERNEL-VERSION      CONTAINER-RUNTIME
jjs         Ready    control-plane   85m   v1.26.0   10.0.0.2       <none>        Ubuntu 20.04.5 LTS   5.15.0-41-generic   containerd://1.6.13
```

완료 !