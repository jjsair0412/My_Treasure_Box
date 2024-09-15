# kubesphere 배포 시 에러 종료
## 1. prometheus pod pv 생성 안돼는 에러
prometheus pv를 동적 프로비저닝해주지 못해서 kubesphere는 정상 배포되었음에도 모니터링이 안됄 때 아래 예시로 pv 생성

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: prom-pv
  namespace: kubesphere-monitoring-system
spec:
  storageClassName: local-storage
  capacity:
    storage: 20Gi
  accessModes:
    - ReadWriteOnce
  hostPath:
    path: "/mnt/data"
```