# crd 개발 과정 - golang
해당 폴더에서는 실제 cr , crd를 yaml파일로 정의한 뒤 , ( etcd에 등록 ) 

golang용 operator를 커스터마이징 하여 해당 cr를 통해서 k8s object를 컨트롤하는 실습 입니다.

## 1. 개발 환경
- macOS m1
- IDE Tool : vscode
- Golang : version go1.19.3 darwin/amd64
- operator version : [operator sdk](https://github.com/operator-framework/operator-sdk)

## 2. 개발 목표
helloworld kind를 가지는 cr을 개발합니다.

기능은 다음과 같습니다.
helloworld resource를 생성하면

1. nginx image를 가진 deployment 배포
2. 해당 deployment를 expose하는 서비스 생성
- service type은 NodePort 사용
3. log로 "hello world i'm jinseong" 출력

해당 CRD는 namespace에 종속적이게끔 개발합니다.

kubectl로 상태를 확인할 때 , hw라는 단축키로 kube-api server에 요청을 보낼 수 있게 끔 개발합니다.

## 3. 개발 과정
1. CRD yaml파일로 etcd에 정의
2. 정의된 CRD를 kind로 하는 CR yaml로 etcd에 정의
3. CR로 k8s object controller 하기 위해 operator-sdk custom하여 개발
- control 해야 할 object 목록
    - deployment
    - service