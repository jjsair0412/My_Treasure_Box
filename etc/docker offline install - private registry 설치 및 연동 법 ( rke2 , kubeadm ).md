



# docker offline install - private registry 설치 및 연동 법 ( rke2 , kubeadm )
- 해당 문서는 docker를 offline 환경에서 설치하는 방법을 설명합니다.
- docker private registry와 docker를 install 합니다.
- 테스트는 rke2 tarball offline환경 | centos 에서 진행하였습니다.
- rpm설치파일을 yumdownloader로 설치할 때에는 , 초기상태의 cnetos에서 진행해야 합니다. 
 의존성이 맺어진 모든 rpm을 설치하기 위해서 입니다. 한번 설치가 된 이후로부터  설치된 요소들은 받아오지 않기 때문에 주의해야 합니다.
## 1. 설치 요소 다운로드 - centos
### 1.1 외부 통신이 가능한 환경에서 진행
- 외부 망이 연결되어있는 환경에서 centos Docker Dependency를 다운로드 합니다.
```
# 기존 Docker를 삭제합니다.
$ sudo yum remove docker \
                  docker-client \
                  docker-client-latest \
                  docker-common \
                  docker-latest \
                  docker-latest-logrotate \
                  docker-logrotate \
                  docker-engine


# yum-utils 설치(Optional)
$ sudo yum install -y yum-utils

# Docker repo를 등록합니다.
$ sudo yum-config-manager \
    --add-repo \
    https://download.docker.com/linux/centos/docker-ce.repo


# Docker Centos RPM 파일 일괄 다운로드 & 압축
$ mkdir ~/docker
$ cd ~/docker
$ sudo rpm -ivh --replacefiles --replacepkgs docker-ce
$ sudo rpm -ivh --replacefiles --replacepkgs docker-ce-cli
$ sudo rpm -ivh --replacefiles --replacepkgs containerd.io
$ sudo rpm -ivh --replacefiles --replacepkgs docker-compose-plugin

# rpm 목록 확인 - 20220620 기준
$ ls -al
total 106608
drwxrwxr-x.  2 centos centos     4096 Jun 20 04:40 .
drwx------. 20 centos centos     4096 Jun 17 07:41 ..
-rw-rw-r--.  1 centos centos 34654484 Jun 20 04:40 containerd.io-1.6.6-3.1.el7.x86_64.rpm
-rw-rw-r--.  1 centos centos    40816 Jun 20 04:40 container-selinux-2.119.2-1.911c772.el7_8.noarch.rpm
-rw-rw-r--.  1 centos centos 23442736 Jun 20 04:40 docker-ce-20.10.17-3.el7.x86_64.rpm
-rw-rw-r--.  1 centos centos 30839424 Jun 20 04:40 docker-ce-cli-20.10.17-3.el7.x86_64.rpm
-rw-rw-r--.  1 centos centos  8640360 Jun 20 04:40 docker-ce-rootless-extras-20.10.17-3.el7.x86_64.rpm
-rw-rw-r--.  1 centos centos  7367636 Jun 20 04:40 docker-compose-plugin-2.6.0-3.el7.x86_64.rpm
-rw-rw-r--.  1 centos centos  3930044 Jun 20 04:40 docker-scan-plugin-0.17.0-3.el7.x86_64.rpm
-rw-rw-r--.  1 centos centos    83764 Jun 20 04:40 fuse3-libs-3.6.1-4.el7.x86_64.rpm
-rw-rw-r--.  1 centos centos    55796 Jun 20 04:40 fuse-overlayfs-0.7.2-6.el7_8.x86_64.rpm
-rw-rw-r--.  1 centos centos    83452 Jun 20 04:40 slirp4netns-0.4.3-4.el7_8.x86_64.rpm


$ tar cvzf ~/docker.tar.gz *
```
### 1.2 offline 환경에서 진행
- Docker를 설치할 환경 (Offline) 에서 진행합니다.
```
# Docker RPM 설치
$ mkdir docker
$ tar xvf docker.tar.gz -C ~/docker
$ cd docker
# 받아온 전체 rpm들을 일괄 설치 합니다.
$ sudo rpm -ivh --replacefiles --replacepkgs *.rpm

# 일반 User 등록
$ sudo usermod -aG docker $USER

# Docker Service 등록 & 시작
$ sudo systemctl enable docker.service
$ sudo systemctl start docker.service

# Docker 실행상태 확인
$ systemctl status docker
● docker.service - Docker Application Container Engine
   Loaded: loaded (/usr/lib/systemd/system/docker.service; enabled; vendor preset: disabled)
   Active: active (running) since Mon 2022-06-20 04:44:26 UTC; 7min ago
     Docs: https://docs.docker.com
 Main PID: 3499 (dockerd)
    Tasks: 8
   Memory: 28.4M
   CGroup: /system.slice/docker.service
           └─3499 /usr/bin/dockerd -H fd:// --containerd=/run/containerd/containerd.sock

Jun 20 04:44:24 ip-10-250-205-213.ap-northeast-1.compute.internal dockerd[3499]: time="2022-06-20T04:44:24.198811583Z" level=info msg="ccResolverWrapper: sending update to cc: {[{unix:///run/contain...odule=grpc
Jun 20 04:44:24 ip-10-250-205-213.ap-northeast-1.compute.internal dockerd[3499]: time="2022-06-20T04:44:24.198823733Z" level=info msg="ClientConn switching balancer to \"pick_first\"" module=grpc
Jun 20 04:44:26 ip-10-250-205-213.ap-northeast-1.compute.internal dockerd[3499]: time="2022-06-20T04:44:26.426944891Z" level=info msg="[graphdriver] using prior storage driver: overlay2"
Jun 20 04:44:26 ip-10-250-205-213.ap-northeast-1.compute.internal dockerd[3499]: time="2022-06-20T04:44:26.436705339Z" level=info msg="Loading containers: start."
Jun 20 04:44:26 ip-10-250-205-213.ap-northeast-1.compute.internal dockerd[3499]: time="2022-06-20T04:44:26.581773817Z" level=info msg="Default bridge (docker0) is assigned with an IP address 172.17....P address"
Jun 20 04:44:26 ip-10-250-205-213.ap-northeast-1.compute.internal dockerd[3499]: time="2022-06-20T04:44:26.645906026Z" level=info msg="Loading containers: done."
Jun 20 04:44:26 ip-10-250-205-213.ap-northeast-1.compute.internal dockerd[3499]: time="2022-06-20T04:44:26.663633019Z" level=info msg="Docker daemon" commit=a89b842 graphdriver(s)=overlay2 version=20.10.17
Jun 20 04:44:26 ip-10-250-205-213.ap-northeast-1.compute.internal dockerd[3499]: time="2022-06-20T04:44:26.663776226Z" level=info msg="Daemon has completed initialization"
Jun 20 04:44:26 ip-10-250-205-213.ap-northeast-1.compute.internal systemd[1]: Started Docker Application Container Engine.
Jun 20 04:44:26 ip-10-250-205-213.ap-northeast-1.compute.internal dockerd[3499]: time="2022-06-20T04:44:26.692986063Z" level=info msg="API listen on /var/run/docker.sock"
Hint: Some lines were ellipsized, use -l to show in ful

```

