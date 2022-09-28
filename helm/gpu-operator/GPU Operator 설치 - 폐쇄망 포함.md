
# GPU Operator helm install - air gap 환경 포함
## 1. 사전 조건 
- 본 설치 테스트 환경은 , RKE2 클러스터에서 테스트하였습니다.
- 모든 솔루션의 설치는 helm chart를 통해 설치합니다.
- 폐쇄망 환경에서 테스트하였으며 , 인터넷 통신이 가능한 상태 환경에서 설치는 맨 아래에 따로 작성하였습니다.
- master , worker 각 한대 총 두대로 구성된 클러스터이며 , worker는 aws g4dn.xlarge 인스턴스를 사용하였습니다.
- gpu의 version과 모델 명은 아래와 같습니다.
```
$ lspci | grep -i nvidia
00:1e.0 3D controller: NVIDIA Corporation TU104GL [Tesla T4] (rev a1)
```
- 먼저 , 커널에 포함된 기본 그래픽 드라이버가 비 활성화 되어 있는지 확인합니다.
  만약 활성화 되어 있다면 , gpu-operator를 설치 할 때 , driver install option을 false로 두고 설치 해야합니다.
  
```
$ lsmod | grep nouveau
  아무것도 뜨지 않으면 비활성화 된 상태.
```

## 2. 설치 과정
- gpu operator는 두 단계로 설치합니다.
1. install node-feature-discovery 
2. install Gpu operator
## 2.1 install node-feature-discovery 
- nfd는 각 노드 설정값을 보고 , label을 붙여주는 역할을 하게 됩니다.
- gpu 노드인지 아닌지 , nvidia-driver가 설치되어 있는지 , 설치되어 있지 않은지 등을 판단하여 labels를 노드에 붙여 줍니다.
- 따라서 먼저 설치합니다.
### 2.1.1 namespace 생성
- nfd 관리용 namespace를 생성합니다.
```
$ kubectl create ns nfd
```
### 2.1.2 helm chart pull & values.yaml setting
- nfd의 helm repo를 추가하고 , pull을 통해 helm chart를 받아옵니다.
```
$ helm repo add k8s-at-home https://k8s-at-home.com/charts/
$ helm repo update
$ helm pull node-feature-discovery/node-feature-discovery... --untar
```
- 만약 air gap 환경이라면 image를 private registry에서 받아와야 하기 때문에 , values.yaml파일 설정을 아래와 같이 변경합니다.
```
$ setting-values.yaml
image:
  repository: private-registry-url/k8s.gcr.io/nfd/node-feature-discovery
  tag: tag-info
```
### 2.1.2 install nfd with helm chart
- helm install 명령어로 설치합니다.
```
$ helm upgrade --install nfd . -n nfd -f values.yaml,setting-values.yaml
```
- 설치 결과를 확인합니다.
```
$ kubectl get all -n nfd
```
- label이 정상적으로 등록 되었는지 확인합니다.
```
$ kubectl get nodes --show-labels 
```
## 2.2 install gpu-operator
- [공식 문서](https://docs.nvidia.com/datacenter/cloud-native/gpu-operator/getting-started.html)
- gpu operator를 설치합니다.