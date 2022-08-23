# jenkins air gap 설치 방안 - helm chart
## 1. Prerequisites
- 해당 문서는 jenkins를 폐쇄망에서 helm chart로 설치하는 방안에 대해 기술합니다.
## 2. jenkins helm install
### 2.1 namespace 생성
- jenkins 관리용 namespace를 생성합니다.
```
$ kubectl create ns jenkins
```
### 2.2 values.yaml 수정
- jenkins를 air gap 환경에서 설치하기 위해선 , 특정 설정들을 변경시켜주어야 합니다.
- jenkins pod가 생성될 때 , initcontainer가 올라가게 되는데 , 해당 initcontainer에서 jenkins의 plugin을 update center로 요청을 보내며 설치하게 됩니다.
- air gap 환경이기에 update center에서 json 파일을 받아올 수 없어 아래처럼 수정하지 않는다면 , initcontainer에서 get 에러가 발생하게 됩니다.
- 또한 harbor 등과 같은 private registry에서 image를 받아와야 하기에 , repository 정보도 수정합니다.
```
$ cat setting-values.yaml
controller:
  image: private_registry_url.com/jenkins/jenkins
  tag: 2.303.2-jdk11
  installPlugins: false # Plugins 설정을 false로 두고 설치하게 되면 , initcontainer가 update center에서 json 파일 ( plugins ) 을 받아오려고 요청을 보내지 않습니다.
  sidecars:
      configAutoReload:
      enabled: true
      image: private_registry_url.com/kiwigrid/k8s-sidecar:1.14.2

```
### 2.3 helm install
- helm chart로 jenkins를 설치합니다.
```
$ helm upgrade --install jenkins . -n jenkins -f values.yaml, setting-values.yaml
```
### 2.4 ingress 구성
- jenkins ingress를 구성합니다.
```
$ cat jenkins-ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: jenkins-ingress
  namespace: jenkins
  annotations:
    kubernetes.io/ingress.class: "nginx"
    ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/proxy-body-size: 1000M # body max size 조절
spec:
  tls:
  - hosts:
    - jinseong.xxx.com
    secretName: ingress-tls
  rules:
  - host: jinseong.xxx.com
    http:
      paths:
      - pathType: Prefix
        path: /
        backend:
          service:
            name: jenkins
            port:
              number: 8080
```
## 3. jenkins 플러그인 설치 방안
- air gap 환경에서 jenkins 플러그인을 설치하는 방법에 대해 기술합니다.
- 플러그인을 설치하는 방법은 총 두가지가 있습니다.
1. jenkins 설치 후 plugin jpi파일 복사 후 업로드
2. 모든 plugin 직접 설치
- 2번의 방법은 의존성을 다 맞춰주어야 하기에 불편합니다. 따라서 1번의 방법을 권장합니다
### 3.1 jenkins 설치 후 plugin jpi파일 복사 업로드 방법
1. 인터넷이 열려있는 환경에서 jenkins를 설치합니다.
2. jenkins ui 접근 후 원하는 플러그인을 설치합니다.
3. jenkinsHome/plugins/ 경로에 위치한 jpi 파일을 모두 복사합니다.
4. 파일의 확장자를 hpi로 변경합니다.
    아래 명령어를 통해서 확장자를 한번에 변경합니다.
```
$ ls | grep '.jpi' | cut -d . -f 1 | while read line; do mv $line.jpi $line.hpi; done

출처: [https://server-engineer.tistory.com/763](https://server-engineer.tistory.com/763) [임대리 개발일지:티스토리]
```
5. air gap 환경에 설치한 jenkins에서 , 플러그인 관리 -> 고급 -> 플러그인 올리기 클릭 후 hpi 파일들을 업로드 합니다.
### 3.2 모든 plugin 직접 설치
- jenkins plugin 또한 의존성이 있기 때문에 , 해당 방법은 너무 힘들고 수고스럽습니다.
- 따라서 권장하진 않습니다.
1. https://plugins.jenkins.io/ 에서 원하는 플러그인을 설치합니다. ( *.hpi )
2. 원하는 플러그인의 의존성 정보를 보고 , 모든 의존성을 따라가면서 설치합니다.
3. 3.1 방법과 동일하게 고급 탭에서 플러그인 올리기로 설치한 플러그인을 올립니다.

## trouble shooting
 - 플러그인 수동 설치시 , 플러그인을 웹 사이트에서 올릴 때 nginx-ingress의 body 사이즈의 기본은 1M이기에 413 에러가 발생할 수 있습니다.
 - 따라서 jenkins ingress annotaion에 아래 정보를 추가하여 max-size를 조절합니다.
```
    nginx.ingress.kubernetes.io/proxy-body-size: 1000M # max size 조절
```