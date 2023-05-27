해당 문서는 kubernetes환경에 기 구축된 ceph filesystem을 관리하기 위해서 minio와 연동하는 방안을 설명합니다.

## **precondition**

ceph는 아래 문서대로 배포 후 minio를 설치합니다.

### 실행 환경

vm 3대
ubuntu 20.04 / 8core , 16G
node당 volume 100G 할당
rke2 kubernetes 위 ceph 배포

## install minio

ceph 스토리지를 관리하기 위한 minio를 배포합니다.

helm chart로 설치합니다.

```bash
helm repo add minio https://charts.bitnami.com/bitnami
helm pull minio/minio --untar
```

minio를 설치할 때 , 사용할 storageClass를 ceph로 설정합니다.

```yaml
cat setting-values.yaml

global:
  storageClass: "ceph-block"
```

생성한 setting-values.yaml 파일을 통해 minio를 설치합니다.

```bash
# minio 관리용 ns 생성
kubectl create ns minio

# minio helm install
helm upgrade --install minio . -n minio -f values.yaml,setting-values.yaml
```

아래 명령어를 통해 minio default user 및 password를 확인합니다.

```bash
user : kubectl get secret --namespace minio minio -o jsonpath="{.data.root-user}" | base64 -d

password : kubectl get secret --namespace minio minio -o jsonpath="{.data.root-password}" | base64 -d
```

minio 접속합니다.

![minio-1][minio-1]
  
[minio-1]:./images/minio-1.PNG

minio에서 Buckets을 생성합니다.

![minio-2][minio-2]
  
[minio-2]:./images/minio-2.PNG

해당 Buckets에 file을 upload . download , share 할 수 있습니다.

![minio-3][minio-3]
  
[minio-3]:./images/minio-3.PNG