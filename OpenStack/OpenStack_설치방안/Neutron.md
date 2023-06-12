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

## ENV
- endpoint API : http://controller:9292
- Domain : controller
- Port : 9292 

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
    - [아키텍처](#Neutron-Precondition)
## Host Networking
- 컨트롤러 노드 , 컴퓨팅 노드 모두 하나의 노드에 몰려있기 때문에 , NIC 구성정보가 동일합니다.
### Controller node
1. NIC 구성
먼저 네트워크 인터페이스부터 구성합니다.

```bash
# usecase
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
auto eth3
iface eth3 inet manual
up ip link set dev $IFACE up
down ip link set dev $IFACE down
```

재부팅하거나 아래 명령어 기입합니다.