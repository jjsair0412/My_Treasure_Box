# crd 개발 과정 - golang
해당 폴더에서는 실제 cr , crd를 yaml파일로 정의한 뒤 , ( etcd에 등록 ) 

golang용 operator를 커스터마이징 하여 해당 cr를 통해서 k8s object를 컨트롤하는 실습 입니다.

## 1. 개발 환경
- macOS m1
- IDE Tool : vscode
- Golang : version go1.19.3 darwin/amd64
- operator version : [operator sdk](https://github.com/operator-framework/operator-sdk)