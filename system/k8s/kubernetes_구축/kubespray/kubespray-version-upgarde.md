
# Kubespray Upgrade 방안

- kubernetes version upgarde 하는 방법에 대해 설명하는 문서 입니다.
- 참고자료: [Kubespray Upgrade](https://github.com/kubernetes-sigs/kubespray/blob/master/docs/upgrades.md)


## 1. Kubespray Upgrade 설정

### 1.1. Limit 값 활용

- limit 옵션으로 특정 Node 1, 2, 3만 Upgrade가 가능합니다.

```
ansible-playbook upgrade-cluster.yml -b -i inventory/mycluster/inventory.ini -e kube_version=v1.22.1 --limit node1,node2,node3
```

### 1.2. 특정 구성 요소 Upgrade

- Kubernetes의 아래 구성  Component들을 선택하여 Upgrade 할 수 있습니다.
	- Docker
	- Containerd
	- etcd
	- kubelet and kube-proxy
	- network_plugin (such as Calico or Weave)
	- kube-apiserver, kube-scheduler, and kube-controller-manager
	- Add-ons (such as KubeDNS)
- [참고자료](https://github.com/kubernetes-sigs/kubespray/blob/master/docs/upgrades.md#component-based-upgrades)

## 2. Kubespray  Upgrade 방안

- 목표 버전은 신규 Kubesparay Git 버전을 다운로드 하고, 기존에 사용하였던 mycluster inventory 파일을 그대로 옮깁니다.
- mycluster를 신규 버전의 Github을 기준으로 생성하고 수정하여 inventory.ini, kubeadm_certificate_key.creds만을 별도로 옮기고 기존에 변경 하였던 설정 값들만 변경하여 사용하여도 가능합니다.
- 설치 가능한 Component 버전은 roles/download/defaults/main.yml 디렉토리에 위치하고 있습니다.

- inventory 파일을 전체 옮겨 사용하는 방안

```
$ cp kubespray-2.17.1/inventory/mycluster/ kubespray-2.18.0/inventory/
```

- inventory의 설정 값을 변경하고 

```
$ cd kubespray-2.18.0
$ cp -rfp inventory/sample/ inventory/mycluster

# inventory 관련 파일들을 옮기고 올바르게 작성 되었는지 확인 합니다.
$ cp ../kubespray-2.17.1/inventory/mycluster/inventory.ini inventory/mycluster/inventory.ini
$ cp -rf ../kubespray-2.17.1/inventory/mycluster/credentials/ inventory/mycluster/
```

### 2.1. Kubernetes Version Upgrade

- Upgrade 전 Node 상태

```
$ kubectl get nodes -o wide
```

아래 명령어 수행
- --limit 옵션을 이용하면 , 특정 노드만 업그레이드 시킬 수 있습니다.
- --limit 옵션이 없다면 , 전체 노드를 업그레이드 시킵니다.

```
$ ansible-playbook ./upgrade-cluster.yml -b -i inventory/mycluster/inventory.ini -e kube_version=v1.22.1 --private-key=~/.ssh/id_rsa --become --become-user=root --limit node-name
```

- Upgrade 후 Node 상태  확인 합니다.
- master node인 node1, node2, node3만 upgrade가 완료 된 것을 확인할 수 있습니다.

```
# Node 1, 2, 3의 버전 확인
$ kubectl get nodes -o wide
```

### 2.2. Containerd Version Upgrade

- Upgrade 전 Node 상태  기록 합니다.
```
$ kubectl get nodes -o wide
```

- Upgrade 실행  node1,node2,node3

```
# 특정 노드만을 업그레이드 시키는 sample ansible-playbook 명령어
$ ansible-playbook -i inventory/mycluster/inventory.ini ./upgrade-cluster.yml --flush-cache -v \
  --private-key=~/.ssh/id_rsa --become --become-user=root --tags=containerd -e container_manager=containerd -e  containerd_version=1.4.12 --limit node-name
```

- Upgrade 후 Containerd 버전을 확인 합니다.

```
$ kubectl get nodes -o wide
```

## 3. Issue

- 만약 Download 시 멈춤 현상이 발생 할 경우 (속도가 KB로 떨어져 굉장히 느려 질 경우에 확인)

```
TASK [container-engine/nerdctl : download_file | Starting download of file] *****************************************************************************************************************
ok: [node1] => {
    "msg": "https://github.com/containerd/nerdctl/releases/download/v0.15.0/nerdctl-0.15.0-linux-amd64.tar.gz"
}
Friday 06 May 2022  14:38:01 +0900 (0:00:00.687)       0:01:25.044 ************

TASK [container-engine/nerdctl : download_file | Set pathname of cached file] ***************************************************************************************************************
ok: [node2] => {"ansible_facts": {"file_path_cached": "/tmp/kubespray_cache/nerdctl-0.15.0-linux-amd64.tar.gz"}, "changed": false}
Friday 06 May 2022  14:38:01 +0900 (0:00:00.689)       0:01:25.733 ************

TASK [container-engine/nerdctl : download_file | Create dest directory on node] *************************************************************************************************************
ok: [node2] => {"changed": false, "gid": 0, "group": "root", "mode": "0755", "owner": "root", "path": "/tmp/releases", "secontext": "unconfined_u:object_r:user_tmp_t:s0", "size": 73, "state": "directory", "uid": 0}

```
```
# node에 SSH 접속
cd /tmp/releases

/tmp/releases 디렉토리에서 강제 설치 후 Ansible 명령 재실행
$ wget https://github.com/containerd/nerdctl/releases/download/v0.15.0/nerdctl-0.15.0-linux-amd64.tar.gz
```

