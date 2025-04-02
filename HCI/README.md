# HCI
## 참고할만한 문서
- [RedHat_하이퍼컨버지드 인프라란?](https://www.redhat.com/ko/topics/hyperconverged-infrastructure/what-is-hyperconverged-infrastructure)
- [네이버 블로그_HCI는 언제부터 사용되었고, 앞으로 어떻게 쓰일것인가](https://m.blog.naver.com/thephum/223492880581)

## 0. HCI란 무엇인가?
HCI는 Hyper-Converged Infrastructure의 약자로써, 컴퓨팅, 스토리지, 네트워킹, 가상화를 단일 시스템에서 통합 제공하는 IT 인프라 솔루션

구성 요소는 다음과 같음
1. Hypervisor
2. SDS(Software Defined Storage)
3. SDN(Software Defined Network)
4. Virtual Machine

기존 데이터센터의 인프라 아키텍처를 가상화하여 데이터센터 복잡성을 줄이고 유연한 아키텍처로 현대적 워크로드 지원이 가능

## 1. 장점
전통적 인프라 아키텍처를 전환할만큼의 장점이 필요

✅ **1. 단순한 인프라 관리**
	•	컴퓨팅, 스토리지, 네트워크, 가상화를 하나의 솔루션에서 통합 관리
	•	물리적 장비 및 복잡한 네트워크 구성을 줄일 수 있음

✅ **2. 확장성(Scalability) 우수**
	•	필요할 때 노드를 추가하여 리소스를 확장 가능 (Scale-out 방식)
	•	기존 인프라처럼 스토리지, 네트워크를 따로 증설할 필요 없음

✅ **3. 비용 절감**
	•	별도의 SAN(Storage Area Network) 없이 로컬 스토리지를 활용해 비용 절감
	•	소프트웨어 정의 스토리지(SDS)로 기존 데이터센터보다 효율적 운영 가능

✅ **4. 성능 최적화**
	•	로컬 SSD 및 NVMe 기반의 고성능 스토리지 활용 가능
	•	데이터 로컬리티(Data Locality) 덕분에 I/O 성능 향상

✅ **5. 고가용성(High Availability) 및 장애 대응**
	•	데이터 복제 및 분산 저장으로 장애 발생 시 자동 복구 가능
	•	한 노드에 장애가 발생해도 서비스 지속 가능

✅ **6. DevOps & 클라우드 친화적**
	•	가상화(VM)뿐만 아니라 컨테이너(Kubernetes) 기반 환경과도 잘 맞음
	•	자동화 및 오케스트레이션 도구와 쉽게 통합 가능 (Ansible, Terraform 등)

## 2. HCI 솔루션 별 비교 도표
### VMware vs Nutanix vs Harvester (HCI 비교)

| 비교 항목        | **VMware vSAN**         | **Nutanix AOS**       | **Harvester**           |
|-----------------|------------------------|-----------------------|-------------------------|
| **가상화 기술**  | vSphere + ESXi         | AHV(KVM 기반) + ESXi 지원 | KVM 기반                 |
| **스토리지 구조** | vSAN(SDS) 기반        | Nutanix ADS + SDS     | Longhorn (Kubernetes 기반) |
| **하드웨어 지원** | HCL 인증 필요         | 다양한 하드웨어 지원  | x86 서버 및 클라우드 네이티브 환경 |
| **확장성(Scalability)** | 수십~수백 노드  | 대규모 확장 가능     | 소규모 및 클라우드 네이티브 환경 최적화 |
| **관리 UI/UX**   | vCenter                | Prism                 | Rancher UI              |
| **라이선스 비용** | 유료(고비용)          | 유료(상대적으로 비쌈) | 오픈소스 (무료)          |
| **Kubernetes 지원** | Tanzu(별도 추가)  | Karbon                | 기본 지원                |
| **성능 최적화**  | 고성능 NVMe 및 캐시 지원 | 데이터 로컬리티 최적화 | 컨테이너 중심 I/O 최적화 |
| **백업 및 DR**   | vSphere Replication 지원 | NearSync/Metro DR     | Rancher 기반 백업 지원   |
| **오픈소스 여부** | ❌ (기업 솔루션)      | ❌ (기업 솔루션)      | ✅ (CNCF 기반 오픈소스)  |

### 📌 비교 요약
- **VMware**: 엔터프라이즈 환경에서 가장 안정적이며, 기업들이 많이 사용. 그러나 비용이 높음.
- **Nutanix**: 유연한 확장성과 강력한 성능을 제공하며, AHV 가상화를 기본 제공. VMware보다 비용 절감 가능.
- **Harvester**: 완전한 오픈소스 기반이며, Kubernetes 네이티브 환경에 적합. 소규모 클러스터나 테스트 환경에 유리.

### ETC
- **Havester**는 오픈소스이기에 라이센스 비용부담이 적음

  KVM기반 가벼운 아키텍처 사용으로 리소스 사용이 효율적

  Bare-metal 서버에 설치가 간단

  Longhorn 기반 스토리지 활용하여 소규모 환경에서도 손쉽게 저장소를 구현하고 관리가 가능

  따라서 소규모 워크로드 혹은 Rancher 통합으로 K8s 기반 인프라 운영을 고려할 때 활용