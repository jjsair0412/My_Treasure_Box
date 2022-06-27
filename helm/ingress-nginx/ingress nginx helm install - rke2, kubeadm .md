# ingress nginx helm install - rke2, kubeadm 등 ..
- 해당 문서는 Nginx ingress를 helm chart로 설치하는 방법에 대해 설명합니다.
- 해당 문서는 nginx ingress v4.1.4 버전을 기반으로 설치합니다.
## 1. Nginx ingress helm install
### 1.1 add chart repo 
- nginx 관리용 ns 생성
```
$ kubectl create ns ingress-nginx
```
```
$ helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
$ helm repo update
```
- 설치버전 확인 및 latest 버전 다운로드
```
$ helm search repo ingress-nginx/ingress-nginx --versions
$ helm pull ingress-nginx/ingress-nginx --version=4.1.4 --untar
```
- helm install 
- 기본 kubeadm , kubespray 를 통해 설치한 k8s일 경우 아래 명령어를 따릅니다.
```
# 기본 k8s일경우
$ helm upgrade --install ingress-nginx . --namespace ingress-nginx \
--set controller.service.type=NodePort \
-f values.yaml,affinity-values.yaml
```
- 만약 rke2 환경에서 설치할 경우 , 아래 순서를 따릅니다.
- rke2 환경에서 위와 같이 기본 helm install을 진행하면 , pod가 생성되지 않고 아래와같은 에러가 발생합니다.
```
PodSecurityPolicy: unable to admit pod: [spec.containers[0].securityContext.capabilities.add: Invalid value: "NET_BIND_SERVICE": capability may not be added spec.containers[0].securityContext.allowPrivilegeEscalation: Invalid value: true: Allowing privilege escalation for containers is not allowed]
```
- rke2 환경 설치 방안
```
# psp에 privileged를 use하는 role을 추가
$ cat ingress-psp-clusterrole.yaml
kind: ClusterRole
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: ingress-psp-clusterrole
rules:
- apiGroups:
  - extensions
  resources:
  - podsecuritypolicies
  resourceNames:
  - privileged
  verbs:
  - use



# serviceaccounts에 해당 role을 binding 해줍니다.
$ kubectl -n ingress-nginx create rolebinding ingress-psp-clusterrole-rolebinding --clusterrole=ingress-psp-clusterrole --group=system:serviceaccounts:nginx-ingress


# helm install. podsecuritypolicy의 enable속성을 true로 주고 helm intsll 진행
$ helm upgrade --install ingress-nginx . --namespace ingress-nginx \
--set controller.service.type=NodePort \
--set podSecurityPolicy.enabled=true \
-f values.yaml,affinity-values.yaml
```

