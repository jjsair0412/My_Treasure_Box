# ceph 설치 방안 - cephadm을 통한 삼중화 클러스터링 구성
## 0. precondition
해당 문서는 ceph를 베어메탈 ubuntu 환경에서 cephadm으로 설치하는 방안에 대해 기술합니다.

cephadm을 설치하는 방법은 apt-get과 curl . 그리고 ansible-cephamd 세 가지로 나뉩니다.
해당 문서는 cephadm을 curl로 받아와서 설치하는 과정에 대해 기술합니다. 
- 해당 방안이 가장 추천되는 방안 입니다. 
  docker container로 ceph cluster를 생성하기에 , ansible보다 빠르고 간편합니다.

***둘중에 하나만 선택해서 진행해야 합니다 .!***

해당 문서는 curl방식으로 진행합니다.

**참고 문서**
- https://docs.ceph.com/en/latest/cephadm/install/
- https://somuch.medium.com/sds-cephadm%EB%A1%9C-ceph-%EA%B5%AC%EC%84%B1%ED%95%98%EA%B8%B0-778a90ba6cc7

## 1. 설치 환경

| ip | os | spec | volume size | docker version |
|--|--|--|--|--|
| 10.0.0.2 | ubuntu 20.04 | 2core 4GB | 20GB |  20.10.12  |
| 10.0.0.3 | ubuntu 20.04 | 2core 4GB | 20GB |  20.10.12  |
| 10.0.0.4 | ubuntu 20.04 | 2core 4GB | 20GB |  20.10.12  |

ceph 설치대상 version 
- quincy

## 2. 설치 전 환경설정
ubuntu인경우 pyton3를 설치합니다.

이미 구성되어있는 경우 설치 안해도 에러가 발생하지 않습니다.
```bash
$ sudo apt-get update
$ sudo apt-get install -y python3

# option . test시 아래 명령어 수행 안해도 설치 정상 작동
$ sudo update-ca-certificates --fresh
$ export SSL_CERT_DIR=/etc/ssl/certs
```

각 노드에 user별 password를 기억하여 저장해 두거나 재 설정합니다.
차후 만들어진 ceph.pub키를 통해 호스트들이 연결되게 되는데 , ssh-copy-id 명령어로 pub키를 등록하여 인증 없이 연결합니다.
따라서 아래 명령어로 user 별 password를 설정해 둡니다.
```
# 사용 예
$ sudo passwd {username}

# 실 사용된 명령어
$ sudo passwd ubuntu
```

/etc/hosts파일에 각 ceph host별 명칭을 ip와 매칭하여 등록해 둡니다.
만약 DNS 서버가 존재하여 ip 리졸빙이 되는 상태라면 , 해당 설정은 필요하지 않습니다.

아래처럼 등록해 두었습니다.
```
$ cat /etc/hosts
10.0.0.2 jjs
10.0.0.3 jjs2
10.0.0.2 jjs3
```

**모든 노드에 도커나 파드맨 (container tool ) 이 설치되어있어야 합니다**

## 3. cephadm 설치
첫번째 mon이 될 node에서 설치를 진행합니다.
- 10.0.0.2 노드에서 진행

git 주소에서 curl 명령어로 받아옵니다..

아래 주소가 원본 git 주소입니다.
- https://github.com/ceph/ceph/


```bash
$ curl --silent --remote-name --location https://github.com/ceph/ceph/raw/quincy/src/cephadm/cephadm
```

해당 문서에는 현 시점 (22.12.19)으로 docs에 나와있는 quincy version을 통해 설치를 진행합니다.
다른 버전을 설치하고싶다면 , release 뒤에 명시해두면 됩니다.

+x 권한을 부여하여 실행 권한을 부여하고 , quincy 레포를 추가한 후 설치를 진행합니다.
```
$ sudo chmod +x cephadm
$ sudo ./cephadm add-repo --release quincy
$ sudo ./cephadm install
```

which 명령어를 수행하여 cephadm이 ```/usr/sbin/cephadm``` 경로에 위치하는지 확인합니다.
```
which cephadm
/usr/sbin/cephadm
```

## 4. Deploy Mon
ceph cluster 노드 중 mon으로 사용할 노드에서 다음 명령어를 통해 bootstrap을 진행합니다.

필요 포트는 다음과 같습니다
- https://access.redhat.com/documentation/ko-kr/red_hat_ceph_storage/5/html/dashboard_guide/ceph-dashboard-installation-and-access#network-port-requirements-for-ceph-dashboard_dash

```
# 사용 예
$ cephadm bootstrap --mon-ip [mon ip]

# 실 사용한 명령어
$ cephadm bootstrap --mon-ip 10.0.0.2 --allow-fqdn-hostname --ssh-user ubuntu
```

만약 ssh key에대한 인증 에러가발생하면 , root user로 ssh연결이 실패하여 에러가 발생하는것이기에 ```--ssh-user```로 현재 ubuntu user명을 명시합니다.
```--allow-fqdn-hostname``` 으로 hostname을 허용합니다.

mon이 구성되면 ui로 접근할 수 있는 주소와 admin 계정이 출력되게 됩니다.
복사하여 기억해 둡니다.

```
Ceph Dashboard is now available at:

             URL: https://jjs:8443/
            User: admin
        Password: ~~~
		...
```

ceph tool이 설치되어있는 컨테이너로 exec하는 명령어 또한 제공합니다.
```
sudo /usr/sbin/cephadm shell --fsid b52b6b2b-7f66-11ed-844c-110c036c1b70 -c /etc/ceph/ceph.conf -k /etc/ceph/ceph.client.admin.keyring
```

dashboard로 접근이 되는지 확인합니다.
```
OSD count 1 < osd_pool_default_size 3
```

