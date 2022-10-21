# storageClass -> pv -> pvc Retain test
- 해당문서는 로컬 폴더와 pod내부 폴더를 연결하는 pvc , pv를 storageClass로 생성한 이후 , pod가 삭제되고 재 배포 되었을 때 생성한 파일이 그대로 남아있는지 테스트하는 문서 입니다.
## test 환경 구성
### 1. mount할 폴더 생성
- pv가 바라보고있을 폴더를 생성합니다.
```
mkdir /home/centos/pv-test

# 권한부여
chomod 777 /home/centos/pv-test
```
### 2. storageClass 생성
- Retain option을 두고 생성합니다.
```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: local-storage
provisioner: kubernetes.io/no-provisioner
volumeBindingMode: Immediate
reclaimPolicy: Retain
```
### 3. pv 생성
-  pv또한 persistentVolumeReclaimPolicy 옵션을 Retain으로 두어 파드가 삭제되도 제거되지 않도록 구성합니다.
```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: example-pv
spec:
  capacity:
    storage: 20Mi
  accessModes:
  - ReadWriteMany
  persistentVolumeReclaimPolicy: Retain
  storageClassName: local-storage
  local:
    path: /home/centos/pv-test/pv
  nodeAffinity:
    required:
      nodeSelectorTerms:
      - matchExpressions:
        - key: kubernetes.io/hostname
          operator: In
          values:
          - kisti-jjs
```
### 4. pvc 생성
- pvc는 생성한 local storageClass를 사용하도록 구성합니다.
```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: foo-pvc
spec:
  resources:
    requests:
      storage: 20Mi
  accessModes: 
    - ReadWriteMany
  storageClassName: "local-storage" # Empty string must be explicitly set otherwise default StorageClass will be set
```
### 5. pod 생성
- pvc를 마운트하는 파드를 생성합니다.
```
apiVersion: v1
kind: Pod
metadata:
  name: test-local-vol
  labels:
    name: test-local-vol
spec:
  containers:
  - name: app
    image: nginx
    volumeMounts:
      - name: local-persistent-storage
        mountPath: /mnt
  volumes:
    - name: local-persistent-storage
      persistentVolumeClaim:
        claimName: foo-pvc
```
## test 수행
### 1. 파일 생성
- 파드에 exec하여 /mnt 폴더에 파일을 하나 생성합니다.
```
$ kubectl exec test-local-vol -it bash

root@test-local-vol:/# cd /mnt/

root@test-local-vol:/# pwd
/mnt

root@test-local-vol:/mnt# cat > hi2.tst
hello

# control+d로 cat에서 빠져나옵니다.
root@test-local-vol:/mnt# cat hi2.tst 
hello
```
### 2. 파드 제거
- 파드를 delete 합니다.
```
$ kubectl delete pods --all
pod "test-local-vol" deleted
```
- 로컬에 마운트시킨 경로로 이동하여 파일이 있는지 확인합니다.
```
cd /home/centos/pv-test/pv

$ ls
hi2.tst
```
### 3. 파드 재 배포 및 결과 확인
- 파드를 재 배포 합니다.
```
$ kubectl apply -f pod.yaml

$ kubectl get pods
```
- 파드내부 /mnt 폴더로 이동하여 hi2.txt파일이 존재하는지 확인합니다.
```
$ kubectl exec test-local-vol -it bash

$ root@test-local-vol:/# cd /mnt/

root@test-local-vol:/mnt# ls
hi2.tst
```
## test 결과
- 파드가 제거되더라도 마운트된 경로에 파일이 남아있다면 , 제거되지 않습니다.
- 반대의 경우 ( 로컬에 폴더 생성 후 파드 내부에 파일 생성되는지 ? ) 도 동일하게 동기화되어 생성되고 작동합니다. 폴더도 동일하게 생성됩니다.