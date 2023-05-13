# Spring Cloud Gateway
해당 문서는 Spring Cloud Gateway에 대해 이론적인 내용을 정리해둔 문서입니다.

Spring cloud Gateway를 사용한 Project Code는 아래 링크에 위치합니다.
- [Spring_Cloud_Gateway를 활용한 실시간 메세지 처리 APP](https://github.com/jjsair0412/Realtime_Messaging_Service_PROJ)

Spring Cloud Gateway를 테스트한 코드는 아래 링크에 위치합니다.
- [Spring_Cloud_Gateway_TEST_CODE](./remind_project/)

## Overview
Spring cloud gateway는 MSA 환경에서 사용하는 API Gateway중 하나 입니다.

SCG를 사용함으로써 , 기본적인 라우팅 및 모니터링/메트릭과 보안 기능을 쉽게 구현할 수 있습니다.

## 구성 요소
SCG의 구성 요소는 크게 다음 3가지로 나뉘어 집니다.
- Route
- Predicate
- Filter

### 1. Route
API Gateway에 가장 기본이 되는 구성요소로 , 서비스 고유값 id, 요청 uri , Predicate , Filter로 구성되어 있습니다.

Client가 요청을 보냈을 때 , Predicates와 조건이 일치한다면 uri 경로로 요청을 매칭시켜서 보내줍니다.
- 요청을 변경할 땐 , 307 리다이렉트 합니다.

### 2. Predicate
API Gateway로 들어온 요청이 , 조건을 만족하는지 검사하는 부분입니다.

조건은 여러개가 올 수 있으며 , 기본적인 Path 뿐만 아니라 HTTP Method (GET, POST, DELETE ...)나 헤더값 , IP 주소 등을 사용할 수 있습니다.

만약 경로가 잘못됐다면 , 404에러가 발생하고 , SCG code상 uri가 잘못됐다면 , 500에러가 발생합니다.

아래 공식문서에서 많은 Predicate 조건을 확인할 수 있습니다.
- [SCG_Predicate_DOCS](https://cloud.spring.io/spring-cloud-gateway/reference/html/#gateway-request-predicates-factories)

### 3. Filter
Spring의 Filter 역할을 하는데 , API Gateway로 들어온 요청에 대해 전 / 후처리를 담당합니다.

## Spring Cloudg Gateway Diagram
Client는 SCG에 요청하고 , Gateway Handler Mapping에서 요청이 Predicate에 걸린 조건과 일치하다고 판단되면 Gateway Web Handler로 요청을 전달합니다.

해당 Gateway Web Handler는 필터 체인을 통해 요청을 처리하게 되며 , 마지막으로 Proxy Service를 통해 요청이 Proxy됩니다.
- 필터 체인은 요청 전 / 후에 작동할 수 있습니다. 

![spring_cloud_gateway_diagram][spring_cloud_gateway_diagram]

[spring_cloud_gateway_diagram]:./images/spring_cloud_gateway_diagram.png

## ETC
만약 uri에 포트가 매핑되어있지 않으면 , 기본적으로 HTTP : 80 , HTTP : 443 인 known port 로 요청을 보내게 됩니다.

## Spring Cloud Gateway Routing 구현
SCG를 구현하는 방법은 2가지로 나뉘게 됩니다.
1. application.yml을 통한 구현
2. 코드 Level 구현

custom filter를 제작해서 구현해야 하거나 , 유지보수가 편리하도록 구현하려면 2번 방안을 따릅니다.
- 대체로 코드에서 작성할것으로 예상. ..

### 1. ENV
||||
|--|--|--|
|명칭|버전|비고|
|Spring Boot Version|3.0.6|-|
|Java Version|17.0.7|-|
|Spring Cloud Gateway Version|2022.0.2|-|

### 2. 의존성 추가
먼저 Gradle dependence를 받아와야 합니다.
- maven 레포지토리 repo.spring.io/milestone 을 추가
- 스프링 클라우드 버전에 대한 변수 지정
- 스프링 클라우드 게이트웨이 dependency 추가
- 스프링 클라우드 의존성 관리를 위한 mavenBom 추가
```build.gradle
repositories {
    mavenCentral()
    maven { url 'https://repo.spring.io/milestone' }
}

ext {
    set('springCloudVersion', "2022.0.2")
}

dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-gateway'
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
    }
}
```

### 3. application.yml을 통한 구현
단순하게 application.yml 파일에 SCG 구성을 입력합니다.
```yaml
spring:
  cloud:
    gateway:
      routes:
      - id: first_route
        uri: http://localhost:8001/
        predicates:
        - Path=/first_route/**
      - id: second_route
        uri: http://localhost:8002/
        predicates:
        - Path=/second_route/**
        filters:
        - AddRequestHeader=second-request, second-request-header
        - AddResponseHeader=second-response, second-response-header
```

- client 요청 uri가 ```~/first_route/``` 인 경우 , http://localhost:8001/ 로 307 리다이렉트 합니다.
- client 요청 uri가 ```~/second_route/``` 인 경우 , http://localhost:8002/ 로 307 리다이렉트 하는데 , 헤더에 second-request를 붙여줍니다.
- 헤더 Name , value 순으로 넣어줍니다.

### 4. 코드 Level 구현
단순하게 RouteLocator를 반환하는 코드입니다.
- RouteLocatorBuilder를 통해서 구현합니다.

```java
@Configuration
public class Router {
    
    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                //first-service
                .route(r -> r.path("/first_route/**")
                        .uri("http://localhost:8081"))
                //second-service
                .route(r -> r.path("/second_route/**")
                        .filters(f -> f.addResponseHeader("second-request", "second-request-header")
                                .addResponseHeader("second-response", "second-response-header"))                
                        .uri("http://localhost:8082"))
                .build();
    }
}

```