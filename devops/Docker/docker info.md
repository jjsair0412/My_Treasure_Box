# Docker info
- 해당 문서는 docker의 기본적인 설치 , 사용방안을 설명하는 문서입니다.

## install docker

```bash
# 설치
$ sudo apt-get update
$ apt-install docker.io

# 설치 확인
$ docker ps
```

[Docker Hub Container Image Library | App Containerization](https://hub.docker.com/)

---

## Docker 레지스트리

도커 레지스트리에는 사용자가 사용할 수 있도록 데이터베이스를 통해 Image를 제공해주고 있음
누구나 이미지를 만들어 푸시할 수 있으며 푸시된 이미지는 다른 사람들에게 공유 가능

---

### Docker 명령어로 이미지 검색 및 다운

```bash
# tomcat 검색
$ sudo docker search tomcat

# tomcat download
$ sudo docker pull tomcat

# check - 다운받은 이미지들 확인
$ sudo docker image
```

---

## Docker public 레지스트리 검색

[Docker Hub Container Image Library | App Containerization](https://hub.docker.com/)

---

## Docker lifecycle

![docker_lifecycle.PNG][docker_lifecycle.PNG]

[docker_lifecycle.PNG]:/.images/docker_lifecycle.PNG

1. **PULL**

레지스트리에서 image 다운로드

1. **Run / create**

Run 명령어는 컨테이너를 create , start 둘다 해준다.

그렇기때문에 이미 생성되어있는 컨테이너를 실행 할 때는 start 명령을 사용하자.

container를 create 해야할 필요가 있을 경우에만 Run 명령어를 실행하자.

1. **STOP**

container 정지

1. **RM**

container 삭제

1. **commit**

container를 다시 image로 생성

1. **RMI**

image 삭제

1. **push**

image를 레지스트리에 업로드

---

## **Docker commands**

[Docker basic commands](https://www.notion.so/Docker-basic-commands-2894117319a740f6a6c71e93a5ffdf0c)

---

## Docker container 생성할 때 환경변수 사용하는법

예제코드는 nginx와 mysql에 대한 예제코드이다.

환경변수 관련해서는 공식문서를 꼭 확인하자. 

### 공식문서

[Mysql - Official Image | Docker Hub](https://hub.docker.com/_/mysql)

### nginx 예시

```bash
$ docker run -d --name nx -e env_name=test1234 nginx
/# printenv env_name
```

### MySQL 서비스 구축하기

```bash
$ docker run --name some-mysql -e MYSQL_ROOT_PASSWORD='!qhdkscjfwj@' -d mysql
$ docker exec -it some-mysql mysql
password: !qhdkscjfwj@
mysql>
```

---

## 볼륨 마운트 옵션으로 로컬파일과 컨테이너 내부위치 공유하기

- 명령어 형식

```bash
docker run -v <호스트 경로>:<컨테이너 내 경로>:<권한> # /tmp:home/user:ro
```

- 권한의 종류
    - ro: 읽기 전용
    - rw: 읽기 및 쓰기

nginx로 볼륨마운트하기 

```bash
sudo docker run -d -p 80:80 --rm -v /var/www:/usr/share/nginx/html:ro nginx
curl 127.0.0.1
echo 1234 > /var/www/index.html
curl 127.0.0.1
```

이렇게 마운트시키면 , 로컬에 호스트경로 파일이 변경되면 컨테이너내 경로의 파일도 변경된다.

반대의 경우도 동일하게 작동한다.

---

## Docker image build and push

1. **Dockerfile 생성**

도커파일은 이름이 꼭 dockerfile 이어야 한다.

[Docker기반 환경 구축 - (3) Dockerfile](https://hoonpro.tistory.com/12)

[정리해 두는 곳 : 네이버 블로그](https://blog.naver.com/jjsair0412/222616583274)

1. **build**

```bash
$ sudo docker build -t <docker_ .
$ sudo docker images
```

1. **docker login**

```bash
$ sudo docker login
```

1. **docker push**

```bash
$ sudo docker push <image_name>
```

---

## Private 레지스트리 구현 및 사용 방법

- 도커 허브와같은 public 환경에 image를 올리는건 , 보안에 취약할 수 있다.
- 그래서 , private 상태의 레지스트리를 선택해서 사용하기도 한다.

### 만드는 방법

1. **private registry 만들기**

docker image를 사용하면 된다.

image 이름은 registry 이다.

```bash
# private registry는 5000번 포트를 사용한다.
$ docker run -d --name docker-registry -p 5000:5000 registry
```

1. **private registry에 이미지 푸쉬하기**

푸쉬할 이미지의 tag를 변경시켜주어야 한다.

docker hub에 개인이 만든 image는 , image 이름 앞에 id가 붙는다.

- jjsair0412/myimage

위처럼 생기게 되는데 , 이거처럼 private registry에 들어갈 image도 앞에 

private registry ip:port 가 들어가야 한다.

- 127.0.0.1:5000/myimage

tag를 저렇게 변경시켜준다.

그리고 push한다. 

push할 때에도 private registry 정보가 필요하다.

```bash
$ docker push <private_registry_ip_addr>:<private_registry_port>/<image_name>

# 사용 예
$ docker push 127.0.0.1:5000/image_name
```

[도커 API 관련 링크](https://docs.docker.com/registry/spec/api)

[인증 관련 참고 링크](https://docs.docker.com/registry/configuration/#auth)