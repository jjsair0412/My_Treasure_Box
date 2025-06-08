# Rancher Server URL 변경 방법
## Overview
해당 문서는 Rancher에 DownStream Cluster가 연결되어 있는 경우, Server의 URL을 변경하는 방법에 대해 기술함.

## 참고 문서
- Rancher 공식 문서 : [modify rancher server url](https://www.suse.com/ko-kr/support/kb/doc/?id=000021274)

## 사전 요구사항
1. DownStream Cluster와 변경 후 Rancher Server URL는 DNS를 통한 통신이 가능해야 한다.
2. Rancher Server의 신규 Domain과 기존 Domain은 인증서 오류가 없어야 한다.

## 작업 순서
1. Rancher Server Helm Value 백업
- 장애 발생 시 빠른 복구를 위함
```bash
# Rancher 백업 (권장)
# 현재 Helm values 확인
helm get values rancher -n cattle-system > rancher-values-backup.yaml
helm get values rancher -n cattle-system -o yaml > rancher-values-full-backup.yaml
```

2. Rancher Server URL 변경
- 원래는 Rancher Server의 hostname을 먼저 변경해주어야 함.
- 그러나 UI 상 Server URL을 먼저 변경하는 것은, 만약 hostname을 수정한 경우 Agent가 신규 hostname에 통신하지 못할 때 DownStream Cluster가 중단될 가능성이 있기 때문임.
- 수동으로 변경햇을때 Agent들이 새로운 Server URL로 자동 Restart
    - 이때 Cluster에 registration 명령어가 신규 URL로 변경되어야 Agent에 신규 Pod가 생김.
시간이 조금 걸리기때문에(10초정도?) 기다리면 됨.
    - 이거, Agent와 연결이 끊긴다음에 수정하면 Pod가 재 생성되지 않음. Agent 연결 끊기기 전에 변경해야 Cluster Agent가 신규배포됨.

    ![modify_server_url](./images/modify_server_url.png)

    - **만약이때 서버URL로 연결하지 못하거나, tls 문제가 발생하면 Agent와 통신이 끊겨서 순단시간이 발생할 가능성이 있음.**
    - **여러 Cluster가 등록된 경우, 클러스터에 등록된 순서대로 순차적으로 register URL이 변경되고 신규 Agent Pod가 배포되면서 업데이트됨.**

3. hostname 변경
- 만약 hostname을 변경한 뒤 Agent(DownStream Cluster)가 신규 Domain으로 통신하지 못할 경우, Agent가 실패함.

```bash
# hostname 변경하여 재배포
helm upgrade rancher rancher-stable/rancher \
  --namespace cattle-system \
  --set hostname=rancher.test.com \
  --version 2.9.3 \
  --reuse-values

# 배포 완료 대기
kubectl -n cattle-system rollout status deploy/rancher
```

## 장애 발생 시 롤백 전략
- 초기 단계에서 백업한 values.yaml로 롤백 수행

```bash
# 문제 발생시 이전 설정으로 롤백
helm upgrade rancher rancher-stable/rancher \
  --namespace cattle-system \
  -f rancher-values-backup.yaml
```

## 트러블슈팅
### 1. Agent 재 배포 안될 경우
- Multi Cluster 환경일 경우, server URL 변경 시 모든 Cluster에 반영되지 않았을 경우에 Agent를 강제로 재 시작 해야함.
1. 다운스트림 클러스터에 등록된 Agent 재 배포
    - 이건 문제생겻을경우 강제로 수행
    - Rancher Server가 2.9 버전 이상일 경우, 하기 방법처럼 Annotation 수정

```bash
# 모든 클러스터 ID 확인
kubectl get clusters.management.cattle.io

# 각 클러스터별로 실행(2.9 이상일 경우)
kubectl annotate clusters.management.cattle.io <CLUSTER_ID> io.cattle.agent.force.deploy=true

# 각 클러스터별로 실행(2.9 이하일 경우)
kubectl patch clusters.management.cattle.io <REPLACE_WITH_CLUSTERID> -p '{"status":{"agentImage":"dummy"}}' --type merge
```
