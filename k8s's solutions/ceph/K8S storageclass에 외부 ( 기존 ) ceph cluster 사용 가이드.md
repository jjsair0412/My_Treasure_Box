# K8S storageclass에 외부 ( 기존 ) ceph cluster 사용 가이드
## 0. precondition
해당 문서는 외부에 구축된 ceph cluster를 Kubernetes의 storageclass로 등록하는 방안에 대한 가이드 문서 입니다.

**ceph cluster와 k8s cluster는 서로 통신이 가능해야 합니다.**

참고 문서
- [ceph 공식 문서 가이드](https://docs.ceph.com/en/quincy/rbd/rbd-kubernetes/#block-devices-and-kubernetes)
- [redhat 가이드](https://access.redhat.com/documentation/ko-kr/openshift_container_platform/3.11/html/configuring_clusters/ceph-rbd-dynamic-example)

## 1. 설치 환경
ceph cluster는 아래 문서대로 설치하였습니다.
- [cephadm 설치 방안 - cephadm을 통한 삼중화 클러스터링 구성](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/ceph/cephadm%20%EC%84%A4%EC%B9%98%20%EB%B0%A9%EC%95%88%20-%20cephadm%EC%9D%84%20%ED%86%B5%ED%95%9C%20%EC%82%BC%EC%A4%91%ED%99%94%20%ED%81%B4%EB%9F%AC%EC%8A%A4%ED%84%B0%EB%A7%81%20%EA%B5%AC%EC%84%B1.md)

### 1.1 ceph cluster 환경
| externalIP | os | spec | volume size | docker version |
|--|--|--|--|--|
| 10.0.0.2 | ubuntu 20.04 | 2core 4GB | 20GB |  20.10.12  |
| 10.0.0.3 | ubuntu 20.04 | 2core 4GB | 20GB |  20.10.12  |
| 10.0.0.4 | ubuntu 20.04 | 2core 4GB | 20GB |  20.10.12  |

**ceph version**
- quincy

### 1.2 Kubernetes 환경
Docker Desktop을 통해 k8s를 배포한 환경에서 ceph을 연동하였습니다.

- **k8s version은 v1.13 이상이어야만 합니다 !**
- **타 k8s라 하더라도 가이드는 동일합니다.**

| ip | os | kubernetes version |
|--|--|--|
| 10.0.0.5 | mac os m1 | v1.25.2 |

## 2. ceph cluster 작업
### 2.1 create pool
먼저 ceph에서 kubernetes storageclass가 사용할 pool을 생성합니다.
- ceph cluster가 HEALTH_OK 인지 확인한 이후 작업합니다.

```
# ceph osd pool create kubernetes
pool 'kubernetes' created
```

새로 만든 pool은 사용하기 위해서 초기화가 필요합니다.
rdb tool을 통해 pool 초기화를 진행합니다.

```
# rbd pool init kubernetes
```

ceph dashboard의 pools로 이동하여 kubernetes pool이 정상 생성됐는지 확인합니다.

### 2.2 ceph-csi 구성
#### 2.2.1 ceph client auth 구성
k8s 및 ceph-csi 에 대한 사용자를 구성합니다.

아래 명령어를 수행한 이후 결과를 복사하여 저장해 둡니다.

차후 ceph-csi와 ceph cluster가 통신하기 위해 configmap을 생성하는데, 이때 해당 정보가 필요합니다.
- user ID : kubernetes
- user Key : 명령어 수행 결과값

```
# ceph auth get-or-create client.kubernetes mon 'profile rbd' osd 'profile rbd pool=kubernetes' mgr 'profile rbd pool=kubernetes'
[client.kubernetes]
        key = ...==
```

#### 2.2.2 ceph-csi configmap 생성
ceph-csi를 사용하기 위해선 k8s에 저장된 configmap이 필요합니다.

ceph mon의 정보가 configmap에 저장되기 때문에 아래 명령어로 ceph dump를 생성한 후 저장해 둡니다.

```
# ceph mon dump
<...>
fsid b9127830-b0cc-4e34-aa47-9d1a2e9949a8
<...>
0: [v2:10.0.0.2:3300/0,v1:10.0.0.2:6789/0] mon.a
1: [v2:10.0.0.3:3300/0,v1:10.0.0.3:6789/0] mon.b
2: [v2:10.0.0.4:3300/0,v1:10.0.0.4:6789/0] mon.c
```

## 3. k8s cluster 작업
### 3.1 configmap 생성
아래와 같은 형태의 configmap을 생성합니다.
각 요소마다 자신의 ceph cluster mon dump값을 넣어 줍니다.
- clusterID : fsid
- monitors : monitor address

```yaml
$ cat <<EOF > csi-config-map.yaml
---
apiVersion: v1
kind: ConfigMap
data:
  config.json: |-
    [
      {
        "clusterID": "b9127830-b0cc-4e34-aa47-9d1a2e9949a8",
        "monitors": [
          "10.0.0.2:6789",
          "10.0.0.3:6789",
          "10.0.0.4:6789"
        ]
      }
    ]
metadata:
  name: ceph-csi-config
EOF
```

apply 명령어로 configmap 생성합니다.

```bash
$ kubectl apply -f csi-config-map.yaml
configmap/ceph-csi-config created
```

### 3.2 KMS (키 관리 서비스) 설정
최신 버전 ceph-csi는 KMS 공급자의 세부 정보를 정의하기 위한 configmap 개체가 필요합니다.
- 설정해야 합니다.

KMS 개체의 예시는 다음과 같습니다.
- https://github.com/ceph/ceph-csi/blob/devel/examples/kms/vault/kms-config.yaml

```yaml
$ cat <<EOF > csi-kms-config-map.yaml
---
apiVersion: v1
kind: ConfigMap
data:
  config.json: |-
    {}
metadata:
  name: ceph-csi-encryption-kms-config
EOF
```

가이드 문서에서는 이래와 같이 matadata.name만 설정한 뒤 apply하여 KMS 설정값 configmap을 k8s에 등록합니다.

```bash
$ kubectl apply -f csi-kms-config-map.yaml
configmap/ceph-csi-encryption-kms-config created
```

### 3.3 csi 내부 ceph.conf파일 정의
최신 버전의 ceph-csi는, csi 컨테이너 내부에 ceph config를 정의할 ceph.conf 파일을 가진 configmap을 생성해야 합니다.

만약 ceph cluster의 config을 바꾸고 싶다면 , 해당 configmap의 설정을 변경해 주면 됩니다.
- 가이드 문서에서는 아래와 같이 default로 생성합니다.

```yaml
$ cat <<EOF > ceph-config-map.yaml
---
apiVersion: v1
kind: ConfigMap
data:
  ceph.conf: |
    [global]
    auth_cluster_required = cephx
    auth_service_required = cephx
    auth_client_required = cephx
  # keyring is a required key and its value should be empty
  keyring: |
metadata:
  name: ceph-config
EOF
```

apply하여 k8s cluster에 등록합니다.
```bash
$ kubectl apply -f ceph-config-map.yaml 
configmap/ceph-config created
```

### 3.4 ceph-csi cephx secret 생성
ceph-csi는 ceph cluster와 통신하기 위해서 cephx 자격 증명이 필요합니다.

따라서 이전에 생성해준 kubernetes user의 id와 cephx 키를 사용하여 configmap을 생성해 줍니다.
- [ceph client auth 구성](#221-ceph-client-auth-구성)
    - userID : userID
    - userKey : 위 링크에 명령어 수행 결과값의 Result

```yaml
$ cat <<EOF > csi-rbd-secret.yaml
---
apiVersion: v1
kind: Secret
metadata:
  name: csi-rbd-secret
  namespace: default
stringData:
  userID: kubernetes
  userKey: AQD9o0Fd6hQRChAAt7fMaSZXduT3NWEqylNpmg==
EOF
```

생성되면 k8s에 apply하여 configmap 생성합니다.
```
$ kubectl apply -f csi-rbd-secret.yaml
secret/csi-rbd-secret created
```

## 4. ceph-csi plugin 구성
ceph-csi에 필요한 ServiceAccount 및 RBAC ClusterRole / ClusterRoleBinding Kubernetes 개체를 만듭니다.

아래 명령어처럼 미리 준비된 yaml파일을 그대로 사용해도 무관합니다.

```bash
$ kubectl apply -f https://raw.githubusercontent.com/ceph/ceph-csi/master/deploy/rbd/kubernetes/csi-provisioner-rbac.yaml
$ kubectl apply -f https://raw.githubusercontent.com/ceph/ceph-csi/master/deploy/rbd/kubernetes/csi-nodeplugin-rbac.yaml
```

ceph-csi provider node plugin을 아래 yaml파일로 생성합니다.

k8s 환경에 맞게 미리 정의해둘 필요가 없기 때문에 , 아래 명령어 그대로 사용해도 무관합니다.

```bash
$ wget https://raw.githubusercontent.com/ceph/ceph-csi/master/deploy/rbd/kubernetes/csi-rbdplugin-provisioner.yaml
$ kubectl apply -f csi-rbdplugin-provisioner.yaml
$ wget https://raw.githubusercontent.com/ceph/ceph-csi/master/deploy/rbd/kubernetes/csi-rbdplugin.yaml
$ kubectl apply -f csi-rbdplugin.yaml
```

## 5. CEPH BLOCK DEVICE 사용
### 5.1 Kubernetes storageclass 생성
kubernetes storageclass를 생성합니다.

stoageclass관련 설정은 여기서 하면 됩니다.
- 예를들어 reclaimPolicy , VOLUMEBINDINGMODE 등 설정하면 됩니다.


아래 yaml파일로 storageclass를 생성하기 위해선 clusterID option이 ceph cluster의 fsid와 일치해야 합니다.

또한 각 구성요소들은 namespace에 종속되므로 , 위의 생성된 요소들의 namespace에 맞게끔 parameter를 변경합니다.
- 해당 가이드에는 default namespace에 csi configmap들을 생성해두었기 때문에 default로 생성합니다.

```yaml
$ cat <<EOF > csi-rbd-sc.yaml
---
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
   name: csi-rbd-sc
provisioner: rbd.csi.ceph.com
parameters:
   clusterID: b9127830-b0cc-4e34-aa47-9d1a2e9949a8
   pool: kubernetes
   imageFeatures: layering
   csi.storage.k8s.io/provisioner-secret-name: csi-rbd-secret
   csi.storage.k8s.io/provisioner-secret-namespace: default
   csi.storage.k8s.io/controller-expand-secret-name: csi-rbd-secret
   csi.storage.k8s.io/controller-expand-secret-namespace: default
   csi.storage.k8s.io/node-stage-secret-name: csi-rbd-secret
   csi.storage.k8s.io/node-stage-secret-namespace: default
reclaimPolicy: Delete
allowVolumeExpansion: true
mountOptions:
   - discard
EOF
```

apply 명령어로 storageclass를 생성합니다.
```
$ kubectl apply -f csi-rbd-sc.yaml
storageclass.storage.k8s.io/csi-rbd-sc created
```

k8s 1.14 및 1.15 버전에서는 ExpandCSIVolumes gate를 enalbe 시켜야 합니다.

## 6. 결과 확인
### 6.1 storageclass 확인
생성된 storageclass를 확인합니다.

```
$ kubectl get sc 
NAME                 PROVISIONER          RECLAIMPOLICY   VOLUMEBINDINGMODE   ALLOWVOLUMEEXPANSION   AGE
csi-rbd-sc           rbd.csi.ceph.com     Delete          Immediate           true                   81s
```
### 6.2 storageclass 사용
csi-rbd-sc를 사용하는 pvc 생성합니다.

```yaml
$ cat <<EOF > raw-block-pvc.yaml
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: raw-block-pvc
spec:
  accessModes:
    - ReadWriteOnce
  volumeMode: Block
  resources:
    requests:
      storage: 1Gi
  storageClassName: csi-rbd-sc
EOF

$ kubectl apply -f raw-block-pvc.yaml
```

raw-block-pvc pvc를 사용하는 pod를 생성합니다.

```yaml
$ cat <<EOF > raw-block-pod.yaml
---
apiVersion: v1
kind: Pod
metadata:
  name: pod-with-raw-block-volume
spec:
  containers:
    - name: fc-container
      image: fedora:26
      command: ["/bin/sh", "-c"]
      args: ["tail -f /dev/null"]
      volumeDevices:
        - name: data
          devicePath: /dev/xvda
  volumes:
    - name: data
      persistentVolumeClaim:
        claimName: raw-block-pvc
EOF

$ kubectl apply -f raw-block-pod.yaml
```