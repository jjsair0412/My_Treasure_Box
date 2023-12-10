# Image Pull Secret 전역 관리
docker image를 private registry에 push해서 관리할 경우, deployment 등의 kubernetes resource가 image를 pull 할 때,(정확히는 kubelet) image pull secret을 따로따로 주어야하는 불편함이 있습니다.

그러나 serviceaccount 자체에 imagePullSecret을 반영하여, 해당 sa를 할당받은 리소스에 대해서 전역적으로 pull secret을 넣어줄 수 있습니다.
## 사용방안
### 1. image pull secret 생성
docker config를 담고있는 secret으로 image pull secret을 생성합니다.

```bash
kubectl create secret generic my-dockerhub-secret \
    --from-file=.dockerconfigjson=<path/to/.docker/config.json> \
    --type=kubernetes.io/dockerconfigjson
```

### 2. 생성한 secret을 sa에 바인딩
ServiceAccount를 정의할 때, pull secret을 할당합니다.

SA의 pull secret은 마운트된 pull secret과 다르게 동작합니다. 각각의 파드가 사용 가능한 pull secret을 사용할 수 있는지를 결정하는것이 아니라, SA를 사용해 특정 ImagePullSecret을 자동으로 추가합니다.

따라서 생성한 SA를 마운트한 파드는, SA에 먹힌 imagePullSecrets 을 사용하게 됩니다.
```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: my-service-account
imagePullSecrets:
- name: my-dockerhub-secret
```

해당 SA를 사용하는 Pod를 생성합니다.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: jinseong-test
spec:
  serviceAccountName: my-service-account # SA 할당
  containers:
  - name: main
    image: tutum/curl
    command: ["sleep", "9999999"]
  - name: test
    image: busybox
```
