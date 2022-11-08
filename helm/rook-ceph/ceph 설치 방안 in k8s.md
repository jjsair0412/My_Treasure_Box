# rook-ceph 설치 방안
## 1. precondition
### 1.1 introducing install process
ceph 설치방안은 두가지 방법이 있습니다.
1. ceph sample file을 통한 설치
2. ceph helm install

helm chart로 설치하는것 보다 , sample file을 create하는 방법으로 설치하는 편이 디스트 메모리를 덜 사용하기에 해당 문서는 sample을 배포하는 형태로 ceph를 배포합니다.

만약 airgap 환경에서 설치해야 한다면, 이미지 레지스트리 경로를 변경해야 하기 때문에 helm chart로 설치하는편이 편합니다.
또한 ceph의 object gateway와 같은 기능들도 helm으로 설치한다면 다 열리기에 , 사용면에 있어 더 편합니다.

helm repo 주소는 다음과 같으며 , 설치 process는 아래 문서와 동일하게 ceph operator 부터 배포한 뒤 ceph-cluster를 배포합니다.
```
helm repo add rook-release https://charts.rook.io/release
```

아래처럼 setting-values.yaml값을 변경한 뒤 helm upgrade를 진행합니다.

```
cephClusterSpec:
  storage:
    nodes:
      - name: "jjs-01" # vm host name
        devices: # specific devices to use for storage can be specified for each node
          - name: "vdb" # vm volume name ( lsblk 명령어 출력 결과 마운트된 볼륨 이름 )
      - name: "jjs-02"
        devices: # specific devices to use for storage can be specified for each node
          - name: "vdb"
      - name: "jjs-03"
        devices: # specific devices to use for storage can be specified for each node
          - name: "vdb"
```
[공식 문서](https://rook.io/docs/rook/v1.9/helm-operator.html)



### 1.2 introducing rook
Rook turns distributed storage systems into self-managing, self-scaling, self-healing storage services. It automates the tasks of a storage administrator: deployment, bootstrapping, configuration, provisioning, scaling, upgrading, migration, disaster recovery, monitoring, and resource management.  
  
Rook uses the power of the Kubernetes platform to deliver its services via a Kubernetes Operator for Ceph.

### 1.3 Ceph Storage Provider

Rook orchestrates the Ceph storage solution, with a specialized Kubernetes Operator to automate management. Rook ensures that Ceph will run well on Kubernetes and simplify the deployment and management experience.

[Rook 공식 사이트](https://rook.io/)

### 1.4 environment required
ceph filesystem을 kubernetes에 설치하기 위해서는 , 아래 환경 요구사항을 충족해야 합니다.
- cluster environment required
	- CPU 6core Memory 16GI
- ceph volume size
	- ceph filesystem의 마운트 대상이되는 volume은 최소 60GI 이상이어야 합니다.
- ceph volume 갯수
	- 최소 3개 이상
		- 각 노드당 한개씩 붙인다면 , 한 클러스터당 한개씩 3개 노드가 위치해야 합니다.
## 2. install ceph
#### 2.1 disk 확인
먼저 , 각 노드에 disk가  정상적으로 생성되었는지 확인합니다.
특정 디렉토리에 disk가 마운트 되어야 하는것이 아니라 , 생성만 되어야 합니다.
마운트는 ceph와 진행합니다.
```
lsblk
```
![ceph-1][ceph-1]
  
[ceph-1]:./images/ceph-1.PNG

#### 2.2 ceph github clone
Ceph 공식 github를 clone 합니다.
```
git clone --single-branch --branch release-1.7 https://github.com/rook/rook.git 
```

rook-ceph 관리용 namespace를 생성합니다.
```
kubectl create ns rook-ceph
```

#### 2.3 deply ceph operator
example file들이 위치한 경로로 이동합니다.
```
cd rook/cluster/examples/kubernetes/ceph 
```

먼저 ceph operator를 배포합니다.
```
kubectl apply -n rook-ceph -f crds.yaml -f common.yaml -f operator.yaml 
```
operator 파드가 모두 running이 된 후에 다음 작업으로 넘어갑니다.
#### 2.4 deply ceph cluster
ceph cluster를 배포합니다.
```
# RBD 설치
kubectl apply -n rook-ceph -f cluster.yaml

# FS 설치
kubectl apply -n rook-ceph -f filesystem.yaml
```
ceph monitoring tool 과 ceph의 storageclass를 생성합니다.
```
kubectl apply -n rook-ceph -f toolbox.yaml 

# RBD storageClass
kubectl apply -n rook-ceph -f csi/rbd/storageclass.yaml

# FS storageClass
kubectl apply -n rook-ceph -f csi/cephfs/storageclass.yaml
```
### 설치 결과 확인
```
kubectl get cephcluster -A
```
위 명령어로 ceph가 설치되며 생성된 CRD를 통해 ceph cluster 상태를 점검합니다.
Health_OK 로 보이면 설치가 완료된 것으로 간주합니다.

### ceph tool box
ceph 명령어를 사용할 수 있는 toolbox또한 같이 배포하였기에 , 사용합니다.
```
kubectl -n rook-ceph exec -it $(kubectl -n rook-ceph get pod -l "app=rook-ceph-tools"  -o jsonpath='{.items[0].metadata.name}')  -- bash
```
대표적으로 ceph -s 명령어로 ceph cluster 상태를 점검합니다.
```
ceph -s
```