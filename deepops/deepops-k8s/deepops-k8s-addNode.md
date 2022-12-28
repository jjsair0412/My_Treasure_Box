# deepops-k8s-addNode
해당 문서는 deepops로 구축된 k8s cluster에 ndoe를 추가하는 방안에 대한 가이드 입니다.

## Prerequirement
deepops에선 kubernetes cluster를 구성할 때 kubespray를 사용하기 때문에 , kubespary의 노드 추가 방안을 그대로 따라갑니다.
- submodule에 kubespray ansible 있음

가이드 작성 시 구축되어있는 cluster 정보는 아래와 같습니다.

**기 구축 환경**
| os | 사양 | k8s version | deepops version | container runtime | role | ip addr |
|--|--|--|--|--|--|--|
| ubuntu 20.04 | 4core 8GB | v1.21.6 | 22.01 | docker://19.3.12 | control plane | 10.0.0.2 |

목표 환경은 다음과 같습니다.

**목표 환경**
| os | 사양 | k8s version | deepops version | container runtime | role | ip addr |
|--|--|--|--|--|--|--|
| ubuntu 20.04 | 4core 8GB | v1.21.6 | 22.01 | docker://19.3.12 | control plane | 10.0.0.2 |
| ubuntu 20.04 | 2core 4GB | v1.21.6 | 22.01 | docker://19.3.12 | worker | 10.0.0.3 |

## 1. 사전 구성
먼저 , k8s cluster를 deepops로 구성되어있다는 전제 하에 해당 문서를 작성합니다.
- 구성 환경

```
$ kubectl get nodes -o wide
NAME     STATUS   ROLES                  AGE     VERSION   INTERNAL-IP   EXTERNAL-IP   OS-IMAGE             KERNEL-VERSION      CONTAINER-RUNTIME
mgmt01   Ready    control-plane,master   8m26s   v1.21.6   172.25.0.8    <none>        Ubuntu 20.04.4 LTS   5.15.0-41-generic   docker://20.10.8
```

추가할 worker node에는 ssh 접근이 가능해야 하기 때문에 , 이전에 생성해두었던 id_rsa.pub key를 추가할 노드의 ```~/.ssh/authorized_keys``` 에 복사&붙여넣기 합니다.

id_rsa key로 ssh 연결이 정상 수행 되는지 확인합니다.

```bash
$ ssh -i id_rsa ubuntu@10.0.0.3
```

## 2. inventory 수정
k8s cluster를 Provisioning 할 때 만들어 두었던 inventory에 추가할 node 정보를 기입합니다.

```bash
$ cd ~/deepops/config

$ vi inventory
[all]
mgmt01     ansible_host=10.0.0.3
mgmt02     ansible_host=10.0.0.2 # 추가

######
# KUBERNETES
######
[kube-master]
mgmt01

# Odd number of nodes required
[etcd]
mgmt01

# Also add mgmt/master nodes here if they will run non-control plane jobs
[kube-node]
mgmt02 # mgmt02 node를 worker로 등록

######
# SSH connection configuration
######
[all:vars]
# SSH User
ansible_user=ubuntu
ansible_ssh_private_key_file='~/.ssh/id_rsa'
```

## 3. ansible 명령어로 kubespray 실행
kubespray의 노드 추가 ansible 명령어를 그대로 사용합니다.
- 관련 문서 : https://github.com/kubernetes-sigs/kubespray/blob/master/docs/nodes.md


```--limit=NODE_NAME``` 옵션을 이용해 다른 노드에 영향이가지 않도록 작업 노드를 제한할 수 있습니다.

kubespray의 playbook은 아래 경로에 위치해 있습니다.

```bash
~/deepops/submodules/kubespray
```

ansible 명령어로 노드를 추가합니다.

이때 (env)가 붙어있는 상태여야만 ansible 명령어를 수행할 수 있습니다.
- setup.sh 파일 수행 이후 상태 . 아래와 같아야 함.
- ```$ (env) ubuntu@jjs:~/deepops $ ```
    - 아니라면 아래 명령어 그대로 실행
    - ```source /opt/deepops/env/bin/activate```

```bash
$ ansible-playbook -l k8s-cluster submodules/kubespray/scale.yml
```

## 4. 결과 확인
kubectl 명령어로 노드가 정상적으로 붙었는지 확인합니다.

```bash
$ kubectl get nodes -o wide
NAME     STATUS   ROLES                  AGE     VERSION   INTERNAL-IP   EXTERNAL-IP   OS-IMAGE             KERNEL-VERSION      CONTAINER-RUNTIME
mgmt01   Ready    control-plane,master   109m    v1.21.6   172.25.0.8    <none>        Ubuntu 20.04.4 LTS   5.15.0-41-generic   docker://20.10.8
mgmt02   Ready    <none>                 4m41s   v1.21.6   172.25.0.73   <none>        Ubuntu 20.04.4 LTS   5.15.0-41-generic   docker://20.10.8
```