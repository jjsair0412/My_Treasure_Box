# Application Access To K8S Resource
Container 내부 Application에서 kubernetes resource를 control 하거나 접근하는 방법 들
## downward api
downward API를 사용하면 , 아래 목록인 파드 메타데이터들을 파드 내에서 실행중인 프로세스에 노출시킬 수 있습니다.
- 파드 이름
- 파드 IP 주소
- 파드가 속한 namespace
- 파드가 실행 중인 노드 이름
- 파드가 실행 중인 서비스 어카운트 이름
- 각 컨테이너의 CPU와 메모리 요청
- 각 컨테이너의 CPU와 메모리 제한
- 파드의 레이블
- 파드의 어노테이션

### 1. downward api로 컨테이너 env에 등록하기
yaml의 env 필드 속성에 , metadata.~ 을 주면 파드내부 env 환경변수에 등록되게 됩니다.
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: downward
spec:
  containers:
  - name: main
    image: busybox
    command: ["sleep", "9999999"]
    resources:
      requests:
        cpu: 15m
        memory: 100Ki
      limits:
        cpu: 100m
        memory: 6Mi
    env:
    - name: POD_NAME
      valueFrom:
        fieldRef:
          fieldPath: metadata.name # 특정 값을 지정하는것이 아닌 , Pod metadata의 name을 참조 . 
    - name: POD_NAMESPACE
      valueFrom:
        fieldRef:
          fieldPath: metadata.namespace
    - name: POD_IP
      valueFrom:
        fieldRef:
          fieldPath: status.podIP
    - name: NODE_NAME
      valueFrom:
        fieldRef:
          fieldPath: spec.nodeName
    - name: SERVICE_ACCOUNT
      valueFrom:
        fieldRef:
          fieldPath: spec.serviceAccountName
    - name: CONTAINER_CPU_REQUEST_MILLICORES # container cpu / memory 요청 / 제한은 resourceFieldRef 사용
      valueFrom:
        resourceFieldRef:
          resource: requests.cpu
          divisor: 1m # 리소스 필드의 경우 , 필요한 단위값을 가지려면 divisor (제수) 를 지정해야 함
    - name: CONTAINER_MEMORY_LIMIT_KIBIBYTES
      valueFrom:
        resourceFieldRef:
          resource: limits.memory
          divisor: 1Ki
```

- busybox의 env를 확인해 보면 , 다음과 같은 결과 출력
```yaml
$ kubectl exec -it downward -- env 
...
CONTAINER_CPU_REQUEST_MILLICORES=15
CONTAINER_MEMORY_LIMIT_KIBIBYTES=6144
POD_NAME=downward
POD_NAMESPACE=default
POD_IP=10.1.0.7
NODE_NAME=docker-desktop
SERVICE_ACCOUNT=default
KUBERNETES_PORT_443_TCP_PROTO=tcp
KUBERNETES_PORT_443_TCP_PORT=443
KUBERNETES_PORT_443_TCP_ADDR=10.96.0.1
KUBERNETES_SERVICE_HOST=10.96.0.1
KUBERNETES_SERVICE_PORT=443
KUBERNETES_SERVICE_PORT_HTTPS=443
KUBERNETES_PORT=tcp://10.96.0.1:443
KUBERNETES_PORT_443_TCP=tcp://10.96.0.1:443
```


### 2. downward api로 컨테이너 안에 메타데이터 파일로 저장하기
볼륨을 지정해서 , 메타데이터 파일로 저장할 수 있습니다.
- /etc/downward 폴더를 볼륨으로 지정하여 , 해당 경로에 메타데이터값으로 저장합니다.

*주의할 점*
- cpu / 메모리 요청 및 제한사항과 같은 컨테이너별 정보인 경우 , 값을 가져올 때 , container 이름을 containerName으로 명시적으로 지정해주어야 합니다.
- 컨테이너 자체 스펙이 아니라 , 볼륨에서 정의하는것이기 때문에 !! 당연한것

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: downward
  labels:
    foo: bar
  annotations:
    key1: value1
    key2: |
      multi
      line
      value
spec:
  containers:
  - name: main
    image: busybox
    command: ["sleep", "9999999"]
    resources:
      requests:
        cpu: 15m
        memory: 100Ki
      limits:
        cpu: 100m
        memory: 6Mi
    volumeMounts:
    - name: downward
      mountPath: /etc/downward
  volumes:
  - name: downward
    downwardAPI:
      defaultMode: 0644
      items:
      - path: "podName"
        fieldRef:
          fieldPath: metadata.name
      - path: "podNamespace"
        fieldRef:
          fieldPath: metadata.namespace
      - path: "labels"
        fieldRef:
          fieldPath: metadata.labels
      - path: "annotations"
        fieldRef:
          fieldPath: metadata.annotations
      - path: "containerCpuRequestMilliCores"
        resourceFieldRef:
          containerName: main
          resource: requests.cpu
          divisor: 1m
      - path: "containerMemoryLimitBytes"
        resourceFieldRef:
          containerName: main
          resource: limits.memory
          divisor: 1
```

