# kubernetes Audit ( 감사 로그 )
## 1. Prerequisites
- kubernetes의 감사로그 관련 설명과 구성 방법을 설명합니다.
- 감사 로그 정책 파일 (policy.yaml) 은, bastion이 아닌 kube-apiserver.yaml이 위치한 master node에 위치해야 합니다. 따라서 master 노드 모두에게 로깅하고싶다면 , 모든 master node에 감사 로그 정책 파일 (policy.yaml) 이 위치해야 합니다.
- [참고 문서](https://kubernetes.io/docs/tasks/debug-application-cluster/audit/)
## 2. Audit 정책 구성
- k8s에서 요청 흐름은 kube-apiserver를 거치게 됩니다. 이때 HTTP API를 통해 요청이 전달되는데 , audit은 **kube-apiserver를 오가는 API를 로깅하는 것입니다.**


### 2.1 기본적으로 audit 로그는 event 객체 구조로 , 아래와 같은 정보들을 담을 수 있습니다.
1. 콜을 날린 클라이언트가 누구인지
2. 언제 날렸는지
3. 어떤 리소스에대해 날렸는지
4. HTTP 성공 여부
5. verb는 무엇인지
6. URI
7. request body , response body 

등 ..

### 2.2 언제 event 객체를 생성할 지 지정할 수 있습니다.
1.   **RequestReceived**

 -- audit 핸들러가 request를 받자마자
2.   **ResponseStarted**

--   response 헤더만 보내지고, reponse body는 아직 안보내졌을 때. long-running request의 경우에만 발생한다 (예: watch)
3.  **ResponseComplete**

--   response body까지 전부 보내진 후
4. **Panic**

--  panic이 발생 했을 때

### 2.3 어느 정도 수준의 정보를 기록할지도 지정할 수 있습니다.
1. **None**

-- 기록하지 않습니다.
2. **Metadata**

--  request metadata ( requesting user, timestamp, resource, verb, etc ) 들은 기록하지만 , request 또는 response body는 기록하지 않습니다.
3. **Request**

-- 이벤트 metadata 및 request body는 기록하지만 response body는 기록하지 않습니다. non-resource request에는 적용되지 않습니다.
4. **RequestResponse**

-- evnet metatdata , request body 및 response body는 기록합니다. non-resource request에는 적용되지 않습니다.

## 3. Audit 설정
### 3.1 Policy.yaml 생성
- 아래의 예는 공식문서의 예시 입니다.
- 기본적으로 감사 정책은 RBAC과 일치한 구조를 가지고 있습니다.
- 주의할 점은 , **모든 rule은 위에서부터 먼저 해당되는 룰에 적용 된다는 점을 주의**해야 합니다.
- [policy 구성 참조 문서](https://kubernetes.io/docs/reference/config-api/apiserver-audit.v1/#audit-k8s-io-v1-Policy)
```
apiVersion: audit.k8s.io/v1 # This is required.
kind: Policy
# 정보 기록 수준을 작성하는 필드입니다. 아래의 예는 RequestReceived 값을 지정했기 때문에 , event metadata, requestbody, response body는 기록하되 non-resource request는 기록하지 않습니다.
omitStages:
  - "RequestReceived"
rules:
  # RequestResponse 수준에서 포드 변경 사항 기록
  - level: RequestResponse
    resources:
    - group: ""
      # pods에 대해서 metadata 수준의 정보를 로깅합니다.
      resources: ["pods"]
  # pods/log , pods/status 에 대해서 metadata 수준의 정보를 로깅합니다.
  - level: Metadata
    resources:
    - group: ""
      resources: ["pods/log", "pods/status"]

  # controller-leader 이름을 가진 configmap은 로깅하지 않습니다.
  - level: None
    resources:
    - group: ""
      resources: ["configmaps"]
      resourceNames: ["controller-leader"]

  # system:kube-proxy 유저의 endpoints와 services에 대한 watch 요청을 로깅하지 않습니다.
  - level: None
    users: ["system:kube-proxy"]
    verbs: ["watch"]
    resources:
    - group: "" # core API group
      resources: ["endpoints", "services"]

  # /api* . /version 요청에 대해 request 요청자가 authenticated 유저 그룹에 속해있다면 로깅하지 않습니다.
  - level: None
    userGroups: ["system:authenticated"]
    nonResourceURLs:
    - "/api*" # Wildcard matching.
    - "/version"

  # kube-system에서 configmap 변경 사항의 request를 로깅합니다.
  - level: Request
    resources:
    - group: "" # core API group
      resources: ["configmaps"]
    # 이 규칙은 "kube-system" 네임스페이스의 리소스에만 적용됩니다.
    # 빈 문자열 ""은 네임스페이스가 없는 리소스를 선택하는 데 사용할 수 있습니다.
    namespaces: ["kube-system"]

  # metadata 수준에서 다른 모든 네임스페이스의 configmap 및 secrets 변경 사항을 로깅합니다.
  - level: Metadata
    resources:
    - group: "" # core API group
      resources: ["secrets", "configmaps"]

  # Request 수준에서 core 및 extensions의 다른 모든 리소스를 로깅합니다.
  - level: Request
    resources:
    - group: "" # core API group
    - group: "extensions" # Version of group should NOT be included.

  # metadata 수준에서 다른 모든 요청을 로깅합니다.
  - level: Metadata
    # 이 규칙에 해당하는 watch와 같은 장기 실행 request는 RequestReceived에서 감사 이벤트를 생성하지 않습니다.
    omitStages:
      - "RequestReceived"
```
### 3.2 kube-apiserver 수정
- /etc/kubernetes/manifests 폴더 안에 위치한 kube-apiserver.yaml을 수정합니다.
- kube-apiserver는 static pod기에 따로 apply 명령을 수행하지 않고 yaml파일만 수정하면 변경사항이 적용됩니다.
```
$ vi /etc/kubernetes/manifests/kube-apiserver.yaml
```

```
...
spec:
  containers:
  - command:
    - kube-apiserver
    - --audit-log-path=/var/log/apiserver/audit.log
    - --audit-log-maxage=10
    - --audit-log-maxbackup=5
    - --audit-log-maxsize=100
    - --audit-policy-file=/etc/kubernetes/audit-policy.yaml
...
```
- 위 설정처럼 적용합니다.
1. audit-log-path : log 파일 저장 경로 . 해당 flag를 지정하지 않는다면 , 로그 백엔드가 비활성화 됩니다.
2. log-maxage : 오래된 감사 로그 파일을 보관할 최대 일수 지정
3. log-maxbackup : 보유할 감사 로그 파일의 최대 수 지정
4. log-maxsize : 감사 로그 파일의 최대 크기 정의
5. policy-file : 위에서 생성해준 policy 파일 경로 

- 그 후 volume 생성합니다.
```
...
...
    volumeMounts:
    - mountPath: /var/log/apiserver
      name: audit-log
    - mountPath: /etc/kubernetes/audit-policy.yaml
      name: audit-policy
      readOnly: true
...
  volumes:
  - hostPath:
      path: /var/log/apiserver
      type: DirectoryOrCreate
    name: audit-log
  - hostPath:
      path: /etc/kubernetes/audit-policy.yaml
      type: FileOrCreate
    name: audit-policy
...
```
## 4. 적용 결과
- 아래 예는 최소 감사 정책 파일을 사용해서 , metadata 수준에서 모든 요청을 기록한 결과 입니다.
### 4.1 policy.yaml 구성
```
$ /etc/kubernetes/audit-policy.yaml

# Log all requests at the Metadata level.
apiVersion: audit.k8s.io/v1
kind: Policy
rules:
- level: Metadata
```
### 4.2 결과
```
...
{"kind":"Event","apiVersion":"audit.k8s.io/v1","level":"Metadata","auditID":"392999e8-15da-4e81-8ee8-67758ebd23ac","stage":"ResponseComplete","requestURIceVersion=2790677","verb":"list","user":{"username":"system:node:controlpalne-prd-1","groups":["system:nodes","system:authenticated"]},"sourceIPs":["127.amd64) kubernetes/d921bc6","objectRef":{"resource":"csidrivers","apiGroup":"storage.k8s.io","apiVersion":"v1"},"responseStatus":{"metadata":{},"code":20037:28.336915Z","stageTimestamp":"2022-05-19T07:37:28.341682Z","annotations":{"authorization.k8s.io/decision":"allow","authorization.k8s.io/reason":""}}
{"kind":"Event","apiVersion":"audit.k8s.io/v1","level":"Metadata","auditID":"11eabf88-e641-481d-89cc-98b5284417ae","stage":"RequestReceived","requestURI"tchBookmarks=true\u0026resourceVersion=2796543\u0026timeout=9m20s\u0026timeoutSeconds=560\u0026watch=true","verb":"watch","user":{"username":"system:node","system:authenticated"]},"sourceIPs":["127.0.0.1"],"userAgent":"kubelet/v1.21.6 (linux/amd64) kubernetes/d921bc6","objectRef":{"resource":"csidrivers","},"requestReceivedTimestamp":"2022-05-19T07:37:28.342311Z","stageTimestamp":"2022-05-19T07:37:28.342311Z"}
{"kind":"Event","apiVersion":"audit.k8s.io/v1","level":"Metadata","auditID":"30317840-9cb5-4f83-99f3-aa9d43905e85","stage":"RequestReceived","requestURI"system:anonymous","groups":["system:unauthenticated"]},"sourceIPs":["10.250.205.112"],"userAgent":"kube-probe/1.21","requestReceivedTimestamp":"2022-05-15-19T07:37:28.623074Z"}
{"kind":"Event","apiVersion":"audit.k8s.io/v1","level":"Metadata","auditID":"30317840-9cb5-4f83-99f3-aa9d43905e85","stage":"ResponseComplete","requestURI"system:anonymous","groups":["system:unauthenticated"]},"sourceIPs":["10.250.205.112"],"userAgent":"kube-probe/1.21","responseStatus":{"metadata":{},"cod19T07:37:28.623074Z","stageTimestamp":"2022-05-19T07:37:28.626663Z","annotations":{"authorization.k8s.io/decision":"allow","authorization.k8s.io/reason":m:public-info-viewer\" of ClusterRole \"system:public-info-viewer\" to Group \"system:unauthenticated\""}}
{"kind":"Event","apiVersion":"audit.k8s.io/v1","level":"Metadata","auditID":"4734dd72-a6e7-4c6b-920c-6283aa8c2e76","stage":"RequestReceived","requestURI"be-system/leases/kube-controller-manager?timeout=5s","verb":"get","user":{"username":"system:kube-controller-manager","groups":["system:authenticated"]},controller-manager/v1.21.6 (linux/amd64) kubernetes/d921bc6/leader-election","objectRef":{"resource":"leases","namespace":"kube-system","name":"kube-cont.io","apiVersion":"v1"},"requestReceivedTimestamp":"2022-05-19T07:37:29.255574Z","stageTimestamp":"2022-05-19T07:37:29.255574Z"}
{"kind":"Event","apiVersion":"audit.k8s.io/v1","level":"Metadata","auditID":"4734dd72-a6e7-4c6b-920c-6283aa8c2e76","stage":"ResponseComplete","requestURIube-system/leases/kube-controller-manager?timeout=5s","verb":"get","user":{"username":"system:kube-controller-manager","groups":["system:authenticated"]}-controller-manager/v1.21.6 (linux/amd64) kubernetes/d921bc6/leader-election","objectRef":{"resource":"leases","namespace":"kube-system","name":"kube-cons.io","apiVersion":"v1"},"responseStatus":{"metadata":{},"code":200},"requestReceivedTimestamp":"2022-05-19T07:37:29.255574Z","stageTimestamp":"2022-05-1ation.k8s.io/decision":"allow","authorization.k8s.io/reason":"RBAC: allowed by ClusterRoleBinding \"system:kube-controller-manager\" of ClusterRole \"sysem:kube-controller-manager\""}}
...
```
## 5. 참고 사항
- policy 정책을 수정하거나 , log파일을 삭제했을 경우 kube-apiserver.yaml의 등록된 audit 정보를 제거하고 저장한 뒤 , 다시 등록 시켜야 합니다.