## 2. Image Registry 실행
### 2.1. 외부 통신이 되는 환경에서 작업
- 외부 통신이 되는 VM에서 Registry Image를 압축 파일로 다운로드
```
$ docker pull registry
$ docker images
$ docker save -o registry.tgz registry:latest
```

- registry.tgz 파일을 Offline Registry 설치 대상 VM으로 SCP로 이동 시킵니다.
### 2.2 Offline Private Registry 실행 환경에서 작업
- 받아온 tar파일을 load 명령어를 통해 다시 docker image로 복구합니다.
```
$ docker load -i registry.tgz
$ docker images
REPOSITORY   TAG       IMAGE ID       CREATED       SIZE
registry     latest    773dbf02e42e   3 weeks ago   24.1MB
```
docker를 통해 registry 실행
### 2.3  Registry 실행
- REGISTRY_STORAGE_DELETE_ENABLED=true 옵션으로 registry에 올라가있는 이미지를 삭제할 수 있게끔 설정합니다. 
- --restart 옵션을 통해 항상 priavte registry가 켜져잇게끔 설정합니다.
```
$ docker run -dit --name docker-registry -e  REGISTRY_STORAGE_DELETE_ENABLED=true --restart=always -p 5000:5000 -v /root/data:/var/lib/registry/docker/registry/v2 registry 
```
### 2.4 private registry image 제거 방법
- 해당 docker container에 exec하여 제거해 줍니다.
1. private repository 조회
```
$ curl 10.xxx.xxx.xxx:5000/v2/_catalog
```
2. 해당 레지스트리로 exec 접속
```
$ docker exec -it docker-registry sh 
```
3. 레파지토리 삭제
```
$ cd /var/lib/registry/docker/registry/v2/repositories

# 폴더내부 image 정보 제거
$ rm -rf *
```
4. 가비지컬렉터 제거
```
$ docker exec -it docker-registry  bin/registry garbage-collect  /etc/docker/registry/config.yml
```
5. registry 재시작
```
$ docker stop docker-registry
$ docker start docker-registry
```

