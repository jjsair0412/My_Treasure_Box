# deepops-k8s-resource 변경방안_runtime,cni 등,,
해당 문서는 deepops를 통해 k8s를 Provisioning 한 이후 , container runtime 및 cni 등의 속성을 변경하는 방안에 대해 기술합니다.

deepops는 k8s를 kubespray로 배포 및 관리하기 때문에 , kubespray의 ansible 명령어 방식을 그대로 따라갑니다.

ansible 명령어를 사용할 때 , (env) 가 붙어있는지 꼭 확인해아 합니다.
- 안붙어있다고 해서 ansible을 설치하면, python import 에러가 발생할 수 있습니다.
- 안붙어있을때에는 , source 명령어로 ansible 명령어를 사용할 수 있게끔 변경합니다.

```bash 
# source 명령어 수행
$ source /opt/deepops/env/bin/activate

# 위 source 명령어 수행 시 , 아래와 같이 (env) 가 추가됨
$ (env) ubuntu@jjs:~/deepops $ 
```

## index
- [container runtime 변경 방안](#1-container-runtime-변경-방안)

## Prerequirement
기 구축된 정보는 다음과 같습니다.

**기 구축 환경**
| os | 사양 | k8s version | deepops version | container runtime | role | ip addr |
|--|--|--|--|--|--|--|
| ubuntu 20.04 | 4core 8GB | v1.21.6 | 22.01 | docker://19.3.12 | control plane | 10.0.0.2 |
| ubuntu 20.04 | 2core 4GB | v1.21.6 | 22.01 | docker://19.3.12 | worker | 10.0.0.3 |

## 1. container runtime 변경 방안
- 참고 문서 : [kubespray_migrate_docker2contianerd.md](https://github.com/kubernetes-sigs/kubespray/blob/master/docs/upgrades/migrate_docker2containerd.md)

목표 환경은 다음과 같습니다.
- container runtime을 containerd로 변경합니다.
- containerd가 아닌 crio 등의 다른 container runtime을 변경할 때에도 방식은 동일합니다.
- 설정파일을 변경해 줄 때 containerd가아니라 crio로 해두면 됩니다.

**목표 환경**
| os | 사양 | k8s version | deepops version | container runtime | role | ip addr |
|--|--|--|--|--|--|--|
| ubuntu 20.04 | 4core 8GB | v1.21.6 | 22.01 | containerd | control plane | 10.0.0.2 |
| ubuntu 20.04 | 2core 4GB | v1.21.6 | 22.01 | containerd | worker | 10.0.0.3 |

### 1.1 group_vars 수정
submodules에 위치한 kubespray 관련 설정파일들을 변경시켜주어야 합니다.

먼저 k8s-cluster.yml 파일을 수정합니다.

```bash 
$ cd ~/deepops/submodules/kubespray/inventory/sample/group_vars/k8s_cluster

# container manager 수정
$ vi k8s-cluster.yml
container_manager: docker > container_manager: containerd로 변경 저장

# resolvconf_mode 변경 저장
resolvconf_mode: docker_dns > resolvconf_mode: host_resolvconf . host_resolvconf로 변경 저장
```

etcd의 etcd_deployment_type을 변경합니다.

```bash 
$ cd ~/deepops/submodules/kubespray/inventory/sample/group_vars

$ vi etcd.yml
etcd_deployment_type: docker > etcd_deployment_type: host로 변경 저장
```

### 2. docker 관련 설정변경

### 2.1 docker , kubelet 정지
docker service와 kubelet service를 정지시킵니다.

```bash 
sudo service kubelet stop
sudo service docker stop
```

### 2.2 uninstall docker + dependencies 
docker와 의존성 패키지들을 같이 제거합니다.

```bash 
$ sudo apt-get remove -y --allow-change-held-packages containerd.io docker-ce docker-ce-cli docker-ce-rootless-extras
 ```

### 1.2 k8s 재 배포
ansible 명령어를 통해 k8s를 재 배포합니다.

이때 deepops의 k8s-cluster.yml이 아닌 submodules/kubespray의 cluster.yml 파일로 ansible 명령어를 수행해야 합니다.

또한 inventory는 미리 만들어두었던 deepops의 inventory를 사용해야 합니다.
- worker , master 한개만 limit 걸어서 수행하는것이 자꾸 에러가 남. 테스트 후 하나씩만 업그레이드 및 변경하는 방안 테스트 예정
```bash 
# kubespray ansible 위치로 이동
$ cd ~/deepops/submodules/kubespray

# ansible 명령어 수행
(env) ubuntu@jjs: $ ansible-playbook -i inventory/sample/inventory.ini cluster.yml --limit=mgmt01,mgmt02
```
