
# Blue/Green 배포

- Blue Green 배포 방식을 테스트합니다.

## Prerequisites
- [참고 문서](https://argoproj.github.io/argo-rollouts/)
- Blue-Green 배포는 애플리케이션 또는 마이크로서비스의 이전 버전에 있던 사용자 트래픽을 이전 버전과 거의 동일한 새 버전으로 점진적으로 이전하는 애플리케이션 릴리스 모델입니다. 이때 두 버전 모두 프로덕션 환경에서 실행 상태를 유지합니다.
- 이전 버전을 blue 환경으로, 새 버전은 green 환경으로 부를 수 있습니다. 프로덕션 트래픽이 blue에서 green으로 완전히 이전되면, blue는 롤백에 대비하여 대기 상태로 두거나 프로덕션에서 가져온 후 업데이트하여 다음 업데이트의 템플릿으로 삼을 수 있습니다.
- 출처 : [Blue-Green 배포란 ?](https://www.redhat.com/ko/topics/devops/what-is-blue-green-deployment)
- Blue Green 배포 방식은 service가 두 개 필요합니다.
-- 아래 테스트에서는 bluegreen-service-preview 와 bluegreen-service-active svc 두 가지를 사용합니다.
- Blue Green 배포의 실제 test 코드는 Gitlab joind-helmchart-old의 dev branch를 사용하였습니다.

## 1. argocd rollout 설치
- argocd rollout을 활용해서 blue/green 배포와 카나리아 배포를 수행할 수 있습니다.
### 1.1 rollout 설치
- rollout 관리용 namespace 생성
```
$ kubectl create namespace argo-rollouts
```
- test application 배포용 namespace 생성
```
$ kubectl create ns dev-test
```
- rollout 설치
```
$ kubectl apply -n argo-rollouts -f https://github.com/argoproj/argo-rollouts/releases/latest/download/install.yaml
```
### 1.2 kubectl argo rollouts plugin 설치
- curl 명령을 이용해 kubectl용 plugin을 설치합니다.
```
curl -LO https://github.com/argoproj/argo-rollouts/releases/latest/download/kubectl-argo-rollouts-linux-amd64
```
- kubectl plugin의 권한을 변경합니다.
```
chmod +x ./kubectl-argo-rollouts-linux-amd64
```
- kubectl plugin 파일 경로를 변경합니다.
```
sudo mv ./kubectl-argo-rollouts-linux-amd64 /usr/local/bin/kubectl-argo-rollouts
```
- 설치 결과 확인
```
$ kubectl argo rollouts version
kubectl-argo-rollouts: v1.2.0+08cf10e
BuildDate: 2022-03-22T00:25:11Z
GitCommit: 08cf10e554fe99c24c8a37ad07fadd9318e4c8a1
GitTreeState: clean
GoVersion: go1.17.6
Compiler: gc
Platform: linux/amd64
```
## 2. Blue Green test 수행
### 2.1 test 전 상태 확인
- argocd의 ui를 확인하여 버전과 배포 상태를 미리 확인합니다.
### 2.2 Service 생성
- Blue Green 방식은 Service가 두개 필요하기에 두 개 생성합니다.
1. 이전에 배포된 blue 서비스
2. 새롭게 배포될 Green 서비스
- 이전에 배포된 bluegreen-service-active svc
```
apiVersion: v1
kind: Service
metadata:
  name: bluegreen-service-active
  namespace: {{ .Release.Namespace }}
  labels:
    "app.kubernetes.io/managed-by": "{{ .Release.Service }}"
spec:
  ports:
    - port: {{ .Values.service.port }}
      protocol: {{ .Values.service.protocol }}
      targetPort: {{ .Values.service.targetPort }}
  selector:
    app: {{ .Release.Name }}
  type: {{ .Values.service.type }}
  sessionAffinity: {{ .Values.service.sessionAffinity }}
```
- 새롭게 배포될 bluegreen-service-preview svc
```
apiVersion: v1
kind: Service
metadata:
  name: bluegreen-service-preview
  namespace: {{ .Release.Namespace }}
  labels:
    "app.kubernetes.io/managed-by": "{{ .Release.Service }}"
spec:
  ports:
    - port: {{ .Values.service.port }}
      protocol: {{ .Values.service.protocol }}
      targetPort: {{ .Values.service.targetPort }}
  selector:
    app: {{ .Release.Name }}
  type: {{ .Values.service.type }}
  sessionAffinity: {{ .Values.service.sessionAffinity }}

```
### 2.3 deploy.yaml 수정
- Blue/Green 배포방식에서는 deployment 대신 Rollout을 사용하며 , apiVersion또한 아래와 같이 상이합니다.
```
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: {{ .Release.Name }}
  namespace: {{ .Release.Namespace }}
...
```
- spec.strategy.blueGreen 부분을 추가합니다.
- autoPromotionEnabled 옵션이 true라면 , 배포를 자동으로 수행하고 , false라면 새로운 파드가 생성되고 배포 되기 전 pause 상태로 정지됩니다.
```
...
spec:
  strategy:
    blueGreen:
      activeService: bluegreen-service-active # 이전에 배포된 blue 서비스
      previewService: bluegreen-service-preview # 새롭게 배포될 Green 서비스
      autoPromotionEnabled: true # 배포 자동수행 여부를 설정
...
```

- autoPromotionEnabled 옵션을 false로 주어 Green이 생성된 후 정지상태라면 , 아래 명령어를 사용해 배포를 진행할 수 있습니다.
```
$ kubectl argo rollouts promote {rollout-name}
```

- deploy.yaml의 spec.template.spec.containers.image를 수정합니다. 새롭게 배포될 image는 new_image입니다.

```
...
containers:
- name: {{ .Release.Name }}
# image: {{ .Values.global.registry }}/{{ .Values.global.project }}/{{ .Values.global.image }}:{{ .Values.global.activetag }}
image: {{ .Values.global.registry }}/{{ .Values.global.project }}/{{ .Values.global.image }}:{{ .Values.global.new_image }}
...
```
### 2.4 git lab push
- 변경 결과를 git lab에 push합니다.
### 2.5 argocd ui 확인
- 새로운 pod가 생성되는것을 argo ui를 통해 확인합니다.
### 2.6 argocd ui 확인 - terminating
- 이전버전 image를 가지고 있는 파드가 terminating 되는것을 확인합니다.
### 2.7 Blue Green 방식으로 update 완료
- 새로운 버전의 image를 가진 Pod만 위치하면서 update가 완료됩니다.
