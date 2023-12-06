# wireguard VPN 구축방안
클라우드 외부 Client에서, Private Subnet에 위치한 Target으로 ssh 및 http, https 등의 연결을하기 위해 wireguard를 docker compose로 구축합니다.

## 1. docker compose 설치
docker compose로 구축할것이기에, compose를 설치해야합니다.

```bash
$ docker compose version
Docker Compose version v2.17.3
```

## 2. security Group allow
wireguard를 구축할 Bastion Host는 Public subent에 위치해야 하며, **51820 번 포트가 security group에 열려있어야 합니다.**
- 51820번 포트를 사용하여 client가 wireguard로 연결하기 떄문, 
- docker compose environment에서 SERVERPORT 를 변경하면 변경된 포트를 열어주어야만 합니다.

wireguard VPN을 통해 ssh 연결할 private subnet은, public subnet과 22번 포트로 연결이 가능해야 합니다.

## 3. wireguard deploy setting
wireguard 설치대상 폴더 생성

```bash
$ sudo mkdir /opt/wireguard-server

# 구축대상 폴더 소유자 권한 변경으로 설정. prod 환경에선 user 생성후 비밀번호 세팅하여 해당 user만 접근할 수 있게끔 구성,
$ sudo chown ubuntu:ubuntu /opt/wireguard-server/
```

docker compose 파일 세팅
```bash
$ vi /opt/wireguard-server/docker-compose.yml
version: "2.1"
services:
  wireguard:                              
    image: ghcr.io/linuxserver/wireguard  # dockerhub의 image주소
    container_name: wireguard             # container의 이름
    cap_add:
      - NET_ADMIN
      - SYS_MODULE
    environment:
      - PUID=1000                         # ubuntu user의 uid, gid
      - PGID=1000
      - TZ=Asia/Seoul # wireguard Timezone 설정
      - SERVERURL=3.x.x.x  # wireguard 구축 server의 public ip or domain
      - SERVERPORT=51820         # default port = 51820 , 변경 가능. 변경하면 ports에 포트포워딩설정을 바꿔주어야 함.(wireguard 컨테이너쪽은 51820 고정)
      - PEERS=3                  # 접속가능한 peer의 숫자, 3개
      - PEERDNS=auto #optional
      - INTERNAL_SUBNET=10.x.x.0/24  # wireguard로 접근할 private subnet의 CIDR 입력.
      - ALLOWEDIPS=0.0.0.0/0 # VPN을 통할 target 설정, 10.x.x.0/24 로 설정하면 해당 CIDR 대역에 접근할 때만 VPN을 통함, 0.0.0.0/0 으로 세팅시 모든 트래픽이 VPN 서버를 통함
    volumes:
      - /opt/wireguard-server/config:/config  # 컨픽디렉터리
      - /lib/modules:/lib/modules             # 모듈디렉터리
    ports:
      - 51820:51820/udp # 포트포워딩설정, SERVERPORT env를 변경했다면 {변경된포트}:51820 으로 세팅,
    sysctls:
      - net.ipv4.conf.all.src_valid_mark=1 # IPv4 트래픽 포워딩설정
    restart: always
```

wireguard deploy
```bash
$ docker compose up

## 로그없이 백그라운드 배포
$ docker compose up -d
...
```

배포가 완료되면, peer들이 들어갈 수 있는 QR코드가 로그에 출력됩니다.


## 4. client Setting
wireguard가 배포된 EC2를 확인해보면, 아래 디렉터리에 peer setting값들이 모여있습니다.

```bash
$ cd /opt/wireguard-server/config

$ ls
coredns  peer1  peer2  peer3  server  templates  wg_confs
```

예를들어 peer1 번 디렉터리에 들어가면, key와 conf파일들을 확인할 수 있습니다.
```bash
$ cd peer1

$ ls
peer1.conf  peer1.png  presharedkey-peer1  privatekey-peer1  publickey-peer1

$ cat peer1.conf 
[Interface]
Address = 10.x.x.2
PrivateKey = {{hidden}}
ListenPort = 51820
DNS = 10.x.x.1

[Peer]
PublicKey = {hidden}
PresharedKey = (hidden)
Endpoint = 3.x.x.x:51820
AllowedIPs = 0.0.0.0/0
```

해당 conf파일을 private subnet에 접근하고싶은 client에서, wireguard를 설치하고 conf로 복사붙여넣기 해주면 됩니다.

3개의 peer에서 2개의 client가 연결되었을 경우, 아래와같이 wireguard container에 exec하여 확인할 수 있습니다.
```bash
$ docker exec -ti wireguard wg
...
key 등 연결정보 출력(ip 포함)
...
```

## 5. peer 변동
peer 개수를 늘리거나 줄이는 등의 작업을 docker-compose.yml 파일을 수정함으로써 진행한 다음, 아래 명령어로 재 배포하면 반영됩니다.
```bash
$ docker-compose up -d --force-recreate
```