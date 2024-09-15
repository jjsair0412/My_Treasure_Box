# Kubernetes architecutre event chain
Kubernetes 구성요소들이 이벤트가 발생하였을 때 작동하는 순서를 정리해두었습니다.

## Kubernetes 구성요소
1. API 서버
2. ETCD
3. 스케줄러
4. 컨트롤러 메니저
    - 다양한 컨트롤러가 컨트롤러 메니저 안에서 동작함
        - ex) deployment controller , replicaset controller 등..
5. kubelet
6. kube-proxy

## Deployment가 생성되었을 경우
먼저, Kubelet이 Deployment manifest를 Kube-API 서버로 HTTP POST 요청을 통해 전달합니다.

API 서버는, 정보확인 후 Deployment 정의를 검증한 뒤, 검증결과를 Response로 응답합니다.

그리고, ***Kubernetes evnet Chain이 작동***합니다.

### Event Chain
먼저, Deployment 목록을 감시하던 Deployment API Controller가 변경을 감지하고 Deployment를 정의합니다.
- 이때 생성은 실제 컨테이너가 작성되는것은 아닙니다.

그리고 Deployment는 Replicaset을 생성하기 때문에, Replicaset 컨트롤러가 해당 요청을 감시하고있다가, API Server에 생성된것을 보고있다가, Replicaset을 생성합니다.

그럼, 스케줄러가 파드 생성을 확인하고 파드를 노드에 할당 후 kubelet이 해당 노드에서 이미지를 pull하며 컨테이너를 생성, 실행하게 됩니다.

## Deployment 생성 시 event 확인
nginx deployment를 하나 생성해서 , event를 확인해 봅니다.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
  labels:
    app: nginx
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:1.14.2
        ports:
        - containerPort: 80
```

event를 통해 실제로 kubernetes에서 발생하는 이벤트를 확인해봅니다.
```bash
$ kubectl get event --watch
0s          Normal   ScalingReplicaSet   deployment/nginx-deployment   Scaled up replica set nginx-deployment-cbdccf466 to 3
0s          Normal   SuccessfulCreate    replicaset/nginx-deployment-cbdccf466   Created pod: nginx-deployment-cbdccf466-26lvf
0s          Normal   SuccessfulCreate    replicaset/nginx-deployment-cbdccf466   Created pod: nginx-deployment-cbdccf466-d6w5f
0s          Normal   Scheduled           pod/nginx-deployment-cbdccf466-26lvf    Successfully assigned default/nginx-deployment-cbdccf466-26lvf to docker-desktop
0s          Normal   SuccessfulCreate    replicaset/nginx-deployment-cbdccf466   Created pod: nginx-deployment-cbdccf466-4pbk2
0s          Normal   Scheduled           pod/nginx-deployment-cbdccf466-d6w5f    Successfully assigned default/nginx-deployment-cbdccf466-d6w5f to docker-desktop
0s          Normal   Scheduled           pod/nginx-deployment-cbdccf466-4pbk2    Successfully assigned default/nginx-deployment-cbdccf466-4pbk2 to docker-desktop
0s          Normal   Pulling             pod/nginx-deployment-cbdccf466-26lvf    Pulling image "nginx:1.14.2"
0s          Normal   Pulling             pod/nginx-deployment-cbdccf466-d6w5f    Pulling image "nginx:1.14.2"
0s          Normal   Pulling             pod/nginx-deployment-cbdccf466-4pbk2    Pulling image "nginx:1.14.2"
0s          Normal   Pulled              pod/nginx-deployment-cbdccf466-26lvf    Successfully pulled image "nginx:1.14.2" in 9.578950755s (9.579033879s including waiting)
0s          Normal   Created             pod/nginx-deployment-cbdccf466-26lvf    Created container nginx
0s          Normal   Started             pod/nginx-deployment-cbdccf466-26lvf    Started container nginx
0s          Normal   Pulled              pod/nginx-deployment-cbdccf466-d6w5f    Successfully pulled image "nginx:1.14.2" in 2.00062575s (11.248408004s including waiting)
0s          Normal   Created             pod/nginx-deployment-cbdccf466-d6w5f    Created container nginx
0s          Normal   Started             pod/nginx-deployment-cbdccf466-d6w5f    Started container nginx
0s          Normal   Pulled              pod/nginx-deployment-cbdccf466-4pbk2    Successfully pulled image "nginx:1.14.2" in 2.003686501s (13.250068631s including waiting)
0s          Normal   Created             pod/nginx-deployment-cbdccf466-4pbk2    Created container nginx
0s          Normal   Started             pod/nginx-deployment-cbdccf466-4pbk2    Started container nginx
```