- busybox의 /etc/downward 경로에 생성된 메타데이터 값을 확인
    - 각 파일의 권한은 , 파드 스펙에서 downwardAPI 볼륨의 defaultMode 속성으로 파일 권한 변경 가능
```bash
$  kubectl exec -it downward -- ls -lL /etc/downward
total 24
-rw-r--r--    1 root     root          1343 May 14 06:22 annotations
-rw-r--r--    1 root     root             2 May 14 06:22 containerCpuRequestMilliCores
-rw-r--r--    1 root     root             7 May 14 06:22 containerMemoryLimitBytes
-rw-r--r--    1 root     root             9 May 14 06:22 labels
-rw-r--r--    1 root     root             8 May 14 06:22 podName
-rw-r--r--    1 root     root             7 May 14 06:22 podNamespace

# podName metaData 확인
$ kubectl exec -it downward -- cat /etc/downward/podName
downward
```
## kubernetes api server
downward api를 통해서 얻는 메타데이터는 , 파드자체 정보만 전달할 수 있다는 한계가 있습니다.

따라서 Kubernetes의 api server에 요청을 보내서 , k8s 전체 리소스정보를 json값으로 확인할 수 있습니다.

api server 정보를 확인
```bash
$ kubectl cluster-info
Kubernetes control plane is running at https://127.0.0.1:6443                            o
CoreDNS is running at https://127.0.0.1:6443/api/v1/namespaces/kube-system/services/kube-dns:dns/proxy                                                                            dns:dns/proxy

To further debug and diagnose cluster problems, use 'kubectl cluster-info dump'.
```

api서버 자체 6443포트로 요청을 보내기 위해선 , ssl 인증과정을 거쳐야 하기 때문에 , kube-proxy 명령으로 프록시 서버를 실행하여 접근합니다.
- kube proxy는 로컬 컴퓨터에서 HTTP 요청을 수신하여 해당 연결을 인증을 관리하면서 API 서버로 proxy하기 때문에 , 인증 토큰을 전달할 필요가 없습니다.
```bash
$ kubectl proxy
Starting to serve on 127.0.0.1:8001

$ curl 127.0.0.1:8001
StatusCode        : 200
StatusDescription : OK
Content           : {
                      "paths": [
                        "/.well-known/openid-configuration",
                        "/api",
...
```

proxy로 열린 8001번 포트로 , k8s 리소스정보를 확인할 수 있으며 , domain정보를 가지고 특정 리소스 정보를 갖고올 수 도 있습니다.
```bash
# k8s 클러스터에 생성된 job 종류 전체확인
$ curl http://127.0.0.1:8001/apis/batch/v1/jobs

# namespace , job name으로 특정 job 확인
$ curl http://127.0.0.1:8001/apis/batch/v1/namespaces/default/jobs/my-job

# 아래 결과와 동일
$ kubectl get job my-job -o json
```

