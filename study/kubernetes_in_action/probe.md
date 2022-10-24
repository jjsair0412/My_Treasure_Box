# Study probes
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

