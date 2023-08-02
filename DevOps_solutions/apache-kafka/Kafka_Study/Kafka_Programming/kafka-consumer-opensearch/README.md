# kafka consumer wikimedia
카프카에서 데이터 스트림을 받아서 opensearch에게 건내주는 컨슈머

대용량 처리 및 성능 개선 방안은 , 해당 README와 코드 주석을 같이 확인해야 합니다.

## overview
OpenSearch consumer의 동작 순서는 다음과 같습니다.

1. Opensearch client 구성
2. Kafka client 구성
3. 메인 코드 로직 수행
4. 구성된 모든것 close

코드 작동에대한 계념 및 클래스 사용법은 , 주석으로 작성해두었습니다.
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
## offset commit 두가지 전략
### **Offset auto commit 옵션이 true일 경우**
따로 commit 전략을 지정해주지 않으면 , 이게 디폴트 옵션으로 설정됩니다.

**At Least once** 옵션이 적용되며 , 커밋 프로세스는 다음과 같습니다.

>consumer에서 kafka cluster에게 poll 할 때 , 항상 커밋됩니다.
> **auto.commit.interval.ms** 가 경과하게 되며 , 예를들어 해당 옵션이 5초고 **auto.commit=5** 일 경우 ,
> .poll을 요청할 때 마다 5초 간격으로 kafka cluster에게 커밋합니다.
> 이것은 당연하겠지만 , 데이터가 정상적으로 프로세싱 해야만 커밋합니다.

코드는 아래와 같아야 합니다.

batch를 계속해서 수행하면서 , 배치에 대한 동시성 처리 또한 계속해서 진행합니다.
- **At Least once** 옵션이 적용되기에 , 데이터 프로세싱이 정상적으로 수행 되어야만 커밋됩니다. 

```java
while(true) {
    List<Records> batch = consumer.poll(Duration.ofMillis(100))
    doSomethingSynchronous(batch)    
}
```
### **Offset auto commit 옵션이 true일 경우**
auto.commit 이 disable이지만 , 배치를 동시에 수행하고 싶을 경우 , 구조는 다음과 같아야만 합니다.

batch 변수에다가 consumer.poll 결과를 누적시키다가 , isReady 함수가 있는 경우에 , 해당 함수값이 true라면 (배치 크기가 충분하거나, 배치를 모으는 시간이 충분히 경과됐다면)
if문 안으로 들어와서 동시성 작업을 수행한다음 offset을 커밋합니다. 
- 이 경우에 , offset이 커밋되는 시기를 조절할 수 있습니다.
  - ex ) batch는 버퍼가 될 수 있기에 , 가능한 한 버퍼에 많이 쌓아놧다가 데이터베이스로 플러시
  - 플러시가 정상 수행됐을 경우에만 offset 커밋하게끔 구현

```java
while(true) {
    batch += consumer.poll(Duration.ofMillis(100))
    if isReady(batch) {
        doSomethingSynchronous(batch)
        consumer.comimtAsync();
    }    
}
```

## consumer offset reset 동작 방식
consumer에서 장애 발생 시 , consumer의 로그를 끊임없이 읽는 특성 때문에 문제가 생길 수 있습니다.

기본적으로 kafka는 데이터를 7일동안 갖구있는데, (2.0버전 이상 = 7일 , 이하 = 하루) 이 시간 이상으로 컨슈머가 장애상태에서 복구되지 못한다면, offset을 잃어버립니다.

따라서 auto.offset.reset옵션을 적절히 구현해야 합니다.

kafka broker의 offset.retention.minutes 옵션으로 데이터 갖고있는 시간 또 한 조절할 수 있습니다.
- 대부분 한달로 지정함

### auto.offset.reset=latest
컨슈머가 로그 끝에서부터 읽어옵니다.

### auto.offset.reset=earliest
컨슈머가 로그 처음부터 다시 읽어옵니다.

### auto.offset.reset=none
컨슈머가 오프셋을 찾지 못하면 , 에러가 발생합니다.
- 컨슈머에 장애가 발생했을 때 , 데이터를 복구할 방법을 찾아야만 할 때 사용합니다.

## consumer 고급 성능개선 방안
### consumer 속도개선

### consumer 비용 최적화 방안