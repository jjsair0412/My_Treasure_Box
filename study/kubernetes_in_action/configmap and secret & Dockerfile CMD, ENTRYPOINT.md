# configmap and secret & Dockerfile CMD, ENTRYPOINT
configmap과 secret에 대해 기술합니다.

***KEY POINT***

- Dockerfile과 k8s manifest 상응 파드 사양 필드 표

| docker |  kubernetes | Discription |
|--|--|--|
| ENTRYPOINT | command | 컨테이너 안에서 실행되는 실행 파일 |
| CMD | args | 실행 파일에 전달되는 인자값 |


## 1. configmap && secret ?
둘다 동일하게 설정 정보를 저장하는 kubernetes resource이지만 , 
configmap은 보안을 지킬 수 없이 그냥 저장되고 , secret은 보안을 지키면서 저장됩니다.

## 2. what is docker ENTRYPOINT and CMD?
Dockerfile에서 ENTRYPOINT와 CMD , 그리고 RUN의 의미는 다음과 같습니다.
- **RUN** : 는 쉘(shell)에서 커맨드를 실행하는 것처럼 이미지 빌드 과정에서 필요한 커맨드를 실행
- **ENTRYPOINT** : 컨테이너가 시작될 때 호출될 명령어 지정
- **CMD** : ENTRYPOINT에 전달되는 인자를 정의

만약 Dockerfile에 다음과 같이 ENTRYPOINT와 CMD가 정의되어 있을 때 ,
```bash
RUN apt-get -y install vim
ENTRYPOINT ["python"]
CMD ["helloworld.py"]
```

실행 프로세스는 docker run 명령어에 따라 달라집니다.

**2.1 docker run에 인자값이 없을 때**
아래와 같은 docker run 명령어로 인자값 없이 실행한다면

```bash
$ docker run hello-world
```

먼저 vim이 apt-get install로 설치되고

"helloworld.py" 파일이 python ENTRYPOINT로 전달되게 됩니다.

- 결론
따라서 아래와 같은 순서대로 실행됩니다.

```bash
apt-get install vim -y
python helloworld.py
```


**2.2 docker run에 인자값이 있을 때**
아래와 같은 docker run 명령어로 인자값 있이 실행한다면

```bash
$ docker run hello-world helloworld2.py
```

먼저 vim이 apt-get install로 설치되고

"helloworld2.py" 파일이 python ENTRYPOINT로 전달되게 됩니다.

- 결론
따라서 아래와 같은 순서대로 실행됩니다.

```bash
apt-get install vim -y
python helloworld2.py
```

## 3. Dockerfile에서 shell과 exec의 차이점
ENTRYPOINT 명령어나 CMD 명령어를 Dockerfile에서 정의할 때 , 두가지 방식을 지원 합니다.
- **shell 형식** : ENTRYPOINT node app.js
- **exec 형식** : ENTRYPOINT ["node" , "app.js"]

둘의 차이점은 정의된 명령어를 bash shell로 호출하는지 여부에 있습니다..

exec 형식으로 설정한다면 , container 가 실행된 이후 프로세스 목록을 출력해보면 , 1번 process가 node app.js인 것을 확인할 수 있습니다..

```bash
# in Dockerfile
ENTRYPOINT ["node","app.js"]

# command
$ docker exec 1234d ps x
PID TTY STAT TIME COMMAND
 1  ?   Ssl  0:00 node app.js
...
```

만약 shell 형식으로 실행한다면 , 1번 PID가 bash shell인 것을 확인할 수 있습니다.

```bash
# in Dockerfile
ENTRYPOINT node app.js

# command
$ docker exec 1234d ps x
PID TTY STAT TIME COMMAND
 1  ?   Ssl  0:00 /bin/sh -c node app.js
...
```

보이는 것 처럼 node process가 shell process로 실행되기 때문에 , ENTRYPOINT 명령에서는 exec 방식을 사용해야 한다.
- **shell은 필요하지 않기 때문**

## 3. Kubernetes에서의 ENTRYPOINT와 CMD
쿠버네티스에서도 ENTRYPOINT와 CMD를 manifest에서 정의하여 사용할 수 있습니다.

k8s에서는 command와 args 속성을 통해 정의할 수 있습니다.
- **command와 args 필드는 파드 생성 이후에 edit로 업데이트가 불가능합니다.**

아래와 같이 spec.containers 필드에 정의합니다.

```yaml
kind: Pod
spec:
  containers:
  - image: some/image
    command: ["/bin/command"]
    args: ["args1","args2","args3"]
```

아래처럼 나열할 수 있습니다.
- 문자열 값은 따옴표로 감쌀 필요는 없지만 , 숫자는 감싸야 합니다.

```yaml
kind: Pod
spec:
  containers:
  - image: some/image
    command: ["/bin/command"]
    args: 
    - foo
    - bar
    - "15"
```

## 4. Pod manifest에 container 환경변수 전달
만약 아래와 같은 도커파일이 있고 , ENTRYPOINT 에 전달되는 스크립트는 다음과 같다고 생각 해 봅시다.

- **Dockerfile**
```Dockerfile
FROM ubuntu:latest

RUN apt-get update ; apt-get -y install fortune
ADD fortuneloop.sh /bin/fortuneloop.sh

ENTRYPOINT ["/bin/fortuneloop.sh"]
```

- **fortuneloop.sh**
```bash
#!/bin/bash
trap "exit" SIGINT

echo Configured to generate new fortune every $INTERVAL seconds

mkdir -p /var/htdocs

while :
do
  echo $(date) Writing fortune to /var/htdocs/index.html
  /usr/games/fortune > /var/htdocs/index.html
  sleep $INTERVAL
done
```

