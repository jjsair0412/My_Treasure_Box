# deepos-k8s-cluster 구축 방안
해당 문서는 deepops를 통해 K8S를 배포하는 방안에 대해 기술합니다.

## Hardware Requirements
deepops의 공식 문서에서 제시하는 vm 최소 사양은 다음과 같습니다.

Total: 8 vCPU, 22 GB RAM, 96 GB Storage

- virtual-login01: 2 vCPU, 2GB RAM and 32GB Storage
- virtual-mgmt01: 4 vCPU, 4GB RAM and 32GB Storage
- virtual-gpu01: 2 vCPU, 16GB RAM and 32GB Storage

## Prerequirement
kube-vela의 MLOps 기능을 사용하기 위해서 k8s version은 20.01 이하여야만 합니다.

따라서 deepops의 20.10 version 으로 구축합니다.

20.10은 k8s 버전이 v1.18.9 버전으로 설치됩니다.

**설치 환경**
| os | 사양 | k8s version | deepops version | container runtime |
|--|--|--|--|--|
| ubuntu 20.04 | 4core 8GB | v1.21.6 | 22.01 | docker://20.10.8 |


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

***script를 수행한 후 exit으로 bash에서 빠져나온 뒤 , 출력되는 source 명령어를 command line에 입력해야 합니다 !***
- ***ansible을 따로 설치해서 수행하면 python import error 발생함***

그래야 현재 ubuntu user에서 ansible 명령어를 수행할 수 있습니다.
```
$ source /opt/deepops/env/bin/activate

# 위 source 명령어 수행 시 , 아래와 같이 (env) 가 추가됨
$ (env) ubuntu@jjs:~/deepops $ 
```

pip markupsafe version 2.0.1로 downgrade

```bash
$ (env) ubuntu@jjs:~/deepops $ pip install markupsafe==2.0.1
Collecting markupsafe==2.0.1
  Downloading MarkupSafe-2.0.1-cp38-cp38-manylinux2010_x86_64.whl (30 kB)
Installing collected packages: markupsafe
Successfully installed markupsafe-2.0.1
```

ansible firewall을 설치하여 galaxy roles들이 추가될 수 있도록 합니다.

```bash
(env) ubuntu@jjs: ansible-galaxy collection install ansible.posix
```

그리고 다시 스크립트를 실행합니다.


```bash
./scripts/setup.sh
```

구성 완료시 나오는 output은 다음과 같습니다.

```bash
Configuration directory '/home/ubuntu/deepops/scripts/../config' exists, not overwriting
Updating Ansible Galaxy roles...

PLAY [Ansible Ad-Hoc] *****************************************************************************

TASK [lineinfile] *********************************************************************************
changed: [localhost]

PLAY RECAP ****************************************************************************************
localhost                  : ok=1    changed=1    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   


*** Setup complete ***
```

exit 명령어로 해당 환경에서 빠져나옵니다.
```bash
$ exit
```

**complete가 출력되고 난 이후에 source 명령어를 한번 더 입력해야 에러가 출력되지 않습니다 !!**

```bash
$ source /opt/deepops/env/bin/activate
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

## 3.2 container runtime 구성
설치 대상이 되는 deepops version 22.01은 container runtime설정이 docker가 default 입니다.

따라서 ansible 구성을 조금 변경시켜주고 , containerd로 변경시켜주어야 합니다.

### 3.2.1 ectd deploy설정 추가
k8s-cluster에 etcd deploy 설정 변경가능 부분을 추가합니다.

containerd는 etcd_deploy 옵션이 host이여야만 합니다.

파일 맨 위에 추가합니다.
```yaml
$ vi ~/deepops/playbooks/k8s-cluster.yml
---
# Kubernetes Cluster Playbook

# Set facts depending on container runtime
# Use GPU operator when container runtime is not docker
# etcd_deployment_type must be `host` when container_manager is not `docker`
- hosts: all
  tasks:
    - name: Set facts when not using docker container runtime (default)
      set_fact:
        deepops_gpu_operator_enabled: true
        etcd_deployment_type: host
      when:
        - container_manager is defined
        - container_manager != "docker"
    - name: Set facts when using Docker container runtime
      set_fact:
        etcd_deployment_type: docker
        gpu_operator_default_runtime: "docker"
      when:
        - container_manager is defined
        - container_manager == "docker"
...
```

### 3.2.2 group_vars 수정
group_vars에 container runtime input option을 추가합니다.

```yaml
$ vi ~/deepops/config/group_vars/k8s-cluster.yml
...
# see: https://github.com/kubernetes/community/blob/master/contributors/devel/sig-storage/flexvolume.md
kubelet_flexvolumes_plugins_dir: /usr/libexec/kubernetes/kubelet-plugins/volume/exec

# container_manager option을 추가합니다.
container_manager: containerd 

# Provide option to use GPU Operator instead of setting up NVIDIA driver and
# Docker configuration.
deepops_gpu_operator_enabled: false
...
```

container_manager 옵션에 docker가 오느냐 , crio가 오느냐 , containerd가 오느냐에 따라서 runtime이 변경되게 됩니다.

## 3.3 K8S Cluster 구성
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
node1   Ready    master   2m55s   v1.18.9   10.0.0.2      <none>        Ubuntu 20.04.4 LTS   5.15.0-41-generic   docker://19.3.12
```

## ETC. known Issue
### 0. ImportError: cannot import name 'soft_unicode' from 'markupsafe'
ansible 명령어 수행 시 아래와 같은 에러 발생

```bash
Traceback (most recent call last):
  File "/opt/deepops/env/bin/ansible-playbook", line 62, in <module>
    import ansible.constants as C
  File "/opt/deepops/env/lib/python3.8/site-packages/ansible/constants.py", line 12, in <module>
    from jinja2 import Template
  File "/opt/deepops/env/lib/python3.8/site-packages/jinja2/__init__.py", line 12, in <module>
    from .environment import Environment
  File "/opt/deepops/env/lib/python3.8/site-packages/jinja2/environment.py", line 25, in <module>
    from .defaults import BLOCK_END_STRING
  File "/opt/deepops/env/lib/python3.8/site-packages/jinja2/defaults.py", line 3, in <module>
    from .filters import FILTERS as DEFAULT_FILTERS  # noqa: F401
  File "/opt/deepops/env/lib/python3.8/site-packages/jinja2/filters.py", line 13, in <module>
    from markupsafe import soft_unicode
ImportError: cannot import name 'soft_unicode' from 'markupsafe' (/opt/deepops/env/lib/python3.8/site-packages/markupsafe/__init__.py)
```

아래 명령어를 수행하여 pip version 낮추는것으로 해결

```
$ pip install markupsafe==2.0.1
```

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
