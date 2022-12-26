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
## Prerequirement
deepops는 kubespray를 사용하긴 하지만 , 모든 버전을 업그레이드 할 수 있는것은 아닙니다.

deepops의 버전에 맞는 kubespary 버전만 업그레이드가 가능 하며 , 업그레이드 가능 버전 확인은 아래 경로에서 할 수 있습니다.

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
| ubuntu 20.04 | 4core 8GB | v1.18.9 | 20.10 | docker://19.3.12 |

업그레이드 대상 k8s version은 아래와 같습니다.

**목표 version 정보**
| os | 사양 | k8s version | deepops version | container runtime |
|--|--|--|--|--|
| ubuntu 20.04 | 4core 8GB | v1.19.0 | 20.10 | docker://19.3.12 |

## 1. inventory 구성
deepops를 통해 cluster를 구성하였다면 , inventory가 미리 구성되어 있겠지만 구성이 제대로 되어있는지 확인합니다.

submodule에 위치한 kubespray playbook inventory는 변경하지 않고 , deepops의 config inventory만 변경합니다.

```
$ vi /deepops/config/inventory
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
ansible_ssh_private_key_file='~/.ssh/id_rsa'
```

## 2. k8s upgrade 
ansible을 통해 만들어둔 inventory 기반으로 k8s upgrade를 수행합니다.

kube_version에 업그레이드 대상 버전을 기입합니다.
```
# deepops 디렉터리로 이동
$ cd ./deepops

# k8s version upgrade
$ ansible-playbook -l k8s-cluster submodules/kubespray/upgrade-cluster.yml -e kube_version=v1.19.0 -e upgrade_cluster_setup=true --limit=node1
```

## 3. upgarde 결과 확인
kubectl get 명령어로 버전이 정상적으로 업그레이드 되었는지 확인합니다.

```
$ kubectl get nodes -o wide
```
