# haproxy 이중화 방안
## Prerequisites
해당 문서는 haproxy를 기본 설치한 이후 , 이중화 설치 방안에 대해 기술합니다.

필요한 구성은 , keepalived를 이중화시킬 vm에 모두 추가로 설치 해야 합니다.

## 이론
haproxy로 동작중인 vm 두 대는 , 동일한 VIP를 가지고 있습니다.

설정한 VIP를 통해 외부에서 cluster 서비스를 제공중입니다. ( 한대를 통해서만 트래픽 통과 )
- 만약 VIP가 10.1.1.2 라면 , DNS에는 10.1.1.2 jenkins.aaa.com 으로 등록됨
- 사용자가 jenkins.aaa.com을 입력하면 , DNS 서버는 10.1.1.2를 반환하고 VIP를 등록시켜놓은 haproxy로 들어옴

이때 작동중인 haproxy한대가 문제 발생하여 down되게 된다면, keepalived가 VIP를 넘겨주고
standby상태인 이중화 vm이 역할을 대신합니다.

## 구성
VIP 는 10.1.1.3으로 등록합니다.
### 1. install haproxy & config setting
이중화 할 노드 모두 아래 문서를 참고하여 haproxy를 설치합니다.

[haproxy 설정 방안](https://github.com/jjsair0412/kubernetes_info/blob/main/haproxy/haproxy%20%EC%84%A4%EC%A0%95%20%EB%B0%A9%EC%95%88.md)

### 2. keepalived 설치
이중화 할 노드 모두 keepalived를 설치합니다.
```
# centos
$ yum install -y keepalived

# ubuntu
$ sudo apt-get install keepalived
```

만약 폐쇄망이라면 , keepalived rpm 파일을 모두 설치 한 이후 , vm으로 옮겨와서 설치를 진행합니다.
```
$ sudo rpm -Uvh *.rpm
```

### 3. sysctl 설정
이중화 할 노드 모두 작업합니다.

keepalived를 이용하여 floating ip를 옮기기 위해 아래 설정을 진행합니다.
```
$ sudo vi /etc/sysctl.conf
```

sysctl.conf 파일에 아래 내용을 추가합니다.
```
net.iipv4.ip_forward = 1
net.ipv4.ip_nonlocal_bind = 1
```

변경된 내용을 저장합니다
```
$ sudo sysctl -p
```

### 4. VIP 설정
이중화 할 노드 모두 작업합니다.

아래 명령어를 통해 이더넷 이름을 확인합니다.
```
$ ip a
2: ens3: <BROADCAST,MULTICAST,UP,LOWER_ ...
...
```

확인한 ens3 이더넷에 VIP를 아래 명령어로 설정합니다.
ens3:0 이름으로 VIP를 설정합니다.
```
# 파일 복사
cat /etc/sysconfig/network-scripts/ifcfg-ens3 >> /etc/sysconfig/network-scripts/ifcfg-ens3:0

# 복사한 파일 수정
# IP , NAME , DEVICE를 변경합니다.
vi ifcfg-ens3:0
TYPE=Ethernet
NAME=eth3:0 # 수정
DEVICE=ens3:0 # 수정
IPADDR=10.1.1.3 # 수정
NETMASK=255.255.255.0
GATEWAY=10.xxx.xxx.xxx
...
```

Network 재 시작합니다.
```
$ systemctl restart network.service
```

VIP가 잘 적용되었는지 확인합니다 !
```
$ ip a
``` 

### 5. Keepalived 설정
이중화 할 노드 모두 작업합니다.

이전에 설치한 Keepalived 설정합니다.

etc 폴더에 conf파일이 위치하기에 , find 명령어로 keepalived.conf파일을 찾아서 복사합니다.
```
$ sudo cp /etc/keepalived/keepalived.conf /etc/keepalived/keepalived.conf.old
```

복사해둔 keepalived.conf파일을 수정합니다.
```
# 메인 haproxy 
$ vi keepalived.conf
! Configuration File for keepalived
global_defs {
    router_id ROUTER_VM1
}

vrrp_instance VI_1 {
    state MASTER
    interface enp2s0
    virtual_router_id 99
    priority 150 # 실행순서 . 백업용 이중화 vm이 숫자가 더 낮아야 한다
    virtual_ipaddress {
        10.1.1.3 # vip
    }
}


# sub haproxy
$ vi keepalived.conf
! Configuration File for keepalived
global_defs {
    router_id ROUTER_VM1
}

vrrp_instance VI_1 {
    state MASTER
    interface enp2s0
    virtual_router_id 99
    priority 149 # 실행순서 . 백업용 이중화 vm이 숫자가 더 낮아야 한다
    virtual_ipaddress {
        10.1.1.3 # vip
    }
}
```
