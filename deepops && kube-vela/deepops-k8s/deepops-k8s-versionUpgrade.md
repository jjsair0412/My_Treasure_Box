# deepos-k8s-version upgrade 방안
해당 문서는 deepops로 구성된 k8s cluster의 버전을 upgrade하는 방안에 대해 기술합니다.

deepops는 k8s version을 업그레이드 하기 위해서 kubespray upgarde 방안을 그대로 따라갑니다.
- [kubespray version upgrade 방안](https://github.com/kubernetes-sigs/kubespray/blob/master/docs/upgrades.md)


아래 공식 문서를 참고하여 작업하였습니다.
- [deepops update](https://github.com/NVIDIA/deepops/blob/master/docs/deepops/update-deepops.md#updating-kubernetes-clusters)

kubespray playbook 위치는 다음과 같습니다.

```
$ cd /submodules/kubespray
```

또한 kubespray는 아래 컴포넌트들을 따로 따로 업그레이드 시킬 수 있습니다.
- docker_version
- containerd_version
- kube_version
- etcd_version
- calico_version
- calico_cni_version
- weave_version
- flannel_version
- kubedns_version

위의 컴포넌트 업그레이드관련 명령어는 다음 URL에 있습니다.
- https://github.com/kubernetes-sigs/kubespray/blob/master/docs/upgrades.md#component-based-upgrades
## Prerequirement
deepops는 kubespray를 사용하긴 하지만 , 모든 버전을 업그레이드 할 수 있는것은 아닙니다.

deepops의 버전에 맞는 kubespary 버전만 업그레이드가 가능 하며 , 업그레이드 가능 버전 확인은 아래 경로에서 할 수 있습니다.

따라서 version을 더 올리고싶다면, deepops 태그를 변경하여 deepops Releases를 변경해야 합니다.

```
$ cat deepops/submodules/kubespray/roles/download/defaults/main.yml
...
# Get kubernetes major version (i.e. 1.17.4 => 1.17)
kube_major_version: "{{ kube_version | regex_replace('^v([0-9])+\\.([0-9]+)\\.[0-9]+', 'v\\1.\\2') }}"
crictl_supported_versions:
  v1.19: "v1.19.0"
  v1.18: "v1.18.0"
...
```

기존 구축된 버전 정보는 아래와 같습니다.

**구축 환경**
| os | 사양 | k8s version | deepops version | container runtime |
|--|--|--|--|--|
| ubuntu 20.04 | 4core 8GB | v1.21.6 | 20.10 | docker://20.10.8 |
| ubuntu 20.04 | 2core 4GB | v1.21.6 | 20.10 | docker://20.10.8 |

업그레이드 대상 k8s version은 아래와 같습니다.

**목표 version 정보**
| os | 사양 | k8s version | deepops version | container runtime |
|--|--|--|--|--|
| ubuntu 20.04 | 4core 8GB | v1.22.0 | 20.10 | docker://20.10.8 |
| ubuntu 20.04 | 2core 4GB | v1.22.0 | 20.10 | docker://20.10.8 |

## 1. inventory 구성
deepops를 통해 cluster를 구성하였다면 , inventory가 미리 구성되어 있겠지만 구성이 제대로 되어있는지 확인합니다.

submodule에 위치한 kubespray playbook inventory는 변경하지 않고 , deepops의 config inventory만 변경합니다.

```bash
$ vi /deepops/config/inventory
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
ansible_user=ubuntu # ssh 접근 가능한 정보
ansible_ssh_private_key_file='~/.ssh/id_rsa'
```

## 2. k8s upgrade 
ansible을 통해 만들어둔 inventory 기반으로 k8s upgrade를 수행합니다.

kube_version에 업그레이드 대상 버전을 기입합니다.

또한 limit 옵션을 이용하여 노드 하나씩 업그레이드 합니다.
- limit=mgmt01로 mgmt01 노드만 업그레이드

```bash
# deepops 디렉터리로 이동
$ cd ./deepops

# k8s version upgrade : mgmt01 node만 업그레이드
$ ansible-playbook -l k8s-cluster submodules/kubespray/upgrade-cluster.yml -e kube_version=v1.22.0 -e upgrade_cluster_setup=true --limit=mgmt01

# mgmt01 node 업그레이드 되었는지 확인
$ kubectl get nodes
NAME     STATUS   ROLES                  AGE    VERSION
mgmt01   Ready    control-plane,master   130m   v1.22.0
mgmt02   Ready    <none>                 25m    v1.21.6
```

완료된 이후 동일 명령어에 limit만 변경하여 mgmt02 노드도 업그레이드 합니다.
- limit로 worker node만 지정했을 때 , kube-control plane node가 없어서 업그레이드 실패
  worker node만 지정해서 업그레이드 수행 및 성공 테스트 필요
```bash
# k8s version upgrade : mgmt02 node만 업그레이드
$ ansible-playbook -l k8s-cluster submodules/kubespray/upgrade-cluster.yml -e kube_version=v1.22.0 -e upgrade_cluster_setup=true --limit=mgmt02,mgmt01


# mgmt02 node 업그레이드 되었는지 확인
$ kubectl get nodes
NAME     STATUS   ROLES                  AGE    VERSION
mgmt01   Ready    control-plane,master   155m   v1.22.0
mgmt02   Ready    <none>                 50m    v1.22.0
```

만약 deepops version이 22.01 이하 버전이라 container runteim을 임의로 docker에서 containerd로 변경시켜 주었다면 , 아래와 같이 etcd deploytype을 ansible 명령어에 환경변수로 너어주어야 합니다.

```bash
# etcd deploy type 지정
$ ansible-playbook -l k8s-cluster submodules/kubespray/upgrade-cluster.yml -e kube_version=v1.22.0 -e upgrade_cluster_setup=true -e etcd_deployment_type=host --limit=node1,node2
```


## 3. upgarde 결과 확인
kubectl get 명령어로 버전이 정상적으로 업그레이드 되었는지 확인합니다.

```bash
$ kubectl get nodes
NAME     STATUS   ROLES                  AGE    VERSION
mgmt01   Ready    control-plane,master   155m   v1.22.0
mgmt02   Ready    <none>                 50m    v1.22.0
```

## ETC
kubespray는 kubeadm을 기반으로 하기 때문에 , minor version upgrade시 1단계씩만 가능합니다.

만약 1.20에서 1.22로 더블 업그레이드를 수행하면 , 아래와 같은 에러로그와 함께 버전 업그레이드가 수행되지 않습니다.

```bash
- Specified version to upgrade to "v1.22.0" is too high; kubeadm can upgrade only 1 minor version at a time
```