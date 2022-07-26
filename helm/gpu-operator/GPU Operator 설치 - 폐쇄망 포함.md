# GPU Operator 설치 - 폐쇄망 설치 포함
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
## 2. 설치 과정
- 설치 과정은 아래와 같습니다.
1. containerd config.toml 설정 변경
2. nfd 설치
3. gpu-operator 설치
## 3. nouveau 해제
- nouveau 활성화 확인
```
$ lsmod | grep nouveau
```
- 해제 명령어는 centos , ubuntu 둘 다 동일하다.
```
$ vi /etc/modprobe.d/blacklist.conf

마지막 줄에 추가
blacklist nouveau
blacklistlbm-nouveau
```
## 3. containerd 설정 변경
- rke2에서는 runtime을 docker가 아닌 containerd를 사용합니다.
- rke2에서 containerd의 설정 값을 변경하기 위해서는 아래 위치의 config.toml 파일 설정값을 변경해주어야 합니다.
```
$ vi /var/lib/rancher/rke2/agent/etc/containerd/config.toml
```
- 그러나 폐쇄망 환경이기에 , registries.yaml 파일이 존재하므로 config.toml을 임의로 생성한다 하더라도 , rke2가 restart 할 때 registries.yaml 설정 값이 임의 설정한 config.toml을 덮어 씌우게 됩니다. -> registries.yaml의 값을 보고 config.toml파일을 생성합니다.
- 따라서 config.toml.tmpl 라는 이름의 파일을 생성하고 , private registry설정을 포함한 모든 설정 값을 넣어주어야 합니다.
- config.toml.tmpl 파일은 , config.toml파일을 생성하는 고급 옵션들이 들어갈 수 있는 파일입니다.

***registries.yaml 파일 설정값***
```
# cat /etc/rancher/rke2/registries.yaml

mirrors:
  docker.io:
    endpoint:
      - "http://10.xxx.xxx.xxx:5000"
  10.xxx.xxx.xxx:5000:
    endpoint:
      - "http://10.xxx.xxx.xxx:5000"
  harbor.xxx.xxx.com:
    endpoint:
      - "http://harbor.xxx.xxx.com"

configs:
  "harbor.xxx.xxx.com":
    auth:
      username: admin
      password: Harbor12345
    tls:
      insecure_skip_verify: true
```
***config.toml.tmpl 설정값***
```
# cat /var/lib/rancher/rke2/agent/etc/containerd/config.toml.tmpl
[plugins.opt]
  path = "/var/lib/rancher/rke2/agent/containerd"

[plugins.cri]
  stream_server_address = "127.0.0.1"
  stream_server_port = "10010"
  enable_selinux = false
  sandbox_image = "index.docker.io/rancher/pause:3.6"

[plugins.cri.containerd]
  default_runtime_name = "nvidia"
  snapshotter = "overlayfs"
  disable_snapshot_annotations = true


[plugins.cri.containerd.runtimes.runc]
  runtime_type = "io.containerd.runc.v2"

[plugins.cri.containerd.runtimes.nvidia]
  privileged_without_host_devices = false
  runtime_engine = ""
  runtime_root = ""
  runtime_type = "io.containerd.runc.v2"

  [plugins.cri.containerd.runtimes.nvidia.options]
     BinaryName = "/usr/bin/nvidia-container-runtime"
     SystemdCgroup = true

[plugins.cri.containerd.runtimes.runc.options]
  SystemdCgroup = true

[plugins.cri.registry.mirrors]

[plugins.cri.registry.mirrors."10.xxx.xxx.xx:5000"]
  endpoint = ["http://10.xxx.xxx.xxx:5000"]

[plugins.cri.registry.mirrors."docker.io"]
  endpoint = ["http://10.xxx.xxx.xxx:5000"]

[plugins.cri.registry.mirrors."gitlab.xxx.xxx.xxx"]
  endpoint = ["http://gitlab.xxx.xxx.xxx"]
  
[plugins.cri.registry.mirrors."harbor.xxx.xxx.xxx"]
  endpoint = ["http://harbor.xxx.xxx.xxx"]

[plugins.cri.registry.configs."gitlab.xxx.xxx.xxx".auth]
  username = "root"
  password = "pgAxVAiuLBUvSx2qcuC9Boub4BWu..."

[plugins.cri.registry.configs."gitlab.xxx.xxx.xxx".tls]
  cert_file = "/home/ubuntu/project-pack/keys/custom.crt"
[plugins.cri.registry.configs."harbor.xxx.xxx.xxx".auth]
  username = "admin"
  password = "Harbor12345"

[plugins.cri.registry.configs."harbor.xxx.xxx.xxx".tls]
  insecure_skip_verify = true
```
***폐쇄망 구성이 아닐경우 config.toml.tmpl 설정값***
```
[plugins.opt]
  path = "/var/lib/rancher/rke2/agent/containerd"

[plugins.cri]
  stream_server_address = "127.0.0.1"
  stream_server_port = "10010"
  enable_selinux = false
  sandbox_image = "index.docker.io/rancher/pause:3.6"

[plugins.cri.containerd]
  default_runtime_name = "nvidia"
  snapshotter = "overlayfs"
  disable_snapshot_annotations = true


[plugins.cri.containerd.runtimes.nvidia.options]
  BinaryName = "/usr/bin/nvidia-container-runtime"

[plugins.cri.containerd.runtimes.nvidia]
  privileged_without_host_devices = false
  runtime_engine = ""
  runtime_root = ""
  runtime_type = "io.containerd.runc.v2"
```

***rke2-server & rke2-agent restart***
```
$ sudo systemctl restart rke2-server

$ sudo systemctl restart rke2-agent
```