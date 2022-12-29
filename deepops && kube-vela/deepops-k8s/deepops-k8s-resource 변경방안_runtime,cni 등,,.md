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

## container runtime 변경 방안
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

기존 kubespray에서 container runtime을 변경하기 위해선 두가지 설정을 변경해야 합니다.
1. container_manager
2. etcd deployment option

이 두가지를 ansible script에 추가하는 방법으로 container runtime을 변경합니다.
- 22.01버전 이상은 container_manager option이나 etcd deploy task관련 설정 부분이 ansible yml에 존재하기때문에 , containerd로 바꿔주기만 하면 됩니다.

## 1. deepops ansible 수정
deepops의 하위버전 (22.01 이하) 들에서는 containerd 관련 설정이 없습니다.

그러나 현재 구성되어있는 deepops의 version이 22.01 이기 때문에 , containerd 관련 설정을 ansible script에 추가해야 합니다.

**만약 deepops version이 22.01 이상이라면 , container manager만 containerd로 변경시키면 됩니다.**

### 1.1 ~/deepops/playbooks/k8s-cluster.yml 수정
deepops의 k8s-cluster.yaml은 파일이 두개가 잇는데 , 둘다 수정해야 합니다.

먼저 playbook의 k8s-cluster.yml파일을 수정하여 etcd deployment type을 containerd에 맞게 (host) 변경할 수 있도록 스크립트를 추가합니다.

위치는 아래와 같습니다.
```bash 
$ vi ~/deepops/playbooks/k8s-cluster.yml
```

아래는 기존 k8s-cluster.yml에선 container runtime을 선택할 수있는 부분이 없습니다. 
따라서 hosts: all 파일에 추가해 주어야 합니다.
```yaml
# Install 'sshpass' program for: https://github.com/ansible/ansible/issues/56629
- hosts: all
  gather_facts: true
  tasks:
    - name: install epel
      package:
        name: epel-release
        state: present
      when: ansible_os_family == "RedHat"
    - name: install sshpass
      package:
        name: sshpass
        state: present
  environment: "{{proxy_env if proxy_env is defined else {}}}"
  tags:
    - bootstrap
```

아래와 같이 container runtime 변경 기능을 추가합니다.
- crio일 경우 , tasks를 하나 더 생성해두면 될 것 같습니다. ( 테스트 필요 )
```yaml
# Make sure Kubespray submodule is correct
- hosts: all
  gather_facts: false
  tasks:
    - name: make sure kubespray is at the correct version
      command: git submodule update --init
      args:
        chdir: "{{ playbook_dir | dirname }}"
      delegate_to: localhost
      run_once: true
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
  vars:
    ansible_become: no
    ansible_connection: local
  tags:
    - local
```

### 1.2 ~/deepops/config/group_vars/k8s-cluster.yml 수정
container runtime을 변경할 수 있도록 input option을 추가합니다.

이후 container runtime을 변경시키고 싶다면, 해당 파일에서 containerd를 docker로 변경하거나 , tasks에 crio를 추가하여 crio로 변경해 주면 됩니다.

```bash 
$ vi ~/deepops/config/group_vars/k8s-cluster.yml
# see: https://github.com/kubernetes/community/blob/master/contributors/devel/sig-storage/flexvolume.md
kubelet_flexvolumes_plugins_dir: /usr/libexec/kubernetes/kubelet-plugins/volume/exec

# container_manager option을 추가합니다.
container_manager: containerd 

# Provide option to use GPU Operator instead of setting up NVIDIA driver and
# Docker configuration.
deepops_gpu_operator_enabled: false
```

## 2. ansible playbook 실행
kubespray change runtime playbook을 통해서 ansible을 실행합니다.

custom playbook이기때문에 deepops/playbooks 경로에 넣어줍니다.
- [change-runtime-playbook.yml](https://github.com/jjsair0412/kubernetes_info/blob/main/deepops%20%26%26%20kube-vela/deepops-k8s/change-runtime-playbook.yml)

```bash 
$ (env) ubuntu@jjs: $ ansible-playbook -l k8s-cluster ~/deepops/playbooks/change-runtime-playbook.yml
```


## 3. 결과 확인
containerd로 runtime이 정상 변경된것을 확인합니다.

```bash 
$ kubectl get nodes -o wide
NAME     STATUS   ROLES                  AGE    VERSION   INTERNAL-IP    EXTERNAL-IP   OS-IMAGE             KERNEL-VERSION      CONTAINER-RUNTIME
mgmt01   Ready    control-plane,master   113m   v1.21.6   10.0.0.2       <none>        Ubuntu 20.04.4 LTS   5.15.0-41-generic   containerd://1.4.9
mgmt02   Ready    <none>                 112m   v1.21.6   10.0.0.2       <none>        Ubuntu 20.04.4 LTS   5.15.0-41-generic   containerd://1.4.9
```

## ETC 
### troubleshooting 
calico pod에서 9099포트 이미 사용한다고 crashloopbackoff 에러낫을 경우

바인딩 되어있는 9099포트 kill 한다.

```bash 
$sudo netstat -lntp | grep 9099
tcp        0      0 127.0.0.1:9099          0.0.0.0:*               LISTEN      129776/calico-node  

$sudo kill -9 129776
```

calico pod 정상상태 확인

```bash
$kubectl get pods -n kube-system
NAME                                         READY   STATUS    RESTARTS   AGE
calico-kube-controllers-7c5b64bf96-rxfvg     1/1     Running   0          68m
calico-node-dpl9x                            1/1     Running   0          2m29s
calico-node-wkwft                            1/1     Running   0          2m29s
coredns-657959df74-dgvdn                     1/1     Running   0          68m
coredns-657959df74-mzr7j                     1/1     Running   0          68m
dns-autoscaler-b5c786945-jp8zt               1/1     Running   0          68m
kube-apiserver-node01                        1/1     Running   2          83m
kube-controller-manager-node01               1/1     Running   1          83m
kube-proxy-4h4s6                             1/1     Running   0          67m
kube-proxy-hdwtk                             1/1     Running   0          67m
kube-scheduler-node01                        1/1     Running   1          83m
kubernetes-dashboard-758cb8b4db-2lvw6        1/1     Running   0          68m
kubernetes-metrics-scraper-6844f9956-k7vvb   1/1     Running   0          68m
nginx-proxy-node02                           1/1     Running   1          83m
nodelocaldns-9d6l6                           1/1     Running   0          83m
nodelocaldns-tcpvg                           1/1     Running   0          83m
```