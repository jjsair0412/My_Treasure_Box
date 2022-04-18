
# Jenkins install

**reference**

https://www.jenkins.io/doc/book/installing/kubernetes/

  
  

## 1. Prerequisites

+  ### Create a namespace

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

- jenkins helm chart 다운로드

```

$ helm pull jenkinsci/jenkins --version 3.8.5 --untar

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

### 4. jenkins 설치

  

- 명령어로 설치하는 방법.

- values.yaml파일의 속성값을 변경하면서 명령어로도 설치가 가능하다.

```

helm upgrade --install jenkins . -n jenkins --set persistence.existingClaim="jenkins-pvc" --set serviceAccount.name=jenkins --set serviceAccount.create=false -f values.yaml

```

  

#

  
  ## 발생 가능성 있는 문제
### 1. 아래와 같은 에러메세지가 발생 할 경우 
```
/var/jenkins_config/apply_config.sh: 4: cannot create /var/jenkins_home/jenkins.install.UpgradeWizard.state: Permission denied
```
- 위 에러는 jenkins의 persistent volume을 hostPath로 설정한 경우 발생한다.
- 해당 경로의 jenkins 폴더 생성 권한에 대한 문제이다.

  
1. values.yaml에 아래 라인을 주석 해제 및 설정 

```

podSecurityContextOverride:

runAsUser: 1000

runAsGroup: 1000

runAsNonRoot: true

fsGroup: 1000

fsGroupChangePolicy: "OnRootMismatch"

  
 # 그 후 아래처럼 values.yaml파일 설정
 # When setting runAsUser to a different value than 0 also set fsGroup to the same value:
 runAsUser: 1000
 fsGroup: 1000
```
2. 실제 jenkins pod가 올라가있는 node에서 작업
- jenkins pod가 올라가있는 node로 이동
```
jenkins-0       2/2     Running   0          5h8m   10.233.96.37   node2   <none>           <none>
```
- 위처럼 구성됐다면 , node2번으로 이동
- 그 후, hostPath로 만들어준 폴더의 권한을 755로 변경한다.
```
$ cd /data/jenkins-volume/
$ sudo chmod 755 jenkins-volume/       
```

  

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

host: xxx.xxx.jinseong

```

  
  
  

- Jenkins UI 확인

  

- 초기 Password 확인하는 명령어

```

$ kubectl exec --namespace jenkins -it svc/jenkins -c jenkins -- /bin/cat /run/secrets/chart-admin-password && echo

xxxxxxxxxxxxxxx

```

  

Jenkins UI 확인