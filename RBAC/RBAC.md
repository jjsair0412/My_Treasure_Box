# RBAC
#
### reference
https://kubernetes.io/docs/reference/access-authn-authz/certificate-signing-requests/#normal-user

## 1. Create user
**1. 인증서 생성**
```
# key파일 생성 
$ openssl genrsa -out myuser.key 2048 


# key파일을 기준으로 csr파일 생성
$ openssl req -new  -key myuser.key -out myuser.csr -subj "/CN=myuser"  

 ```

**2. kubernetes에 생성한 유저 등록**

- request에 들어갈 인증서 정보 생성 명령어
```
cat <csr_File_Name>  | base64 | tr -d "\n"
```
```
apiVersion: certificates.k8s.io/v1
kind: CertificateSigningRequest
metadata:
  name: infra-user # csr name
spec:
  request: # 방금전 만들어줬던 인증서를 확인해서 인증서 내용을 넣어 주어야 함 
  signerName: kubernetes.io/kube-apiserver-client
  expirationSeconds:  86400 # one day . 유효기간을 의미.
  usages:
  - client auth

```
**3. apply**
```
$ kubectl apply -f <yaml_file_name>
```
**4. 등록상태 확인**
```
$ ﻿kubectl get csr
```
- pending 상태 인 것을 볼 수 있음. 따로 승인을 해주어야 한다.

```
$ ﻿kubectl certificate approve <User_Name>
```
등록상태를 재 확인 하면 , **Approved,Issued** 상태로 변환 된 것을 볼 수 있다.
```
$ ﻿kubectl get csr
```

**6.  crt 파일 생성**
```
$ kubectl get csr <User_Name>  -o jsonpath='{.status.certificate}'| base64 -d >  <crt.file.name>

# use case
$ kubectl get csr myuser -o jsonpath='{.status.certificate}'| base64 -d > myuser.crt
```



## 2. Create Role
**1. Create yaml file**
```
apiVersion: rbac.authorization.k8s.io/v1 
kind: Role 
metadata: 
  namespace: default 
  name: pod-reader 
rules:  
- apiGroups: [""] 
  verbs: ["get","watch","list"...]
  resources: ["pod","service"...]
```
**apiGroups**

- **어디에 해당 Role을 지정할것인가**를 정의
- 비워두면 모든 리소스가 대상이 됨

**verbs**

- **지정한 api그룹에 어떤 권한을 부여할것인가**를 정의

### verb의 종류
| **kind**| **description** |
|--|--|
|**create**|새로운 리소스 생성|
| **get** | 개별 리소스 조회 |
|**list**|여러건의 리소스 조희|
|**update**|기존 리소스 내용 전체 업데이트|
|**delete**|개별 리소스 삭제|
|**deletecollection**|여러 리소스 삭제|



## 3. Create RoleBinding
```
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  creationTimestamp: null
  name: default-rolebinder
  namespace: default # namespace별로 구분
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: Role
  name: vault-all-role # role 이름
subjects:
- apiGroup: rbac.authorization.k8s.io
  kind: User
  name: test-user-va-all # user 이름
```
```
$ kubectl apply -f <rolebinding.yaml_file_name>
```


## 4. Config file 등록
**1. kubectl 명령어를 통해서 context 정보를 확인**
```
$ ﻿kubectl config view
```



**2. 아래의 형식을 통해서 context에 user를 등록**
```
$ ﻿kubectl config set-credentials <User_Name> --client-key=<key_File_Name> --client-certificate=<crt_File_Name> --embed-certs=true

# use case
$ ﻿kubectl config set-credentials myuser --client-key=myuser.key --client-certificate=myuser.crt --embed-certs=true
```


**3. context 추가**
```
﻿$ kubectl config set-context myuser --cluster=<Cluster_Name> --user=<User_Name> 

# use case
﻿$ kubectl config set-context myuser --cluster=kubernetes --user=myuser 
```


**4. context 변경**
```
$ ﻿kubectl config current-context <context_name>
```