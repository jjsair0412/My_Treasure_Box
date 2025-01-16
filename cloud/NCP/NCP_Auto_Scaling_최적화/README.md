# NCP Auto Scaling 최적화
## Overview
NCP Cloud의 Auto Scaling Group(ASG)에서 임계치 트리거로 인해 VM 인스턴스의 수가 증가할 경우, 트래픽 유입 속도가 인스턴스 생성 속도보다 빠르면 에러가 발생할 수 있습니다. 

이를 해결하기 위한 방안과 VM Graceful Shutdown 구현 방안에 대해 검토했습니다.