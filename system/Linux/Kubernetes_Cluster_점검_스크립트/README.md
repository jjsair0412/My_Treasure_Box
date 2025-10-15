# Kubernetes Cluster Inspection Script
## 개요
여러 K8s 클러스터의 리소스 사용량(CPU/Memory/Disk)을 Prometheus 메트릭으로 수집해서 리포트 생성

## 전제조건
- kubectl 설치 및 설정 완료
- 각 클러스터에 Prometheus 설치되어 있어야 함
  - 네임스페이스: `monitoring`, `cattle-monitoring-system`, `prometheus`, `kube-system` 중 하나
  - Label: `app.kubernetes.io/name=prometheus`

## 실행방법
```bash
chmod +x cluster_inspection.sh
./cluster_inspection.sh
```

## 결과물
각 클러스터별로 타임스탬프가 포함된 리포트 파일 생성:
- `{cluster-name}_report_20250115_143052.txt`

**리포트 내용:**
- Deployment별 CPU/Memory 사용량
- Node별 CPU/Memory/Disk 사용량
- Cluster 전체 리소스 합산

## 주의사항
- 스크립트는 `kubectl config get-contexts`로 조회된 모든 컨텍스트를 순회
- 한 클러스터에서 에러 발생 시 전체 스크립트 중단됨 (`set -e`)
- Prometheus가 없는 클러스터는 스킵되고 에러 메시지만 기록

## 실행 시간
실행 완료 후 총 소요시간 출력됨