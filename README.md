# kubernetes_info
## INDEX
- [AWS - AWS 관련](#AWS)
    - [AWS basic information - AWS 개념 정리]()
    - [My AWS architecture - 만들어본 AWS 아키텍처 및 테라폼 코드 ( 코드 작성 예정 )]()
        - [haproxy - rke2 - ceph composition]()
- [backup - backup 방안]()
    - [argo - CronJob으로 argcd backup . 타 솔루션에서 해당 방안 응용 가능 ]()
- [cicd - cicd pipeline]()
    - [springBoot-gradle - gradle project cicd pipeline 및 코드]()
        - [gradle-helm-chart - helm chart]()
        - [springBoot ( gradle ) jenkins pipeline - jenkins pipeline 및 실제 코드]()
    - [springBoot-maven - maven project cicd pipeline 및 코드]()
- [Docker]()
    - [docker compose - docker compose 개념 정리 및 사용방안 정의]()
    - [docker info - docker 개념정리 및 사용방안 정의]()
- [EFK - EFK stack info]()
    - [efk index lifecycle info - efk stack 사용 방안 및 lifecycle 정의]()


## storageClass local provisioner information
Local volumes do not currently support dynamic provisioning, however a StorageClass should still be created to delay volume binding until Pod scheduling.

동적 프로비저닝이 필요할 때에는 , storageclass를 local로 생성하는것 보다 nfs 등을 사용해서 storageclass를 사용하는 편이 편합니다.
local은 pv 동적 프로비저닝이 되지 않습니다.

[관련 문서](https://kubernetes.io/docs/concepts/storage/storage-classes/#local)

## known issues
### 1. kubeconfig file's location is not set in right direction.
The connection to the server localhost:8080 was refused - did you specify the right host or port?

- cp kube.config file into $HOME/.kube/config
- kubeconfig file is for each different which k8s provider systems.
    - exampe : location of rancher's kubeconfig file is /etc/rancher/rke2/rke2.yaml

```
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
```
### 2. helm install
```
curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
chmod 700 get_helm.sh
./get_helm.sh
```
### 3. install kubectl in linux
first , kubectl latest releases version download
```
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
```
install kubectl
```
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
```
If you don't have root permission, you can install kubectl in ~/.local/bin directory
```
chmod +x kubectl
mkdir -p ~/.local/bin
mv ./kubectl ~/.local/bin/kubectl
# 그리고 ~/.local/bin 을 $PATH의 앞부분 또는 뒷부분에 추가
```