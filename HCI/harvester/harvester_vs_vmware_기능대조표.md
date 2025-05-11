# Harvester vs VMware 기능 대조표
## 1. 주요 기능 영역 비교

| 구분 | VMware | Harvester |
|------|--------|-----------|
| **핵심 플랫폼** | vSphere (ESXi + vCenter) | KubeVirt, Kubernetes, Rancher |
| **가상화 기술** | ESXi (Type-1 하이퍼바이저) | KVM 기반 KubeVirt (Type-1 하이퍼바이저) |
| **관리 솔루션** | vCenter Server | Rancher UI, Kubernetes API |
| **컴퓨팅 리소스 관리** | DRS(분산 리소스 스케줄러) | Kubernetes 스케줄러, Resource Quotas |
| **고가용성** | vSphere HA | Kubernetes Pod 레플리케이션, LiveMigration |
| **스토리지 솔루션** | vSAN, VMFS, NFS | Longhorn 기반 StorageClass, CSI 드라이버 |
| **네트워킹** | NSX-T, vDS, vSS | Cluster Network(CNI), VLAN, Multus |
| **가상머신 관리** | Clone, Template, Snapshot | VM 템플릿, 볼륨 스냅샷, YAML 매니페스트 |
| **컨테이너 통합** | Tanzu Kubernetes Grid | 네이티브 Kubernetes |
| **보안 기능** | NSX-T 방화벽, 마이크로세분화 | K8s NetworkPolicy, OPA Gatekeeper |
| **백업 및 복구** | vSphere Data Protection, SRM | Backup 및 Restore 컨트롤러, Velero |
| **모니터링 및 로깅** | vRealize Operations, Log Insight | Monitoring 스택(Prometheus/Grafana), Logging(Fluentd) |
| **자동화 및 API** | PowerCLI, REST API | Kubernetes API, kubectl, Terraform |
| **스케일링 및 업그레이드** | vSphere Update Manager | K8s Rolling Update, Cluster API |
| **멀티클러스터 관리** | vCenter Enhanced Linked Mode | Rancher Fleet, Cluster API |

## 2. 컴퓨팅 가상화 비교

| VMware | Harvester |
|--------|-----------|
| vMotion(라이브 마이그레이션) | KubeVirt LiveMigration |
| DRS(자원 자동 밸런싱) | K8s 리소스 스케줄링 |
| CPU/메모리 오버커밋 | K8s 리소스 제한 및 요청 |
| NUMA 최적화 | CPU 매니저, 토폴로지 매니저 |
| 핫 추가(CPU, 메모리) | VM CRD 수정을 통한 리소스 변경 |

## 3. 스토리지 비교

| VMware | Harvester |
|--------|-----------|
| vSAN(분산 스토리지) | Longhorn(분산 블록 스토리지) |
| 정책 기반 스토리지 관리 | StorageClass 프로필 |
| 스토리지 vMotion | 볼륨 마이그레이션 |
| 스냅샷 및 클론 | CSI 스냅샷, 클론 |
| VMFS/NFS 데이터스토어 | PV/PVC 기반 스토리지 |
| 중복제거 및 압축 | Longhorn 볼륨 압축 |

## 4. 네트워킹 비교

| VMware | Harvester |
|--------|-----------|
| NSX-T(네트워크 가상화) | Cluster Network(CNI 기반) |
| vDS(분산 스위치) | 멀티 네트워크 인터페이스(Multus) |
| VLAN/VXLAN | VLAN, Bridge 네트워크 |
| 마이크로세분화 | NetworkPolicy, Calico 정책 |
| 로드 밸런싱 | MetalLB, Service LoadBalancer |
| L2/L3 라우팅 | K8s 서비스 라우팅 |

## 5. 관리 및 운영 비교

| VMware | Harvester |
|--------|-----------|
| vCenter UI | Harvester 웹 UI, Rancher |
| Roles & Permissions | Kubernetes RBAC |
| 태그 및 사용자 지정 속성 | 레이블 및 어노테이션 |
| 알람 및 이벤트 | 이벤트, 알림 매니저 |
| 성능 모니터링 | Prometheus 지표, Grafana 대시보드 |
| 라이센싱 모델 | 오픈소스, 상용 지원 옵션 |

## 6. 아키텍처 및 배포 비교

| 구분 | VMware | Harvester |
|------|--------|-----------|
| **기본 아키텍처** | 독자적 하이퍼바이저 및 관리 시스템 | Kubernetes 기반 플랫폼 |
| **최소 배포 요구사항** | 3+ 호스트(vSAN/HA용) | 3+ 노드(고가용성용) |
| **에지 배포 옵션** | vSphere 2노드 클러스터 | Harvester 2노드 클러스터 |
| **리소스 풋프린트** | 높음(vCenter 요구) | 중간(Kubernetes 오버헤드) |
| **업그레이드 방식** | Rolling 업그레이드, vSphere Update Manager | K8s 컴포넌트 Rolling 업데이트 |
| **확장성** | vSphere 클러스터 확장 | 노드 추가 및 Rancher 관리 |

## 7. 비용 및 생태계 비교

| 구분 | VMware | Harvester |
|------|--------|-----------|
| **라이센싱 모델** | 구독 또는 영구 라이센스, CPU 단위 | 오픈소스(무료) + 상용 지원 옵션 |
| **벤더 종속성** | 높음(VMware 생태계) | 낮음(표준 Kubernetes API) |
| **개발 커뮤니티** | VMware 기업 지원 | CNCF 프로젝트, 오픈소스 커뮤니티 |
| **기술 지원** | VMware 공식 지원 | SUSE/Rancher 지원, 커뮤니티 |
| **생태계 통합** | 광범위한, 성숙한 파트너 생태계 | 성장 중인 클라우드 네이티브 생태계 |
| **교육 및 자격증** | 공식 VMware 자격증, 폭넓은 교육 | Kubernetes 자격증, Rancher 교육 |