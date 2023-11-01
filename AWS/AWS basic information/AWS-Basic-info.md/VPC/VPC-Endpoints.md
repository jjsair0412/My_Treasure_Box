# VPC Endpoints
## Overview
VPC Endpoints는, IGW를 통해 Public Internet을 경유하지 않고, AWS 리소스에 액세스할 수 있게 도와주는 서비스 입니다.

Private Subnet만을 거쳐서 접근할 수 있게 됩니다.

## 사용되는 이유 - 장점
일단 모든 AWS 서비스들은 Public하게 접근할 수 있게끔 구성되어 있습니다.

그래서 단순히 Private Subnet에 위치한 EC2 서버가, Public Subnet에 위치한 Nat Gateway를 통해서 IGW를 뚫고 AWS 리소스에 닿을순 있겠지만, **이러한 방법은 비용이 많이나가며 홉이 여러개이기때문에 성능상 좋지 않습니다.**
- Nat를 닿을때 비용,, 등등..

그래서, VPC endpoints를 사용합니다.

VPC Endpoints는 AWS PrivateLink를 통해서 Private으로 AWS 리소스에 접근하므로, ***Public internet을 통하지 않고도 리소스에 접근***할 수 있습니다.

IGW나 NAT를 만들지 않고 리소스에 접근할 수 있기에, ***아키텍처가 간단해지고 비용이 덜 든다***는 장점이 있습니다.

또한 VPC Endpoints는 ***수평확장이 가능***하다는 장점도 있습니다.

## VPC Endpoints 유형
### 1. Interface Endpoint
모든 리소스를 대상으로 사용할 수 있습니다.

ENI를 프로비저닝하기에, 보안그룹이 꼭 연결되어야 합니다.
>ENI ? : VPC Private IP 주소이자 AWS 접근 포인트

***요금은 시간단위*** 또는 ***처리 데이터 GB 단위***로 청구됩니다.

### 2. Gateway Endpoint
***Amazon S3*** 또는 ***DynamoDB***를 대상으로만 사용할 수 있습니다.

***Gateway를 프로비저닝 하는데, Gateway는 반드시 RouteTable에 Target으로 설정되어야만 합니다.***
- IP주소나 보안그룹을 생성하지 않아도 사용할 수 있음

***요금이 무료***이며, Route Table로 액세스하는것이기에 자동 확장됩니다.

## 3. Interface Endpoint vs Gateway Endpoint
만약 S3에 접근해야한다면, 둘중 무엇을 선택해야할까?

대부분의 경우에 Gateway Endpoint가 이득.

일단 무료이고, 라우팅 테이블만 수정하면 되기에 더 간단합니다.

***Interface Endpoint는 외부 온프레미스 IDC와 Private으로 연결할 필요가 있을 때 사용하게 됩니다.***
- VPN도 있음 ..