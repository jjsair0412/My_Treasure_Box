# Study probes
livenessProbe와 readinessProbe에 대해 기술합니다.

둘의 차이점을 명확히 아는것이 가장 중요하다고 보여집니다.
### 1. 라이브니스 프로브란 ?
컨테이너가 살아있는지 확인한다. 
즉 , 컨테이너가 동작한 이후에 동작한다.
주기적으로 probe를 수행하고 probe가 실패할 경우 컨테이너 restart
livenessPorbe는 kubelet에 의해 재 시작 된다. ( 수행한다 . )
#### 1.1 k8s 라이브니스 프로브 종류
1. HTTP GET probe
- 지정한 IP 주소 , 포트 , 경로에 HTTP GET  요청 수행
- probe가 응답 코드를 수신한 이후 , 응답 코드가 오류를 나타내지 않는 경우 probe가 성공했다고 간주함.
- HTTP request code 중 2xx , 3xx 만 성공으로 간주
2. TCP socket probe
- probe가 지정된 포트에 TCP 연결 시도
- 성공하면 probe 성공 , 실패하면 probe 실패
3. exec probe
- 컨테이너 내의 임의의 명령을 수행하고 , 명령의 종료 상태 코드 확인
- 종료 상태 코드가 0인 경우에만 성공,, 모든 다른 코드는 실패로 간주
### 1.2 liveness probe 실패 이후 에러로그 보는 유용한 명령어
previous 옵션 사용하면 된다.
```
kubectl logs mypod --previous
```
### 1.3 Exit code 확인법
아래 예제는 liveness probe , http probe가 실패한 이후 반환된 describe 결과이다.
Exit Code가 143으로 떨어진것을 확인할 수 있는데 , 해당 숫자는 두 수를 더한 값으로 ,
128 + x 이다.

여기서 x는 프로세스에 전송된 시그널 번호이며 , 해당 시그널로 인해 컨테이너가 종료 되었다는 것을 알 수 있다.
```yaml
...
  Last State:     Terminated
      Reason:       Error
      Exit Code:    143
      Started:      Tue, 25 Oct 2022 00:17:01 +0900
      Finished:     Tue, 25 Oct 2022 00:18:18 +0900
    Ready:          False
    Restart Count:  13
    Liveness:       http-get http://:8080/ delay=0s timeout=1s period=10s #success=1 #failure=3
    Environment:    <none>
...
```

### 1.4 liveness probe 주의 사항
#### 1.4.1 initialDelaySeconds
livenessprobe는 파드가 정상 수행된 이후에 점검해야 하기에 , 수행 시기를 잘 맞추어줘야 한다.
initialDelaySeconds로 시간을 조정한다.
```yaml
livenessProbe:
  httpGet:
    path: /
    port: 8080
  initialDelaySeconds: 15 
```
initialDelaySeconds를 두면 , 쿠버네티스는 첫 번째 probe 실행 까지 설정된 값에 따라 대기한다.
위처럼 설정하면 , 15초를 대기하게 된다.

따라서 application 시작 시간을 고려해 initialDelaySeconds을 적절히 두어야 한다.

#### 1.4.2 운영상의 k8s에서 ..
prod 환경에서 실행 중인 파드는 반드시 livenessProbe가 들어가야 한다.
정의하지 않으면 , k8s는 파드가 정상 수행중인지 알 수 있는 방법이 없기 때문이다.

#### 1.4.3 사용 시점의 주의사항
httpGet probe를 사용한다면 , 엔드포인트에 인증이 필요하지 않은지 꼭 체크해야 한다.
인증이 들어가있으면 ,,컨테이너가 항상 실패하여 무한 재시작한다.

livenessProbe는 외부 요인의 영향을 받지 않도록 해야 한다.

livenessProbe는 너무 많은 연산을 가지면 안됀다 . 
느려진다..

livenessProbe는 실패하면 어차피 다시 시작하기에 , 다시 실행하는 연산을 둘 필요가 없다.

### 2. 레디니스 프로브란 ?
k8s에서는 livenessProbe와 비슷한 readinessProbe 가 있다.

livenessProbe와 같이 readinessProbe는 파드 내부 container마다 정의할 수 있으며 ,
주기적으로 컨테이너의 실행 상태를 점검한다.

readinessProbe는 주기적으로 호출되어 특정 파드가 ( 파드 내부 컨테이너가 ) 클라이언트 요청을 수행할 수 있는지 확인한다.
따라서 readinessPorbe가 성공 ( 1 ) 을 반환하면 , 클라이언트 요청을 수행할 수 있다는 것을 의미한다.

요청을 수행할 준비가 됐다 라는 것은 application마다 상이하기 때문에 특정 URL 경로에 GET 요청으로 판단하기도 하고 , 전체적인 항목을 검사하기도 한다.

