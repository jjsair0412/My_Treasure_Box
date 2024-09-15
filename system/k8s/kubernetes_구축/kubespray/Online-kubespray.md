
  
# kubespray로 kubernetes 구축 - Online 환경
- 해당 예제는 , ansible 명령어를 이용해 kubespray로 kubernetes를 구축하는 방법입니다.
- 예제 환경은 외부 접근이 가능한 Online 입니다.
- 클러스터는 총 3대 , master 1개 , worker 2개
- root 권한으로 설치를 진행합니다.
[Kubespray git 주소](https://github.com/kubernetes-sigs/kubespray)

## 1. 설치 준비
### 1.1 kubespray 파일 설치
- 외부접근이 가능하기에 , curl 명령어를 사용하는 방법과 git clone 방법 두가지가 있습니다.
- git clone 방법
```
$ git clone https://github.com/kubernetes-sigs/kubespray.git

$ git tag

# kubespray 2.17.1 version 가져옴
$ git checkout tags/v2.17.1

# kubespray 2.18.1 version 가져옴
$ git checkout tags/v2.18.1
```

- curl 명령어를 사용하는 방법
```
$ curl -LO https://github.com/kubernetes-sigs/kubespray/archive/refs/tags/v2.17.1.tar.gz

$ tar xvf v2.17.1.tar.gz
```
## 2. Dependency 설치와 Config 설정
### 2.1 ansible 설치
- kubespray는 ansible을 기본적으로 사용합니다.
```
$ sudo apt install ansible python3-argcomplete

# 설치 확인
$ ansible --version
ansible 2.10.8
```
### 2.2 python3 설치
- ubuntu에서는 기본적으로 python3가 설치되어 있습니다.
```
$ sudo apt-get install python3

$ python3 --version
Python 3.10.4
```
### 2.3 모든 Node에 들어가 Swap, 브릿지 등 설정
- kubernetes를 설치하기 위해선 swap 메모리를 모두 비활성화 해야 합니다.
- 모든 node에서 작업합니다.
```
$ sudo swapoff -a

$ sudo sed -i '/ swap / s/^\(.*\)$/#\1/g' /etc/fstab

$ cat <<EOF | sudo tee /etc/modules-load.d/k8s.conf
br_netfilter
EOF

$ cat <<EOF | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-ip6tables = 1
net.bridge.bridge-nf-call-iptables = 1
EOF

$ sudo sysctl --system
```
### 2.4 Bastion Node에서 Key Pair 생성 후 다른 모든 Node에 authorized_key 생성
- 모든 node에서 bastion을 접근할 수 있도록 Key Pair을 등록해 줍니다.
```
# Bastion VM에서 keygen
$ sudo su
$ ssh-keygen -t rsa

# 나머지 모든 Node에 SSH 접근하여 모든 Node에 키 등록
$ sudo su

$ cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys

$ cat ~/.ssh/authorized_keys
```
### 2.5 Host 등록 Config File 설정
```
# kubespray 소스코드 파일 디렉토리로 이동
$ cd /home/centos/kubespray/kubespray-2.17.1

$ cp -rfp inventory/sample/ inventory/mycluster

$ cat inventory/mycluster/inventory.ini

[all]
controlpalne-prd-1 ansible_host=10.250.205.112 ip=10.250.205.112 etcd_member_name=etcd1
controlpalne-prd-2 ansible_host=10.250.199.224 ip=10.250.199.224 etcd_member_name=etcd2
controlpalne-prd-3 ansible_host=10.250.196.143 ip=10.250.196.143 etcd_member_name=etcd3

worker-prd-1 ansible_host=10.250.194.64 ip=10.250.194.64
worker-prd-2 ansible_host=10.250.192.140 ip=10.250.192.140
worker-prd-3 ansible_host=10.250.202.107 ip=10.250.202.107

[kube_control_plane]
controlpalne-prd-1
controlpalne-prd-2
controlpalne-prd-3

[etcd]
controlpalne-prd-1
controlpalne-prd-2
controlpalne-prd-3

[kube_node]
controlpalne-prd-1
controlpalne-prd-2
controlpalne-prd-3

worker-prd-1
worker-prd-2
worker-prd-3

[calico_rr]


[k8s_cluster:children]
kube_control_plane
kube_node

calico_rr
```
- Runtime 변경 설정
  kubernetes에서 docker를 더이상 지원하지 않기 때문에 , containerd로 변경합니다.
```
# /kubespray-2.17.1/inventory/local/group_vars/k8s_cluster/k8s-cluster.yml 파일 수정
container_manager: docker > container_manager: containerd로 변경 저장
```
- ETCD 변경 설정
```
# /kubespray-2.17.1/inventory/local/group_vars/etcd.yml 파일 수정
etcd_deployment_type: docker > etcd_deployment_type: host로 변경 저장
```
## 3. kubespray로 k8s 설치
```
# kubespray 설치한 폴더로 이동
$ cd /home/centos/kubespray/kubespray-2.17.1

# ansible 명령어로 kubespray 실행
# pem key 파일 경로를 private-key 세팅값에 추가 합니다.
$ ansible-playbook -i inventory/mycluster/inventory.ini ./cluster.yml --flush-cache -b -v \
--private-key=~/.ssh/id_rsa
```

## 4. user 명령어 허옹
kubespray로 k8s를 설치한다면 kubectl 권한이 sudo 에만 부여되어 있습니다.

user 계정에서 아래 명령어를 작성하여 user에게 kubectl권한을 부여합니다.
```
kubectl 명령어 허용 작업
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
```

## 4. 발생 문제점

### 1. The conditional check ''127.0.0.1' | ipaddr' failed. The error was: The ipaddr filter requires python's netaddr be installed on the ansible controller
- netaddr 설치
```
$ pip3 install netaddr
```

### 2. ansible 버전 에러

- ansible 버전이 맞지 않아 명령어가동작하지 않는 경우   
 --extra-vars maximal_ansible_version=2.13.0 으로 최대 버전을 늘려준다.
```
$ ansible-playbook -i inventory/mycluster/inventory.ini ./cluster.yml --flush-cache -b -v --private-key=~/.ssh/id_rsa --extra-vars maximal_ansible_version=2.13.0
```

### 3. "No package matching 'aufs-tools' is available"

- 상단에 사용할 컨테이너 런타임을 변경시켜주지 않아서 발생하는 문제이다.
다시 위로 올라가서 docker를 containerd로 변경 시켜 주자. etcd파일도 같이 수정시킨다.