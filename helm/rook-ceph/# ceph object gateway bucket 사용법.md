# ceph object gateway bucket 사용법

ceph의 ojbect gateway의 buckect의 구조와 사용법에 대해 기술한 문서입니다.

[관련 문서](https://rook.io/docs/rook/v1.10/Storage-Configuration/Object-Storage-RGW/object-storage/)

## 이론

object gateway는 ceph를 helm chart로 설치하였을 경우 open된 상태로 배포됩니다.

ceph-bucket이라는 storageclass를 사용하면 됩니다.

![object1][object1]
  
[object1]:./images/object1.PNG

object gateway를 통해서 ceph의 bucket을 생성할 수 있으며 , s3 api로 bucket 내부 파일을 관리할 수 있습니다.

pvc를 ceph-bucket storageclass로 생성하게 된다면, ceph에서 pv와 동일한 계층인 **ObjectBucketClaim** type의 kind를 가진 리소스를 생성 해야 pod에서 마운트 되며 , ceph에서 bucket이 생성됩니다.

![object2][object2]
  
[object2]:./images/object2.PNG

 

```yaml
# ObjectBucketClaim yaml file
apiVersion: objectbucket.io/v1alpha1
kind: ObjectBucketClaim
metadata:
  name: ceph-bucket
  namespace: rook-ceph
spec:
  generateBucketName: ceph-bkt # 생성할 bucket 이름
  storageClassName: ceph-bucket # storageclass 이름, helm 설치시 ceph-bucket이 default
```

생성된 bucket은 재 사용이 가능합니다.

또한 ObjectBucketClaim 을 생성하면 , 등록한 namepsace에 configmap과 secret이 생성됩니다.

![object3][object3]
  
[object3]:./images/object3.PNG

이름은 ObjectBucketClaim의 이름과 동일하게 생성이 되며 , 각각 가지고있는 정보는 상이합니다.

### ceph-bucket configmap

```yaml
apiVersion: v1
data:
  BUCKET_HOST: rook-ceph-rgw-ceph-objectstore.rook-ceph.svc
  BUCKET_NAME: ceph-bkt-ce4e5fc1-397f-4b0f-9890-c3b58216c413
  BUCKET_PORT: "80"
  BUCKET_REGION: ""
  BUCKET_SUBREGION: ""
kind: ConfigMap
metadata:
  creationTimestamp: "2022-11-09T05:22:55Z"
  finalizers:
  - objectbucket.io/finalizer
  labels:
    bucket-provisioner: rook-ceph.ceph.rook.io-bucket
  name: ceph-bucket
  namespace: rook-ceph
...
```

configmap을 edit한 결과입니다.

1. BUCKET_HOST : 
    - endpoint가 되는 [cephobjectstores.ceph.rook.io](http://cephobjectstores.ceph.rook.io/)의 진입점 정보가 들어갑니다.
    - endpoint 정보는 k8s AAA type으로 정의됩니다. ( servicename.namespace.svc.cluster.local )
    - 따라서 [cephobjectstores.ceph.rook.io](http://cephobjectstores.ceph.rook.io/) kind를 가진 리소스를 외부에 노출 ( ingress, nodeport ) 시킨다면 , 외부에서 s3명령어로 ceph bucket 파일을 관리할 수 있습니다.
    - helm chart로 ceph를 배포했을 경우 , 외부노출은 안된 상태로 한개가 생성되며 , cephadm으로 ceph를 배포했다면 따로 생성시켜주어야 합니다.

![object4][object4]
  
[object4]:./images/object4.PNG

1. BUCKET_NAME
    - object gateway에서 생성되는 bucket의 이름입니다.
2. BUCKET_PORT
    - host에 접근할 port번호입니다. ( default : 80 )

### ceph-bucket secrets

```yaml
apiVersion: v1
data:
  AWS_ACCESS_KEY_ID: QVA4T0NFN1M0TVczMEg4NVI2SFE=
  AWS_SECRET_ACCESS_KEY: MkEzSmR6d0JTRnN6R0xDVmVFQnk1UmlSNU5wbHBSY3dmdENWdUxlTg==
kind: Secret
metadata:
  creationTimestamp: "2022-11-09T05:22:55Z"
  finalizers:
  - objectbucket.io/finalizer
  labels:
    bucket-provisioner: rook-ceph.ceph.rook.io-bucket
  name: ceph-bucket
  namespace: rook-ceph
  ownerReferences:
  - apiVersion: objectbucket.io/v1alpha1
    blockOwnerDeletion: true
...
```

secret을 edit한 결과입니다.

둘다 base64로 인코딩한 결과값이 들어갑니다.

1. AWS_ACCESS_KEY_ID
    - bucket을 가지고있는 user의 ID값입니다.
2. AWS_SECRET_ACCESS_KEY
    - bucket을 가지고있는 user의 pwd 값입니다.

## TEST

object gateway를 사용하는 pod를 생성해봅시다.

### ObjectBucketClaim 생성

이론쪽에서 기술한 yaml 예시대로 ObjectBucketClaim을 생성합니다.

```yaml
apiVersion: objectbucket.io/v1alpha1
kind: ObjectBucketClaim
metadata:
  name: ceph-bucket
  namespace: rook-ceph
spec:
  generateBucketName: ceph-bkt # 생성할 bucket 이름
  storageClassName: ceph-bucket # storageclass 이름, helm 설치시 ceph-bucket이 default
```

configmap , secret이 생성되는지 확인합니다.

### pvc 생성

이론쪽에서 기술하였듯이 , storageclass ceph-bucket을 사용하는 pvc를 생성합니다.

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: raw-pvc
spec:
  accessModes:
    - ReadWriteOnce
  volumeMode: Block
  resources:
    requests:
      storage: 1Gi
  storageClassName: ceph-bucket
```

생성한 pvc를 사용하는 pod를 생성합니다.

기존에 pod에서 pv를 마운트시키는것 처럼 volume을 추가하는것이 아니라 , 

ObjectBucketClaim이 생성되면서 같이 생성된 configmap과 secret을 컨테이너의 env로 등록시켜야 합니다.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: pod-with-block-volume
  namespace: rook-ceph
spec:
  containers:
    - name: fc-container
      image: fedora:26
      command: ["/bin/sh", "-c"]
      args: ["tail -f /dev/null"]
      envFrom:
      - configMapRef:
          name: ceph-bucket # configmap name
      - secretRef:
          name: ceph-bucket # secret name
```

bucket과 pod , configmap , secret이 생성된 결과를 확인합니다.