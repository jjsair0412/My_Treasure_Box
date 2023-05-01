# Kong_Basic_information
해당 문서는 Kong Gateway를 공부한 내용을 작성한 문서입니다.

기초 설치방안 및 사용방안 또한 해당 디렉터리에 위치합니다.

## 이론
### Kong API Gateway란 ?
콩 게이트웨이는 , MSA 및 분산 아키텍처에 최적화된 하이브리드 및 멀티 클라우드용으로 만들어진 API Gateway 입니다.
- **nginx ingress controller와 같은 api-gateway지만 , 둘은 특성이 다르기때문에 사용 용도가 다릅니다. -> 다른 문서에서 정리 예정** 

콩 게이트웨이는 
nginx를 기반으로 하기 때문에 , nginx ingress의 기본 기능인 역방향 프록시를 토대로 , 다양한 모듈 및 플러그인을 통해 기능을 확장할 수 있습니다.

콩 게이트웨이는 **nginx에서 실행되는 Lua Application입니다.**
- Lua Application이란 , Lua 언어를 사용해서 작성된 App이라는것을 의미합니다.
- 그런 맥락에서 콩은 , Nginx를 기본 웹 서버로 사용하면서 (프록시), Nginx 내부에서 실행하는 Lua 스크립트를 , 사용자 지정 Lua 플러그인으로 기능을 확장한것 입니다.
- Kong Gateway가 Nginx 내에서 Lua 스크립트를 플러그인으로 사용하여 기능을 확장하고 사용자 지정 API 관리 기능을 제공한다는 의미 -> by chatGPT 

콩 게이트웨이는 플러그인 확장을 위해 Lua 언어만을 지원하진 않습니다.
사용자 지정 플러그인을 직접 코딩하여 제작할 수 있습니다. - 아래 참고
- [plugin development guide in kong-docs](https://docs.konghq.com/gateway/3.2.x/plugin-development)
- [PDK Docs-lua 언어로 개발할 경우..](https://docs.konghq.com/gateway/3.2.x/plugin-development/pdk/)

### Kong API Gateway의 특징
1. 워크플로우를 자동화하고, 모던 GitOps 사례들을 활용
2. 애플리케이션/서비스를 분산화하고 마이크로서비스로 전환
4. API 관련 이상 및 위협을 사전 식별
5. API/서비스를 보호 및 관리, API 가시성 확대

### Kong API Gateway Features
콩은 엔터프라이즈와 무료버전이 나뉘어져 있으며 ,. 기능차이가 있습니다.
- 연습할땐 당연히 Open Source로 ..
- [Kong Enterprise vs Open Source](https://docs.konghq.com/gateway/3.2.x/#features)

### Kong API Gateway 동작 방식
콩 게이트웨이를 관리(Kong 설정 변경 및 모니터링)는 Kong admin API로 REST API 통신하는 방법으로 하게 됩니다.

콩 게이트웨이에서 발생하는 api 요청들의 가시성을 확대하기 위해, Kong은 Kong Manager라는 GUI 툴을 제공합니다.
- Free
- [Kong GUI - Kong docs](https://docs.konghq.com/gateway/3.2.x/#kong-manager)

### Kong with Kubernetes
Kong은 api gateway기 때문에 , k8s ingress컨트롤러를 제공합니다.

k8s 위에 helm으로 Kong을 배포하고 , 사용하는 방안과 , 기본 베어메탈 환경에서 Kong을 사용하는 방안 두가지 모두 문서화할 예정입니다.

### Kong API Gateway 설치 방안
콩은 설치방법이 두가지로 나뉩니다.
1. cloud-hosted with Kong Konnect
2. on-premises

## Kong DB less vs DB mode
Kong Gateway는 plugin , route , service와 같은 엔티티들을 저장하기 위해서 , DB가 필요합니다.
- Kong Gateway는 kong.conf 파일을 통해 DB 및 다양한 설정 정보를 지정합니다.

그러나 Kong Gateway는 DB 없이 실행하는 DB-less 모드 또한 지원합니다.

Kong의 DB-less 모드는  Kong 1.1 이후 버전에서 Declarative Config 와 함께 사용 가능하며, 이는 YAML 또는 JSON file 에 설정 정보를 저장하여 사용하는 방식 입니다.

DB-less 모드의 이점은 다음과 같습니다.
- 종속성 감소
    - Back end DB를 관리할 필요가 없어지기 때문에 편리합니다. -> 리소스 절약 가능
- CI/CD 자동화
    - Kong 설정파일 자체를 Yaml이나 json 한곳에서 사용함으로써 배포 파이프라인을 자동화할 수 있습니다.

단점은 다음과 같습니다.
- DB 의존적 Plugin 사용 불가
- RESTful API를 통해 Kong Cluster를 관리할 수 있는 Admin API 사용 불가
    - 읽기 전용으로만 사용이 가능합니다.
- Kong 설정 파일을 변경하려면 conf를 수정한 후 재 배포해야 합니다.

