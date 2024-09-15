# AWS Site to Site VPN
## 0. Overview
만약 특정 IDC와 AWS를 VPN을 통해 비공개 연결하기 위해서 아래의 두가지 세팅이 필요합니다.

- IDC에서의 Gateway
- AWS에서의 VPN Gateway

또한 각 Gateway를 연결해줄 Site to Site VPN Connection을 public internet에서 연결해야 비공개연결이 완료됩니다.

## 1. VPN 구성
### 1.1 Virtual Priavte Gateway (VGW)
AWS측에 있는 VPN 집선기

ASN을 지정할 수 도 있습니다.

### 1.2 Customer Gateway (CGW)
AWS와 VPN망으로 연결될 고객측의 소프트웨어 혹은 물리적 장치

## 2. 구성방안
### 2.1 CGW가 Public일 경우
VPN 연결 대상이되는 CGW에 인터넷 라우팅이 가능한 IP가 존재하기 때문에, 해당 IP와 연결하면 됩니다.
- CGW의 Public IP와 연결

### 2.2 CGW가 Private일 경우
Private일 경우, 대부분 NAT장비 뒤에 Private Subnet이 존재하기때문에, Nat의 Public IP를 CGW에 사용해서, 해당 NAT Public IP와 연결합니다.
- NAT의 Public IP와 연결

### 2.3 구성 순서
1. AWS에서 Custom Gateways를 생성
2. AWS에서 Virtual Private Gateways를 생성
3. AWS에서 Site-to-Site VPN을 생성해서 둘을 연결

## 3. AWS VPN CloudHub
AWS에서 VGW가 갖추어진뒤 연결대상 Network에 Custom Gateway가 위치한다면, CloudHub에서는 여러 VPN연결을 통해서 모든 연결대상 Private Network와의 연결을 보장합니다.

## 4. 주의사항
- **Subnet의 VPC에서 라우트전파를 활성화 해야 함. 안그러면 Site-to-Site VPN이 작동하지 않습니다.**

