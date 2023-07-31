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

## Kafka consumer commit 전달론
kafka consumer에서 offset 이 언제 커밋되는지에 대한 방법론은 다음과 같은종류가 있습니다.
### **1. At most once ( 최대한 한번 )**
   - 메시지 배치만큼 받자마자 오프셋이 커밋되는 경우
   - 만약 처리가 잘못되면 ( 배치가 잘못되면 ) 못읽은 메시지는 손실됩니다.
   - 메시지가 손실되도 , 커밋된 시점부터 다시 갖고오기 때문입니다.

#### 수행 순서 ( consumer group이 충돌 낫을 경우 )
1. 배치 읽음
2. 오프셋 커밋
3. 데이터 프로세싱
4. 커밋된 부분부터 다시 읽어옴

### **2. At Least once**
   - 메시지가 처리된 이후 , 커밋하는 경우
   - 처리 (processing)가 잘못되면 메시지를 다시 읽어옵니다.
   - 프로세싱은 멱등해야 합니다.

#### 수행 순서 ( consumer group이 충돌 낫을 경우 )
1. 배치 읽음
2. 데이터 프로세싱
3. 처리(프로세싱) 된 데이터까지만 커밋
4. 커밋된 부분부터 다시 읽어옴

따라서 , At Least once 방법론을 사용하는것이 안전하며 , 그걸 사용하는 방법은 아래와 같은 방안들이 있습니다.

각 데이터에 ID값을 지정해허 아래 두가지 방법 중 하나를 선택해서 사용해야 함

1. kafka 레코드 좌포값을 사용해서 , ID 값 정의
```java
String id = record.topic() + "_" + record.partition() + "_" + record.offset();
```

2. kafka에서 받아오는 데이터 값 자체에 ID값을 정의해서 , 그걸 사용하는 방법
```java
    // 동일한 id를 두번 요청하면 , 덮어씌우는 기능하는 메서드
    private static String extractId(String json) {
        // gson libray 사용해서 json 파싱
        String asString = JsonParser.parseString(json)
                .getAsJsonObject()
                .get("meta")
                .getAsJsonObject()
                .get("id")
                .getAsString();
        return asString;
    }
```