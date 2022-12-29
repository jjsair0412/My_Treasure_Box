# deepops-k8s-modfiy_nfs
해당 문서는 deepops의 nfs 설정을 변경하는 방안에 대해 기술합니다.

## Prerequirement
deepops를 이용하여 k8s를 배포하면 , ansible이 돌면서 자동으로 nfs client를 설치하여 nfs storageclass를 생성하게 됩니다.

아래 경로의 playbook들 중 , nfs-client-provisioner.yml playbook을 변경합니다.

```bash
$ cd /home/ubuntu/deepops/playbooks/k8s-cluster/nfs-client-provisioner.yml
```

default option은 inventory의 kube-master tag중 0번째 노드에 nfs 서버가 설치되고 , 거기에 마운트 되게 됩니다.

default 마운트 경로는 다음과 같습니다.

```bash
$ pwd
/export/deepops_nfs
```