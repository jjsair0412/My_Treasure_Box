# Memcached 
- [Yoga_ubuntu_Memcached_설치방안_docs](https://docs.openstack.org/install-guide/environment-memcached-ubuntu.html)


## OverView
OpenStack에서 KeyStone과 같은 ID 인증 서비스의 메커니즘은 , Backend의 Memcached를 사용하여 토큰을 캐시 합니다.

**memcached 서비스는 일반적으로 Controller 노드에서 같이 실행**되며 , Prod일 경우엔 보안을 위해서 외부로 찢어놓고 방화벽 , 인증 및 암호화 조합을 같이 사용하는것이 좋습니다.

## ETC
Memcached의 log파일은 default로 아래 경로에 쌓이게 됩니다.
```bash
$ pwd
/var/log/memcached.log
```

## Memcached Precondition
### 구성요소 설치 및 conf 구성
1. 패키지 설치
설치대상 ubuntu 버전에 따라 명령어가 다릅니다.

- ubuntu 18.04 이전일 경우
```bash
$ sudo apt-get install memcached python-memcache
```

- ubuntu 18.04 및 latest일 경우
    - 지금 설치환경이 20.04 ubuntu이기 때문에 , 얘로 설치
```bash
$ sudo apt-get install memcached python3-memcache
```

2. conf파일 구성
conf파일 위치는 다음과 같습니다.
```bash
$ sudo vi /etc/memcached.conf
```

controller node의 IP 대역에 접근할 수 있게끔 ip를 구성합니다.
- 해당 -1 뒤에오는 IP가 memcached가 listen하고있는 ip입니다.
- 따라서 현재 네트워크구성에 맞게끔 listen ip를 변경합니다.
    - 현재는 테스트라 , 하나의 노드에 모든 서비스 리소스가 다 들어갑니다.
    - 따라서 localhost로 두고 설치해도 무관하지만 , 환경이 변경됨에 따라 해당 IP대역을 변경시켜주어야만 합니다. (방화벽 x listen ip)
```conf
...
# Specify which IP address to listen on. The default is to listen on all IP addresses
# This parameter is one of the only security measures that memcached has, so make sure
# it's listening on a firewalled interface.
-l 127.0.0.1
...
```

### memcached 설치
memcached 서비스를 재 시작 합니다.
```bash
$ service memcached restart 
```