# migration vmware to harvester 
## Overview
vmware에서 harvester로 migration을 위한 확인사항, 절차 정리

## 1. 개념 비교
### 1.1 가상화 타입
- vmware : ESXi
- harvester : KVM(kube-virt 기반)

둘은 Type-1 하이퍼바이저라는 공통점을 가짐

### 1.2 harvester <-> vmware 기능 대조
- [기능대조표](./harvester_vs_vmware_기능대조표.md)