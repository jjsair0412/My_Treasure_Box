# Network Firewall
## Overview
VPC 전체를 보호하는 방화벽

AWS Network Firewall은 3계층 ~ 7계층 까지 보호합니다.

모든 방향에서 들어오는 모든 트래픽을 검사합니다.
- Direct Connect, Site to Site VPN 또한 검사함

인터넷과의 모든 트래픽, 피어링된 VPC과의 트래픽, Site to Site VPN을 오가는 모든 트래픽을 보호합니다.

## 필터링 규칙
VPC 수준에서 수천개의 규칙을 지원합니다.
1. IP를 IP와 포트별로 필터링
2. 프로토콜별로 필터링
    - 예) 아웃바운드에서 SMB 프로토콜 비활성화
3. 도메인별로 필터링
    - 예) VPC의 아웃바운드 트래픽에 대해서 특정 도메인에서만 엑세스 허용 
    - 타사 repository에서만 엑세스 가능
4. 트래픽 허용, 차단 알림설정 가능


## 특징
Network Firewall에는 자체 규칙을 갖고 있으며, 중앙집중식으로 관리되고 여러 계정과 VPC에 적용됩니다.

플로우 로그 검사또한 지원합니다.