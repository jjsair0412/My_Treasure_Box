# Kong's basic function
해당 문서는 Kong Gateway 기본 기능들에 대해 정리해놓은 문서입니다.

Kong은 아래 5가지 기능 이외에 , Kong의 가장 장점인 수많은 plugin들을 통해 다양한 기능을 제공합니다.
- Lua application이기에 확장성또한 뛰어납니다. 사용자가 직접 plugin을 코딩하여 만들 수 도 있습니다.

## 1. 서비스 라우팅
Kong API Gateway는 기본적으로 **서비스**와 **라우팅** 이라는 계념을 사용합니다.

![Kong_service_and_Route][Kong_service_and_Route]

[Kong_service_and_Route]:./images/Kong_service_and_Route.png

**서비스**는 Kong Gateway에서 가지고있는 기본 URL 입니다.

서비스는 upstream Application의 URL 입니다.

만약 Backend Application의 URL이 아래와 같다면 ,
```bash
http://example.jjs.com/
```
서비스는 아래와 같습니다.
```bash
http://example.jjs.com/
```

**route**는 BackEnd Application 내의 리소스 경로 입니다.

Application에 액세스 하기 위해 route가 서비스에 연결되며 ,. 해당 route는 BackEnd Application의 endpoint에 매핑됩니다.

route의 예 :
```bash
http://example.jjs.com/hi


http://example.jjs.com/hello


http://example.jjs.com/hello/jinseong
```
route의 구성은 다음 요소들을 가질 수 있습니다.
1. 프로토콜 : 업스트림 애플리케이션과 통신하는 데 사용되는 프로토콜입니다.
2. 호스트 : 경로와 일치하는 도메인 목록
3. 메서드 : 경로와 일치하는 HTTP 메서드
4. 헤더 : 요청 헤더에 예상되는 값 목록
5. 리디렉션 상태 코드 : HTTPS 상태 코드
6. 태그 : 경로를 그룹화하기 위한 선택적 문자열 세트

## 2. 속도 제한
- Kong의 속도 제한 기능은 특정 plugin으로 제공되며 , 설치해아합니다.

enterprise version kong에선 아래 기능과 더불어 더 향상된 기능들을 사용할 수 있지만.... 공부목적이기에 쓸 수 없습니다.

Kong은 특정 route로 들어오는 요청 수를 제한하여 , Dos 공격을 방지하고 요청을 남용하여 풀이 꽉 차는것을 방지할 수 있습니다.

Kong에선 plugin을 통해 api 속도 제한을 걸 수 있으며 , enterprise version에서 쓸 수 있는 속도 제한 plugin과 free버전이 나뉘어져 있습니다.

속도 제한은 전체 서비스 ( 글로벌 ) 로 먹이거나 , route 별로도 먹일 수 있으며 , 요청마다 Kong에 Consumer를 생성해서 , 특정 Consumer에게 속도 제한을 먹일 수 도 있습니다.


## 3. 프록시 캐싱을 통한 캐시서버 사용
- Kong의 프록시 캐싱 기능은 기본적으로 제공되는 plugin으로 사용할 수 있습니다.

Kong은 프록시 캐싱을 통해서 Kong Gateway가 대신 캐싱된 결과로 main페이지를 응답하게끔 사용할 수 있습니다.
- 캐시서버로 활용 가능

Kong의 프록시 캐시 서버 기능은 , 속도 제한기능과 동일하게 전체 서비스로 먹이거나 , route별로 먹이거나, 특정 요청마다 Kong에 Consumer를 생성해서 , 특정 Consumer에게 캐시서버를 먹일 수 도 있습니다.

Kong은 캐시 서버를 사용하기 위해 , 캐시 TTL(Time to Live) 기능을 사용합니다.

TTL로 client에 오래된 페이지가 노출되지 않도록 새로고침 빈도를 조절할 수 있으며 , BackEnd App이 어떤것이냐에 따라서 TTL을 다르게 주어야합니다.
- 바뀔 가능성이 적은 App은 TTL 값이 비교적 길 수 있습니다. 
- 바뀔 빈도수가 높을것 같은 App은 TTL 값이 비교적 짧을 수 있습니다.

Kong에서 캐시된 특정 entity들을 제공된 api endpoint로 관리할 수 있습니다.
- 전체 제거 , 특정 entity 제거

## 4. Key Authentication
Key 값 인증은, client가 특정 리소스에 접근 권한이 있는지를 확인하는 프로세스 입니다.

Kong은 다양한 Key 인증 plugin들을 통해서 backend 리소스에 접근하는것을 관리할 수 있으며 , 해당 기능또한 서비스 , 글로벌 , route별로 적용시킬 수 있습니다.
- [Kong Key 지원 plugin list](https://docs.konghq.com/hub/#authentication)

OAuth , LDAP , JWT Token , OpenID 등 다양한 인증기관을 사용할 수 있습니다.

Kong Gateway는 모든 auth 시도에 대해서 가시성을 제공(모니터링툴) 하고 , 경고 기능을 갖추고 있습니다.

**리소스 접근 권한을 plugin으로 인증하고 , 권한이 없다면 401 에러를 떨어트립니다.**

Kong의 key Authentication 기능 동작 메커니즘은 , Kong에 특정 consumers를 생성하고 , 해당 consumers에 key를 할당해서 , 해당 consumers가 갖고 있는 key와 같은 key일 경우에 요청을 승인하는 형태로 plugin이 작동합니다.

## 5. LoadBalancing
Kong은 traffic을 LoadBalancing해주기도 합니다.

특정 서버중 한개를 사용하지 못할 경우 , health check를 통해 문제를 감지하고 트래픽을 정상 작동중인 서버로 라우팅할 수 있도록 합니다.

Kong은 업스트림을 통해서 해당 기능을 수행합니다.
- 업스트림이란 , Kong API Gateway가 요청을 전달하는 API, APPLICATION , Micro service를 의미합니다.

업스트림을 생성할 때 weight값을 주어 , 해당 weigth값으로 부하분산을 진행합니다.
- [kong weight값 확인 커맨드](https://docs.konghq.com/gateway/latest/admin-api/#target-object)


아래 표를 참고해서 이해하면 됩니다.

![upstream-targets.png][upstream-targets.png]

[upstream-targets.png]:./images/upstreamtargets.png