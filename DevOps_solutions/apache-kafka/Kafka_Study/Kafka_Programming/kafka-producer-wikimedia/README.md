# kafka producer wikimedia
wikimedia 스트림값을 받아서 카프카로 전송하는 프로듀서

producer 성능 최적화 방안 등 주석으로 설명되어있는것도 많아서 , 해당 README와 코드를 같이 읽어야만 합니다.

## overview
코드 수행순서는 다음과 같습니다
1. kafka producer 생성
2. 이벤트헨들러 생성하여 스트림에 이벤트 생길때 마다 broker로 전송
3. 이벤트헨들러 실행 스레드 분리

코드에 대한 설명은 주석으로 작성해 두었습니다.
- [codes](./src/main/java/io/conduktor/demos/kafka/wikimedia/)

## 예외처리
프로듀서가 카프카 브로커로 데이터 전송 시 에러가 발생했을 때 , 예외처리를 안만들어두면 데이터 유실가능성 있음.

### 방안 1. retries
에러처리하기 귀찮고 재시도하려면 해당 옵션 주어야함.

- kafka 2.0 이하 default 재시도 숫자 : 0
- kafka 2.1 이상 default 재시도 숫자 : 2147483647

retry.backoff.ms 설정으로 다음 재시도 까지 대기 시간또한 조절 가능
- default : 100ms

#### producer timeout
프로듀서가 무한하게 재시도할 경우 timeout 발생

kafka producer가 해당 타임아웃시간 내에 정상응답 받지 못하면 요청 실패하여 에러 발생
- kafka 2.1 부터 delivery.timeout.ms 옵션으로 타임아웃 설정가능
- default : 120,000ms - 2min

### 방안 2. 멱등 프로듀서 - 3.0 이상 버전에는 기본옵션. 중요
retireis 설정이 되어 있을 때 , 중복된 데이터가 브로커에 적재될 수 있음.
- 요청 한개의 대한 ack가 정상적이지 않을 때 , 프로듀서가 다시 같은데이터를 보냄. 근데 그 데이터는 적재는 됐는데 요청만 안온거라면, 데이터가 중복됨.\
- 멱등 프로듀서에는 카프카가 자동으로 중복 데이터임을 감지해서 , 중복된것은 커밋하지 않고 ack만 보냄.
- 3.0 부터는 해당 기능이 default. 멱등 프로듀서는 무조건 써야됨..
- 해당 오류를 해결하는 방안이 멱등 프로듀서.

멱등 프로듀서를 설정하면 , default 설정이 아래와 같이 변경됨.
- retries=Integer.MAX_VALUE(2^31-1)
- max.in.flight.requests=0 (kafka == 0.11 버전에는 0)
- max.in.flight.requests=5 (kafka >= 1.0 이상 버전에는 5)
- ack=all

멱등 프로듀서를 사용하고싶다면 , producer 코드에서 아래와 같은 값만 넣어주면됨.
```java
producerProps.put("enable.idempotence", true);
```

#### 멱등 프로듀서 관련 기타 정보
카프카 하위 버전 (1.0 이하) 에는 요청을 재시도했을 경우 데이터 순서가 뒤바뀔 수 있는데 , 이런 문제를 멱등 프로듀서로 해결 가능

max.in.flight.request.per.connection 설정을 통해 키 기준 정렬을 사용할 수 있음.
- default : 5
- 카프카가 하위 버전이며 , retries 값이 있다면 1로 설정해야 키 기준 정렬 사용 가능


## 안전한 프로듀서 설정 - best practice
아래와 같은 설정값 추천.  만약 kafka 3.0 이상 버전 사용한다면 기본적으로 다 되어있어서 설정안건드려도 되지만 , 이하 버전에는 아래설정 다 해줘야함.
- 코드에는 아래 설정값들을 모두 설정해두었음. ( kafka 2.8.0 사용중 ..)
- [WikimediaChangesProducer](./src/main/java/io/conduktor/demos/kafka/wikimedia/WikimediaChangesProducer.java)

```java
properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,"true");
properties.setProperty(ProducerConfig.ACKS_CONFIG,"all"); // -1 설정과 동일
properties.setProperty(ProducerConfig.RETRIES_CONFIG,Integer.toString(Integer.MAX_VALUE));
```

- ack=all (-1)
  - 데이터 수신 안전히 받기
