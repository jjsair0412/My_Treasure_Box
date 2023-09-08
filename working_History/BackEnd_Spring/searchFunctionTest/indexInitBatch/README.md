# spring batch server
elastic search에 데이터를 주기적으로 넣어주는 배치 api

참고 문서 :

batch 사용방안 정리
- https://velog.io/@cho876/Spring-Batch-job-%EC%83%9D%EC%84%B1

batch 최신 변동사항 정리
- https://alwayspr.tistory.com/49

batch 생성 스키마 정리
- https://zzang9ha.tistory.com/426
## dependency
spring data elasticsearch 의존성을 추가합니다.
- [spring_data_elasticsearch_maven_repository](https://mvnrepository.com/artifact/org.springframework.data/spring-data-elasticsearch)
>해당 예제에선 5.1.2 버전 사용

## 프로세스
![class-diagram](http://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/jjsair0412/My_Treasure_Box/working_History/BackEnd_Spring/searchFunctionTest/indexInitBatch/indicesDiagram.puml)
