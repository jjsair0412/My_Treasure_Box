# Helm 사용법
- 해당 문서는 Helm chart의 사용법과 이론을 정리해 둔 문서 입니다.
## Helm 설치

[Installing Helm](https://helm.sh/docs/intro/install/#helm)

---

## Helm Chart Start guide

Helm Chart를 사용하고 싶다면 , 파일경로에 맞춰서 생성해줘야 한다. 

**Chart.yaml파일과 , values.yaml파일 , templates 폴더가 있어야 한다.**

Helm 차트 파일 경로 

```bash
mychart/
  Chart.yaml
  values.yaml
  charts/
  templates/
  ...
```

이거 tree구조 말하는거다.

mychart라는 폴더안에 chart.yaml, values.yaml, charts폴더 , templates폴더가 위치해야 한다.

template 생성 방법
일단 yaml파일들을 만들어준다. ( deployment.yaml , service.yaml )
yaml들을 template 폴더로 이동시킨다.

이제 설치에 필요한 필수파일들이 필요하다.

차트 가이드에 보면 values.yaml과 chart.yaml이 있다.
***values.yaml파일***
: template에 있는 값을 동적으로 변경하고 싶을 때 사용하는 파일

***chart.yaml파일***
: helm차트의 버전, 이름 등 메타정보를 저장한 파일

**values.yaml과 chart.yaml은 꼭 생성해주어야 한다.**

**생성 위치는 template 디렉토리와 동등위치에 있어야 한다.**

---

## Chart.yaml

[Charts](https://helm.sh/docs/topics/charts/#the-chartyaml-file)

```yaml
apiVersion: The chart API version (required)
name: The name of the chart (required)
version: A SemVer 2 version (required)
kubeVersion: A SemVer range of compatible Kubernetes versions (optional)
description: A single-sentence description of this project (optional)
type: The type of the chart (optional)
keywords:
  - A list of keywords about this project (optional)
home: The URL of this projects home page (optional)
sources:
  - A list of URLs to source code for this project (optional)
dependencies: # A list of the chart requirements (optional)
  - name: The name of the chart (nginx)
    version: The version of the chart ("1.2.3")
    repository: (optional) The repository URL ("https://example.com/charts") or alias ("@repo-name")
    condition: (optional) A yaml path that resolves to a boolean, used for enabling/disabling charts (e.g. subchart1.enabled )
    tags: # (optional)
      - Tags can be used to group charts for enabling/disabling together
    import-values: # (optional)
      - ImportValues holds the mapping of source values to parent key to be imported. Each item can be a string or pair of child/parent sublist items.
    alias: (optional) Alias to be used for the chart. Useful when you have to add the same chart multiple times
maintainers: # (optional)
  - name: The maintainers name (required for each maintainer)
    email: The maintainers email (optional for each maintainer)
    url: A URL for the maintainer (optional for each maintainer)
icon: A URL to an SVG or PNG image to be used as an icon (optional).
appVersion: The version of the app that this contains (optional). Needn't be SemVer. Quotes recommended.
deprecated: Whether this chart is deprecated (optional, boolean)
annotations:
  example: A list of annotations keyed by name (optional).
```

예제보면 required라고 적혀잇는 것을 볼 수 있는데 , 얘네는 필수로 작성해주어야 한다.

apiversion도 나와있다.

[Charts](https://helm.sh/docs/topics/charts/#the-apiversion-field)

헬름차트의 **yaml파일은 _ 보단 - 를 쓰는것을 권장**한다.

![헬름_최소_트리구조][헬름_최소_트리구조]

[헬름_최소_트리구조]:./images/헬름_최소_트리구조.PNG

---

## Helm Chart install

[시작하기](https://helm.sh/ko/docs/chart_template_guide/getting_started/)

```bash
$ helm install <release-name> <chart_경로>

# 사용 예시
$ helm install test .
```

---

## Helm list

얘는 실패하던 성공하던 다 보인다.

```bash
$ helm list
```

---

## Helm namespace

헬름은 namespace를 지정하는것 또한 가능하다.

![helm_list_result][helm_list_result]

[helm_list_result]:./images/helm_list_result.PNG

좌측부터 릴리즈 이름, namespace, REVISION , update 시간 , 상태 , chart 이름 , api version 정보

```bash
# test namespace의 helm 조회
$ helm list -n test
```

---

## Helm delete

```bash
$ helm delete <release 이름>

# usecase
$ helm delete v1
```

---

## 다양한 template 문법들

[Charts](https://helm.sh/docs/topics/charts/#predefined-values)

---

## template 문법 - values.yaml ?

values.yaml파일을 조작해서 **template 내부 리소스들의 설정값을 변경**시킬 수 있다.
예를들어 **이미지나 서비스 타입들을 지정**해줄 수 있다.

그래서 만약 **deployments의 replicas 개수가 2개라면 각 pod들에게 다른 type을 줄 수 있다.**

이건 바로쓸수는 없고 , **준비과정이 필요**하다.

1. **동적 수정하고싶은 위치에 템플릿 문법을 사용**해야 한다.
    
    
    예제코드 : templates/deployment.yaml
    
    ```yaml
    apiVersion: apps/v1
    kind: Deployment
    metadata:
      name: hello
      labels:
        app: hello 
    spec:
      replicas: 3
      selector:
        matchLabels:
          app: nginx
      template:
        metadata:
          labels:
            app: nginx
        spec:
          containers:
          - name: nginx
            image: {{ .Values.image }} # values.yaml파일의 값 받아오는 부분
            ports:
            - containerPort: 80
            env:
            - name: hello
              value: world
    ```
    

1. values.yaml파일에는 필드와 값을 적어준다.
    
    
    예제코드 : values.yaml
    
    ```yaml
    image: nginx:stable
    ```
    

이렇게해주면 ****

**deployment의 image쪽이 헬름에서 install될 때 values.yaml파일의 image 값으로 등록되어 install** 된다.

---

## template 문법 - release

이전 **values.~ 하는 template 문법은 동적으로 값을 변경시킬때 사용하는 template문법**이다.

기존 **helm차트는 문제가 있었다.**

**yaml파일 이름이 고정되어 있어서 , relesase는 1개만 가능**하다는것이다.

예를들어 **helm을 가지고 install을 시켰다면 똑같은 helm으로 relesase할 수 없다는 문제점**이다.

이걸 release template 문법으로 해결이 가능하다.

예제처럼 {{ .Release.Name }} 을 적어놔주면 helm install 할때 생긴 release 이름이 

{{ .Release.Name }} 으로 넣어지게 된다. 

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ .Release.Name }}
  labels:
    app: {{ .Release.Name }} 
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: {{ .Values.image }} 
        ports:
        - containerPort: 80
        env:
        - name: hello
          value: world
```

---

## namespace 변경

```bash
$ helm install --namespace < namespac> <relase-Name>

# 사용 예시
$ helm install -n test t1 .
```

이것도 yaml파일에서 tepmlate 언어로 Release yaml파일을 작성해주어야 한다.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:	
  namespace: {{ .Release.Namespace }}
  name: {{ .Release.Name }}
  labels:
    app: {{ .Release.Name }} 
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: {{ .Values.image }} 
        ports:
        - containerPort: 80
        env:
        - name: hello
          value: world
```

test라는 namespace에 t1 relase name으로 헬름차트 install이라는 의미

---

## Helm Debugging command

현재 release 정보 확인 명령어

```bash
$ helm get all -n <namespace> <relase>

# 사용 예시
$ helm get all -n test t1
```

**명령어 해석** 
test namespace에 t1 릴리즈들을 보는 명령어

내가 작성한 차트 문법을 검사할 수 있는 명령어

```bash
$ helm lint
```

내가 작성한 템플릿과 values.yaml 파일값이 적용된 결과를 보고 싶을 때 사용하는 명령어

```bash
$ helm template -n <namespace> <release> <chart-path>

# 사용 예시
$ helm template -n test t2 .

# 요렇게 작성하면 된다. 
# 내가 위치한 경로와 헬름경로가 동일하다면 
```

---

## values.yaml override

helm install할 때 value.yaml 파일의 값을 변경하고 싶을 때 사용하는것

2가지 방법이 있다.

1. -**-set 인자 사용**

```bash
$ helm install --set <change_value> -n <namespace>

# 사용 예
$ helm install --set image=nginx:latest -n test latest .
```

기존 values.yaml은 image값이 stable인데 , latest로 변경

1. **파일을 이용하는 방법**

```bash
$ helm install -f <파일경로> -n <namespace> <relase-name> <path>

# 사용 예
$ helm install -f override_values.yaml -n test t3 .
```

override_values.yaml파일을 읽어와서 해당 파일값의 정보로 변경

---

## helm upgrade 명령어

기존에 배포된 helm release 내용을 변경시키는것

1. **values.yaml파일 변경**

```bash
$ helm upgrade --set <바꿀값> -n <namespace> <relase-name> <path>

# 사용 예
$ helm upgrade --set image=nginx:1.21.4 -n test v1 .
```

upgrade하면 revision값이 1씩 올라간다.

1. **template 변경**

template 내부 파일을 vi명령어로 변경시킨 후
**upgrade 명령어로 변경시킨다면 변경된값으로 바뀐다.**

```bash
$ helm upgrade -n test v1 .
```

그리고 revision 값또한 +1

※ upgrade 할때는 이전 revision값을 가지고온다.

**helm upgrade시 주의사항**
upgrade하면 정책에 따라 **Pod가 재기동**된다.
**helm3와 helm2의 upgrade 결과는 다르다.**

---

## Helm rollback

[Helm Rollback](https://helm.sh/ko/docs/helm/helm_rollback/)

```bash
$ helm rollback v1 -n test 1
```

test namespace에 있는 v1이라는 release를 1 revision으로 rollback한다.