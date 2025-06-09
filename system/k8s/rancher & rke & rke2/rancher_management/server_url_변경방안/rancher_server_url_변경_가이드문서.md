# Rancher Server URL 변경 가이드
## 목차
1. 개요
2. 사전 준비사항
3. 위험도 및 주의사항
4. 작업 절차
5. 모니터링 방법
6. 작업 완료 후 검증
7. 롤백 절차
9. 작업 후 정리 


## 1. 개요
### 1.1 목적
본 가이드는 Rancher Server의 URL(hostname)을 변경할 때 다운스트림 클러스터의 연결을 안전하게 유지하면서 작업을 수행하는 방법을 제시합니다.

### 1.2 적용 범위
- Rancher 2.7 이상 버전
- Helm으로 배포된 Rancher Server
- 다운스트림 클러스터가 연결된 환경

### 1.3 참고 문서
- [Rancher 공식 문서 - How to change Rancher 2.x server-url](https://www.suse.com/ko-kr/support/kb/doc/?id=000021274)

## 2. 사전 준비사항

### 2.1 환경 확인
- [ ] 현재 Rancher Server 상태가 정상(Active)
- [ ] 모든 다운스트림 클러스터 상태가 정상(Active)
- [ ] 신규 도메인의 DNS 해석이 정상 동작
- [ ] 신규 도메인의 TLS 인증서가 준비됨

### 2.2 네트워크 요구사항
- [ ] 다운스트림 클러스터에서 신규 URL로 HTTPS 통신 가능
- [ ] WebSocket 연결이 정상 동작 (프록시/LB 설정 확인)
- [ ] 방화벽에서 신규 도메인 허용

### 2.3 백업 준비
```bash
# etcd 백업 (권장)
kubectl create backup -n cattle-system

# Helm values 백업
helm get values rancher -n cattle-system > rancher-values-backup.yaml
helm get values rancher -n cattle-system -o yaml > rancher-values-full-backup.yaml
```

## 3. 위험도 및 주의사항

### 3.1 위험도: **중간**
- 작업 중 일시적인 클러스터 연결 끊김 가능
- TLS/DNS 문제 시 전체 클러스터 관리 불가

### 3.2 주요 주의사항
**Server URL과 hostname 변경 순서가 중요합니다**
- 반드시 Server URL을 먼저 변경하고, hostname을 나중에 변경
- 순서를 바꿀 경우 클러스터 연결이 끊어질 수 있음 (통신 불가능한 경우)

**작업 시간 고려**
- 클러스터 수에 따라 전체 작업 시간이 달라짐 (클러스터당 1-2분)
- 업무 시간 외 작업 권장

## 4. 작업 절차

### 4.1 1단계: 현재 상태 확인
```bash
# 현재 Server URL 확인
kubectl get settings.management.cattle.io server-url -o yaml

# 클러스터 상태 확인
kubectl get clusters.management.cattle.io -o wide

# Rancher Pod 상태 확인
kubectl get pods -n cattle-system
```

### 4.2 2단계: Server URL 변경 (UI에서)
1. Rancher UI 접속: `https://기존도메인`
2. Global Settings → Advanced → `server-url` 클릭
3. 값을 신규 URL로 변경: `https://신규도메인`
4. **Save** 클릭

**예상 동작:**
- 변경 후 클러스터의 registration 명령어가 신규 URL로 업데이트
- 10-30초 후 각 클러스터의 Agent Pod가 클러스터 별로 순차적 재 배포
    - 만약 재 배포되지 않을 경우, 6.1 강제 Agent 재 배포 방법 절차 수행
- ***클러스터가 많을 경우 수 분 소요 가능***

### 4.3 3단계: Agent 재시작 모니터링
```bash
# 실시간 클러스터 상태 모니터링
watch kubectl get clusters.management.cattle.io -o wide

# Agent Pod 재시작 확인
kubectl get pods -n cattle-system -A
```

**대기 시간:** 모든 클러스터가 Active 상태가 될 때까지 대기 (보통 2-5분)

### 4.4 4단계: hostname 변경
**모든 클러스터가 신규 Server URL로 정상 연결된 것을 확인 후 진행**

```bash
# hostname 변경
helm upgrade rancher rancher-stable/rancher \
  --namespace cattle-system \
  --set hostname=신규도메인 \
  --version 2.9.3 \
  --reuse-values

# 배포 완료 대기
kubectl -n cattle-system rollout status deploy/rancher
```

### 4.5 5단계: 최종 확인
```bash
# 새로운 URL로 접근 확인
curl -k https://신규도메인/ping

# 모든 클러스터 상태 확인
kubectl get clusters.management.cattle.io -o wide

# 신규 URL로 Rancher 접근
```

## 5. 모니터링 방법

### 5.1 실시간 모니터링 명령어
```bash
# 터미널 1: 클러스터 상태 모니터링
watch kubectl get clusters.management.cattle.io

# 터미널 2: Agent Pod 상태 모니터링
watch kubectl get pods -n cattle-system -l app=cattle-cluster-agent

# 터미널 3: Rancher 로그 모니터링
kubectl logs -n cattle-system -l app=rancher -f
```

### 5.2 정상 상태 확인 기준
- 모든 클러스터 상태: `Active`
- Agent Pod 상태: `Running`
- Rancher Pod 상태: `Running`

## 6. 트러블슈팅

### 6.1 Agent 자동 재배포가 안 되는 경우
```bash
# 강제 Agent 재배포
# 모든 클러스터 ID 확인 
kubectl get clusters.management.cattle.io 

# 각 클러스터별로 실행(2.9 이상일 경우) 
kubectl annotate clusters.management.cattle.io <CLUSTER_ID> io.cattle.agent.force.deploy=true
```

### 6.2 클러스터가 "Updating" 상태에서 멈춘 경우
```bash
# 클러스터 상태 초기화
kubectl patch clusters.management.cattle.io <CLUSTER_ID> --type=merge -p='{"status":{}}'

# 5분 대기 후 Agent 강제 재배포
kubectl annotate clusters.management.cattle.io <CLUSTER_ID> io.cattle.agent.force.deploy=true
```


## 7. 작업 완료 후 검증

### 7.1 기능 검증 체크리스트
- [ ] 신규 URL로 Rancher UI 정상 접근
- [ ] 모든 클러스터가 Active 상태
- [ ] kubectl shell 정상 동작
- [ ] 워크로드 생성/수정/삭제 정상 동작
- [ ] Fleet 동기화 정상 동작

### 7.2 검증 명령어
```bash
# API 접근 확인
curl -k https://신규도메인/ping

# kubectl 기능 테스트 (각 클러스터에서)
kubectl get nodes
kubectl get pods --all-namespaces
```

## 8. 롤백 절차

### 8.1 긴급 롤백 (문제 발생 시)
```bash
# 1. Server URL 롤백 (UI에서)
# Global Settings → Advanced → server-url을 기존 URL로 복원

# 2. hostname 롤백
helm upgrade rancher rancher-stable/rancher \
  --namespace cattle-system \
  -f rancher-values-backup.yaml

# 3. Agent 강제 재배포
kubectl annotate clusters.management.cattle.io <CLUSTER_ID> io.cattle.agent.force.deploy=true
```

### 8.2 완전 복구 (심각한 문제 시)
```bash
# etcd 백업에서 복구
kubectl restore backup <BACKUP_NAME> -n cattle-system
```

## 9. 작업 후 정리

### 9.1 기존 도메인 정리
- [ ] DNS 레코드 정리 (필요시)
- [ ] 기존 TLS 인증서 정리
- [ ] 방화벽 규칙 정리

### 9.2 문서 업데이트
- [ ] 운영 문서의 URL 정보 업데이트
- [ ] 사용자 가이드 업데이트
- [ ] 백업/복구 스크립트 URL 업데이트

## 10. 체크리스트

### 작업 전 체크리스트
- [ ] 백업 완료
- [ ] 네트워크 연결 확인
- [ ] TLS 인증서 준비
- [ ] 작업 시간 확보
- [ ] 롤백 계획 수립

### 작업 중 체크리스트
- [ ] Server URL 변경 완료
- [ ] Agent 자동 재시작 확인
- [ ] 모든 클러스터 Active 상태 확인
- [ ] hostname 변경 완료
- [ ] 최종 기능 검증 완료

### 작업 후 체크리스트
- [ ] 전체 기능 검증 완료
- [ ] 기존 도메인 정리
- [ ] 문서 업데이트
- [ ] 사용자 공지

---

**주의:** 본 가이드는 일반적인 환경을 기준으로 작성되었습니다. 특수한 네트워크 구성이나 보안 정책이 있는 경우 추가 고려사항이 있을 수 있습니다.