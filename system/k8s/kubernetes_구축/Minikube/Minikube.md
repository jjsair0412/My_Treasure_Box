
# MiniKube info

  

minikube는 가상환경을 사용해 쿠버네티스 클러스터를 가상으로 구현한다.

  

쿠버네티스는 무겁지만 , minikube는 가볍다.

  

가벼운대신 image 용량이 크거나 무거운 것을 배포하기에는 적합하지 않다.

  

미니큐브는 리눅스 , mac , 윈도우 모두 제공하는데 , 일반적으론 가상환경을 제공하는 애플리케이션을 사용하서 구축한다.

  

[MiniKube 설치 방법 - docs ](https://minikube.sigs.k8s.io/docs/start/)

  

## 설치 방법

  

- 아래 설치방법은 우분투 환경에서 설치하는 방법이다.

  

1.  **도커 설치**

  

```bash
$ sudo apt install docker.io -y
```

  

설치가 완료된 이후 , chmod로 일반 사용자도 도커를 사용할 수 있도록 권한을 부여한다.
그러한 이유는 , minikube에서 도커를 사용하는데 root로 도커를 사용하지 않기 때문이다.

  

```bash
$ sudo usermod -aG docker $USER && newgrp docker

# 잘 설치되었는지 확인한다.
$ docker ps -a
```

  

2.  **minikube 다운로드 및 설치**
- 우분투 환경에서 minikube를 설치하는 명령이다.

  

```bash
$ curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube_latest_amd64.deb

$ sudo dpkg -i minikube_latest_amd64.deb
```

  

3.  **minikube 실행**
```bash
$ minikube start --driver=docker\
```
4.  **minikube 접속**
- minikube도 kubernetes처럼 kubectl 명령어를 사용한다.
kubectl을 설치해주자
```bash
$ sudo snap install kubectl --classic ( 에러발생한 경우에만 classic 옵션 붙임 )

# kubectl 명령어로 노드를 조회한다.
$ kubectl get nodes
NAME STATUS ROLES AGE VERSION
minikube Ready control-plane,master 18m v1.23.3
```

  

## MiniKube 진입
- ssh 명령으로 minikube 내부로 진입할 수 있다.
```bash
$ minikube ssh
```

  

## MiniKube 아키텍쳐 구성도

  

![mini][mini]

  

[mini]:./images/mini.PNG

  

- vm 내부에 쿠버네티스 노두가 두개 있는것을 볼 수 있다.