그리고 이친구들로 아래와 같이 Pod를 생성했을 때 , ENTRYPOINT에 들어가는 ```$INTERVAL``` 환경 변수를 env로 넣어줄 수 있습니다.

- Pod.yaml
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: fortune-env
spec:
  containers:
  - image: jjsair0412/fortune:env
    env: 
    - name: INTERVAL # ENTRYPOINT에 들어가는 스크립트 안에 INTERVAL 환경 변수
      value: "30" # 30 대입
```

env 환경변수에서 다른 값을 ```$(VAR)``` 구문을 이용해 참조할 수 있습니다.
- 아래와 같은 경우 , second_val의 value값은 helloworld가 됩니다.
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: fortune-env
spec:
  containers:
  - image: jjsair0412/fortune:env
    env: 
    - name: first_val
      value: "hello"
    - name: second_val
      value: "$(first_val)world" # helloworld
```

이렇게 환경변수를 하드코딩하여 사용하는것은 효율적이지만 , 반복적으로 넣어주어야 할 때는 재사용의 측면에서 환경 변수를 Pod manifest와 분리시키는것이 효율적입니다.

따라서 configmap resource를 사용합니다.

## 5. What is configmap ?
kubernetes에서는 config option을 configmap이라는 별도 오브젝트로 분리할 수 있고 , 얘는 key-value로 구성된 map 입니다.

짧은 문자열에서부터 전체 설정값까지 모두 가질 수 있습니다.

### 5.1 configmap의 이점
configmap을 사용했을때 이점으로 , 같은 manifest로 실행된 Pod들에게 다른 환경 변수값을 가진 configmap을 각각 부여하면 , 설정값이 바뀌기 때문에 다르게 동작할 수 있다는것이 이점입니다.

따라서 각각 다른 환경에 관해 동일한 이름으로 configmap에 관한 여러 pod manifest를 유지할 수 있습니다.


### 5.2 configmap의 생성
create 명령어로 생성한 뒤 , 생성 결과를 확인해 봅시다.
- **--from-literal 환경변수를 안넣고 configmap을 생성하면 , 아래처럼 문자열을 2개 이상 연속해서 ( key , value 연속 ) 생성할 수 없습니다.**

```bash
$ kubectl create configmap jjs-config --from-literal=sleep-interval=25
configmap/jjs-config created


# --from-literal 없이 생성시 에러 발생
$ kubectl create configmap jjs-config2 sleep-jjs=50
error: exactly one NAME is required, got 2
See 'kubectl create configmap -h' for help and examples


$ kubectl get cm
NAME               DATA   AGE
jjs-config         1      2s


$ kubectl describe cm jjs-config
Name:         jjs-config
Namespace:    default
Labels:       <none>
Annotations:  <none>

Data
====
sleep-interval:
----
25

BinaryData
====

Events:  <none>
```

yaml manifset로 configmap을 생성하려면 다음과 같습니다.

```yaml
$ cat config_map.ymal
apiVersion: v1
kind: ConfigMap
metadata:
  name: fortune-config
data:
  sleep-interval: "25"

# 생성
$ kubectl create -f config_map.yaml
```

### 5.2 configmap 여러 생성 옵션
**1. 파일 내용으로 configmap을 생성할 수 있습니다.**

--from-file 인수를 사용합니다.

아래처럼 생성하면 , 등록한 파일 이름이 key값이 됩니다. 따라서 key값을 지정할 수 도 있습니다.
- key : test.yaml
- value : test.yaml의 내용

```bash
$ kubectl create configmap file_configmap --from-file test.yaml
```

아래처럼 생성하면 test key값 안에 test.yaml 내용이 value 로 등록되게 됩니다.
```bash
$ kubectl create configmap file_configmap --from-file=jjs=test.yaml
```
- key : jjs
- value : test.yaml의 내용

**2. 디렉터리에 있는 모든 파일로 configmap으로 생성.**
각 파일을 개별적으로 만드는 대신에 , 디렉터리 안에 있는 모든 파일을 가져올 수 있습니다.

```bash
$ kubectl create configmap file_configmap --from-file=jjs=/var/lib/test
```

**다양한 option 인자값 결합**
```bash
--from-file=foo.json # 단일 파일
--from-file=/var/lib/config-option/ # 디렉터리 안 모든 파일 (config-option 안의 모든 파일)
--from-literal=some=thing # 문자열
```

## 6. configmap을 Pod에서 참조하는 법
pod manifest에서 env 필드에 valueFrom.configMapKeyRef 필드로 설정합니다.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: jjs-env-from-cm
spec:
  containers:
  - image: jjs/fortune:env
    env:
    - name: INTERVAL # 환경변수 이름
      valueFrom: 
        configMapKeyRef:
          name: jjs-config # 참조할 configmap 이름
          key: sleep-interval # configmap의 해당 key 아래에 저장된 value로 변수 설정
```

### 6.1 존재하지 않는 configmap을 참조햇을 경우
만약 Pod에서 존재하지 않는 cm을 참조했을 경우에 , 기본적으로는 컨테이너를 시작하는데 실패합니다.

그러나 Pod에서 cm을 참조하지 않는 container는 실행을 성공하기 때문에 , 필요한 cm을 생성해주면 모두 running이 됩니다.

만약 존재하지 않아도 실행하게하고 싶다면 , ```configmapKeyRef.optional: true```로 설정하면 , cm이 없어도 container가 실행됩니다.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: jjs-env-from-cm
spec:
  containers:
  - image: jjs/fortune:env
    env:
    - name: INTERVAL
      valueFrom: 
        configMapKeyRef:
          name: jjs-config 
          key: sleep-interval 
          optional: true # jjs-config가 없어도 해당 container 실행됨
```