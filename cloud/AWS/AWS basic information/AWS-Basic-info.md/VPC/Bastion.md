# Bastion Host
## Overview
Private Subnet에 엑세스 하기 위해서 , Private Subnet과 같은 VPC에 Public Subnet을 생성한 이후 , Public Subnet에 EC2 인스턴스와 같은 VM을 두고,

각 Private Subnet에 존재하는 EC2 호스트 및 AWS 리소스에 SSH 연결하여 접근하는 방법

## Bastion 보안
Bastion은 Public Subnet에 존재하기 때문에 , Security Group과 같은 방화벽으로 Public Access를 막아야 합니다.
- 예를들어 사무실 IP 대역에서만 접근을 허용한다던가 하며 ..