# Rancher Backup - helm
### 0. 관련 정보
1. rancher backup은 기본으로 s3 버킷을 사용한다. s3버킷에 사용자가 생성한 모든 백업파일을 저장하게 된다.

3. values.yaml파일에 persistence를 enable로 두었을 경우, pvc를 생성할 때 mountPath를 /var/lib/backups으로 설정해야 한다.
4. pv 사이즈는 기본적으로 2Gi 고정이고 , 특정 storageClasses만 pv 크기 조절이 허용된다.
- https://kubernetes.io/blog/2018/07/12/resizing-persistent-volumes-using-kubernetes/ 위 내용 여기 참조
5. values.yaml의 persistence enable true 로 주면 pvc가 생성된다. 여기 옵션을 뭘로주느냐에 따라서 자유자제로 저장공간을 바꿀 수 있을듯 
6. 백업과 restore는 모두 yaml파일을 통해서 동작한다.
7. 백업 yaml파일을 렌처에서 작성해 create하면 백업파일이 생성된다. 
 당연히 cattle-resources-system ns의 pod들이 실제 올라가있는 node에 생성되게 된다.
 
8. rancher ui에서 백업을 삭제하더라도 , 백업 tar 파일은 삭제되지 않는다.


###  1. 설치 순서
1. 관리용 namespace 생성
2. rancher-charts/rancher-backup-crd helm 이용 설치
 3. rancher-charts/rancher-backup helm 이용 설치
## 2. Reference
https://rancher.com/docs/rancher/v2.5/en/backups/

## 3. Prerequisites
### 3.1 Create namespace
- rancher-backup 관리용 ns 생성
```
$ kubectl create ns cattle-resources-system
```
- ns 생성 확인
```
$ kubectl get ns | grep cattle-resources-system
```

## 4. Install rancher-backup with Helm
### 4.1 helm repo 추가
```
helm repo add rancher-charts https://charts.rancher.io
helm repo update

helm repo list
# list 결과
rancher-charts  https://charts.rancher.io
```
### 4.2 helm search
- helm search 명령어를 통해 rancher-backup 버전 확인
```
helm search repo rancher-charts | grep rancher-backup
rancher-charts/rancher-backup                   2.1.1                           2.1.1           Provides ability to back up and restore the Ran...
rancher-charts/rancher-backup-crd               2.1.1                           2.1.1           Installs the CRDs for rancher-backup.
```
### 4.3 helm pull
- values.yaml파일 수정 및 확인 위해 pull
```
# helm pull
helm pull rancher-charts/rancher-backup --version 2.1.1

# 압축해제
tar -xf ./rancher-backup-2.1.1.tgz
```
### 4.4 Persistent volume 생성
- rancher-backup 설치 시 , 설치되는 pvc와 연결되는 pv 생성
- 백업파일 저장 경로 지정 가능. 
- 아래 yaml은 hostPath에 백업 파일을 저장하는 예시.
```
apiVersion: v1
kind: PersistentVolume
metadata:
  name: rancher-backup-pv
spec:
  capacity:
    storage: 2Gi
  volumeMode: Filesystem
  accessModes:
    - ReadWriteOnce
  hostPath:
    path: /home/centos/rancherBackup/backupfile # 백업파일 저장할 경로 .
```
### 4.5 helm install
- 백업파일이 저장되는 공간은 , 기본적으로 s3가 default이다.
- values.yaml파일에는 false로 s3가 설정되어있기 때문에 만들지는 않지만, 사용하고싶다면 values.yaml을 수정하자.
- 여기서는 hostPath를 사용한다.
- storageClass를 설정해서 롱혼과 같은 스토리지를 등록하는것 또한 가능하다.
```
# install rancher-backup-crd
$ helm install rancher-backup-crd rancher-charts/rancher-backup-crd -n cattle-resources-system

# install rancher-backup
$ helm upgrade --install rancher-backup . -n cattle-resources-system \
--set persistence.enabled=true \
--set persistence.volumeName= \
-f values.yaml
```
## 5. Backup & restore



