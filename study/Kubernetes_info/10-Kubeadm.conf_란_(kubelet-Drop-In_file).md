# 10-Kubeadm.conf 파일 이란 ? ( Kubelet Drop-In file ) 
## Overview
kubernetes를 설치하고 kubelet을 실행하면 , 다음과 같은 로그를 확인할 수 있습니다.
```bash
$ systemctl status kubelet
● kubelet.service - kubelet: The Kubernetes Node Agent
     Loaded: loaded (/lib/systemd/system/kubelet.service; enabled; vendor preset: enabled)
    Drop-In: /etc/systemd/system/kubelet.service.d
             └─10-kubeadm.conf
     Active: active (running) since Thu 2023-06-01 08:55:59 UTC; 6 days ago
       Docs: https://kubernetes.io/docs/home/
   Main PID: 15188 (kubelet)
      Tasks: 18 (limit: 4677)
     Memory: 48.7M
     CGroup: /system.slice/kubelet.service
             └─15188 /usr/bin/kubelet --bootstrap-kubeconfig=/etc/kubernetes/bootstrap-kubelet.conf --kubeconfig=/etc/kubernetes/kubelet.conf --config=/var/lib/kubelet/config.yaml --network-plugin=cni --pod-infra-container-ima>

Jun 07 11:37:31 master kubelet[15188]: 2023-06-07 11:37:31.319 [INFO][32069] ipam.go 1216: Successfully claimed IPs: [172.16.219.67/26] block=172.16.219.64/26 handle="k8s-pod-network.88e218ebb2f53a606fdad43a5c1e4c71098685b5efd>
Jun 07 11:37:31 master kubelet[15188]: 2023-06-07 11:37:31.319 [INFO][32069] ipam.go 847: Auto-assigned 1 out of 1 IPv4s: [172.16.219.67/26] handle="k8s-pod-network.88e218ebb2f53a606fdad43a5c1e4c71098685b5efda00c39ec1f18050608>
Jun 07 11:37:31 master kubelet[15188]: time="2023-06-07T11:37:31Z" level=info msg="Released host-wide IPAM lock." source="ipam_plugin.go:378"
Jun 07 11:37:31 master kubelet[15188]: 2023-06-07 11:37:31.319 [INFO][32069] ipam_plugin.go 287: Calico CNI IPAM assigned addresses IPv4=[172.16.219.67/26] IPv6=[] ContainerID="88e218ebb2f53a606fdad43a5c1e4c71098685b5efda00c39>
Jun 07 11:37:31 master kubelet[15188]: 2023-06-07 11:37:31.324 [INFO][32037] k8s.go 383: Populated endpoint ContainerID="88e218ebb2f53a606fdad43a5c1e4c71098685b5efda00c39ec1f18050608e7a" Namespace="kube-system" Pod="coredns-64>
Jun 07 11:37:31 master kubelet[15188]: 2023-06-07 11:37:31.325 [INFO][32037] k8s.go 384: Calico CNI using IPs: [172.16.219.67/32] ContainerID="88e218ebb2f53a606fdad43a5c1e4c71098685b5efda00c39ec1f18050608e7a" Namespace="kube-s>
Jun 07 11:37:31 master kubelet[15188]: 2023-06-07 11:37:31.325 [INFO][32037] dataplane_linux.go 68: Setting the host side veth name to caliadc02026c1a ContainerID="88e218ebb2f53a606fdad43a5c1e4c71098685b5efda00c39ec1f18050608e>

```

이때 , kubelet이 /etc/systemd/system/kubelet.service.d 폴더에 설치하는 Drop-In 파일인 ***10-kubeadm.conf*** 파일에 대해서 기술합니다.

## 이론
***10-kubeadm.conf*** 파일은 . kubeadm이 생성하는 파일로써 , systemd가 kubelet을 어떻게 실행시켜야하는지가 정의되어있는 conf 파일 입니다.
- kubeadm에 의해 10-kubeadm.conf 파일이 /etc/systemd/system/kubelet.service.d 경로에 파일을 생성시키며 , systemd는 해당 파일로 kubelet을 실행합니다.

## 10-kubeadm.conf 파일의 생김세
kubeadm으로 kubernetes를 프로비저닝 했을 경우 , 10-kubeadm.conf 파일이 다음과 같이 생깁니다.
- k8s version : v1.23.10
```conf
# Note: This dropin only works with kubeadm and kubelet v1.11+
[Service]
Environment="KUBELET_KUBECONFIG_ARGS=--bootstrap-kubeconfig=/etc/kubernetes/bootstrap-kubelet.conf --kubeconfig=/etc/kubernetes/kubelet.conf"
Environment="KUBELET_CONFIG_ARGS=--config=/var/lib/kubelet/config.yaml"
# This is a file that "kubeadm init" and "kubeadm join" generates at runtime, populating the KUBELET_KUBEADM_ARGS variable dynamically
EnvironmentFile=-/var/lib/kubelet/kubeadm-flags.env
# This is a file that the user can use for overrides of the kubelet args as a last resort. Preferably, the user should use
# the .NodeRegistration.KubeletExtraArgs object in the configuration files instead. KUBELET_EXTRA_ARGS should be sourced from this file.
EnvironmentFile=-/etc/default/kubelet
ExecStart=
ExecStart=/usr/bin/kubelet $KUBELET_KUBECONFIG_ARGS $KUBELET_CONFIG_ARGS $KUBELET_KUBEADM_ARGS $KUBELET_EXTRA_ARGS
```

