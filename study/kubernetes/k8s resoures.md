# k8s resources
해당 문서는 k8s의 모든 resource에 대한 내용을 담고있지 않습니다.
공부하면서 몰랏던 부분을 정리 해 둔 문서 입니다.
### 1. Deamonset
#### 1.1 Deamonset + nodeSelector
Deamonset resource는 기본적으로 모든 노드에 파드가 생성됩니다.

그러나 nodeSelector를 같이 선언할 수 있기 때문에 , 특정 label이 붙어있는 node에만 Deamonset 파드를 유지시킬 수 있습니다.

아래 예제 Deamonset은 k8s node 중 label이 disk=ssd인 node에만 Deamonset pod를 유지할 수 있습니다.
```yaml
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: ssd-ds
spec:
  selector:
    matchLabels:
      app: ssd-ds 
  template:
    metadata:
      labels:
        app: ssd-ds
    spec:
      nodeSelector:
        disk: ssd # node중 label이 disk=ssd 인 node에만 파드 생성
      containers:
      - name: main
        image: image
```

### 2. Job
#### 2.1 batch job 순차 실행
batch job의 컨테이너를 여러번 순차적으로 실행할 수 있습니다.
**spec.completions**을 추가하면 , value 숫자에 맞춰서 몇번 실행할지를 선언할 수 있습니다.
```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: batch-job
spec:
  completions: 3 # 3번 순차 실행
  template:
    metadata:
      labels:
        app: batch-job
    spec:
      restartPolicy: OnFailure
      containers:
      - name: main
        image: image
```
#### 2.2 batch job 병렬 실행
**spec.completions**과 함께 **spec.parallelism** 옵션을 추가하면 , 몇개의 파드를 병렬 실행할것인지를 선언할 수 있습니다.
```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: batch-job
spec:
  completions: 3 # 3번 순차 실행
  parallelism: 2 # 최대 2개 파드가 병렬 실행
  template:
    metadata:
      labels:
        app: batch-job
    spec:
      restartPolicy: OnFailure
      containers:
      - name: main
        image: image
```
#### 2.3 batch job 실행 시간 제한
**spec.activeDeadlineSeconds**를 선언하여 해당 시간 이상으로 파드가 실행되면 job을 실패한것으로 간주할 수 있습니다.

**spec.backoffLimit**를 선언하여 실패한 것으로 표시되기 전에 , job을 재 실행 하는 횟수를 설정할 수 있습니다. default : 6
```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: batch-job
spec:
  backoffLimit: 3 # 실패 전 3번 실행 , default 6
  activeDeadlineSeconds: 30 # 30초 이상 실행하면 job 실패로 처리
  completions: 3 # 3번 순차 실행
  parallelism: 2 # 최대 2개 파드가 병렬 실행
  template:
    metadata:
      labels:
        app: batch-job
    spec:
      restartPolicy: OnFailure
      containers:
      - name: main
        image: image
```
### 3. CronJob
#### 3.1 cronjob 데드라인 설정
cronjob에서 예정된 시간을 너무 초과하여 실행될 경우 , **spec.startingDeadlineSeconds** 필드를 선언하여 job을 실패로 간주하게끔 할 수 있습니다.

아래 예제는 15로 선언되었기에 , 매 시간의 15분 15초 까지 job이 시작하지 않으면 job이 시작되지 않고 실패로 표시됩니다.
```yaml
apiVersion: batch/v1beta1
kind: CronJob
metadata:
  name: batch-job-every-fifteen-minutes
spec:
  schedule: "0,15,30,45 * * * *" # 15분마다 실행 , 분 시 일 월 요일 순서
  startingDeadlineSeconds: 15
  jobTemplate:
    spec:
      template:
        metadata:
          labels:
            app: periodic-batch-job
        spec:
          restartPolicy: OnFailure
          containers:
          - name: main
            image: image
```