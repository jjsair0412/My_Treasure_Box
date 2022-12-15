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

### apt update
작업 전 update 진행합니다.
```
sudo apt-get update
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
### 2.1 containerd 설치
master , etcd , worker 등 모든 노드에 docker를 설치합니다.

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

# Add the Kubernetes apt repository:
echo "deb [signed-by=/etc/apt/keyrings/kubernetes-archive-keyring.gpg] https://apt.kubernetes.io/ kubernetes-xenial main" | sudo tee /etc/apt/sources.list.d/kubernetes.list

# kubeadm , kubelet , kubectl 설치
sudo apt-get update
sudo apt-get install -y kubelet kubeadm kubectl # 이걸로 진행

# 특정 버전 설치 ( 1.21.0 설치 방안 )
sudo apt-get install -y kubelet=1.21.0-00 kubeadm=1.21.0-00 kubectl=1.21.0-00 # 해당문서는 사용 안했음.
```

## 5. kubeadm으로 ETCD 설치
kubeadm을 이용해서 etcd 클러스터를 구성합니다.

***작업대상 노드는 ETCD node에서만 진행합니다 !***

### Prerequirement
- ETCD 노드들은 기본적으로 2379 , 2380 포트로 통신이 가능해야 합니다.
- 각 ETCD 호스트 들에게는 systemd , bash shell이 설치되어 있어야 합니다.
- 각 호스트 들에게는 kubeadm, kubelet , container runtime ( 문서에는 docker ) 가 설치되어 있어야 합니다.
- 각 호스트 들에게는 k8s container image registry인 registry.k8s.io에 접근할 수 있어야 합니다. 
  만약 접근할 수 없다면 , ```kubeadm config images list/pull``` 명령어로 image를 미리 가지고와야 합니다.
- 호스트 간에 파일복사가 가능해야 합니다. ( scp , ssh )

### 5.1 kubelet 설정
먼저 kubelet을 설정해야 합니다.

```bash
$ cat << EOF > /etc/systemd/system/kubelet.service.d/20-etcd-service-manager.conf
[Service]
ExecStart=
# Replace "systemd" with the cgroup driver of your container runtime. The default value in the kubelet is "cgroupfs".
# Replace the value of "--container-runtime-endpoint" for a different container runtime if needed.
ExecStart=/usr/bin/kubelet --address=127.0.0.1 --pod-manifest-path=/etc/kubernetes/manifests --cgroup-driver=systemd --container-runtime=remote --container-runtime-endpoint=unix:///var/run/containerd/containerd.sock
Restart=always
EOF

$ systemctl daemon-reload

$ systemctl restart kubelet

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
# No need to move the certs because they are for HOST0

# clean up certs that should not be copied off this host
find /tmp/${HOST2} -name ca.key -type f -delete
find /tmp/${HOST1} -name ca.key -type f -delete
```

테스트시 실제 수행한 명령어들은 다음과 같습니다.

노드가 두개이기 때문에 , 아래와 같이 수행합니다.
HOST 부분에는 이전에 스트립트파일에서 사용한 호스트와 동일한 변수 입니다.
```bash
kubeadm init phase certs etcd-server --config=/tmp/10.0.0.3/kubeadmcfg.yaml
kubeadm init phase certs etcd-peer --config=/tmp/10.0.0.3/kubeadmcfg.yaml
kubeadm init phase certs etcd-healthcheck-client --config=/tmp/10.0.0.3/kubeadmcfg.yaml
kubeadm init phase certs apiserver-etcd-client --config=/tmp/10.0.0.3/kubeadmcfg.yaml

cp -R /etc/kubernetes/pki /tmp/10.0.0.3/
find /etc/kubernetes/pki -not -name ca.crt -not -name ca.key -type f -delete

kubeadm init phase certs etcd-server --config=/tmp/10.0.0.4/kubeadmcfg.yaml
kubeadm init phase certs etcd-peer --config=/tmp/10.0.0.4/kubeadmcfg.yaml
kubeadm init phase certs etcd-healthcheck-client --config=/tmp/10.0.0.4/kubeadmcfg.yaml
kubeadm init phase certs apiserver-etcd-client --config=/tmp/10.0.0.4/kubeadmcfg.yaml

cp -R /etc/kubernetes/pki /tmp/10.0.0.4/
find /etc/kubernetes/pki -not -name ca.crt -not -name ca.key -type f -delete

find /tmp/10.0.0.4 -name ca.key -type f -delete
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