- min.insync.replicas=2
  - isr중 적어도 둘 이상의 브로커가 응답하도록 설정
- enable.idempotence=true
  - 네트워크 재시도로 인해 데이터 유실 방지
- retries=MAX_INT
  - timeout 시간까지 재 시도 하도록 설정
- delivery.timeout.ms=120000
  - 2분동안 재시도하도록 설정
- max.in.flight.requests.per.connection=5
  - 메시지 순서를 유지하도록 함

## 프로듀서 메세지 압축
메시지를 압축해서 보냄으로서 , 카프카로의 데이터 전송속도를 더 빠르게 할 수 있으며 , 저장공간을 절약할 수 있음.

메시지 배치 단위로 압축되기 때문에 , 배치 크기가 클수록 압축률이 올라가 효율이 증가함.

메시지 압축 활성화는 필수로 진행해야 함.
### 압축 시기
1. 프로듀서에서 메시지를 압축해 브로커에 보낼 수 있음 (default)
- ```compression.type=producer``` 옵션으로 가능한데 , 프로듀서가 압축해서 카프카로 보내고 카프카는 따로 재압축 안하고 그대로 토픽에 저장

2. 브로커에서 메시지를 압축할 수 있음
- 모든 토픽 대상으로 압축 , 특정토픽 지정 가능


### 압축 타입 지정
```compression.type``` 옵션으로 메시지를 다양한 타입으로 압축할 수 있음
1. gzip (default)
2. lz4 , ```compression.type=lz4```
- 토픽에 설정된 압축 유형이 , 프로듀서 설정과 동일할 경우 데이터를 다시 압축하지 않고 그대로 디스크에 저장됨.
- 근데 다르다면 , 브로커가 배치를 압축 해제하고 브로커가 lz4로 다시 압축시킴.
3. snappy
- 메세지가 txt나 json 등의 값일때 유용함
4. zstd (kafka 2.1에서 동작)

```compression.type=producer``` 해당 옵션이 default

```compression.type=none``` 설정하면 카프카에 전송된 모든 배치를 브로커가 압축 해제하게 됨
- 비효율적

### 압축 사이즈 강제조절
```batch.size``` 및 ```linger.ms``` 옵션을 조절해서 배치 크기를 조절할 수 있음.

프로듀서 처리량을 올릴라면 , linger.ms 및 batch.size를 늘려서 처리량을 조절할 수 있음.
- 코드에는 압축사이즈를 조절하는 코드를 적용해 두었음.
- [WikimediaChangesProducer](./src/main/java/io/conduktor/demos/kafka/wikimedia/WikimediaChangesProducer.java)

1. ```linger.ms```
- default=0
- 배치를 전송할 때 까지 기다리는 시간

2. ```batch.size```
- linger.ms 시간이 되기 전에 , 배치가 다 차면 배치를 전송하는데 , 배치 크기를 해당 옵션으로 조절 가능
- default=16KB
- 크기 늘릴수록 압축률 , 처리량 , 요청효율 향상
  - 그런데 너무 크게잡으면 , 메모리낭비가 심해서 주의해야됨.
  - Kafka Producer Metrices 사용해서 평균 배치크기 모니터링 가능

```java
// 처리량을 늘리기 위한 프로듀서 설정
properties.setProperty(ProducerConfig.LINGER_MS_CONFIG,"20");
properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG,Integer.toString(30*1024));
properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG,"snappy");
```

## Default Partitional
key가 null이 아니면 어디 파티션에 데이터를 넣을지 정해서 고 파티션에서만 들어감
- 동일한 키를 가진 데이터는 동일한 파티션에 들어감

근데 만약 토픽이 추가되면 동일한 키가 동일한 파티션에 할당된다는 옵션이 깨짐
- 따라서 토픽을 추가하는것이 아닌 , 파티션을 추가하는것이 좋음. 파티션을 추가하면 해당 옵션이 깨지지 않기 때문

### sticky partitional
kafka 2.3 이하는 라운드로빈 방식으로 파티션에데이터가 들어감

kafka 2.4 이상은 스티키 파티셔너 구현되어 성능 향상됨.
- 배치가 다 차거나 linger.ms가 될 때까지 파티션에 고정됨.
- 배치가 다 전송된 이후 고정시킬 파티션이 변경됨
- 계속 진행시키면 라운드로빈과 동일하게 균등하게 파티션들에게 데이터가 전송되기에 좋은 옵션임.