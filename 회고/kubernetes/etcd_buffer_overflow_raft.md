# ETCD Raft 복제 장애 분석
## 개요
- ETCD 노드의 Commit-Applied Gap의 차이가 5000 개 이상 발생한 경우의 시나리오

## 배경 지식
### 1. Raft 알고리즘
- ETCD는 분산 시스템의 데이터 정합성과 합의를 위해 Raft 알고리즘 사용
- 노드 역할은 아래와 같음

#### 1.1 노드 역할(State)
```
Leader                                   
- 모든 쓰기 요청 처리                        
- Follower에게 로그 복제                  
- Heartbeat 전송                        

           ↓ 복제

Follower                                
- Leader로부터 로그 수신                 
- 로컬 BoltDB에 적용 (Apply)                
- 참고: K8s 상, ETCD Pod는 HostNetwork, HostPath 사용함. DS로 배포됨.
- ACK 응답                              

           ↓ Election Timeout - (Leader Heartbeat 실패할 경우)
           
Candidate                               
- Leader 선출 시도                      
- RequestVote 전송                      
```

### 2 Commit Index vs Applied Index
- ETCD는 안정성 유지를 위해 로그 합의(Commit)와 실제 적용(Apply)을 분리함.
- 둘을 분리함으로써, 실제 LocalDB에 fsync() 작업이 이루어져 속도가 느린 Applied를 비동기 처리하여 성능 개선

#### 2.1 **Commit 과정** : Raft 노드가 쓰기 로그에 합의 완료되었음을 나타냄.
    1. Leader 쓰기 요청 인입
    2. Commit 로그 생성 및 Follower 전송
        * MsgApp 패킷
    3. 과반수 Follower(Majority)가 "받았음" 전송
        * ACK 패킷
    4. Committed Index Count 증가
    
#### 2.2 **Applied 과정** : 실제 BoltDB에 쓰여졌을 경우
    1. Committed 로그 가져옴
    2. BoltDB에 Write 수행
        * fsync()
    3. Applied Index Count 증가


### 2. Commit-Applied의 최대 Gap 차이
- 최대 Index Gap 차이는 5000개
- [Default 값 확인 가능 코드](https://github.com/etcd-io/etcd/blob/88feab3eecbeabd018876e86a9b9e448a26cd610/server/etcdserver/v3_server.go#L886)

```go
// etcd/server/etcdserver/v3_server.go
const (
    // In the health case, there might be a small gap (10s of entries)
    // between the applied index and committed index.
    // However, if the committed entries are very heavy to apply, 
    // the gap might grow.
    // We should stop accepting new proposals if the gap growing 
    // to a certain point.
    maxGapBetweenApplyAndCommitIndex = 5000
)

func (s *EtcdServer) processInternalRaftRequestOnce(...) {
    ai := s.getAppliedIndex()
    ci := s.getCommittedIndex()
    
    if ci > ai+maxGapBetweenApplyAndCommitIndex {
        return nil, errors.ErrTooManyRequests  // 503 에러!
    }
    // ...
}
```

### 3. Node Network Buffer의 한계
- Default Buffer Size는 4096개
- MsgApp, SnapShot, HeartBeat 패킷은 4096개가 넘어갈 경우 Drop
- [ETCD Default Buffer Size 코드](https://github.com/etcd-io/etcd/blob/88feab3eecbeabd018876e86a9b9e448a26cd610/server/etcdserver/api/rafthttp/peer.go#L236)

```go
// etcd/server/etcdserver/api/rafthttp/peer.go
const (
    streamBufSize = 4096  // 메시지 4,096개
)

type streamWriter struct {
    msgc chan raftpb.Message  // 버퍼 크기: 4,096개
}
```

## Leader Commit Index - Applied Index Count Gap 5000 이하로 발생한 경우
- Leader는 MsgApp 패킷 한개씩 전송
- ex: Index 801번 ~ 1000번까지 로그 2000개 전송 수행
    - 이때 Network Buffer 최대 크기인 4096을 넘기지 않아야 함

## Leader Commit Index - Applied Index Count Gap 5000 이상 발생한 경우
- Leader는 Network Buffer 크기를 넘기지 않기 위해, SnapShot으로 전체를 압축하여 보냄

## 장애 발생 시나리오 
## 1. Network Buffer OverFlow
### 조건
1. 초기 Gap 발생
   - VM/Pod 재시작
   - 네트워크 일시 단절
   - Disk I/O 병목

2. Gap 증가
   - 새로운 쓰기 계속 유입
   - Apply 처리 지연
   - Gap 5,000 초과

3. Snapshot 전송 실패
   - 버퍼 이미 포화 (4,096/4,096)
   - MsgSnap 추가 불가
   - 드롭 발생

#### 실제 로그 예시
```json
{
  "level": "warn",
  "msg": "dropped internal Raft message since sending buffer is full",
  "message-type": "MsgHeartbeat",
  "remote-peer-active": false
}
```

### Drop 메시지 목록
- 전체 메시지 Drop 가능성 높음
1. MsgApp (로그 복제)
2. MsgSnap (SnapShot)
3. MsgHeartbeat (Health Check)

## 대응 방안
### 1. Gap Size 모니터링(예방)
- Commit-Applied Index Count를 상시 모니터링하며 최대 Gap 크기인 5,000 넘기지 않도록 조정

### 2. 즉시 조치 방법
- Gap 발생 ETCD 노드 분리 후 재 시작

### 3. 재발 방지대책
#### 리소스 확보
```yaml
# ETCD Pod 리소스 증설
resources:
  requests:
    cpu: "2"        # 1 → 2 core
    memory: "4Gi"   # 2 → 4 GB
  limits:
    cpu: "4"
    memory: "8Gi"
```

#### 설정 튜닝
```yaml
# ETCD 설정
etcd-arg:
  # Snapshot 생성 빈도 증가
  - "snapshot-count=50000"
    # 기본: 100,000개
    # 변경: 50,000개
    # 목적:
    #   1. 디스크 공간 절약 (WAL 파일 정리 빈도 증가)
    #   2. 메모리 사용량 감소 (메모리 내 로그 개수 감소)
    #   3. 재시작 시 복구 속도 개선 (WAL 재생 개수 평균 감소)
    # 주의:
    #   - Snapshot 생성 시 CPU/디스크 I/O 발생
  
  # Heartbeat 전송 간격 증가
  - "heartbeat-interval=200"
    # 기본: 100ms
    # 변경: 200ms
    # 목적:
    #   1. Heartbeat 메시지 빈도 50% 감소
    #   2. Network Buffer 공간 확보로 OverFlow 압박 완화
    #   3. 네트워크 일시적 지연 허용
    # 주의:
    #   - 장애 감지 시간 100ms → 200ms로 증가
  
  # Election Timeout 증가
  - "election-timeout=2000"
    # 기본: 1000ms
    # 변경: 2000ms
    # 목적:
    #   1. Leader 일시적 과부하 시 불필요한 Election 방지
    #   2. 버퍼 오버플로우로 인한 Heartbeat 드롭 허용
    #   3. 클러스터 안정성 증가
    # 주의:
    #   - 실제 Leader 장애 시 복구 시간 1초 증가
```

## 참고 자료
- [kakao tech-Kubernetes 운영을 위한 etcd 기본 동작 원리의 이해](https://tech.kakao.com/posts/484)