### 2.4 정상 동작상태 확인
```
$ curl localhost:5000/v2/_catalog
{"repositories":[]}

$ sudo docker ps
CONTAINER ID   IMAGE      COMMAND                  CREATED          STATUS          PORTS                                       NAMES
f4c14ed76654   registry   "/entrypoint.sh /etc…"   12 minutes ago   Up 12 minutes   0.0.0.0:5000->5000/tcp, :::5000->5000/tcp   docker-registry
```
### 2.5 private registry rest api 종류
#### 2.5.1 올라간 image 확인
```
$ curl -X GET <Repository URL>/v2/_catalog>

# usecase
$ curl 10.xxx.xxx.xxx:5000/v2/_catalog
```
#### 2.5.2 tag 확인
```
$ curl -X GET <Repository URL/v2/<repository 이름>/tags/list

# usecase
$ curl -X GET http://10.xxx.xxx.xxx:5000/v2/registry.k8s.io/ingress-nginx/controller/tags/list
```
#### 2.5.3 diget(hash) 확인
- 해당 명령은 private registry 컨테이너가 작동중인 노드에서 curl명령을 수행해야 합니다.
```
$ curl -v --silent -H "Accept: application/vnd.docker.distribution.manifest.v2+json" -X GET <Repository URL>/v2/<Repository 이름>/manifests/<Tag> 2>&1 | grep Docker-Content-Digest | awk '{print ($3)}'

# usecase
$ curl -v --silent -H "Accept: application/vnd.docker.distribution.manifest.v2+json" -X GET http://10.xxx.xxx.xxx:5000/v2/registry.k8s.io/ingress-nginx/controller/manifests/v1.2.1 2>&1 | grep Docker-Content-Digest | awk '{print ($3)}'
```
## 3. priavte Image Registry Pull & push 방법
- 아래 설명해놓은 방법은 push하는 과정의 테스트입니다.
  실제 사용할 때에는 따로 정리해놓은 스크립트파일을 사용하면 됩니다.
