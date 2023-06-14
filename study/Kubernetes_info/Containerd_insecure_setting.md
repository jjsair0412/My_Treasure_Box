# Containerd_insecure_setting
## Overview 
containerd를 container runtime으로 사용하는 k8s cluster에서 , 특정 private reigstry에 대한 tls인증을 무시하도록 insecure 설정하는 방안에 대해 기술한 문서입니다.

## ETC
만약 config.toml 파일의 설정이 잘못됐을 경우 ,. containerd.sock 파일이 생성되지 않으며 , containerd 프로세스는  containerd.sock 파일을 찾지 못하여 실행에 실패하는 에러 로그가 발생하게 됩니다.

## 설정 진행
containerd의 config.toml 파일을 수정합니다.

config.toml 파일의 위치는 다음과 같습니다.
```bash
$ pwd
/etc/containerd/config.toml
```

아래 설정대로 설정합니다.
- usecase
```toml
...
      [plugins."io.containerd.grpc.v1.cri".registry.configs]
        [plugins."io.containerd.grpc.v1.cri".registry.configs."Registry_Domain".tls]
          insecure_skip_verify = true
      [plugins."io.containerd.grpc.v1.cri".registry.headers]

      [plugins."io.containerd.grpc.v1.cri".registry.mirrors]
        [plugins."io.containerd.grpc.v1.cri".registry.mirrors."Registry_Domain"]
          endpoint = ["Registry_Domain"]
...
```

- 실제 사용 명령어
```toml
...
      [plugins."io.containerd.grpc.v1.cri".registry.configs]
        [plugins."io.containerd.grpc.v1.cri".registry.configs."private_regi.jinseong.com".tls]
          insecure_skip_verify = true
      [plugins."io.containerd.grpc.v1.cri".registry.headers]

      [plugins."io.containerd.grpc.v1.cri".registry.mirrors]
        [plugins."io.containerd.grpc.v1.cri".registry.mirrors."private_regi.jinseong.com"]
          endpoint = ["private_regi.jinseong.com"]
...
```
