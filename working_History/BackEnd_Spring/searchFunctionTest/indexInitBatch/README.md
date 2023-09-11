# spring batch server
elastic search에 데이터를 주기적으로 넣어주는 배치 api

해당 문서는 Spring Batch 사용 방안 및 elastic소

## 참고 문서
batch 사용방안 정리
- https://velog.io/@cho876/Spring-Batch-job-%EC%83%9D%EC%84%B1

batch 최신 변동사항 정리
- https://alwayspr.tistory.com/49

batch 생성 스키마 정리
- https://zzang9ha.tistory.com/426
## dependency
아래 의존성 추가합니다.
- JPA 페이징 사용하여 select 해야되기때문에 JPA 추가
- elasticSearch에 색인해야하기 때문에 spring-boot-starter-data-elasticsearch 추가
>해당 예제에선 3.1.2 버전 사용

```gradle
// elasticSearch 에 색인 하기 위한 dependency
// https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-data-elasticsearch
implementation 'org.springframework.boot:spring-boot-starter-data-elasticsearch:3.1.2'

//JPA
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
```

## 프로세스
![class-diagram](http://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/jjsair0412/My_Treasure_Box/working_History/BackEnd_Spring/searchFunctionTest/indexInitBatch/indicesDiagram.puml)