#### 2.1 readinessProbe의 종류
livenessProbe와 같이 세 종류가 있다.
1. HTTP GET probe
- 지정한 IP 주소 , 포트 , 경로에 HTTP GET  요청 수행
- probe가 응답 코드를 수신한 이후 , 응답 코드가 오류를 나타내지 않는 경우 probe가 성공했다고 간주함.
- HTTP request code 중 2xx , 3xx 만 성공으로 간주
2. TCP socket probe
- probe가 지정된 포트에 TCP 연결 시도
- 성공하면 probe 성공 , 실패하면 probe 실패
3. exec probe
- 컨테이너 내의 임의의 명령을 수행하고 , 명령의 종료 상태 코드 확인
- 종료 상태 코드가 0인 경우에만 성공,, 모든 다른 코드는 실패로 간주

#### 2.2 readinessProbe의 동작 방식
컨테이너가 실행될 때 readinessProbe가 검사를 시작할 시간을 선택할 수 있다.

livenessProbe와 동일하게 initialDelaySeconds를 통해서 시간이 경과하기를 기다릴 수 있다.
```yaml
...
spec:
  containers:
  - name: sample
    readinessProbe:
      initialDelaySeconds: 5  // 5초 대기
...
```

readinessProbe는 주기적으로 파드 내부 컨테이너에게 요청 및 명령을 수행한다.

#### 2.3 readinessProbe의 활용 방안
만약 한 서비스가 세개의 frontend 파드를 바라보고 있다고 치자.

이때 세개중 하나의 파드가 DB 연결 문제가 발생하여 요청을 정상 수행하지 못한다고 했을 때 ,
readinessProbe를 미리 선언하여 모든 파드를 떨어트렷다가 다시 생성시키는 것이 아니라 ,
문제가 있는 파드만 service의 endpoint에서 제외시킨다면 서비스가 중단되지 않을 것이며
파드의 문제를 파악하는데에도 더 간편할 것이다.

#### 2.4 readinessProbe 주의 사항!!
1. readinessProbe를 항상 사용하자
만약 사용하지 않으면 , 파드 내부 컨테이너가 준비되지 않았음에도 service endpoint에 파드가 등록된다.
따라서 사용자는 connection refuse 에러를 볼 것이기에 ,,
readinessProbe를 항상 사용하여 pod가 준비됐을 때만 service endpoint로 등록하자. 

2. readinessProbe에 파드 종료 코드를 포함하면 안됀다.
k8s에서는 파드가 삭제되면 service의 endpoint에서 즉시 제거되기 때문에 ,
파드가 종료될 때 내부 컨테이너 application의 종료 코드가 필요하지 않다.

### 3. import !!! livenessProbe vs readinessProbe
livenessProbe는 검사가 실패하면 컨테이너를 종료시키고 다시 실행한다.
그러나 readinessProbe는 컨테이너를 종료하고 다시 실행하지 않는다.

livenessProbe는 검사가 실패한 파드를 아예 종료시키고 다시 실행하기 때문에 , service의 endpoint가 그대로 유지되지만 ,
readinessProbe는 검사가 실패하더라도 컨테이너를 종료하고 다시 실행시키지 않기 때문에 service의 endpoint에서 제거되고 
검사가 성공한 파드만 요청을 처리하게끔 한다.

예를들어 아래와 같은 readinessProbe가 있다고 쳐보자.


```yaml
apiVersion: v1
kind: ReplicationController
metadata:
  name: sample
spec:
  replicas: 3
  selector:
    app: sample
  template:
    metadata:
      labels:
        app: sample
    spec:
      containers:
      - name: sample
        image: jjsair0412/sample
        ports:
        - name: http
          containerPort: 8080
        readinessProbe:
          exec:
            command:
            - ls
            - /var/ready
```

execProbe를 주기적으로 수행하며 , /var/ready 라는 파일이 없는 파드는 status가 running이지만 0/1 상태로 컨테이너가 생성되지 않는다.
- ls명령어를 /var/폴더 안에서 주기적으로 실행 . 
- ready가 없다면 0 반환 , 있다면 1 반환 ( 1은 성공 )
```bash
$ kubectl get pods
NAME                       READY    STATUS    RESTARTS      AGE
sample-29zbx                0/1     Running      0          23m
sample-gwl2s                0/1     Running      0          23m
sample-l2c5p                0/1     Running      0          23m
```

그러나 touch 명령어를 통해 sample-29zbx 파드 내부 /var/ready 파일을 생성하게 되면 ,
```bash
kubectl exec sample-29zbx -- touch /var/ready
```

sample-29zbx 파드만 컨테이너가 생성되고 service의 endpoint로 등록된다.
```bash
$ kubectl get pods
NAME                       READY    STATUS    RESTARTS      AGE
sample-29zbx                1/1     Running      0          23m
sample-gwl2s                0/1     Running      0          23m
sample-l2c5p                0/1     Running      0          23m
```

마치 service의 셀렉터와 파드의 label이 맞지 않을때 생기는 현상과 비슷하다. ( 둘다 service endpoint에서 pod가 생략됨 )

