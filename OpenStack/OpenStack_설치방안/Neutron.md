# Neutron 설치 방안
- [Yoga_전체환경_Neutron_설치방안_docs](https://docs.openstack.org/neutron/yoga/install/)
- [Yoga_ubuntu_Neutron_설치방안_docs](https://docs.openstack.org/neutron/yoga/install/install-ubuntu.html)

Neutron을 설치하기 전 , 아래 docs를 꼭 읽어보아야 합니다.
- [Neutron_Overview](https://docs.openstack.org/neutron/yoga/install/overview.html)

해당 문서는 Neutron을 학습하기 위해 최소 요구사항에 부합되게끔만 설치하였기 때문에 , 프로덕션 레벨에서는 적합하지 않습니다.
Prod에서는 다음을 고려해야 합니다.
- 성능과 이중화 요구사항을 충족하도록 서비스를 구성해야 합니다.
- 방화벽 , 암호화 및 서비스 정책으로 보안을 강화합니다.
- Ansible , Chef , Salt같은 배포 도구를 통해서 Prod 환경의 배포 및 관리를 자동화합니다.

## ETC
설치 후 로그들은 아래 경로에 위치합니다
```bash
$ pwd
/var/log/neutron

$ ls
neutron-dhcp-agent.log         neutron-linuxbridge-cleanup.log  neutron-server.log     
neutron-linuxbridge-agent.log  neutron-metadata-agent.log
```

## ENV
- endpoint API : http://controller:9696
- Domain : controller
- Port : 9696 

## Neutron 아키텍처
- [공식문서_Neturon_아키텍처](https://docs.openstack.org/neutron/yoga/install/overview.html#networking)

### 1. Provider Network 아키텍처
아래 문서에서 정의할 아키텍처 입니다.

주로 L2 계층 서비스 (브리징, 스위칭) 및 네트워크 VLAN 분할을 이용하여 가장 간단한 방법으로 OpenStack의 네트워크를 구성합니다.

기본적으로 가상 네트워크를 물리적 네트워크에 연결하고 레이어 3(라우팅) 서비스를 위해 물리적 네트워크 인프라에 의존합니다. 또한 DHCP<동적 호스트 구성 프로토콜(DHCP) 서비스는 인스턴스에 IP 주소 정보를 제공합니다.

만약 운영에서 OpenStack을 배포할 것 이라면 , Provider Network 옵션은 권장되지 않습니다.
- 사설네트워크 및 로드벨런싱과 같은 고급서비스를 지원하는것이 부족하기 때문입니다.

![Provider][Provider]

[Provider]:./images/Provider.png

### 2. Self-Service Network 아키텍처
셀프 서비스 아키텍처는 , provider networks 옵션에 3계층 (라우팅) 서비스를 추가 함으로써 Virtual Extensible LAN (VXLAN)과 같은 오버레이 분할 방법을 사용하여 self-service networks를 가능하게 합니다.

기본적으로 NAT를 사용하여 가상 네트워크를 물리 네트워크로 라우팅합니다. 또한 해당 아키텍처는 LoadBalancer-as-a-service와 같은 고급 기능을 지원합니다.

![Self][Self]

[Self]:./images/Self.png

## Neutron Precondition
Neutron을 배포하기 위해선 네트워크 계층 구조를 어떻게 할것인지를 먼저 선택해야만 합니다.
- 테스트로 설치를 진행할 땐 , 공식문서에 나온 아키텍쳐대로 구성합니다.

일단 Neutron을 구성하기 위해선 배포하기로 선택한 아키텍쳐의 각 노드에 os를 설치한 다음 , NIC를 구성해야 합니다. 

또한 모든 노드는 패키지설치 , 보안 업데이트 , DNS 및 NTP(Network Time Protocol) 와 같은 관리 목적을 위해서 인터넷과 연결할 수 있어야 합니다.
- airgap 설치도 되긴 하겠지

해당 문서는 Provider 아키텍쳐대로 설치합니다.

해당 네트워크 아키텍처는 모든 인스턴스가 Provider 네트워크에 직접 연결되는데 , self-service 아키텍처에선 인스턴스는 self-service 또는 provider 네트워크로 연결될 수 있습니다.

Self-service는 Openstack 내부에서만 네트워크가 돌게 하거나 , Nat를 사용하여 일정 부분만 외부 인터넷과 통신하게끔 구성하는것 또한 가능합니다.

![networklayout][networklayout]

[networklayout]:./images/networklayout.png

위 예제 아키텍처는 다음 네트워크를 사용한다 가정합니다.
- 근데 Vagrantfile에선 아니기 때문에 설명을 잘 보고 , 자신의 환경에 맞게끔 설정합니다.

    - 게이트웨이 10.0.0.1을 사용하여 10.0.0.0/24에서 관리
    
        이 네트워크에는 패키지 설치, 보안 업데이트, DNS(도메인 이름 시스템) 및 NTP(네트워크 시간 프로토콜)와 같은 관리 목적으로 모든 노드에 대한 인터넷 액세스를 제공하는 게이트웨이가 필요합니다.

    - 게이트웨이 203.0.113.1을 사용하는 203.0.113.0/24 Provider 대역

        이 네트워크에는 OpenStack 환경의 인스턴스에 인터넷 액세스를 제공하기 위한 게이트웨이가 필요합니다.

    현재 Vagrant 환경의 private IP 대역은 다음과 같습니다.
        - 192.168.50.0/24
        - broadcast 192.168.50.255
        - gateway 192.168.50.1


또한 설치시 주의할 점으로 , 각 노드에선 IP 호출이 아닌 다른 이름 (DNS) 으로 각 노드끼리 호출 가능해야만 합니다. 
- 현재 테스트 환경에선 /etc/hosts 파일을 수정하여 진행합니다.

## ***Neutron 설치 및 구성 시작***
- 상단 아키텍처를 참고하며 구성해야 편합니다.
- 현재 테스트 환경은 한 노드에 모든 서비스가 배포되어있기 때문에 , 실제로 nic를 추가해서 해당 nic IP , netmask , gateway 등을 할당하지는 않습니다. 그러나 나눠진다면 , 꼭 할당해야만 합니다.
    - [아키텍처](#Neutron-Precondition)
## Host Networking 구성 ( NIC로 OpenStack LAN 구성 )
- 컨트롤러 노드 , 컴퓨팅 노드 모두 하나의 노드에 몰려있기 때문에 , NIC 구성정보가 동일합니다.
### Controller node
1. NIC 구성
먼저 네트워크 인터페이스부터 구성합니다.

```bash
# usecase - Provider 아키텍처의 Controller 노드 참고
- IP 주소 : 10.0.0.11
- 네트워크 마스크 : 255.255.255.0(또는 /24)
- 기본 게이트웨이 : 10.0.0.1

# 실 구성정보
- IP 주소 : 192.168.50.10
- 네트워크 마스크 : 255.255.255.0(또는 /24)
- 기본 게이트웨이 : 192.168.50.1
```

/etc/network/interfaces 파일에 다음과 같이 기입합니다.
- INTERFACE_NAME 을 실제 interface 이름으로 변경해야 합니다.
    - ex ) eth0 , ens224
```bash
# usecase
vi /etc/network/interfaces
# The provider network interface
auto INTERFACE_NAME
iface INTERFACE_NAME inet manual
up ip link set dev $IFACE up
down ip link set dev $IFACE down

# 실사용 명령어
vi /etc/network/interfaces
# The provider network interface
auto eth0
iface eth0 inet manual
address 192.168.50.10
network 192.168.50.0
gateway 192.168.50.1
netmask 255.255.255.0
broadcast 192.168.50.255
```

재부팅하거나 networking service를 재 시작하여 반영합니다.
```bash
$ service networking restart
```

2. DNS 구성
위에 작성한대로 , OpenStack의 Neutron을 통해 연결된 모든 서비스는 , IP가 아닌 DNS로 통신할 수 있어야만 합니다.

따라서 DNS서버를 사용하거나 , /etc/hosts 파일을 수정하여 설정합니다.
```bash
# Provider 아키텍처 참고
# controller
10.0.0.11       controller

# compute1
10.0.0.31       compute1

# block1
10.0.0.41       block1

# object1
10.0.0.51       object1

# object2
10.0.0.52       object2

# 실 반영 명령어
$ vi /etc/hosts
...
# controller
192.168.50.10      controller

# compute1
192.168.50.10       compute1

# block1
192.168.50.10       block1

# object1
192.168.50.10      object1

# object2
192.168.50.10      object2
```

### Compute Node
Compute 노드도 controller 노드와 동일 작업을 똑같이 진행합니다.
1. NIC 설정
    - 네트워크 카드를 수동으로 설정하여 , Compute 노드의 ip , netmask , gateway를 지정합니다.
2. DNS 설정
    - /etc/hosts 또는 DNS 서버를 구성하여 Domain Name으로 상대 서비스를 호출할 수 있도록 구성합니다.

차이점으론 , 당연하겠지만 Compute 노드의 ip . addr 에 맞게끔 구성합니다.

```bash
# usecase - Provider 아키텍처의 Controller 노드 참고
- IP 주소 : 10.0.0.31
- 네트워크 마스크 : 255.255.255.0(또는 /24)
- 기본 게이트웨이 : 10.0.0.1

# 실 구성정보
- IP 주소 : 192.168.50.10
- 네트워크 마스크 : 255.255.255.0(또는 /24)
- 기본 게이트웨이 : 192.168.50.1
```

### Block Storage node
Block Storage 노드도 controller , Compute 노드와 동일 작업을 똑같이 진행합니다.
1. NIC 설정
    - 네트워크 카드를 수동으로 설정하여 , Block Storage 노드의 ip , netmask , gateway를 지정합니다.
2. DNS 설정
    - /etc/hosts 또는 DNS 서버를 구성하여 Domain Name으로 상대 서비스를 호출할 수 있도록 구성합니다.

차이점으론 , 당연하겠지만 Block Storage 노드의 ip . addr 에 맞게끔 구성합니다.

```bash
# usecase - Provider 아키텍처의 Controller 노드 참고
- IP 주소 : 10.0.0.41
- 네트워크 마스크 : 255.255.255.0(또는 /24)
- 기본 게이트웨이 : 10.0.0.1

# 실 구성정보
- IP 주소 : 192.168.50.10
- 네트워크 마스크 : 255.255.255.0(또는 /24)
- 기본 게이트웨이 : 192.168.50.1
```

### 구성 완료 검증
각 노드에서 서로 ping 테스트를 진행하여 , 추가된 NIC로 icmp 프로토콜 통신이 가능한지 확인합니다.
```bash
$ ping compute1
~

$ ping controller1
~

$ ping block1
~
``` 

또한 **해당 문서대로 설치한 Provider 아키텍쳐일 경우 ,** 모든 노드가 인터넷에 접근이 가능한지 확인합니다.
```bash
$ ping 8.8.8.8
~
```

## ***Controller 노드 설치및 구성***
## Precondition
OpenStack Neutron 서비스를 구성하기 전 , 지금까지 해왔던 작업 처럼 DB , service 자격 증명 생성 및 API 엔드포인트를 구성해야 합니다.

### MySQL USER 구성
DB에 접근합니다.
```bash
$ mysql -u root -p
mysql>
```

- neutron 데이터베이스 생성 및 데이터베이스 액세스 권한 부여
    - neutron 계정에 적절한 비밀번호를 지정합니다. **해당 문서는 가이드이기에 1234로 설정**
```bash
# neutron 이름의 데이터베이스 생성
mysql> CREATE DATABASE neutron;

# neutron localhost user 생성 (pwd 1234)
mysql> CREATE USER 'neutron'@'localhost' IDENTIFIED BY '1234';

# neutron user에게 localhost 권한부여
mysql> GRANT ALL PRIVILEGES ON *.* TO 'neutron'@'localhost' WITH GRANT OPTION;

# neutron user에게 neutron 데이터베이스 localhost에서 관리 권한부여
mysql> GRANT ALL PRIVILEGES ON neutron.* TO 'neutron'@'localhost' WITH GRANT OPTION;

# neutron user 생성 (pwd 1234)
mysql> CREATE USER 'neutron'@'%' IDENTIFIED BY '1234';

# neutron user에게 권한부여
mysql> GRANT ALL PRIVILEGES ON *.* TO 'neutron'@'%' WITH GRANT OPTION;

# neutron user에게 neutron 데이터베이스 관리 권한부여
mysql> GRANT ALL PRIVILEGES ON neutron.* TO 'neutron'@'%' WITH GRANT OPTION;
```

모든 쿼리가 실행됐다면 mysql에서 빠져나옵니다.
```bash
mysql> exit
Bye
```

### admin 관리자 소싱
계속 사용했던 admin-openrc 파일을 이용해 환경변수를 소싱하여 admin 관리자 전용 CLI 명령 접근 권한을 가집니다.
```bash
$ cat admin-openrc
export OS_USERNAME=admin
export OS_PASSWORD=1234
export OS_PROJECT_NAME=admin
export OS_USER_DOMAIN_NAME=Default
export OS_PROJECT_DOMAIN_NAME=Default
export OS_AUTH_URL=http://controller:5000/v3
export OS_IDENTITY_API_VERSION=3

$ . admin-openrc
```

### neutron 서비스 사용자 생성
먼저 , ```neutron``` 사용자를 생성합니다.
- 비밀번호는 동일하게 12345 로 세팅합니다.
```bash
$ openstack user create --domain default --password-prompt neutron
User Password: 12345
Repeat User Password: 12345
+---------------------+----------------------------------+
| Field               | Value                            |
+---------------------+----------------------------------+
| domain_id           | default                          |
| enabled             | True                             |
| id                  | 591eda726c6f4b23a570099efa101567 |
| name                | neutron                          |
| options             | {}                               |
| password_expires_at | None                             |
+---------------------+----------------------------------+
```

생성한 neutron 사용자에게 admin 권한을 부여합니다.
- 해당 명령은 output이 없습니다.
```bash
# usecase
# 예제에서 사용하는 프로젝트명은 admin 이다.
$ openstack role add --project service --user neutron admin

# 실사용 명령어
$ openstack role add --project admin --user neutron admin
```

OpenStack neutron 서비스를 생성합니다.
```bash
$ openstack service create --name neutron \
  --description "OpenStack Networking" network
+-------------+----------------------------------+
| Field       | Value                            |
+-------------+----------------------------------+
| description | OpenStack Networking             |
| enabled     | True                             |
| id          | 76288842480344559bbc540e6cc4d509 |
| name        | neutron                          |
| type        | network                          |
+-------------+----------------------------------+
```

생성한 neutron 서비스의 API Endpoint를 구성합니다.
- 9696 포트로 open합니다.
- endpoint는 3가지 생성하며 , interface로 구분됩니다.
    - public
    - internal
    - admin
```bash
# neutron 서비스 endpoint : http://controller:9696
# interface : public
$ openstack endpoint create --region RegionOne \
  network public http://controller:9696
+--------------+----------------------------------+
| Field        | Value                            |
+--------------+----------------------------------+
| enabled      | True                             |
| id           | 9d539753d57c477ab726f67c485b631b |
| interface    | public                           |
| region       | RegionOne                        |
| region_id    | RegionOne                        |
| service_id   | 76288842480344559bbc540e6cc4d509 |
| service_name | neutron                          |
| service_type | network                          |
| url          | http://controller:9696           |
+--------------+----------------------------------+

# interface : internal
$ openstack endpoint create --region RegionOne \
  network internal http://controller:9696
+--------------+----------------------------------+
| Field        | Value                            |
+--------------+----------------------------------+
| enabled      | True                             |
| id           | 06cbcfc07f5344de9951eaba67428c18 |
| interface    | internal                         |
| region       | RegionOne                        |
| region_id    | RegionOne                        |
| service_id   | 76288842480344559bbc540e6cc4d509 |
| service_name | neutron                          |
| service_type | network                          |
| url          | http://controller:9696           |
+--------------+----------------------------------+

# interface : admin
$ openstack endpoint create --region RegionOne \
  network admin http://controller:9696
+--------------+----------------------------------+
| Field        | Value                            |
+--------------+----------------------------------+
| enabled      | True                             |
| id           | f347514e5dfc412ba0baf00d5d500454 |
| interface    | admin                            |
| region       | RegionOne                        |
| region_id    | RegionOne                        |
| service_id   | 76288842480344559bbc540e6cc4d509 |
| service_name | neutron                          |
| service_type | network                          |
| url          | http://controller:9696           |
+--------------+----------------------------------+
```

## ***Networking 옵션 구성***
상단의 

***1. [provider_아키텍처](#1-provider-network-아키텍처)***

***2. [self_service_아키텍처](#2-self-service-network-아키텍처)***

를 확인하여 현재 자신이 설치하고있는 상황에 알맞게 네트워킹 옵션을 구성해야만 합니다.
- 해당 문서는 1번 아키텍처를 따릅니다.
- self-service-network의 networking 옵션 방안은 , 공식문서 링크로 대체합니다.

1번 아키텍처는 , 가장 단순하지만 사설 네트워크 및 라우터 , 유동 IP주소가 없기 때문에 OpenStack 연습용 아키텍처로 적합합니다. 또한 admin 권한이 있는 사용자만이 네트워크를 관리할 수 있습니다.

2번 아키텍처는 , osi 3계층 서비스 (라우팅) 를 통해서 1번 아키텍처의 단점을 보완하게 되는데 , 일반 사용자나 데모 계정과 같은 권한없는 사용자들도 네트워크를 관리할 수 있습니다. 추가로 유동 IP를 지원하여 인터넷과 같이 self-service-network 내부에서 각 인스턴스들의 연결을 지원합니다.

## self-service-network 구성 
- 공식문서 링크로 대체합니다.
    - [링크](https://docs.openstack.org/neutron/yoga/install/controller-install-option2-ubuntu.html)

## provider network 구성
### 1. 구성요소 설치
각 구성 요소를 apt 명령어로 설치합니다.
```bash
$ apt install neutron-server neutron-plugin-ml2 \
  neutron-linuxbridge-agent neutron-dhcp-agent \
  neutron-metadata-agent
```

### 2. server component 구성
네트워킹 서버 컴포넌트의 구성 요소로는 database , message queue , authentication mechanism , topology change notifications 과 plugin 이 포함됩니다.

아래 문서의 기본구성과 prod버전은 구성 요소가 달라질 수 있습니다.

**1. neutron.conf 파일 구성**

neutron.conf 파일 구성합니다.

conf파일의 위치는 다음과 같습니다.
```bash
$ pwd 
/etc/neutron/neutron.conf
```

[database] 섹션에서 database 엑세스 구성을 기입합니다.
- NEUTRON_DBPASS 에 DB를 생성할 때 만들어진 암호를 기입합니다.
```bash
[database]
# usecase
connection = mysql+pymysql://neutron:NEUTRON_DBPASS@controller/neutron

# 실 사용 명령어
connection = mysql+pymysql://neutron:1234@controller/neutron
```

[DEFAULT] 섹션에서 ML2(Modular layer 2) 플러그인을 활성화 한 후 , 추가 플러그인을 비활성화 합니다.
- ML2 란 , 네트워크 스위치나 라우터같은 네트워크 장비에서 사용되는 소프트웨어 기술입니다. 이 기술은 네트워크 장비의 L2 스위칭 기능을 모듈화하여 제공하며, 다양한 L2 스위칭 기능을 구성하고 조합할 수 있는 유연성을 제공합니다.

```bash
[DEFAULT]
# ...
core_plugin = ml2
service_plugins =
```

[DEFAULT] 섹션에서 message queue 엑세스 정보를 구성합니다.
- RabbitMQ를 설치했기 때문에 , RabbitMQ로 구성합니다.
- 이전에 RabbitMQ를 설치할 때 , openstack 계정의 password를 ```1234``` 로 구성하여 설치를 진행했습니다. RABBIT_PASS 를 설정한 password로 변경한 뒤 설정합니다.
    - [Message_Queue_설치방안](./Message_Queue.md)
```bash
# usecase
[DEFAULT]
# ...
transport_url = rabbit://openstack:RABBIT_PASS@controller

# 실사용 명령어
transport_url = rabbit://openstack:1234@controller
```

[DEFAULT] 및 [keystone_authtoken] 섹션에서 KeyStone 서비스 (ID 서비스) 엑세스 정보를 구성합니다.
- NEUTRON_PASS 칸에 설정한 비밀번호로 변경하여 구성합니다.

```bash
# usecase
[DEFAULT]
# ...
auth_strategy = keystone

[keystone_authtoken]
# ...
www_authenticate_uri = http://controller:5000
auth_url = http://controller:5000
memcached_servers = controller:11211
auth_type = password
project_domain_name = default
user_domain_name = default
project_name = service
username = neutron
password = NEUTRON_PASS

# 실사용 명령어
# usecase
[DEFAULT]
# ...
auth_strategy = keystone

[keystone_authtoken]
# ...
www_authenticate_uri = http://controller:5000
auth_url = http://controller:5000
memcached_servers = controller:11211
auth_type = password
project_domain_name = default
user_domain_name = default
project_name = admin
username = neutron
password = 12345
```

[DEFAULT] 및 [nova] 섹션에서 네트워크 토폴로지 변경이 일어났을 때 변경 사항을 알릴 수 있도록 네트워킹을 구성합니다.
- NOVA_PASS 에 KeyStone에 등록된 nova 유저 password를 구성합니다.
```bash
# usecase
[DEFAULT]
# ...
notify_nova_on_port_status_changes = true
notify_nova_on_port_data_changes = true

[nova]
# ...
auth_url = http://controller:5000
auth_type = password
project_domain_name = default
user_domain_name = default
region_name = RegionOne
project_name = service
username = nova
password = NOVA_PASS

# 실 사용 명령어
[DEFAULT]
# ...
notify_nova_on_port_status_changes = true
notify_nova_on_port_data_changes = true

[nova]
# ...
auth_url = http://controller:5000
auth_type = password
project_domain_name = default
user_domain_name = default
region_name = RegionOne
project_name = admin
username = nova
password = 12345
```

[oslo_concurrency] 섹션에서 잠금 경로를 구성합니다.
```bash
[oslo_concurrency]
# ...
lock_path = /var/lib/neutron/tmp
```

**2. ML2(Modular Layer2) 플러그인 구성**
ML2 플러그인을 구성하여 Linux 브릿지 메커니즘을 통해 인스턴스용 2계층 (브리징 및 switching) 가상 네트워킹 인프라를 구축합니다.

ML2를 구성하기 위해서 , 아래 경로에 있는 ```ml2_conf.ini``` 파일을 수정해야 합니다.
```bash
$ pwd
/etc/neutron/plugins/ml2/ml2_conf.ini
```

[ml2] 섹션에서 flat 및 VLAN 네트워크를 활성화 합니다.
```ini
[ml2]
# ...
type_drivers = flat,vlan
```

[ml2] 섹션에서 self-service-network를 disable 합니다.
```ini
[ml2]
# ...
tenant_network_types =
```

[ml2] 섹션에서 Linux 브릿지 메커니즘을 활성화합니다.
```ini
[ml2]
# ...
mechanism_drivers = linuxbridge
```

[ml2] 섹션에서 포트 보안 확장 드라이버를 활성화 합니다.
```ini
[ml2]
# ...
extension_drivers = port_security
```

[ml2_type_flat] 섹션에서 flat network를 provider로 구성합니다.
```ini
[ml2_type_flat]
# ...
flat_networks = provider
```

[securitygroup] 섹션에서 , ipset을 활성화하여 보안 그룹 규칙의 효율성을 높힙니다.
```bash
[securitygroup]
# ...
enable_ipset = true
```

**3. Linux 브릿지 에이전트 구성**
Linux의 브릿지 에이전트는 , 보안 그룹2계층 가상 네트워크 인프라를 구축하여 인스턴스와 보안그룹을 처리합니다.

Linux의 브릿지 에이전트를 구성하기 위해서 , 아래 경로에 있는 ```linuxbridge_agent.ini``` 파일을 수정해야 합니다.

```bash
$ pwd
/etc/neutron/plugins/ml2/linuxbridge_agent.ini
```

[linux_bridge] 섹션에서 provider 가상 네트워크를 provider 물리 네트워크 인터페이스에 매핑시킵니다.
- PROVIDER_INTERFACE_NAME 은 실제 physical network interface 이름으로 변경합니다.
```ini
# usecase
[linux_bridge]
physical_interface_mappings = provider:PROVIDER_INTERFACE_NAME

# 실 사용 명령어
[linux_bridge]
physical_interface_mappings = provider:eth1
```

[vxlan] 섹션에서 VXLAN overlay network를 비활성화 합니다.
```ini
[vxlan]
enable_vxlan = false
```

[securitygroup] 섹션에서 security group을 활성화 하고 , Linux 브릿지의 iptables 방화벽 드라이버를 구성합니다.
```ini
[securitygroup]
# ...
enable_security_group = true
firewall_driver = neutron.agent.linux.iptables_firewall.IptablesFirewallDriver
```

아래 명령어를 수행하여 , sysctl 값이 모두 1인지 확인하여 , Linux os 커널이 네트워크 브릿지 필터를 지원하는지 확인해야 합니다.
- 만약 지원하지 않는다면 , br_netfilter 커널 모듈을 로드해야 합니다.
```bash
$ sysctl net.bridge.bridge-nf-call-iptables
net.bridge.bridge-nf-call-iptables = 1

$ sysctl net.bridge.bridge-nf-call-ip6tables
net.bridge.bridge-nf-call-ip6tables = 1
```

지원하지 않는다면 , 아래명령어로 br_netfilter 커널 모듈을 로드합니다.
```bash
$ cat <<EOF | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-ip6tables = 1
net.bridge.bridge-nf-call-iptables = 1
EOF

$ sudo sysctl --system
```

**4. DHCP 에이전트 구성**
DHCP 에이전트를 구성하여 가상 네트워크에 DHCP 서비스를 제공합니다.

DHCP 에이전트를 구성하기 위해서 , 아래 경로에 있는 ```dhcp_agent.ini``` 파일을 수정해야 합니다.

```bash
$ pwd
/etc/neutron/dhcp_agent.ini
```

[DEFAULT] 섹션에서 Linux 브리지 인터페이스 드라이버 , Dnsmasq DHCP 드라이버를 구성하고 공급자 네트워크의 인스턴스가 네트워크를 통해 메타데이터에 액세스할 수 있도록 격리된 메타데이터를 활성화합니다.
```ini
[DEFAULT]
# ...
interface_driver = linuxbridge
dhcp_driver = neutron.agent.linux.dhcp.Dnsmasq
enable_isolated_metadata = true
```

## ***metadata 에이전트 구성***
metadata agent는 자격증명과 같은 구성 정보를 인스턴스에 제공하게 됩니다.

metadata agent를 구성하기 위해서 , 아래 경로에 위치한 ```metadata_agent.ini``` 파일을 구성해야 합니다.
```bash
$ pwd
/etc/neutron/metadata_agent.ini
```

[DEFAULT] 섹션에서 metadata host 및 공유 비밀번호를 구성합니다.
- METADATA_SECRET 섹션에 프록시에 적절한 암호를 구성합니다.
- 테스트기때문에 , ```12345``` 로 구성합니다.
```ini
# usecase
[DEFAULT]
# ...
nova_metadata_host = controller
metadata_proxy_shared_secret = METADATA_SECRET

# 실 반영 명령어
[DEFAULT]
# ...
nova_metadata_host = controller
metadata_proxy_shared_secret = 12345
```

## ***네트워킹 서비스 (neutron) 을 nova에서 사용하도록 nova 구성***
- 해당 섹션을 완료하기 위해선 , nova를 설치해야만 합니다.
    - [nova_설치방안](./Nova.md)

nova.conf 파일을 구성해야 합니다. 아래 경로에 nova.conf파일이 위치합니다.
```bash
$ pwd
/etc/nova/nova.conf
```

[neutron] 섹션에서 엑세스 정보들을 알맞게 기입하며 , metadata proxy를 활성화 하고 , 암호를 기입합니다.
- NEUTRON_PASS 에는 keystone에 등록시킨 neutron 서비스에대한 암호를 기입합니다.
- METADATA_SECRET 에는 방금전 설정한 metadata_proxy 비밀번호를 기입합니다.
```bash
# usecase
[neutron]
# ...
auth_url = http://controller:5000
auth_type = password
project_domain_name = default
user_domain_name = default
region_name = RegionOne
project_name = service
username = neutron
password = NEUTRON_PASS
service_metadata_proxy = true
metadata_proxy_shared_secret = METADATA_SECRET

# 실 반영 명령어
[neutron]
# ...
auth_url = http://controller:5000
auth_type = password
project_domain_name = default
user_domain_name = default
region_name = RegionOne
project_name = admin
username = neutron
password = 12345
service_metadata_proxy = true
metadata_proxy_shared_secret = 12345
```

## ***설치 마무리***
아래 명령어로 데이터베이스를 동기화 합니다.
```bash
$ su -s /bin/sh -c "neutron-db-manage --config-file /etc/neutron/neutron.conf \
  --config-file /etc/neutron/plugins/ml2/ml2_conf.ini upgrade head" neutron
...
INFO  [alembic.runtime.migration] Running upgrade 97c25b0d2353 -> 2e0d7a8a1586
INFO  [alembic.runtime.migration] Running upgrade 2e0d7a8a1586 -> 5c85685d616d
  OK
```

compute-api 서비스인 nova-api를 재 시작합니다.
```bash
$ service nova-api restart
```

네트워킹 서비스인 neutron을 재 시작합니다.
- provider 아키텍처일 경우
```bash
$ service neutron-server restart
$ service neutron-linuxbridge-agent restart
$ service neutron-dhcp-agent restart
$ service neutron-metadata-agent restart
```

- self-service-network 아키텍처 일 경우
    - 3계층 서비스 포함해서 재 시작합니다.
```bash
$ service neutron-server restart
$ service neutron-linuxbridge-agent restart
$ service neutron-dhcp-agent restart
$ service neutron-metadata-agent restart
# 추가
service neutron-l3-agent restart
```

## ***설치결과 확인***
admin-openrc 자격증명을 소싱합니다.
```bash
$ . admin-openrc
```

성공적인 설치결과를 확인하기 위해 , neutron-server 프로세스들의 리스트를 확인합니다.
- 실제 출력은 다를 수 있음
```bash
$ openstack extension list --network
+---------------------------+---------------------------+----------------------------+
| Name                      | Alias                     | Description                |
+---------------------------+---------------------------+----------------------------+
| Default Subnetpools       | default-subnetpools       | Provides ability to mark   |
|                           |                           | and use a subnetpool as    |
|                           |                           | the default                |
| Availability Zone         | availability_zone         | The availability zone      |
|                           |                           | extension.                 |
| Network Availability Zone | network_availability_zone | Availability zone support  |
|                           |                           | for network.               |
| Port Binding              | binding                   | Expose port bindings of a  |
|                           |                           | virtual port to external   |
|                           |                           | application                |
| agent                     | agent                     | The agent management       |
|                           |                           | extension.                 |
| Subnet Allocation         | subnet_allocation         | Enables allocation of      |
|                           |                           | subnets from a subnet pool |
| DHCP Agent Scheduler      | dhcp_agent_scheduler      | Schedule networks among    |
|                           |                           | dhcp agents                |
| Neutron external network  | external-net              | Adds external network      |
|                           |                           | attribute to network       |
|                           |                           | resource.                  |
| Neutron Service Flavors   | flavors                   | Flavor specification for   |
|                           |                           | Neutron advanced services  |
| Network MTU               | net-mtu                   | Provides MTU attribute for |
|                           |                           | a network resource.        |
| Network IP Availability   | network-ip-availability   | Provides IP availability   |
|                           |                           | data for each network and  |
|                           |                           | subnet.                    |
| Quota management support  | quotas                    | Expose functions for       |
|                           |                           | quotas management per      |
|                           |                           | tenant                     |
| Provider Network          | provider                  | Expose mapping of virtual  |
|                           |                           | networks to physical       |
|                           |                           | networks                   |
| Multi Provider Network    | multi-provider            | Expose mapping of virtual  |
|                           |                           | networks to multiple       |
|                           |                           | physical networks          |
| Address scope             | address-scope             | Address scopes extension.  |
| Subnet service types      | subnet-service-types      | Provides ability to set    |
|                           |                           | the subnet service_types   |
|                           |                           | field                      |
| Resource timestamps       | standard-attr-timestamp   | Adds created_at and        |
|                           |                           | updated_at fields to all   |
|                           |                           | Neutron resources that     |
|                           |                           | have Neutron standard      |
|                           |                           | attributes.                |
| Neutron Service Type      | service-type              | API for retrieving service |
| Management                |                           | providers for Neutron      |
|                           |                           | advanced services          |
| resources: subnet,        |                           | more L2 and L3 resources.  |
| subnetpool, port, router  |                           |                            |
| Neutron Extra DHCP opts   | extra_dhcp_opt            | Extra options              |
|                           |                           | configuration for DHCP.    |
|                           |                           | For example PXE boot       |
|                           |                           | options to DHCP clients    |
|                           |                           | can be specified (e.g.     |
|                           |                           | tftp-server, server-ip-    |
|                           |                           | address, bootfile-name)    |
| Resource revision numbers | standard-attr-revisions   | This extension will        |
|                           |                           | display the revision       |
|                           |                           | number of neutron          |
|                           |                           | resources.                 |
| Pagination support        | pagination                | Extension that indicates   |
|                           |                           | that pagination is         |
|                           |                           | enabled.                   |
| Sorting support           | sorting                   | Extension that indicates   |
|                           |                           | that sorting is enabled.   |
| security-group            | security-group            | The security groups        |
|                           |                           | extension.                 |
| RBAC Policies             | rbac-policies             | Allows creation and        |
|                           |                           | modification of policies   |
|                           |                           | that control tenant access |
|                           |                           | to resources.              |
| standard-attr-description | standard-attr-description | Extension to add           |
|                           |                           | descriptions to standard   |
|                           |                           | attributes                 |
| Port Security             | port-security             | Provides port security     |
| Allowed Address Pairs     | allowed-address-pairs     | Provides allowed address   |
|                           |                           | pairs                      |
| project_id field enabled  | project-id                | Extension that indicates   |
|                           |                           | that project_id field is   |
|                           |                           | enabled.                   |
+---------------------------+---------------------------+----------------------------+
```