10-kubeadm.conf 파일 상단의 Environment 및 EnvironmentFile을 정의하여 ExecStart 섹션에서  kubelet 실행에 대한 command를 완성하여 systemd가 kubelet 프로세스를 실행시킵니다.

구성 파일은 다음과 같습니다.
- ```--kubeconfig=/etc/kubernetes/kubelet.conf```
    
    - kubelet 구성 파일 , 
    
    - kubelet.conf 파일의 경로를 명시적으로 지정해 주면 , kubelet이 해당 파일 내용을 토대로 k8s 인증 및 구성 정보를 가져와 API 서버와의 인증 및 통신에 사용됩니다.
- ```--config=/var/lib/kubelet/config.yaml```
    
    - KUBELET CONFIG ARG 구성 파일 , 
    
    - kubelet의 구성 옵션들을 포함하는 파일입니다.
    클러스터와의 네트워크 통신을 위한 주소, 클러스터 인증 설정, 로그 디렉토리, 리소스 할당 및 제한, 노드 레이블 등의 정보를 설정할 수 있습니다.
- ```--bootstrap-kubeconfig=/etc/kubernetes/bootstrap-kubelet.conf```
    
    - TLS bootstrap을 위해 사용되는 파일 , 
    
    - **```--kubeconfig=/etc/kubernetes/kubelet.conf``` 파일이 없을 경우에만 사용됩니다. !**
- ```/var/lib/kubelet/kubeadm-flags.env```
    - 

### 1. kubelet.conf 파일의 생김세
kubelet.conf 파일은 다음과 같이 생겨먹었습니다.
- 파일을 보면 알겠지만 , Kubelet이 Kubernetes cluster와 연결할 서버 정보 및 인증서 , 계정 정보를 갖게 됩니다.
- 따라서 kubernetes api 서버와 연결이 안될 때는 해당 파일을 확인해보면 됩니다.
```yaml
apiVersion: v1
clusters:
- cluster:
    certificate-authority-data: LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUMvakNDQWVhZ0F3SUJBZ0lCQURBTkJna3Foa2lHOXcw....
    server: https://10.0.2.15:6443
  name: kubernetes
contexts:
- context:
    cluster: kubernetes
    user: system:node:master
  name: system:node:master@kubernetes
current-context: system:node:master@kubernetes
kind: Config
preferences: {}
users:
- name: system:node:master
  user:
    client-certificate: /var/lib/kubelet/pki/kubelet-client-current.pem
    client-key: /var/lib/kubelet/pki/kubelet-client-current.pem
```

### 2. config.yaml 파일의 생김세
kubelet의 구성요소를 포함하는 파일 답게 , DNS 정보 및 리소스 할당 및 제한 등의 설정이 있는것을 확인할 수 있습니다.

```yaml
apiVersion: kubelet.config.k8s.io/v1beta1
authentication:
  anonymous:
    enabled: false
  webhook:
    cacheTTL: 0s
    enabled: true
  x509:
    clientCAFile: /etc/kubernetes/pki/ca.crt
authorization:
  mode: Webhook
  webhook:
    cacheAuthorizedTTL: 0s
    cacheUnauthorizedTTL: 0s
cgroupDriver: systemd
clusterDNS:
- 10.96.0.10
clusterDomain: cluster.local
cpuManagerReconcilePeriod: 0s
evictionPressureTransitionPeriod: 0s
fileCheckFrequency: 0s
healthzBindAddress: 127.0.0.1
healthzPort: 10248
httpCheckFrequency: 0s
imageMinimumGCAge: 0s
kind: KubeletConfiguration
logging:
  flushFrequency: 0
  options:
    json:
      infoBufferSize: "0"
  verbosity: 0
memorySwap: {}
nodeStatusReportFrequency: 0s
nodeStatusUpdateFrequency: 0s
resolvConf: /run/systemd/resolve/resolv.conf
rotateCertificates: true
runtimeRequestTimeout: 0s
shutdownGracePeriod: 0s
shutdownGracePeriodCriticalPods: 0s
staticPodPath: /etc/kubernetes/manifests
streamingConnectionIdleTimeout: 0s
syncFrequency: 0s
volumeStatsAggPeriod: 0s
```
### 3. bootstrap-kubelet.conf 파일의 생김세
해당 파일은 kubelet.conf 파일이 없을때만 사용되기 때문에 , 실 구성된 파일이 아닌 예시 파일로 대체합니다.
```yaml
apiVersion: kubelet.config.k8s.io/v1beta1
kind: KubeletConfiguration
authentication:
  anonymous:
    enabled: false
  webhook:
    enabled: true
  x509:
    clientCAFile: "/etc/kubernetes/pki/ca.crt"
authorization:
  mode: Webhook
clusterDomain: "cluster.local"
clusterDNS:
  - "10.96.0.10"
```

### 4. kubeadm-flags.env 파일의 생김세
동적 env를 담고있는 파일입니다.
- 여기에 container-runtime의 sock파일 endpoint가 들어가게 됩니다.
- 만약 endpoint를 변경하고 싶다면 , 여기서 변경하고 kubelet을 restart 합니다.
```env
KUBELET_KUBEADM_ARGS="--network-plugin=cni --pod-infra-container-image=k8s.gcr.io/pause:3.6" --container-runtime-endpoint=/var/run/containerd/containerd.sock"
```