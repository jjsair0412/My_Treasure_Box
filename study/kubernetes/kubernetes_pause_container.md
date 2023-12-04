# Kubernetes Pause Container
Kubernetes는 파드 안에 여러 컨테이너가 존재할 수 있습니다.

또한 kubernetes는 kubelet이 컨테이너를 워커 노드에 실행하는 동작을 담당하여 진행하게 됩니다.

***이때 kubelet은 파드 내의 컨테이너만 실행하는것이 아니라, pause container라는것 또한 같이 실행합니다.***

## 1. Pause Container
실제로 Kubelet이 동작중인 worker node에 ssh 접근하여 docker ps로 작동중인 컨테이너목록을 확인해보면, 파드 내부의 컨테이너 이외의 /pause command가 실행된 컨테이너가 파드별로 한개씩 추가동작중인것을 볼 수 있습니다.

```bash
$ docker ps
...
cf68684c7dcb   nginx                                                 "nginx -g 'daemon of…"   8 minutes ago    Up 8 minutes        
991e4aa18897   registry.k8s.io/pause:3.9                             "/pause"                  8 minutes ago    Up 8 minutes       
...
```

**이 "/pause" 컨테이너는, 파드의 모든 컨테이너를 함께 담고있는 컨테이너 입니다.**

얘는 파드의 모든 컨테이너가, 동일한 네트워크와 리눅스 네임스페이스를 공유하게 되는데, 이때 이러한 네임스페이스를 모두 보유하는것이 유일한 목적인 Infra 계층 컨테이너 입니다.

파드의 컨테이너들은, 파드가 제거될수도, 생성될수도 있기 떄문에, 컨테이너가 항상 제거, 생성이 반복됩니다. 그런데 이때 파드 내부의 모든 컨테이너들은, 재생성되더라도 동일한 리눅스 네임스페이스를 사용해야 하기 때문에, 파드가 생성될 때 /pause 컨테이너가 네임스페이스를 보유한 상태로 같이 생성되며, 파드 내부의 컨테이너들은 pause 컨테이너의 네임스페이스를 사용함으로써 파드 내부의 컨테이너들이 항상 같은 네임스페이스를 사용하는데 도움을 줍니다.

이러한 특성을 가지는 pause 컨테이너는, 파드와 라이프사이클이 동일합니다.