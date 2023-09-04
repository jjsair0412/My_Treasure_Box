# elasticSearch helm install
elastic search를 helm chart로 설치할 때 , trouble shooting 방안에 대해 기술합니다.

## 1. storageClass
elastic search 설치할 때 , storageClass를 사용해야 하거나  , readiness probe에서 에러가 난다면 values.yaml에 아래 정보를 추가해야 합니다.

```yaml
volumeClaimTemplate:
  accessModes: [ "ReadWriteMany" ]
  storageClassName: # 추가 .
  resources:
    requests:
      storage: 200Gi


clusterHealthCheckParams: 'wait_for_status=yellow&timeout=1s' # default green


readinessProbe:
  initialDelaySeconds: 120 # DeplaySecond 수정
```
