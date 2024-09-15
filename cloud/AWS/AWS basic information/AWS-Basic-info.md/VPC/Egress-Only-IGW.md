# Egress Only Internet Gateway
## 송신전용 Internet Gateway란 ?
IPv6 전용 서비스

IPv4의 Nat Gateway처럼, Private Subnet에서 Internet으로 **송신**만 가능하게하는 서비스 입니다.

송신만 가능하다는것은, 외부인터넷에서 Private Subnet의 존재하는 IP CIDR를 보지 못한다는것을 의미합니다.

## NAT vs Egress-only IGW vs IGW
**IGW는** subnet 내부의 특정 인스턴스가, Internet과 양방향 통신이 가능하게끔 도와줍니다.
이는 Internet에서도 subnet 내부로 접근할 수 있고, subnet 내부에서도 인터넷으로 접근할 수 있다는 의미

**NAT gateway는 IPv4 전용 서비스입니다.** 이를 사용하면, Private subnet 내부의 특정 인스턴스가, Public Subnet의 NAT를 통해서 IP가 변환된 이후, 변환된 IP로 Internet에 접근이 가능하게끔 도와줍니다.
이는 private Subnet에서 NAT를 통한다면 Internet으로 통신할 수 있지만, 인터넷에선 private subnet에는 접근할 수 없다는것을 의미합니다.
    - Internet에서 알 수 있는것은, NAT의 Public IP만 알 수 있습니다.

**Egress-only IGW**는 IPv6 전용 서비스입니다. 이를 사용하면, Private subnet 내부의 특정 인스턴스가, Public Subnet의 NAT를 통해서 IP가 변환된 이후, 변환된 IP로 Internet에 접근이 가능하게끔 도와줍니다.