## 파드 내에서 API 서버 통신
파드 내에서 API서버와 통신하려면 , kubectl이 없기에 세 단계를 거쳐야 합니다.
1. API 서버 위치 찾기
2. API 서버와 통신하고 있는지 확인
  - API서버 인 척 가장하는 proxy같은애랑 통신하는것이 아니라 ..
3. API 서버로 인증

### 1. Pod 생성
먼저 API 서버와 통신할 Pod를 생성해야 합니다.
- curl 바이너리가 포함된 이미지를 사용해야 합니다.
- 해당 예제에선 curl image를 사용합니다.
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: curl
spec:
  containers:
  - name: main
    image: curlimages/curl
    command: ["sleep", "9999999"]
```
해당 pod로 exec 합니다.

```bash
$ kubectl exec -it curl -- sh 
/ $
```

### 2. Kubernetes svc 확인
kubernetes는 기본 svc로 Kubernetes를 갖고 있으며 , 얘는 443 포트를 사용합니다.
- 이 svc로 api 서버와 통신합니다.
```bash
$ kubectl get svc
NAME         TYPE        CLUSTER-IP   EXTERNAL-IP   PORT(S)   AGE
kubernetes   ClusterIP   10.96.0.1    <none>        443/TCP   152m
```

Pod는 Kubernetes의 각 서비스에 대해 환경 변수가 구성되어 있습니다.
- 10.96.0.1의 기본 포트 : 443
```bash
/ $ env | grep KUBERNETES_SERVICE
KUBERNETES_SERVICE_PORT=443
KUBERNETES_SERVICE_PORT_HTTPS=443
KUBERNETES_SERVICE_HOST=10.96.0.1
```

또한 각 서비스는 FQDN를 갖고 있으며 , Pod는 env에 등록된 svc 정보와 , Pod 네트워크는 Kube-dns를 통과하기 때문에 , FQDN로 curl 명령을 수행할 수 있습니다.
- http로 curl을 날리면 안됩니다. 왜냐면 kubernetes svc의 기본 port를 pod env에서 https인 443으로 알고 있기 때문입니다.
- ping도 안됩니다. 왜냐면 service의 cluster ip는 가상 ip이므로 , port번호가 없다면 의미가 없기 때문입니다.
```bash
/ $ curl https://kubernetes.default.svc.cluster.local
curl: (60) SSL certificate problem: unable to get local issuer certificate
More details here: https://curl.se/docs/sslcerts.html

curl failed to verify the legitimacy of the server and therefore could not
establish a secure connection to it. To learn more about this situation and
how to fix it, please visit the web page mentioned above.

# http is not working
/ $ curl http://kubernetes.default.svc.cluster.local
curl: (7) Failed to connect to kubernetes.default.svc.cluster.local port 80 after 21050 ms: Couldnt connect to server

# Ping not working
/ $ ping kubernetes.default.svc.cluster.local
PING kubernetes.default.svc.cluster.local (10.96.0.1): 56 data bytes
--- kubernetes.default.svc.cluster.local ping statistics ---
8 packets transmitted, 0 packets received, 100% packet loss
```

### 3. 서버 인증 생성
모든 Pod는 생성될 때 , ```default-token-xyz``` 라는 이름의 secret이 아래 경로에 마운트되어 생성됩니다.
- 해당 경로엔 , ca.crt , namespace , token 세 가지 파일이 위치하게 됩니다. 
```bash
$ pwd
/var/run/secrets/kubernetes.io/serviceaccount/

