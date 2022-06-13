

# RKE2 offlinet 환경 설치 방안 ( Tarball Method ) - RKE2 Air-Gap

## 1. Requirements
- RKE2를 네트워크가 없는 온프레미스 환경에서 설치하는 방안을 설명합니다.
  방법은 Private Registry Method와 Tarball Method가 존재하는데 해당 문서는 Tarball Method 설치 방안에 대해 설명합니다.
  [Tarball Method 설치 방안 공식 문서](https://docs.rke2.io/install/airgap/#tarball-method)
## 2. Install Essential Element
- RKE2를 설치하기 위해 , 필수 요소들을 먼저 가지고 옵니다.
- 해당 작업은 rke2-server , rke2-agent 모든 vm에서 작업합니다.
1. rke2-images
2. rke2
3. sha256sum
4. install script
- 위 요소들을 가지고 옵니다. 테스트 환경에서는 인터넷을 열어두고 다운로드 받아 진행하지만 , 실제 환경에서는 private registry에 해당 요소들을 넣어놓고 , 필요할 때 꺼내다 쓰는 방식을 사용합니다.
- 필수 요소 다운로드가 다 완료되면 , 외부 인터넷망을 끊어서 폐쇄망 환경으로 테스트를 진행합니다.
### 2.1 RKE2-images, RKE2 , sha256sum 다운로드
[RKE2 git](https://github.com/rancher/rke2/releases) 
-   rke2-images.linux-amd64.tar.zst
-   rke2.linux-amd64.tar.gz
-   sha256sum-amd64.txt

 설치 환경이 기본 요소가 아닌 , 특정 cni등을 사용하고 싶다면 , 그에 맞게끔 변경해 설치하면 됩니다.
[Air-Gap 설치 방안](https://docs.rke2.io/install/airgap/#tarball-method)

### 2.2 Install Script 다운로드
```
$ curl -sfL https://get.rke2.io --output install.sh
```
## 3. RKE2 install
### 3.1 RKE2를 설치합니다.
- Rancher server를 설치할 모든 노드에 접속해서 , Swap을 비활성화 하고 , network 브릿지를 설정합니다.
  첫번째 rke2-server node에서만 작업합니다.
```
Rancher Server 설치 대상 모든 Node 환경에 접속하여 Swap 비활성화, Network 브릿시 설정
$ sudo swapoff -a
$ sudo sed -i '/ swap / s/^\(.*\)$/#\1/g' /etc/fstab

$ cat <<EOF | sudo tee /etc/modules-load.d/k8s.conf
br_netfilter
EOF

$ cat <<EOF | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-ip6tables = 1
net.bridge.bridge-nf-call-iptables = 1
EOF
$ sudo sysctl --system
```

```
# 위의 준비과정에서 다운로드한 파일이 있는 디렉토리로 이동
$ cd /home/jjsair0412/Desktop/rke2

# 다운로드한 파일을 artifacts 폴더를 생성하여 그곳으로 복사
$ sudo mkdir /root/rke2-artifacts
$ sudo cp rke2-images.linux-amd64.tar.zst /root/rke2-artifacts
$ sudo cp rke2.linux-amd64.tar.gz /root/rke2-artifacts
$ sudo cp sha256sum-amd64.txt /root/rke2-artifacts

# INSTALL_RKE2_ARTIFACT_PATH에 방금 생성한 artifacts 폴더를 지정하여 install.sh를 실행
$ sudo su
$ INSTALL_RKE2_ARTIFACT_PATH=/root/rke2-artifacts sh install.sh
```
- 첫 번째 Node에서 Config File을 생성하여 rancher server를 시작하고 확인합니다.
```
# Config 파일 작성
mkdir -p /etc/rancher/rke2
cat << EOF >>  /etc/rancher/rke2/config.yaml
write-kubeconfig-mode: "0644"
profile: "cis-1.5"
selinux: true
EOF
```
- CIS mode를 enable 합니다.
```
# selinux 설정
$ sudo cp -f /usr/local/share/rke2/rke2-cis-sysctl.conf /etc/sysctl.d/60-rke2-cis.conf
$ sysctl -p /etc/sysctl.d/60-rke2-cis.conf
$ useradd -r -c "etcd user" -s /sbin/nologin -M etcd
```


### 3.2 Enable rke2
- rke2를 활성화 시킵니다.
```
# rke2-server service 활성화
systemctl enable rke2-server.service

# service 실행
systemctl start rke2-server.service

# rke 실행 로그 확인
journalctl -u rke2-server -f
```
### 3.3 ( 추가 ) HA 구성 위한 2 ~ 3번째 rancher server node 작업
#### 3.3.1 config파일 작성
```
$ mkdir -p /etc/rancher/rke2
$ sudo cat << EOF >>  /etc/rancher/rke2/config.yaml
write-kubeconfig-mode: "0644"
server:  https://192.168.65.134:9345 # Control Plane FQDN이 필요 할 수 있음 
token:  K106b9afcb136aa3a088e508882ad4fa1f94b9d814f36cd7c85b8a5c87643510d16::server:ca24fd2bebf3d41ec7c180cceb3b2768 # Token 값은 첫번 째 Node의 /var/lib/rancher/rke2/server/node-token 디렉토리 참조
profile: "cis-1.5"
selinux: true
EOF
```
#### 3.3.2 ( option ) ip route 설정
- 만약 offline환경에 network 설정이 아무것도 되어있지 않다면 , iproute 경로를 지정해주어야 한다.
```
$ sudo ip route add default via 192.168.65.135
```
#### 3.3.3 selinux 설정
```
$ sudo cp -f /usr/local/share/rke2/rke2-cis-sysctl.conf /etc/sysctl.d/60-rke2-cis.conf
$ sysctl -p /etc/sysctl.d/60-rke2-cis.conf
$ useradd -r -c "etcd user" -s /sbin/nologin -M etcd
```
#### 3.3.4 rke2 server up
```
# RKE2 Server UP
$ systemctl enable rke2-server.service
$ systemctl start rke2-server.service
$ journalctl -u rke2-server -f
```

### 3.3 방화벽 allow
- rke2 필요 방화벽을 열어줍니다.
  테스트를 진행할때는 default ubuntu에서 진행하였기 때문에 , 방화벽 allow 작업은 생략해도 무관합니다.
  [방화벽 목록](https://docs.rke2.io/install/requirements/#networking)
```
# For etcd nodes, run the following commands:
firewall-cmd --permanent --add-port=2376/tcp
firewall-cmd --permanent --add-port=2379/tcp
firewall-cmd --permanent --add-port=2380/tcp
firewall-cmd --permanent --add-port=8472/udp
firewall-cmd --permanent --add-port=9099/tcp
firewall-cmd --permanent --add-port=10250/tcp

# For control plane nodes, run the following commands:
firewall-cmd --permanent --add-port=80/tcp
firewall-cmd --permanent --add-port=443/tcp
firewall-cmd --permanent --add-port=2376/tcp
firewall-cmd --permanent --add-port=6443/tcp
firewall-cmd --permanent --add-port=8472/udp
firewall-cmd --permanent --add-port=9099/tcp
firewall-cmd --permanent --add-port=10250/tcp
firewall-cmd --permanent --add-port=10254/tcp
firewall-cmd --permanent --add-port=30000-32767/tcp
firewall-cmd --permanent --add-port=30000-32767/udp

# For worker nodes, run the following commands:
firewall-cmd --permanent --add-port=22/tcp
firewall-cmd --permanent --add-port=80/tcp
firewall-cmd --permanent --add-port=443/tcp
firewall-cmd --permanent --add-port=2376/tcp
firewall-cmd --permanent --add-port=8472/udp
firewall-cmd --permanent --add-port=9099/tcp
firewall-cmd --permanent --add-port=10250/tcp
firewall-cmd --permanent --add-port=10254/tcp
firewall-cmd --permanent --add-port=30000-32767/tcp
firewall-cmd --permanent --add-port=30000-32767/udp
```
## 4. rke2 worker ( agent ) 설치 방안
- rke2 agent 설치 방안입니다.
### 4.1 파일 복사
```
$ cd /home/jjsair0412/Desktop/rke2

# 다운로드한 파일을 artifacts 폴더를 생성하여 그곳으로 복사
$ sudo mkdir /root/rke2-artifacts
$ sudo cp rke2-images.linux-amd64.tar.zst /root/rke2-artifacts
$ sudo cp rke2.linux-amd64.tar.gz /root/rke2-artifacts
$ sudo cp sha256sum-amd64.txt /root/rke2-artifacts

# INSTALL_RKE2_ARTIFACT_PATH에 방금 생성한 artifacts 폴더를 지정하여 install.sh를 실행
$ sudo su
$ INSTALL_RKE2_ARTIFACT_PATH=/root/rke2-artifacts sh install.sh
```
### 4.2 config파일 작성
```
$ mkdir -p /etc/rancher/rke2
$ sudo cat << EOF >>  /etc/rancher/rke2/config.yaml
write-kubeconfig-mode: "0644"
server:  https://192.168.65.134:9345
token:  K106b9afcb136aa3a088e508882ad4fa1f94b9d814f36cd7c85b8a5c87643510d16::server:ca24fd2bebf3d41ec7c180cceb3b2768 # Token 값은 첫번 째 Node의 /var/lib/rancher/rke2/server/node-token 디렉토리 참조
profile: "cis-1.5"
selinux: true
EOF
```
### 4.3 ( option ) ip route 설정

-   만약 offline환경에 network 설정이 아무것도 되어있지 않다면 , iproute 경로를 지정해주어야 합니다.
```
$ sudo ip route add default via 192.168.65.135
```
### 4.4 selinux 설정
```
$ sudo cp -f /usr/local/share/rke2/rke2-cis-sysctl.conf /etc/sysctl.d/60-rke2-cis.conf
$ sysctl -p /etc/sysctl.d/60-rke2-cis.conf
$ useradd -r -c "etcd user" -s /sbin/nologin -M etcd
```
### 4.5 rke2 agent up
```
# RKE2 agent UP
$ systemctl enable rke2-agent.service
$ systemctl start rke2-agent.service
# 로그 확인
$ journalctl -u rke2-agent-f
```
## 5. 설치 완료 확인
-   추가적인 유틸은 `/var/lib/rancher/rke2/bin/` 아래에 설치되며, `kubectl`, `crictl`, `crt` 등이 존재합니다.
-   두 개의 CleanUp 스크립트가 `/usr/local/bin/rke2` 아래에 설치되며, 각각 `rke2-killall.sh`, `rke2-uninstall.sh` 입니다
-   kubeconfig 파일은 `/etc/rancher/rke2/rke2.yaml` 에 작성됩니다.
-   다른 서버 또는 에이전트 노드를 등록하는 데 사용할 수 있는 토큰은 다음 위치에 생성됩니다. `/var/lib/rancher/rke2/server/node-token`

```
# 노드 확인
/var/lib/rancher/rke2/bin/kubectl \
        --kubeconfig /etc/rancher/rke2/rke2.yaml get nodes

# 파드 확인
/var/lib/rancher/rke2/bin/kubectl \
        --kubeconfig /etc/rancher/rke2/rke2.yaml get pods --all-namespaces

# containerd 사용가능 이미지 확인
ctr --address /run/k3s/containerd/containerd.sock images ls
```
## 추가 setting
```
# Path에 추가
export PATH=$PATH:/var/lib/rancher/rke2/bin/

# Kubeconfig 설정
export KUBECONFIG=/etc/rancher/rke2/rke2.yaml

# ctr config 설정
export CONTAINERD_ADDRESS=/run/k3s/containerd/containerd.sock
```
## troubleshooting
### 1. rke2-server.service를 start 시켯을 때 , default route가 없다고 하는 에러
 - offline 설치 ( 이더넷 , nat 등 ) 을 전부다 해제했을 때 route가 없으면 발생하는 에러이다.
   아래 
```
# rke2-server master 한대를 지정해서 default 경로를 지정해주면 된다.
$ sudo ip route add default via 192.168.65.134
```