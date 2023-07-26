# kafka consumer wikimedia
카프카에서 데이터 스트림을 받아서 opensearch에게 건내주는 컨슈머

## overview
OpenSearch consumer의 동작 순서는 다음과 같습니다.

1. Opensearch client 구성
2. Kafka client 구성
3. 메인 코드 로직 수행
4. 구성된 모든것 close

코드 작동에대한 기본 계념 및 클래스 사용법은 , 주석으로 작성해두었습니다.
- [OpenSearch Consumer java code](./src/main/java/io/conduktor/demos/kafka/opensearch/OpenSearchConsumer.java)

## Prerequirement
해당 모듈엔 opensearch docker-compose.yml 파일또한 위치합니다.
- [opensearch docker-compose file](./docker-compose.yml)


gradle dependency의 아래 의존성이 추가로 필요합니다.
```.gradle
dependencies {
    // https://mvnrepository.com/artifact/org.apache.kafka/kafka-clients
    implementation ("org.apache.kafka:kafka-clients:3.1.0")
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    implementation ("org.slf4j:slf4j-api:1.7.36")
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
    implementation ("org.slf4j:slf4j-simple:1.7.36")


    // json 값 처리를 위한 gson 의존성
    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation 'com.google.code.gson:gson:2.9.0'

    // opensearch Rest client
    // https://mvnrepository.com/artifact/org.opensearch.client/opensearch-rest-high-level-client
    implementation 'org.opensearch.client:opensearch-rest-high-level-client:1.2.4'

}
```