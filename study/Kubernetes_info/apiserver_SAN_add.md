# apiserver_SAN_add

## overview 
k8s api서버에 subject alternative name , SAN을 추가하는 방안에 대해 기술하면서 , kubeadm token의 private network 인증절차를 공부한 기록입니다.
 
## 1. SAN ( subject alternative name ) 이란 ?
SAN은 Subject Alternative Name의 약자 입니다.
- 다른 의미로 ***주체대체이름*** 이라고도 부릅니다.

RFC 국제 표준 X.509 확장 기술입니다.

간단하게 SAN은 TLS 인증서에 IP나 도메인 형태로 포함되어 있는 요소라고 볼 수 있습니다.

**SAN은 도메인 또는 IP로 볼 수 있으며 , 1개의 SAN이 추가된다 라고 한다면 , 1개의 도메인 또는 IP가 추가된다고 볼 수 있습니다.**

- [출처_KoreaSSL](https://www.koreassl.com/support/faq/106)

## 2. 에러 내용
kubeadm token을 사용해서 다른 노드에 join하려 할 때 , 아래와 같은 에러가 발생하였습니다.
```bash
error execution phase preflight: couldn't validate the identity of the API Server: Get "https://192.168.50.10:6443/api/v1/namespaces/kube-public/configmaps/cluster-info?timeout=10s": x509: certificate is valid for 10.96.0.1, 10.0.2.15, 192.168.50.11, 192.168.50.12, not 192.168.50.10
```

해당 에러는 apiserver.crt 파일이 가지고있는 x509 의 SAN에 , 추가하려는 vm의 ip나 도메인이 없어서 이런 에러가 발생하는것입니다.

## 3. 해결 방안 및 SAN 추가 방안
아래 명령어를 kubeadm master node에서 날리면 , apiserver.crt 파일의 내용을 출력할 수 있습니다.
```bash
$ openssl x509 -in /etc/kubernetes/pki/apiserver.crt -text
```

파일이 긴데 , 여기서 Subject Alternative Name , SAN 부분을 확인해보면 됩니다.
```bash
X509v3 Subject Alternative Name:
    DNS:kubernetes, DNS:kubernetes.default, DNS:kubernetes.default.svc, DNS:kubernetes.default.svc.cluster.local, DNS:master, IP Address:10.96.0.1, IP Address:10.0.2.15, IP Address:192.168.50.11, IP Address:192.168.50.12
```

여기 정보를 확인해보면 , IP Address가 정의되어 있는것을 확인할 수 있습니다.
- IP Address에 cluster에 add할 worker vm의 private ip를 추가해주어야 합니다.

### 3.1 conf파일 추가 방안
#### 1. command로 SAN 추가
kube-system namespace에 정의되어있는 configmap을 보면 , kubeadm-config 가 정의되어 있습니다.
- 이친구를 수정해야 합니다.
```bash
$ kubectl get cm -n kube-system
NAME                                 DATA   AGE
calico-config                        4      73m
coredns                              1      6d3h
extension-apiserver-authentication   6      6d3h
kube-proxy                           2      6d3h
kube-root-ca.crt                     1      6d3h
kubeadm-config                       1      6d3h
kubelet-config-1.23                  1      6d3h

$ kubectl get cm -n kube-system | grep kubeadm-config
kubeadm-config                       1      6d3h
```

kubeadm-config은 다음과 같이 생겼습니다.
- certSANs 칸이 채워져있는 이유는 , 문서 작성 전 테스트하기 위해서 추가해보았기 때문입니다 . .
```bash
$ kubectl get cm kubeadm-config -n kube-system -o yaml
apiVersion: v1
data:
  ClusterConfiguration: |
    apiServer:
      extraArgs:
        authorization-mode: Node,RBAC
      timeoutForControlPlane: 4m0s
      certSANs:
      - 192.168.50.11
      - 192.168.50.12
    apiVersion: kubeadm.k8s.io/v1beta3
    certificatesDir: /etc/kubernetes/pki
    clusterName: kubernetes
    controllerManager: {}
    dns: {}
    etcd:
      local:
        dataDir: /var/lib/etcd
    imageRepository: k8s.gcr.io
    kind: ClusterConfiguration
    kubernetesVersion: v1.23.17
    networking:
      dnsDomain: cluster.local
      serviceSubnet: 10.96.0.0/12
    scheduler: {}
kind: ConfigMap
metadata:
  creationTimestamp: "2023-06-01T08:55:57Z"
  name: kubeadm-config
  namespace: kube-system
  resourceVersion: "2011"
  uid: ecb354ce-1012-4ece-8ff1-90b53a8729ee
```

해당 configmap을 update하기 위해 , kubeadm command line으로 진행합니다.
- [k8s 공식 문서의 --apiserver-cert-extra-sans 옵션 설명](https://kubernetes.io/docs/reference/setup-tools/kubeadm/kubeadm-init/#options)
- 	인증서를 제공하는 API 서버에 사용할 SAN(주체 대체 이름)입니다. IP 주소와 DNS 이름이 모두 될 수 있습니다. 라고 적혀 있습니다.
- stringSlice 부분에 도메인이나 IP 주소를 넣어줍니다.
```bash
$ kubeadm init phase certs apiserver --apiserver-cert-extra-sans stringSlice
```

- **주의**
    - 해당 명령어는 ```apiserver.key``` 및 ```apiserver.crt``` 파일을 새로 생성시키는 명령어 입니다. 따라서 명령어를 수행하기 전 , 기존에 만들어진 key 및 crt 파일을 다른곳에 옮겨두어야 합니다. (제거하거나)

먼저 ```apiserver.key``` 와 ```apiserver.crt``` 파일을 옮겨둡니다.
```bash
# 파일 위치
$ pwd
/etc/kubernetes/pki

$ mv apiserver.crt apiserver.crt.old
$ mv apiserver.key apiserver.key.old
```

다음 kubeadm init 명령어로 SAN을 추가합니다.
```bash
# usecase
$ kubeadm init phase certs apiserver --apiserver-cert-extra-sans=192.168.50.10
[certs] Generating "apiserver" certificate and key
[certs] apiserver serving cert is signed for DNS names [kubernetes kubernetes.default kubernetes.default.svc kubernetes.default.svc.cluster.local master] and IPs [10.96.0.1 10.0.2.15 192.168.50.10]
```

다시 openssl 명령어로 Subject Alternative Name 섹션을 확인하면 , 추가된것을 볼 수 있습니다.
```bash
X509v3 Subject Alternative Name:
    DNS:kubernetes, DNS:kubernetes.default, DNS:kubernetes.default.svc, DNS:kubernetes.default.svc.cluster.local, DNS:master, IP Address:10.96.0.1, IP Address:10.0.2.15, IP Address:192.168.50.10
```


#### 2. configmap 수정
kubeadm-config cm 또한 수정해 줍니다.

```bash
$ kubectl edit cm -n kube-system kubeadm-config
apiVersion: v1
data:
  ClusterConfiguration: |
    apiServer:
      extraArgs:
        authorization-mode: Node,RBAC
      timeoutForControlPlane: 4m0s
      certSANs:
      - 192.168.50.10 # 추가
      - 192.168.50.11
      - 192.168.50.12
...
```

## 4. 결과 확인
다시 join 명령어를 수행하면 , 에러가 해결된것을 확인할 수 있습니다.
- private ip 대역이 다르며 6443 포트가 open되지 않았기 때문에 ., connection refused 발생
```bash
$  kubeadm join 192.168.50.10:6443 --token trsqti.jzkyu9sxyafqmvna --discovery-token-ca-cert-hash sha256:eb738877fc3f317c1c6d2f7691ad94fa964dcbbe27078f96b87f71e92d8cca26
[preflight] Running pre-flight checks
        [WARNING SystemVerification]: this Docker version is not on the list of validated versions: 24.0.2. Latest validated version: 20.10
[preflight] Reading configuration from the cluster...
[preflight] FYI: You can look at this config file with 'kubectl -n kube-system get cm kubeadm-config -o yaml'
error execution phase preflight: unable to fetch the kubeadm-config ConfigMap: failed to get config map: Get "https://10.0.2.15:6443/api/v1/namespaces/kube-system/configmaps/kubeadm-config?timeout=10s": dial tcp 10.0.2.15:6443: connect: connection refused
To see the stack trace of this error execute with --v=5 or higher
```