위와 같은 에러가 발생하는것이 정상입니다.
ceph에서 OSD라는 단위로 저장공간인 pool을 관리합니다.
OSD는 ceph cluster host개수 입니다.
따라서 osd의 갯수는 mon의 개수와 동일합니다.

현재는 mon ( host ) 가 1개밖에 없기에 ( 저장 공간 (volume)) pool의 default size인 3을 충족하지 못해서 에러가 발생합니다.
호스트를 추가하면서 ceph -s로 mon이 추가되고 osd갯수가 추가되는것을 확인합니다.

## 4.1 cephadm shell option
아래 명령어를 입력하여 cephadm shell을 계속치지 않아도 기존 user에서 ceph command를 통해 작업할 수 있도록 설정합니다.
- option . cephadm shell로 접속해서 수행해도 무관합니다.
```
alias ceph='sudo cephadm shell -- ceph'
```

## 5. Add Host
ceph pub key를 각 호스트에 cp합니다.

cephadm shell에 접속하여 ceph 명령어로 수행합니다.
```
$ ./cephadm shell
```

### 5.1 key cp
```
# ceph.pub key 꺼내오기
$ ceph cephadm get-pub-key > ceph.pub

# 해당 경로로 이동
# ls
ceph.pub
```

꺼내온 pub key를 ssh명령어를 통해 ceph cluster가 될 노드에 이동합니다.
처음 etc/host에 jjs2의 ip를 등록해 두었고 , jjs2 node의 ubuntu 계정 password를 설정해 두었기 때문에 에러가 발생하지 않아야 합니다.
permission deny등의 에러가 발생하면 , 두 설정을 다시 체크합니다.
- 만약 cephadm shell 내부에서 진행중이라면 , etc/hosts파일을 다시 체크해봅니다. ( ceph 컨테이너 내부라 재 설정 필요 )
```
# 사용 예
ssh-copy-id -f -i ~/ceph.pub ubuntu@jjs2
ssh-copy-id -f -i ~/ceph.pub ubuntu@jjs3
```

그냥 원격지 서버의 ```.ssh/authorized_keys``` 에 해당 ceph.pub키를 복사 붙여넣기해도 상관없습니다.

### 5.2 orch host 추가
아래 명령어를 통해 host들을 추가합니다.

```
ceph orch host add jjs2
ceph orch host add jjs3
```

추가된 host 결과를 확인합니다.
```
ceph orch device ls
HOST        PATH      TYPE  DEVICE ID              SIZE  AVAILABLE  REFRESHED  REJECT REASONS                                                 
jjs         /dev/vdb  hdd   47a0b605-a64a-4944-9  21.4G             37s ago    Insufficient space (<10 extents) on vgs, LVM detected, locked  
jjs2        /dev/vdb  hdd   19634bfa-4615-4d75-9  21.4G             38s ago    Insufficient space (<10 extents) on vgs, LVM detected, locked  
jjs3        /dev/vdb  hdd   83af627b-5574-4c9b-9  21.4G  Yes        35s ago
```

## 6. 설치 결과 확인
orch 개수가 3개로 충족되었기 때문에 HEALTH_OK가 출력되야 합니다.

또한 20GB 볼륨 3개가 orch이기 때문에 , 60GIB가 마운트 되어야 합니다.
```
$ ceph -s
quay.io/ceph/ceph@sha256:0560b16bec6e84345f29fb6693cd2430884e6efff16a95d5bdd0bb06d7661c45
  cluster:
    id:     b52b6b2b-7f66-11ed-844c-110c036c1b70
    health: HEALTH_OK
 
  services:
    mon: 3 daemons, quorum jjs,jjs2,jjs3 (age 76s)
    mgr: jjs.ywagcn(active, since 80m), standbys: jjs2.oipoqb
    osd: 3 osds: 3 up (since 50s), 3 in (since 70s)
 
  data:
    pools:   1 pools, 1 pgs
    objects: 2 objects, 449 KiB
    usage:   62 MiB used, 60 GiB / 60 GiB avail
    pgs:     1 active+clean
```

ceph host 추가된 상태 확인합니다.
```
$ ceph orch device ls
HOST        PATH      TYPE  DEVICE ID              SIZE  AVAILABLE  REFRESHED  REJECT REASONS                                                 
jjs         /dev/vdb  hdd   47a0b605-a64a-4944-9  21.4G             16m ago    Insufficient space (<10 extents) on vgs, LVM detected, locked  
jjs2        /dev/vdb  hdd   19634bfa-4615-4d75-9  21.4G             16m ago    Insufficient space (<10 extents) on vgs, LVM detected, locked  
jjs3        /dev/vdb  hdd   83af627b-5574-4c9b-9  21.4G             2m ago     Insufficient space (<10 extents) on vgs, LVM detected, locked  
```

## ETC. cluster 제거
만약 설치중 cluster에 문제가 생겻을 경우 , 아래 방법을 토대로 클러스터를 제거한 후 다시 시도합니다.

ceph shell에 접속합니다.

```
$ ./cephadm shell
```

cephadm을 비활성화하여 모든 오케스트레이션 작업을 중지합니다. (새 데몬 배포 방지) 

```
# ceph mgr module disable cephadm
```

해당 호스트의 클러스터 FSID를 확인 후 결과를 저장해 둡니다.

```
# ceph fsid
```

아래 명령어로 클러스터의 모든 호스트에서 ceph deamon을 제거합니다.

```
$ ./cephadm rm-cluster --force --zap-osds --fsid <fsid>
```

sudo cephadm bootstrap --mon-ip 172.25.0.46 --allow-fqdn-hostname --ssh-user ubuntu