- [shell script](https://github.com/jjsair0412/kubernetes_info/blob/main/rancher%20%26%20rke%20%26%20rke2/install%20rancher/private%20registry%20push%20shell%20script.md)
### 3.1 Online 환경에서 작업
- Online 연결이 되어있는 환경에서 sample image를 받아온 후 , save 명령어를 통해 tar파일로 생성합니다.
  그 후 , 생성한 tar파일을 scp명령어로 offline 환경으로 옮깁니다.
```
$ docker pull nginx
$ docker save -o nginx.tar nginx:latest 
```
### 3.2 Offline 환경에서 작업
- 받아온 sample을 load명령어로 다시 이미지화 시킵니다.
```
$ docker load -i nginx.tar
# 확인
$ docker images
```
-   sample 이미지의 태그를 추가 해줍니다.

```
# 이미 존재하는 이미지일 경우
docker image tag nginx:latest 192.168.xx.xx:5000/test:1.0

#새로 빌드하는 경우
docker build --tag 192.168.xx.xx:5000/test:1.0 nginx
```
- docker push
```
$ docker push 10.xxx.xxx.xxx:5000/test:1.0
```
- docker pull
```
$ docker pull 10.xxx.xxx.xxx:5000/test:1.0
```
## Troubleshooting
### 1.  response from daemon: Get "https://xxx.xxx.xxx.xxx:5000/v2/": http: server gave HTTP response to HTTPS client 에러 발생 시
#### 1.1 runtime이 docker일 경우 - 일반 k8s ( kubeadm )
- 만약 runtime을 docker로 사용하고 있다면 , daemon.json을 추가하여 insecure-registries 를 설정 합니다.
```
$ cat /etc/docker/daemon.json
{
"insecure-registries": ["xxx.xxx.xxx.xxx:5000"]
}

# docker 재시작
$ systemctl restart docker
```
#### 1.2 runtime이 containerd일 경우 - rke2 포함
- 만약 container runtime을 containerd로 사용하고 있을 경우, 1.3으로 가서 mirror 파일을 수정하거나 1.2의 내용대로 직접 containerd 설정값을 변경시켜 줍니다.
- 둘중 하나면 변경시켜도 , 서로 동기화되기때문에 무관합니다.
```
# containerd 설정파일 위치로 이동 
$ cd /etc/containerd
# 기존 config파일 이름 변경
$ mv config.toml config.toml.old

# containerd 설정파일의 default를 아래 명령어를 통해 가지고옵니다.
$ containerd config default > /etc/containerd/config.toml
```
1.1.2 config파일 세팅 값 변경

- 아래처럼 mirror 값을 변경합니다. private registry의 서버 주소로 변경하고 config파일을 추가해 줍니다.
- priavet registry의 auth가 필요하다면 , config파일에 값을 추가해 줍니다. 관련 포스팅 참고
[관련 포스팅](https://ikcoo.tistory.com/230)
[관련 포스팅](https://mrzik.medium.com/how-to-configure-private-registry-for-kubernetes-cluster-running-with-containerd-cf74697fa382)
[containerd github](https://github.com/containerd/containerd)
```
...
      [plugins."io.containerd.grpc.v1.cri".registry.auths]
        auth = "cmVnYWRtaW46QzBtcG4zdCE="  
      [plugins."io.containerd.grpc.v1.cri".registry.configs]

      [plugins."io.containerd.grpc.v1.cri".registry.headers]

      [plugins."io.containerd.grpc.v1.cri".registry.mirrors]
        [plugins."io.containerd.grpc.v1.cri".registry.mirrors."docker.io"]
          endpoint = ["https://registry-1.docker.io"]
        [plugin."io.containerd.grpc.v1.cri".registry.mirrors."10.xxx.xxx.xxx"]
          endpoint = ["http://10.xxx.xxx.xxx:5000"]
...
```

- [plugins."io.containerd.grpc.v1.cri".registry.auths]  내부의 auth 값은 , 아래의 경로에서 찾을 수 있습니다.
```
$ sudo cat /root/.docker/config.json
```
1.1.3 secret 생성
- 아래의 공식문서 방법을 참조하여 secret을 생성하고 pod 및 deploy정보에 secret값을 넣어줍니다.
  secret 생성방법은 두가지가 있는데 , 둘중 하나만 따라하면 됩니다.
https://kubernetes.io/ko/docs/tasks/configure-pod-container/pull-image-private-registry/#registry-secret-existing-credentials
#### 1.2 runtime이 containerd일 경우 - rke2 포함
[공식 참조 문서](https://docs.rke2.io/install/containerd_registry_configuration/)
- rke2인 경우 , rke2가 start하면서 /etc/rancher/rke2 폴더 안에 registries.yaml 파일이 존재하는지 확인합니다.
 존재하지 않는다면 기본 세팅값이 적용 ( 일반 도커 허브에서 pull ) 되어지고 , 존재한다면 해당 값으로 rke2의 containerd 세팅을 변경하게 됩니다.
 - 파드가 올라가는 worker 노드에 registries.yaml파일을 생성시켜 줍니다. 
```
$ cat registries.yaml
mirrors:
  docker.io:
    endpoint:
      - "http://10.xxx.xxx.xxx:5000"
  10.xxx.xxx.xxx:5000:
    endpoint:
      - "http://10.xxx.xxx.xxx:5000"

```
- 엔드포인트는 여러개가 위치할 수 있으며 , 위의 yaml과 같이 설정해준다면 , 10.xxx.xxx.xxx:5000 주소의 private registry에서 아래의 태그정보를가지고 이미지를 pull 하게 됩니다.
```
10.xxx.xxx.xxx/busybox:latest
```
- 엔드포인트가 여러개 위치한다면 , 최상위 엔드포인트부터 pull을 시도하며 , 처음 성공했던 엔드포인트에서 이미지를 pull하게 됩니다.
- 만약 dns가 존재하는 harbor가 private registry로 사용되어지고 잇다면 , 아래와 같이 설정할 수 있을 것 입니다.
- 여러 옵션들은 [공식 참조 문서](https://docs.rke2.io/install/containerd_registry_configuration/) 여기서 찾아볼 수 있습니다.
```
# harbor dns addr
jinseong.harbor.com

$ cat registries.yaml
mirrors:
  docker.io:
    endpoint:
      - "http://10.xxx.xxx.xxx:5000"
      - "https://jinseong.harbor.com"
  10.xxx.xxx.xxx:5000:
    endpoint:
      - "http://10.xxx.xxx.xxx:5000"
configs:
  "jinseong.harbor.com":
    tls:
      insecure_skip_verify: true # ssl 인증 무시하는 config 옵션

jinseong.harbor.tag/busybox:latest
```