$ ls
ca.crt     namespace  token
```

ca.crt로 kubernetes api 서버에 인증하게 되는데 , 그냥 --cacert 옵션으로 curl 날리면 아래와 같은 에러가 발생합니다.
- 403 에러가 발생했기에 , 서버가 신뢰할 수 있는 인증서인지까진 확인했지만 , 인증 처리가 필요합니다.
- client는 API Server를 신뢰 하지만 , API Server는 client가 누군지 모르기 떄문에 403에러를 발생시킵니다.
```bash
$ curl https://kubernetes.default.svc.cluster.local --cacert ca.crt
{
  "kind": "Status",
  "apiVersion": "v1",
  "metadata": {},
  "status": "Failure",
  "message": "forbidden: User \"system:anonymous\" cannot get path \"/\"",
  "reason": "Forbidden",
  "details": {},
  "code": 403
}
```

export 명령어로 curl 명령을 날릴 때 해당 ca.cert로 자동 인증하게끔 환경 변수를 설정합니다.
```bash
$ export CURL_CA_BUNDLE=/var/run/secrets/kubernetes.io/serviceaccount/ca.crt

# --cacert 없이도 자동 인증
$ curl https://kubernetes.default.svc.cluster.local
{
  "kind": "Status",
  "apiVersion": "v1",
  "metadata": {},
  "status": "Failure",
  "message": "forbidden: User \"system:anonymous\" cannot get path \"/\"",
  "reason": "Forbidden",
  "details": {},
  "code": 403
}
```

서버에 인증하기 위해 , default-token secret에서 제공하는 Token 파일을 사용합니다.
- Token을 환경변수에 로드합니다.
```bash
$ TOKEN=$(cat /var/run/secrets/kubernetes.io/serviceaccount/token)
```

해당 Token을 이용해 curl을 날려봅니다.
```bash
$ curl -H "Authorization: Bearer $TOKEN" https://kubernetes
{
  "kind": "Status",
  "apiVersion": "v1",
  "metadata": {},
  "status": "Failure",
  "message": "forbidden: User \"system:serviceaccount:default:default\" cannot get path \"/\"",
  "reason": "Forbidden",
  "details": {},
  "code": 403
}
```

아직 403에러가 발생하는데 , 그 이유는 RBAC이 활성화 되어 있기 때문입니다.

특정 파드마다 role을 다르게 부여해야 하는데 , 테스트기 떄문에 아래 명령어로 모든 service account에 cluster-admin 권한을 부여하여 해당 에러를 우회합니다.
- 실 개발에선 절대 이렇게하면 안됩니다.
```bash
$ kubectl create clusterrolebinding permissive-binding \
  --clusterrole=cluster-admin \
  --group=system:serviceaccounts
```

다시 curl 해보면 , api 서버로 정상 접근 되는것을 확인할 수 있습니다.
```bash
$ curl -H "Authorization: Bearer $TOKEN" https://kubernetes
{
  "paths": [
    "/.well-known/openid-configuration",
    "/api",
    "/api/v1",
    "/apis",
    "/apis/",
    "/apis/admissionregistration.k8s.io",
    "/apis/admissionregistration.k8s.io/v1",
    ...
  ]
}
```

### 4. 파드 실행중인 Namespace 얻기
default-token-xyz 시크릿 볼륨의 마지막 파일인 namespace를 사용합니다.

얘는 해당 파드가 위치한 namespace 이름을 갖고있는데 , 이걸 NS 환경변수에 로드하여 api server에 요청을 보내면 됩니다.

이런식으로 GET , PUT , DELETE 등 명령을 수행하면 됩니다.
```bash
# default namespace에 위치
$ cat namespace
default

# 환경변수 로드
$ NS=$(cat /var/run/secrets/kubernetes.io/serviceaccount/namespace)

# default namespace의 pod들 get
$ curl -H "Authorization: Bearer $TOKEN" https://kubernetes/api/v1/namespaces/$NS/pods

# default namespace의 service들 get
$ curl -H "Authorization: Bearer $TOKEN" https://kubernetes/api/v1/namespaces/$NS/services
```

그냥 default를 넣으면 , 404 에러 발생
```bash
$ curl -H "Authorization: Bearer $TOKEN" https://kubernetes/api/v1/namespaces/default/service
{
  "kind": "Status",
  "apiVersion": "v1",
  "metadata": {},
  "status": "Failure",
  "message": "the server could not find the requested resource",
  "reason": "NotFound",
  "details": {},
  "code": 404
}
```
