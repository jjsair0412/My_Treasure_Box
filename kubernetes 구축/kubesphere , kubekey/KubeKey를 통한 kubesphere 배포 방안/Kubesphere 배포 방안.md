
# Kubesphere 배포 방안
## 1. Prerequisites
- 해당 문서는 kubekey 를 기반으로하는 Kubesphere을 배포하는 방법에 대해 기술합니다.
- 에어겝 설치 방안은 아래 링크에 위치합니다.
  [kubesphere air gap install](https://github.com/jjsair0412/kubernetes_info/blob/main/kubernetes%20%EA%B5%AC%EC%B6%95/KubeKey%EB%A5%BC%20%ED%86%B5%ED%95%9C%20kubesphere%20%EB%B0%B0%ED%8F%AC%20%EB%B0%A9%EC%95%88/Kubesphere%20%EB%B0%B0%ED%8F%AC%20%EB%B0%A9%EC%95%88%20-%20air%20gap.md)
- 전체 설치 방안은 아래의 공식 문서를 참조하였습니다. 
  [설치 방안 공식 문서](https://kubesphere.io/docs/v3.3/installing-on-linux/introduction/multioverview/)
## 2.  System Requirements
### 2.1 node 리소스 사양
- HA 구성을 하기 위해서는 아래의 최소 요구 사항에 따라 3개의 hosts를 준비해야 합니다.

| systems |Minimum Requirements (Each node)  |
|--|--|
|**Ubuntu**  _16.04, 18.04, 20.04_  | CPU: 2 Cores, Memory: 4 G, Disk Space: 40 G |
|**Debian**  _Buster, Stretch_|CPU: 2 Cores, Memory: 4 G, Disk Space: 40 G|
|**CentOS**  _7_.x|CPU: 2 Cores, Memory: 4 G, Disk Space: 40 G|
|**Red Hat Enterprise Linux**  _7_|  CPU: 2 Cores, Memory: 4 G, Disk Space: 40 G|
|**SUSE Linux Enterprise Server**  _15_  **/openSUSE Leap**  _15.2_|CPU: 2 Cores, Memory: 4 G, Disk Space: 40 G|
### 2.2 node 요구 사항
- 모든 노드는 ssh 통신이 가능해야 합니다. 
  ex ) ssh ~
- 모든 노드의 시간은 동기화 되어야 합니다.
- sudo , curl 등 모든 노드에서 사용해야 합니다.
### 2.3 container runtime 요구사항
- 클러스터에는 사용 가능한 container runtime이 존재해야 합니다.
- kubekey를 사용할 경우 , default container runtime은 docker 입니다.
- 클러스터 설치 시 , 원하는 container runtime을 직접 설치할 수 있습니다.
- 아래의 container runtime 들이 지원되어지고 있습니다.

| 컨테이너 런타임 | 버전 |
|--|--|
|Docker  |19.3.8+  |
|containerd|Latest|
|CRI-O (experimental, not fully tested)|Latest|
|iSula (experimental, not fully tested)|Latest|
### 2.4 dependency requirements
- kubekey는 kubernetes와 kubeSphere를 함께 설치합니다.
- 설치해야 하는 dependency는 k8s 버전에 따라 다를 수 있습니다.
- 설치하기 전 , 아래 Required dependency들을 설치해야 합니다.
- 따라서 에어갭 설치 환경이라면 socat과 conntrack를 yum install , apt-get install 할 수 없기에 패키지를 따로 구성하여 설치해야 합니다. ( rpm ,dpkg 등 )

|Dependency  | Kubernetes Version ≥ 1.18 | Kubernetes Version < 1.18 |
|--|--|--|
| socat | Required | Optional but recommended |
|conntrack|Required|Optional but recommended|
|ebtables|Optional but recommended|Optional but recommended|
|ipset|Optional but recommended|Optional but recommended|
### 2.4 network requirements
- DNS 주소를 /etc/resolv.conf 에서 사용할 수 있는지 확인해야 합니다. 그렇지 않으면 DNS 이슈가 발생할 수 있습니다.
- cluster config 파일을 작성할 때 설정해주는 dns 정보를 resolv.conf가 해석할 수 있어야 합니다.
## 3.  install KubeKey
### 3.1 get install package
- github에서 kubekey releases를 직접 선택하여 설치하거나 , 아래 curl 명령어로 설치 패키지를 가져옵니다.
  [kubekey github](https://github.com/kubesphere/kubekey/releases)
 - 해당 문서는 curl을 통해서 keysphere 2.2.1 버전을 가지고와서 설치합니다.
```
$ curl -sfL https://get-kk.kubesphere.io | VERSION=v2.2.1 sh -
```
가지고온 파일을 x권한을 부여하여 실행 파일로 변환합니다.
```
$ chmod +x kk
```

### 3.2 kubernetes cluster 구성 파일 설정
- 아래 명령어로 cluster 구성 파일을 꺼내옵니다.
```
$ ./kk create config [--with-kubernetes version] [--with-kubesphere version] [(-f | --file) path]
```
-   KubeSphere 3.3.0에 권장되는 Kubernetes 버전: v1.19.x, v1.20.x, v1.21.x, v1.22.x 및 v1.23.x(실험 지원). Kubernetes 버전을 지정하지 않으면 KubeKey는 기본적으로 Kubernetes v1.23.7을 설치합니다. 지원되는 Kubernetes 버전에 대한 자세한 내용은 [지원 매트릭스](https://kubesphere.io/docs/v3.3/installing-on-linux/introduction/kubekey/#support-matrix) 를 참조하십시오 .
    
-   이 단계에서 명령에 플래그를 추가하지 않으면 구성 파일의 필드를 사용하여 설치하거나 나중에 사용할 때 이 플래그를 다시 추가 `--with-kubesphere`하지 않는 한 KubeSphere가 배포되지 않습니다 .`addons``./kk create cluster`
    
-   KubeSphere 버전을 지정하지 않고 플래그를 추가하면 `--with-kubesphere`최신 버전의 KubeSphere가 설치됩니다.
- 해당 문서에서는 다음 예제로 설치합니다.
```
$ ./kk create config --with-kubesphere v3.3.0
```
- HA 노드의 구성 파일을 편집합니다.
  구성 파일의 각 노드 DNS 이름은 위에 명시했던 것 처럼 /etc/resolv.conf에서 해석 될 수 있어야 합니다.
```yaml
$ cat config-sample.yaml
apiVersion: kubekey.kubesphere.io/v1alpha2
kind: Cluster
metadata:
  name: sample
spec:
  hosts:
  - {name: node1, address: 192.168.208.145, internalAddress: 192.168.208.145, user: jinseong, password: "1234"} # node의 접근 정보를 입력합니다. 
  - {name: node2, address: 192.168.208.146, internalAddress: 192.168.208.146, user: jinseong, password: "1234"}
  - {name: node3, address: 192.168.208.147, internalAddress: 192.168.208.147, user: jinseong, password: "1234"}
  roleGroups:
    etcd:
    - node1 # etcd 노드 선택
    control-plane:
    - node1 # master 노드 선택
    worker:
    - node2 # worker 노드 선택
    - node3
  controlPlaneEndpoint:
    ## Internal loadbalancer for apiservers
    # internalLoadbalancer: haproxy # 앞단 LB 설정또한 가능합니다.

    domain: lb.kubesphere.local
    address: ""
    port: 6443 # port 지정 ( default 권장 )
  kubernetes:
    version: v1.23.7 # k8s version
    clusterName: cluster.local # cluster의 AAA 방식 dns name 선택
    autoRenewCerts: true
    containerManager: docker # container runtime 설정
  etcd:
    type: kubekey
  network: 
    plugin: calico # cni 선택
    kubePodsCIDR: 10.233.64.0/18
    kubeServiceCIDR: 10.233.0.0/18
    ## multus support. https://github.com/k8snetworkplumbingwg/multus-cni
    multusCNI:
      enabled: false
  registry:
    privateRegistry: ""
    namespaceOverride: ""
    registryMirrors: []
    insecureRegistries: []
  addons: []

```
- 만약 ssh를 사용한 암호 없는 로그인인 경우에는 아래와 같이 노드 접근 정보를 기입합니다.
```
hosts: - {name: master, address: 192.168.0.2, internalAddress: 192.168.0.2, privateKeyPath: "~/.ssh/id_rsa"}
```
- 여러가지 노드 접근 정보 기입 예는 공식 문서에 작성되어 있습니다.
### 3.3 cluster 구성 파일을 사용하여 클러스터 생성
- 아래 명령어를 통해 클러스터를 생성합니다.
- 전체 설치 프로세스는 10 ~ 20분정도 소요될 수 있습니다.
```
$ ./kk create cluster -f config-sample.yaml
```
## 4. 설치 결과 확인
- 기본적으로 30880 포트를 사용하게 되며 , 웹 콘솔에 엑세스하여 정상 설치 여부를 확인합니다.
- default id와 pwd는 아래와 같습니다.
```
id : admin
pwd : P@88w0rd
```
## 5. 추가 설정
- app store를 사용하거나 cicd를 사용하기 위해선 default가 false로 설치되기 때문에 , true로 변경해야 합니다.
- 클러스터 구성시 config-sample.yaml파일 하단에 해당 설정들이 위치합니다.
- enabled 하면서 kubesphere을 설치할 수 있습니다.
- 아래 공식문서를 참고하면 됩니다.
https://kubesphere.io/docs/v3.3/pluggable-components/app-store/#enable-the-app-store-after-installation

### 5.0 addons 사용 방안
- kubesphere와 kubekey를 설치하면서 , helm chart 및 yaml파일을 같이 배포시킬 수 있습니다.
- 아래의 예시는 openebs를 같이 설치하는 예제입니다.
- yaml파일을 배포하는 방안은 , 아래 공식문서 주소에서 예시를 보고 따라하면 됩니다.

[addone 사용 방안](https://github.com/kubesphere/kubekey/blob/master/docs/addons.md)
```yaml
apiVersion: kubekey.kubesphere.io/v1alpha2
kind: Cluster
metadata:
  name: sample
spec:
  hosts:
  - {name: master, address: 172.xxx.xxx.xx1, internalAddress: 172.xxx.xxx.xx1, privateKeyPath: "~/.ssh/jin.pem"} 
  - {name: worker1, address: 172.xxx.xxx.xx2, internalAddress: 172.xxx.xxx.xx2, privateKeyPath: "~/.ssh/jin.pem"} 
  - {name: worker2, address: 172.xxx.xxx.xx3, internalAddress: 172.xxx.xxx.xx3, privateKeyPath: "~/.ssh/jin.pem"} 
  roleGroups:
    etcd:
    - master
    control-plane: 
    - master
    worker:
    - worker1
    - worker2
  controlPlaneEndpoint:
    ## Internal loadbalancer for apiservers 
    # internalLoadbalancer: haproxy

    domain: lb.kubesphere.local
    address: ""
    port: 6443
  kubernetes:
    version: v1.23.7
    clusterName: cluster.local
    autoRenewCerts: true
    containerManager: docker
  etcd:
    type: kubekey
  network:
    plugin: calico
    kubePodsCIDR: 10.233.64.0/18
    kubeServiceCIDR: 10.233.0.0/18
    ## multus support. https://github.com/k8snetworkplumbingwg/multus-cni
    multusCNI:
      enabled: false
  registry:
    privateRegistry: "harbor.xxx.xxx/kubesphere_image"
    namespaceOverride: ""
    registryMirrors: []
    insecureRegistries: []
    auths:
      "harbor.xxx.xxx/kubesphere_image":
        username: "admin"
        password: "Harbor12345"
  addons:
  - name: openebs # helm name
    namespace: openebs # 배포대상 namespace 명시 ( 없다면 생성됨 )
    sources:
      chart:
        name: openebs # relase name
        repo: https://openebs.github.io/charts # repo 이름
        valuesFile: /home/centos/openebs/openebs-values.yaml ( setting-values.yaml 파일 설정 가능 )
```

### 5.1 app store enabled
- kubectl 명령어는 아래 patch 명령어를 사용하면 됩니다.
```
$ 
```

### 5.2 cicd enabled 
- kubectl 명령어는 아래 patch 명령어를 사용하면 됩니다.
```
$ 
```

### 5.3 serviceMash enalbed
- kubectl 명령어는 아래 patch 명령어를 사용하면 됩니다.
```
$ 
```