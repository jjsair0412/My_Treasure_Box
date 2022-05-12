# Kubernetes with kubeadm
- kubeadm을 활용하여 kubernetes를 설치한다.
## Docker install
- 도커는 master node ( control plane ) , worker node 모두 가지고 있어야 한다.
- 아래 명령어 작성하여 도커 다운로드

```bash
sudo apt-get update
sudo apt-get install -y \\
    ca-certificates \\
    curl \\
    gnupg \\
    lsb-release

curl -fsSL <https://download.docker.com/linux/ubuntu/gpg> | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

echo \\
  "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] <https://download.docker.com/linux/ubuntu> \\
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install docker-ce docker-ce-cli containerd.io

# 도커 시작
sudo systemctl enable docker
sudo systemctl start docker
sudo docker version
```

## ****Kubernetes install****

설치 순서가 있다.

1.  설치 전 환경설정
2.  kubeadm, kubelet, kubectl 설치
3.  control-plane 구성
4.  worker node 구성

아래 공식문서 참고하기

**before you begin 꼭 보고 확인**.

**swap를 enable**시켜야 하고 , **최소권장사양등**의 필수로 읽어야할 것들이 적혀있음

[Installing kubeadm](https://kubernetes.io/docs/setup/production-environment/tools/kubeadm/install-kubeadm/)

아래 명령어들로 **control-plane 구성하기 직전까지 설치**

```bash
# root로 세팅하는게 편하다. sudo명령어 안써도되니까

#swap 끄기
swapoff -a && sed -i '/swap/s/^/#/'/etc/fstab

cat <<EOF | sudo tee /etc/modules-load.d/k8s.conf
br_netfilter
EOF

cat <<EOF | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-ip6tables = 1
net.bridge.bridge-nf-call-iptables = 1
EOF
sudo sysctl --system

#방화벽 disable
systemctl stop firewalld
systemctl disable firewalld

# 여기까지 설치전 환경설정

# kubeadm, kubelet, kubectl 설치
# 여기도 root계정으로 하면 , sudo명령어 필요없으니까 편하다.
sudo apt-get update
sudo apt-get install -y apt-transport-https ca-certificates curl
sudo curl -fsSLo /usr/share/keyrings/kubernetes-archive-keyring.gpg <https://packages.cloud.google.com/apt/doc/apt-key.gpg>
echo "deb [signed-by=/usr/share/keyrings/kubernetes-archive-keyring.gpg] <https://apt.kubernetes.io/> kubernetes-xenial main" | sudo tee /etc/apt/sources.list.d/kubernetes.list
sudo apt-get update
sudo apt-get install -y kubelet kubeadm kubectl
sudo apt-mark hold kubelet kubeadm kubectl

systemctl start kubelet
systemctl enable kubelet
```

## json 파일 생성

```bash
udo mkdir /etc/docker

cat <<EOF | sudo tee /etc/docker/daemon.json
{
"exec-opts": ["native.cgroupdriver=systemd"],
"log-driver": "json-file",
"log-opts": {
"max-size": "100m"
},
"storage-driver": "overlay2"
}
EOF
 
sudo systemctl enable docker
sudo systemctl daemon-reload
sudo systemctl restart docker

```

=> [](https://kubernetes.io/docs/setup/production-environment/container-runtimes/#docker)[https://kubernetes.io/docs/setup/production-environment/container-runtimes/#docker](https://kubernetes.io/docs/setup/production-environment/container-runtimes/#docker)

## Control-Plane 구성

싱글마스터 구성 설치 매뉴얼 공식문서 :
[Creating a cluster with kubeadm](https://kubernetes.io/docs/setup/production-environment/tools/kubeadm/create-cluster-kubeadm/)

**master node ( control plane ) 에서만 해당 명령어 실행**

```bash
$ kubeadm init
```

위 커멘드 실행 시 , master node에 컴포넌트 ( coreDNS, controller, schduler )들이 실행 된다.

**설치 완료되면 join token 출력 , 해당 join token 복사**

- kubectl 명령어 허용 작업

```bash
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config

```

- **노드 개수 확인**

```bash
$ kubectl get nodes
```

- **Pod network add-on 작업**
아래 명령어도 master node ( control plane ) 에서만 실행해야 함.

cni 설치
-- cni는 weave-net, calico 등 많지만 , 해당 예제에서는 weave net으로 구성한다.

```bash
$ kubectl apply -f "<https://cloud.weave.works/k8s/net?k8s-version=$>(kubectl version | base64 | tr -d '\\n')"
```

## master node 와 worker node join
복사해두었던 join token을 worker node에 복사-붙여넣기
## worker node 확인

```bash
$ kubectl get nodes -o wide

```

worker node들 올라가있는지 확인 . ( Ready 상태인지 확인 )


## Multi master 환경 설치

기본적으로 설치방법은 동일하다.

master node를 설치한다.

각 master node에는 kubeadm , kubelet 전부다 필요하다.

init은 master노드 한군데에만 실행한다.

```bash
sudo kubeadm init --control-plane-endpoint jinseong.xxx.xxx:6443 --upload-certs --pod-network-cidr=192.168.0.0/16
```

init 명령어를 수행할 때, endpoint를 생성해주어야 한다.

endpoint는 각 노드들이 한지점으로 모이는 LB역할을 한다고 보면 되는데 ,

hostpath에 등록시켜주어야 한다.

경로는 ...

```bash
$ vi /etc/hosts
```

이며 , 여기에 예제에서 작성해놓은 것 처럼 한 지점 ( master node중 하나의 ip ) 를 지정해놓는다.

_**hosts 파일**_

```bash
127.0.0.1 localhost

# The following lines are desirable for IPv6 capable hosts
::1 ip6-localhost ip6-loopback
fe00::0 ip6-localnet
ff00::0 ip6-mcastprefix
ff02::1 ip6-allnodes
ff02::2 ip6-allrouters
ff02::3 ip6-allhosts

master-node-ip-addr jinseong.xxx.xxx.net

```

맨아래부분처럼 작성시켜 놓으면 , 좌측이 우측 ip에대한 도메인주소가 되는 것이다.

그러나 진짜 dns처럼 외부에서 접속이 가능한것은 아니다.

윈도우도 동일하게 설정해줄 수 있다.

만약 calico cni를사용할 경우 pod-network-cidr값을 설정해주어야 한다.

해당 init 명령어를 실행하고 나면 , join명령어가 두가지 출력된다.

1.  master 등록시키는 join
2.  기존 worker 등록시키는 join

순서대로 1 , 2 출력되게 되는데 , worker node를 join시켯을때와 동일하게 master node도 join시켜주면 된다.

----------

## kubeadm reset

[](https://wookiist.dev/143)[https://wookiist.dev/143](https://wookiist.dev/143)

[](https://scbyun.com/m/810)[https://scbyun.com/m/810](https://scbyun.com/m/810)