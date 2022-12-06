# Study information
해당 문서는 storageclass에 관한 내용을 담고 있습니다.

## 1. why storageclass ?
클러스터 관리자가 volume으로 일일히 많은 스토리지를 다 마운트시켜서 생성해 둘 수 있지만 ,
storageclass로 미리 스토리지클래스 타입으로 정의해 두고
pvc를 생성할 때 storageclass를 참조하면 pv가 자동으로 생성되며 마운트 됩니다. ( 동적 프로비저닝 )

생성되는 pv의 claim 정책은 default로 delete를 갖고 있습니다.

가장 큰 장점은 pv가 부족하지 않다는점입니다.

## 2. pvc에서 storageclassname을 빈 칸으로 둔다면 ?
만약 storageClassName을 빈 칸으로 두면 , 미리 프로비저닝된 PersistentVolume에 바인딩 됩니다.

빈 칸으로 두지 않고 지정하지 않거나 명시한다면 , pv가 동적 프로비저닝 됩니다.
```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: test-pvc 
spec:
  resources:
    requests:
      storage: 1Gi
  accessModes:
  - ReadWriteOnce
  storageClassName: ""
```