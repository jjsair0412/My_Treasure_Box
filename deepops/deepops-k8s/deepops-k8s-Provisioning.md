# deepos-k8s-cluster 구축 방안
해당 문서는 deepops를 통해 K8S를 배포하는 방안에 대해 기술합니다.

## Prerequirement
kube-vela의 MLOps 기능을 사용하기 위해서 k8s version은 20.01 이하여야만 합니다.

따라서 deepops의 20.10 version 으로 구축합니다.

20.10은 k8s 버전이 v1.18.9 버전으로 설치됩니다.
**설치 환경**
| os | 사양 | k8s version | deepops version | container runtime |
|--|--|--|--|--|
| ubuntu 20. | 4core 8GB | v1.21.6 | 22.01 | docker://20.10.8 |


## 01. 설치 전 환경 구성
deepops는 ansible을 바탕으로 각종 솔루션 ( slurm , k8s 등 ) 을 배포하게 됩니다.

ansible은 차후에 실행할 스크립트 파일에서 같이 설치되어 source 명령어로 사용을 등록합니다.
```
# update
$ sudo yum update -y

# python3 설치
$ sudo yum install python3 -y

$ python3 --version
Python 3.10.4

# git 설치
$ sudo yum install git -y
```

## 02. deepops 구성
deepops의 github를 clone합니다.

```
$ git clone --recurse-submodules https://github.com/NVIDIA/deepops
# git version 2.16.2 와 이전버젼은 --recursive 를 사용
```

받아온 폴더로 이동 후 , checkout으로 deepops 설치 대상 version으로 이동합니다.

```
$ cd deepops

$ git checkout 22.01
```

submodule을 update 합니다.

deepops의 release를 변경시킬 때 마다 submodule을 update해야합니다.

```
$ cd deepops

$ git submodule update
```

config 폴더를 복사합니다.
```
$ cp -rfp config.example config
```

복사 결과 디렉터리 구조 확인합니다.

```
$ ls
ansible.cfg  config          docs     playbooks  roles    src         virtual
CLA          config.example  LICENSE  README.md  scripts  submodules  workloads
```

설치 전 , scripts/setup.sh 스크립트를 실행해 ansible 설치 및 기본 폴더를 구성합니다.

```
$ cd deepops/scripts

# bash나 source명령어 없이 그냥 수행
$ ./setup.sh
```

setup 스크립트 수행 후 출력되는 source 명령어를 command line에 입력해야 합니다 !

그래야 현재 ubuntu user에서 ansible 명령어를 수행할 수 있습니다.
```
$ source /opt/deepops/env/bin/activate

# 위 source 명령어 수행 시 , 아래와 같이 (env) 가 추가됨
$ (env) ubuntu@jjs:~/deepops $ 
```
## 03. K8S cluster 구성
### 3.0 ssh key 생성
각 노드들에게 접속할 수 있도록 ssh key를 생성한 뒤 , 노드들의 authorized_keys 파일에 pub key를 등록하여 ssh 연결을 할 수 있도록 설정합니다.

```
# ssh key 생성
$ ssh-keygen -t rsa

$ cd ~/.ssh
$ ls
authorized_keys  id_rsa  id_rsa.pub  known_hosts

# 나머지 모든 노드들에게 id_rsa.pub 키 복사 붙여넣기
$ cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys
```

### 3.1 inventory 설정
deepops를 실행시키기 위해 configration파일을 수정해야 합니다.

inventory파일을 열어서 항목을 수정합니다.

node1번으로 K8S cluster를 구성하고 , slurm cluster는 구성하지 않기에 설정하지 않습니다.

ssh 연결을 위해서 pem key의 경로를 기입합니다.
```
[all]
mgmt01    ansible_host=10.128.0.10 # ssh reachable한 manager IP 주소
gpu01     ansible_host=10.128.0.11 # ssh reachable한 gpu server IP 주소
 
[etcd]
mgmt01
 
[kube-master]
mgmt01
 
[kube-node]
gpu01
 
[all:vars]
# SSH User
ansible_user=deepops #ssh 접속 가능 계정
ansible_ssh_private_key_file='~/.ssh/id_rsa' # key file path (pub)
```

## 3.2 K8S Cluster 구성
ansible을 통해 만들어둔 inventory 기반으로 cluster를 구성합니다.

```
# deepops 디렉터리로 이동
$ cd ./deepops

# k8s 구성
$ ansible-playbook -l k8s-cluster playbooks/k8s-cluster.yml
```

## 4 설치 결과 확인
kubectl get 명령어로 설치 결과를 확인합니다.

```
$ kubectl get nodes -o wide
NAME    STATUS   ROLES    AGE     VERSION   INTERNAL-IP   EXTERNAL-IP   OS-IMAGE             KERNEL-VERSION      CONTAINER-RUNTIME
node1   Ready    master   2m55s   v1.18.9   172.25.0.81   <none>        Ubuntu 20.04.4 LTS   5.15.0-41-generic   docker://19.3.12
```

## ETC. known Issue
### 1. ansible.posix.firewalld error
엔서블 수행하여 k8s provisioning 시 아래와 같은 에러 발생

```
ERROR! couldn't resolve module/action 'ansible.posix.firewalld'. This often indicates a misspelling, missing collection, or incorrect module path.

The error appears to be in '/home/ubuntu/deepops/roles/nfs/tasks/firewall.yml': line 17, column 3, but may
be elsewhere in the file depending on the exact syntax problem.

The offending line appears to be:


- name: configure firewall to allow NFS server
  ^ here
```

ansible galaxy로 ansible.posix 설치

```
$ ansible-galaxy collection install ansible.posix
```

### 2. ERROR! the role 'DeepOps.chrony' was not found
엔서블 수행하여 k8s provisioning 시 아래와 같은 에러 발생

```
ERROR! the role 'DeepOps.chrony' was not found in /home/ubuntu/deepops/playbooks/generic/roles:/home/ubuntu/deepops/roles/galaxy:/home/ubuntu/deepops/roles:/home/ubuntu/deepops/submodules/kubespray/roles:/home/ubuntu/deepops/playbooks/generic

The error appears to be in '/home/ubuntu/deepops/playbooks/generic/chrony-client.yml': line 8, column 15, but may
be elsewhere in the file depending on the exact syntax problem.

The offending line appears to be:

      include_role:
        name: DeepOps.chrony
              ^ here
```

- scripts/setup.sh 스크립트 재 실행하여 구성요소 설치 필요
