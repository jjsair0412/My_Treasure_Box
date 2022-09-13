# Kubesphere 배포 방안 - air gap 
## 1. Prerequisites
- 해당 문서는 kubekey를 기반으로 kubesphere를 배포하는 방안에 대해 기술합니다.
air gap 방식으로 설치하며 , 해당 예제에서는 custom한 ks-console 이미지를 사용합니다.  

- 해당 문서는 쿠버네티스가 설치되어 있는 환경에서 테스트 합니다.

- 전체 설치 방안은 아래의 공식 문서를 참조하였습니다. 
 [설치 방안 공식 문서](https://kubesphere.io/docs/v3.3/installing-on-kubernetes/on-prem-kubernetes/install-ks-on-linux-airgapped/#step-1-prepare-a-private-image-registry)
## 2. System Requirements
- 시스템 환경 요구사항 정보는 아래의 링크에서 확인하실 수 있습니다.
  [시스템 환경 요구사항 정보](https://github.com/jjsair0412/kubernetes_info/blob/main/kubernetes%20%EA%B5%AC%EC%B6%95/KubeKey%EB%A5%BC%20%ED%86%B5%ED%95%9C%20kubesphere%20%EB%B0%B0%ED%8F%AC%20%EB%B0%A9%EC%95%88/Kubesphere%20%EB%B0%B0%ED%8F%AC%20%EB%B0%A9%EC%95%88.md#2--system-requirements)
## 3. Install KubeKey
-  kubesphere가 올라갈 kubekey cluster를 구성합니다.
- 인터넷이 연결된 환경에서 먼저 설치합니다.
	- 해당 문서는 kubesphere만을 air-gap으로 설치하는 방안에 대해 기술하기에 , kubekey는 인터넷이 되는 환경에서 설치합니다.
### 3.1 get kk binary file 
- kubekey의 kk 바이너리 파일을 curl 명령어로 가져 옵니다.
	VERSION 뒤에 오는 releases version은 아래 문서에서 찾아 지정합니다.
	[kubekey github](https://github.com/kubesphere/kubekey/releases)
```
curl -sfL https://get-kk.kubesphere.io | VERSION=v2.2.1 sh -
```
### 3.2 kubekey install
- kk 바이너리 파일을 사용하여 , kubesphere를 제외한 kubekey만 설치 합니다.
	- 아래 명령어를 통해 default 설치를 진행합니다.
```
./kk create cluster
```
-  특정 kubernetes version을 지정하여 설치할 수 도 있습니다.
```
./kk create cluster --with-kubernetes v1.24.1 --container-manager containerd
```
## 4. Config kubesphere
### 4.1 kubesphere image pull
- 아래 명령어를 통해 kubesphere의  image list를 받아옵니다.
  download 뒤의 v3.3.0에 특정 버전을 명시할 수 도 있습니다.
```
curl -L -O https://github.com/kubesphere/ks-installer/releases/download/v3.3.0/images-list.txt
```
- 그 후 , kubesphere offline 스크립트를 curl 명령어로 다운로드 받습니다.
kubesphere image list에 명시한 버전과 동일해야 합니다.
```
curl -L -O https://github.com/kubesphere/ks-installer/releases/download/v3.3.0/offline-installation-tool.sh
```
- 받아온 스크립트에 x 권한을 주어 실행 파일로 변환합니다.
```
chmod +x offline-installation-tool.sh
```
### 4.2 custom image save
- image pull을 받기 전에 , ks-console의 custom image를 build 합니다.
version 명과 tag , image name도 원본과 동일하게 build 합니다.
사용할 private registry harbor에 push까지 진행합니다.
```
docker build -t kubesphere/ks-console:v3.3.0 .

# save tar file 
docker save -o ks-console.tar kubesphere/ks-console:v3.3.0

# push
docker push harbor.xxx.co.kr/kubesphere-imagelist/kubesphere/ks-console:v3.3.0
```
### 4.2 image list pull
- offline-installation-tool.sh 스크립트를 사용하여 image를 pull 합니다.
custom image를 사용할 것이기 때문에 image-list.txt file에 ks-console을 제거한 뒤 아래 명령어를 수행합니다.
```
./offline-installation-tool.sh -s -l images-list.txt -d ./kubesphere-images
```
### 4.3 private registry push
- pull 받은 image들을 모두 private registry에 push 합니다.
  이전에 custom ks-console image를 올려놓은 project에 push 합니다.
	-  -r 옵션에 private registry 정보가 들어갑니다.
	-   -d 옵션에 pull한 image 경로가 들어갑니다.
	 -  -l 옵션에 image-list.txt 경로가 들어갑니다.
```
./offline-installation-tool.sh -l images-list.txt -d ./kubesphere-images -r harbor.xxx.co.kr/kubesphere-imagelist
```
## 5. install kubesphere 
- 온라인 환경에서 kubesphere를 설치하는것과 동일한 방식을 사용합니다.
- cluster-configuration.yaml , kubesphere-installer.yaml 두 yaml을 사용하여 kubesphere를 kubekey 위에 설치 합니다. ( 타 k8s cluster 위에서도 동일하게 설치 합니다. )
	- 아래 명령어를 통해 두 yaml 파일을 가지고 옵니다.
		- ks-installer를 통해 kubesphere  설치 
		- cluster-configuration을 통해 kubesphere 구성 정보 설정
### 5.1 yaml 파일 다운로드
- 아래 명령어를 통해서 두 파일을 설치 합니다.
	-	온라인 환경에서 진행합니다.
```
curl -L -O https://github.com/kubesphere/ks-installer/releases/download/v3.3.0/cluster-configuration.yaml
curl -L -O https://github.com/kubesphere/ks-installer/releases/download/v3.3.0/kubesphere-installer.yaml
```
### 5.2 yaml 파일 구성
- cluster-configuration.yaml 파일에 자신의 private registry 정보를 기입합니다.
- local_registry 에 private registry 정보가 들어갑니다.
	- harbor domain : harbor.xxx.co.kr
	- harbor project : kubesphere-imagelist
```
...
spec:
  persistence:
    storageClass: ""        # If there is no default StorageClass in your cluster, you need to specify an existing StorageClass here.
  authentication:
    jwtSecret: ""           # Keep the jwtSecret consistent with the Host Cluster. Retrieve the jwtSecret by executing "kubectl -n kubesphere-system get cm kubesphere-config -o yaml | grep -v "apiVersion" | grep jwtSecret" on the Host Cluster.
  local_registry: harbor.xxx.co.kr/kubesphere-imagelist        # Add your private registry address if it is needed.
  # dev_tag: ""               # Add your kubesphere image tag you want to install, by default it's same as ks-installer release version.
  etcd:
    monitoring: false       # Enable or disable etcd monitoring dashboard installation. You have to create a Secret for etcd before you enable it.
    endpointIps: localhost  # etcd cluster EndpointIps. It can be a bunch of IPs here.
    port: 2379              # etcd port.
    tlsEnable: true
...
```
- kubesphere-installer.yaml 파일에도 registry 정보를 기입합니다.
- 아래 명령어를 통해 private registry 정보로 yaml 내용을 수정합니다.
```
sed -i "s#^\s*image: kubesphere.*/ks-installer:.*#        image: harbor.xxx.co.kr/kubesphere-imagelist/kubesphere/ks-installer:v3.3.0#" kubesphere-installer.yaml
```
### 5.3 install kubesphere
- kubectl 명령어를 사용해 아래 순서대로 설치 합니다.
```
kubectl apply -f kubesphere-installer.yaml 
kubectl apply -f cluster-configuration.yaml
```