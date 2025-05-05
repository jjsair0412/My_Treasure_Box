# Longhorn
[longhorn docs](https://longhorn.io/)

## 최소 요구사항
- 3 nodes
- 4 vCPUs per node
- 4 GiB per node
- SSD/NVMe or similar performance block device on the node for storage (recommended)
- HDD/Spinning Disk or similar performance block device on the node for storage (verified)
    - 500/250 max IOPS per volume (1 MiB I/O)
    - 500/250 max throughput per volume (MiB/s)

## Longhorn 구성 전 주의사항
0. 호스트 fileSystem 지원 타입은 아래와 같음
    - ext4
    - XFS

1. ```bash```, ```curl```, ```findmnt```, ```grep```, ```awk```, ```blkid```, ```lsblk``` 명령어가 반드시 필요

2. 모든 노드에 Iscsi 설치
    - [open-iscsi 설치 방안 공식문서](https://longhorn.io/docs/1.8.1/deploy/install/#installing-open-iscsi)
    - [iscsI with Longhorn 참고](../../iscsI_with_longhorn.md)

3. 디스크 샤딩
    - Longhorn은 현재 **여러 디스크 간의 샤딩(데이터 분할 저장)을 지원하지 않기에, LVM을 통해 모든 디스크를 단일 파티션으로 통합하는것이 권장**
    - LVM을 사용하여 모든 디스크가 Longhorn에게 하나의 디스크로 보이도록 해야 함.

4. RWX를 위한 NFS 구성
    - Longhorn의 생성할 PVC Type을 RWX로 구성하기 위해선, NFSv4 client를 설치해야 함.
    - [NFSv4 설치 방안 공식문서](https://longhorn.io/docs/1.8.1/deploy/install/#installing-nfsv4-client)

### Architecture
Longhorn supports the following architectures:
- AMD64
- ARM64
- s390x (experimental)

## Overview
***Longhorn은 Kubernetes 환경을 위한 분산 블록스토리지 시스템***

**이때 분산 블록 스토리지**란, 데이터를 여러 서버/노드에 걸쳐 저장하는 시스템.

1. 블록 스토리지?
    
    블록 스토리지는 데이터를 블록 단위로 관리. 
    각 블록은 고유 주소를 가지며 블록 디바이스에 의해 관리.

2. 분산?

    데이터 블록이 단일 서버가 아닌 여러 서버에 분산되어 저장. 다음과 같은 이점을 가짐.

    - 고가용성 : 한 서버가 실패해도 데이터가 다른 서버에 복제되어 있어 손실 X
    - 확장성 : 스토리지 용량 확장 시 새 서버를 추가하기만 하면 됨.
    - 성능 : 여러 서버에서 동시에 데이터를 읽고 쓸 수 있어 성능 향상


## Longhorn 아키텍처
Longhorn은 2가지 계층으로 구성됨.

1. Data Plane : Longhorn Engine
2. Control Plane : Longhorn Manager

### 1. Longhorn Manager와 Longhorn Engine
Longhorn Manager는 각 노드에 DaemonSet Type으로 실행됨. 얘는 K8s Cluster에서 볼륨 생성/관리 하며 Longhorn UI 또는 Longhorn CSI 플러그인의 API 호출 지원.

Longhorn Manager는 K8s API와 통신하여 Longhorn Volume CR을 생성하고, API 응답을 모니터링하여 API 서버가 CR이 생성되었음을 인지하면 새 볼륨을 생성함.

Longhorn Manager가 Volume 생성 요청을 받을 경우, 볼륨이 연결된 노드에 Longhorn Engine 인스턴스를 생성하고, 데이터 복제본이 배치될 노드에 복제본을 생성함. **HA를 보장하기 위해선 이때 데이터 복제본을 별도 Host 노드에 배치해야 함.**

Engine에 문제가 발생하더라도, 모든 복제본이나 볼륨 접근에는 영향을 미치지 않으며, Pod는 정상 작동함.

Longhorn Engine은 항상 Longhorn Volume을 사용하는 Pod와 같은 노드에 실행되며, 여러 노드에 분산 저장된 복제본에 볼륨을 동기적으로 복제함.

![Longhorn_arch](./images/longhorn_arch.svg)

- Longhorn 볼륨이 있는 3개의 인스턴스가 있습니다.
- 각 볼륨에는 Longhorn Engine이라 불리는 전용 컨트롤러가 있습니다. V1 볼륨의 경우 엔진은 Linux 프로세스로 실행되며, V2 볼륨의 경우 SPDK RAID 블록 디바이스(bdev)로 작동합니다.
- 각 Longhorn 볼륨은 2개의 복제본을 가지고 있습니다. V1에서 복제본은 Linux 프로세스로 실행되는 반면, V2에서는 SPDK 논리 볼륨 bdev로 구현됩니다.
- 그림의 화살표는 볼륨, 컨트롤러 인스턴스, 복제본 인스턴스, 디스크 간의 읽기/쓰기 데이터 흐름을 나타냅니다.
- 각 볼륨마다 별도의 Longhorn Engine을 생성함으로써, 하나의 컨트롤러가 실패하더라도 다른 볼륨의 기능에는 영향을 미치지 않습니다.
