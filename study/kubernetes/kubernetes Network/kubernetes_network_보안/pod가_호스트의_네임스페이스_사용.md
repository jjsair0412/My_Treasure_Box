# pod가 호스트의 네임스페이스 사용하는 방안들
## 1. Pod가 호스트의 network namespace를 사용하는 경우
Pod는 pause 컨테이너가 리눅스 네임스페이스 정보를 갖고잇으면서 해당 pause 컨테이너의 네임스페이스를 파드 내부 컨테이너들이 공유하며 작동합니다.

따라서 Pod 내부의 컨테이너는 모두 같은 네임스페이스를 공유하게 되는데, 이것은 호스트의 네임스페이스와는 분리된 독립적인 네임스페이스 입니다. 그렇기에 파드는 분리된 network namespace를 사용하기에 독립적인 IP:Port를 가질 수 있고, 프로세스 간 통신 메커니즘(IPC)로도 서로 통신할 수 있습니다.

그러나 때에 따라서, host의 기본 네임스페이스에서 동작해야하는 경우가 있는데, 예를들어 가상 네트워크 어뎁터 대신 호스트의 실제 네트워크 어뎁터를 사용하는 경우가 있습니다. 이럴땐, ```Pod Template```에서 ```hostNetwork``` 속성을 ```true```로 두면 됩니다.
- 이렇게 두면 host network namespace를 사용하게 됩니다.


```yaml
apiVersion: v1
kind: Pod
metadata:
  name: pod-with-host-network
spec:
  hostNetwork: true # 호스트와 동일 네트워크 어뎁터 사용
  containers:
  - name: main
    image: alpine
    command: ["/bin/sleep", "999999"]
```

위 예시 Pod를 생성하고 exec 명령어로 ifconfig를 날려보면, Host와 동일한 네트워크 어뎁터를 사용하기에 결과가 호스트의 ifconfig와 동일한것을 확인할 수 있으며, IP 대역또한 Host와 동일한것을 볼 수 있습니다.

```bash
# 둘은 동일
$ kubectl exec pod-with-host-network -- ifconfig
...

$ ifconfig
...
```

network 대역이 동일한것을 확인할 수 있습니다.
- 원래는 CNI에서 할당한 네트워크 대역이어야 함
```bash
$ kubectl get pods -o wide
NAME                    READY   STATUS    RESTARTS   AGE   IP             NODE             NOMINATED NODE   READINESS GATES
pod-with-host-network   1/1     Running   0          46s   192.x.x.x      docker-desktop   <none>           <none>


# hostNetwork 없이 생성된 pod의 IP 결과
$ kubectl get pods -o wide
NAME                    READY   STATUS    RESTARTS   AGE   IP           NODE             NOMINATED NODE   READINESS GATES
pod-with-host-network   1/1     Running   0          5s    10.1.0.164   docker-desktop   <none>           <none>
```


## 2. Host Network Namespace를 사용하지 않고 파드가 Host Port에 바인딩하는 방법
hostNetwork 옵션으로 노드의 기본 네임스페이스에 바인딩 할 수 있지만, 여전히 고유한 네트워크 네임스페이스를 갖습니다.

따라서 직접 호스트의 실제 포트와 파드를 바인딩 할 수 있는데, ```sepc.containers.ports.hostPort``` 속성을 통해 실제 포트와 파드를 바인딩시킬 수 있습니다.

### 2.1 NodePort vs hostPort
NodePort와 혼동할 수 있습니다. 그러나 동작 방식이 엄연히 다릅니다.

NodePort는 Kubernetes Cluster의 모든 WorkNode들에게 해당 Port가 열리고, 열린 Port를 통해 들어온 패킷은(iptables mode라면) Kube-proxy가 관리하는 table로 흐르게 되고, iptables에서 rule을 확인하고 해당 패킷은 NAT되어 서비스 백엔드 파드로 전달되게 됩니다.

이때 백엔드 파드는 Kubernetes Cluster 어디에 위치하던 상관없이 전송되게 되고, 해당 파드가 실행되지 않는 노드라 할지라도 해당 포트는 Open되어 바인딩 되게 됩니다.

그러나, hostPort는 파드가 배포된 워커노드 자체의 포트랑 바인딩 됩니다.

따라서 **hostPort는 파드가 배포되지않은 노드에선 hostPort가 열리지 않고, 배포된 노드에서만 바인딩됩니다.** 만약 파드의 replica가 4이고 노드가 3대만 존재한다면, **파드 한대는 hostPort가 겹치기 때문에(한 노드에 2개포트를 바인딩할수없음)3대만 running이고 나머지 한대는 pending상태에 놓이게 됩니다.**
- 두 프로세스가 동일한 포트를 바인딩할 수 없어서 pending상태인겁니다. 파드가 프로세스라는 증거가 하나 더 생김

또한 패킷의 전달구조도 다른데, **hostPort로 전달된 패킷은 kube-proxy를 거치지 않고 파드에 직접닿게됩니다.**

### 2.1 사용방안
사용 방안은 간단합니다. ```sepc.containers.ports.hostPort``` 속성에 호스트 노드의 포트를 지정하고, ```sepc.containers.ports.containerPort``` 에 호스트 노드포트에 바인딩할 컨테이너 포트를 지정하면 됩니다.

해당 파드가 배포된 노드에서만 접근할 수 있고, 다른 노드에선 접근이 불가능합니다.
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: jinseong-hostport
spec:
  containers:
  - image: nginx
    name: kubia
    ports:
    - containerPort: 8080
      hostPort: 9000
      protocol: TCP
```

### 2.2 사용 사례
해당 hostPort는 기본적으로 데몬셋을 사용해 모든 노드에 배포되는 시스템을 노출하는데 사용합니다.

## 3. 호스트 노드의 PID와 IPC Namespace 사용하기
hostNetwork와 유사한 옵션으로 hostPID, hostIPC 가 있습니다.

이를 true로 설정하면 각각

hostPID
  - 파드의 컨테이너에서 파드가 배포된 호스트에 실행중인 다른 프로세스를 확인할 수 있음.
hostIPC
  - 파드의 컨테이너에서 파드가 배포된 호스트에 실행중인 프로세스와 IPC로 통신할 수 있음.

과 같은 효과를 볼 수 있습니다. 이는 파드 컨테이너가 호스트 리눅스 네임스페이스에 PID를 부여받는다는것을 의미합니다.
- 파드 container가 pause 컨테이너의 네임스페이스를 사용하지 않고, 파드 내의 컨테이너는 호스트의 Process ID (PID) 네임스페이스를 사용하게 됩니다.


### 3.2 사용방안
hostPID 와 hostIPC 를 true로 둡니다.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: pod-with-host-pid-and-ipc
spec:
  hostPID: true
  hostIPC: true
  containers:
  - name: main
    image: alpine
    command: ["/bin/sleep", "999999"]
```

생성 한 뒤 exec 명령어로 ps aux 명령어를 수행하여 프로세스 목록을 확인하면, 호스트에 작동중인 프로세스 목록을 확인할 수 있으며, IPC로 통신또한 가능합니다.

```bash
$ kubectl exec pod-with-host-pid-and-ipc ps aux
```
