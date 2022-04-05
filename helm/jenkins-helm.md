# Jenkins install
**reference**
https://www.jenkins.io/doc/book/installing/kubernetes/


## 1.  Prerequisites
+ ### Create a namespace
```
$ kubectl create namespace jenkins 

# check
$ kubectl get namespaces
```
## Install Jenkins with Helm v3
### 1. jenkins repository 추가
``` 
$ helm repo add jenkinsci https://charts.jenkins.io
$ helm repo update
```
#
### 2. create persistent volume 
```
apiVersion: v1
kind: PersistentVolume
metadata:
  name: jenkins-pv
  namespace: jenkins
spec:
  storageClassName: jenkins-pv
  accessModes:
  - ReadWriteOnce
  capacity:
    storage: 20Gi
  persistentVolumeReclaimPolicy: Retain
  hostPath:
    path: /data/jenkins-volume/
    
    
# apply file
$ kubectl apply -f jenkins-volume.yaml
```
#
### 3. create service account
service account는 필요에 맞게끔 role을 변경시켜서 만든다.
```
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: jenkins
  namespace: jenkins
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  annotations:
    rbac.authorization.kubernetes.io/autoupdate: "true"
  labels:
    kubernetes.io/bootstrapping: rbac-defaults
  name: jenkins
rules:
- apiGroups:
  - '*'
  resources:
  - statefulsets
  - services
  - replicationcontrollers
  - replicasets
  - podtemplates
  - podsecuritypolicies
  - pods
  - pods/log
  - pods/exec
  - podpreset
  - poddisruptionbudget
  - persistentvolumes
  - persistentvolumeclaims
  - jobs
  - endpoints
  - deployments
  - deployments/scale
  - daemonsets
  - cronjobs
  - configmaps
  - namespaces
  - events
  - secrets
  verbs:
  - create
  - get
  - watch
  - delete
  - list
  - patch
  - update
- apiGroups:
  - ""
  resources:
  - nodes
  verbs:
  - get
  - list
  - watch
  - update
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  annotations:
    rbac.authorization.kubernetes.io/autoupdate: "true"
  labels:
    kubernetes.io/bootstrapping: rbac-defaults
  name: jenkins
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: jenkins
subjects:
- apiGroup: rbac.authorization.k8s.io
  kind: Group
  name: system:serviceaccounts:jenkins


# apply file
$ kubectl apply -f jenkins-sa.yaml
```
#
### 4.  jenkins 설치

- 명령어로 설치하는 방법. 
- values.yaml파일의 속성값을 변경하면서 명령어로도 설치가 가능하다.
```
helm upgrade --install jenkins . \
--namespace=jenkins \
--set controller.jenkinsUrl=https://jenkins.heun.leedh.xyz \
--set persistence.existingClaim="jenkins-pvc" \ # pvc name
--set persistence.accessMode="ReadWriteMany" \ # pv access mode
--set serviceAccount.name=jenkins \ # service account 이름 들어감
--set serviceAccount.create=false \ # service account 생성 false
-f values.yaml
```



- value.yaml파일을 생성하는 방법
- 필요에 맞게끔 설정값을 변경한다.
```
# Default values for jenkins.
# This is a YAML-formatted file.
# Declare name/value pairs to be passed into your templates.
# name: value

## Overrides for generated resource names
# See templates/_helpers.tpl
# nameOverride:
# fullnameOverride:
# namespaceOverride:

# For FQDN resolving of the controller service. Change this value to match your existing configuration.
# ref: https://github.com/kubernetes/dns/blob/master/docs/specification.md
clusterZone: "cluster.local"

renderHelmLabels: true

controller:
  # Used for label app.kubernetes.io/component
  componentName: "jenkins-controller"
  image: "jenkins/jenkins"
  # tag: "2.332.1-jdk11"
  tagLabel: jdk11
  imagePullPolicy: "Always"
  imagePullSecretName:
  # Optionally configure lifetime for controller-container
  lifecycle:
  #  postStart:
  #    exec:
  #      command:
  #      - "uname"
  #      - "-a"
  disableRememberMe: false
  numExecutors: 0
  # configures the executor mode of the Jenkins node. Possible values are: NORMAL or EXCLUSIVE
  executorMode: "NORMAL"
  # This is ignored if enableRawHtmlMarkupFormatter is true
  markupFormatter: plainText
  customJenkinsLabels: []

```
```
# 차트 install
$ helm upgrade --install jenkins -n jenkins -f jenkins-values.yaml
```

# 


-   아래와 같은 에러메세지가 발생 할 경우 values.yaml에 아래 라인을 주석 해제
    -   /var/jenkins_config/apply_config.sh: 4: cannot create /var/jenkins_home/jenkins.install.UpgradeWizard.state: Permission denied

```
  podSecurityContextOverride:
    runAsUser: 1000
    runAsGroup: 1000
    runAsNonRoot: true
    fsGroup: 1000
    fsGroupChangePolicy: "OnRootMismatch"

```
#

### 5. ingress 구성
```
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  namespace: jenkins
  name: minimal-ingress
  annotations:
    kubernetes.io/ingress.class: "nginx"
    ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
spec:
  rules:
  - http:
      paths:
      - path: / 
        pathType: Prefix
        backend:
          service:
            name: jenkins 
            port:
              number: 8080 
    host: jenkins.apps.ks.leedh.xyz
```