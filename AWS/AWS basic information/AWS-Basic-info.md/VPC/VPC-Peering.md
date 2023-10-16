# VPC Peering
## What is VPC Peering?
VPC Peering은 서로다른 VPC 네트워크를 연결하기 위해 사용합니다.

## VPC Peering을 사용하는 이유
모든 VPC들을 같은 네트워크에서 작동되게끔 만들기 위해서 사용합니다.

다른 AWS 계정의 VPC를 연결하거나, 다른 리전의 VPC를 연결하거나 연결하고 싶지 않다면, CIDR가 서로 멀리 떨어져있어야 하며,.

연결했을 때 CIDR가 겹친다면 통신할 수 없기에 사용합니다.

## 주의사항
VPC Peering을 사용할 때 , 통신에 있어서 주의사항이 있습니다.

만약 아래와같이 세개의 VPC가 있을 경우, 
- 1. A-vpc
- 2. B-vpc
- 3. C-vpc

각 VPC들은 연결하기 위해서 아래와 같이 모두가 연결되어야 통신이됩니다.
```
A-vpc <-> B-vpc <-> C-vpc

A-vpc <-> C-vpc
```

## 특장점
VPC Peering은 보안 그룹을 참조할 수 도 있으며, 아예다른 AWS 계정에서 만든 VPC랑도 연결할 수 있습니다.
- 다른리전도 가능

또한 굳이 CIDR 또는 IP를가지지 않아도 됩니다.

## 사용방안
VPC Peering을 생성한 이후, 연결할 각 VPC들의 서브넷 라우팅테이블을 수정해주어야 합니다.

예를들어 아래처럼 연결한다면 ,

```
A-vpc <-> B-vpc <-> C-vpc

A-vpc <-> C-vpc
```

라우팅테이블은 다음과 같습니다.

### 1. A-vpc-subnet Route table
|target|목적지|비고|
|--|--|--|
|B-vpc-subnet-CIDR|생성한-peer-VPC||
|C-vpc-subnet-CIDR|생성한-peer-VPC||

### 2. B-vpc-subnet Route table
|target|목적지|비고|
|--|--|--|
|A-vpc-subnet-CIDR|생성한-peer-VPC||
|C-vpc-subnet-CIDR|생성한-peer-VPC||

### 3. C-vpc-subnet Route table
|target|목적지|비고|
|--|--|--|
|A-vpc-subnet-CIDR|생성한-peer-VPC||
|B-vpc-subnet-CIDR|생성한-peer-VPC||