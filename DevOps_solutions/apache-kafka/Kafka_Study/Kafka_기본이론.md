# Kafka 기본이론
## 문서 색인


## 토픽이란 ?
Kafka cluster에있는 데이터 스트림을 의미함.
- DB의 테이블과 비슷함

이름을 통해서 Kafka에서 토픽을 식별함.

Topic은 데이터 포멧을 다 지원함
- json , Avro , txt 다 보내도 상관없음.

Topic안에 있는 메세지들의 순서를 **데이터 스트림**이라고 함

데이터 스트림을 쿼리할 수 없지만 , 토픽을 읽고 쓰면서 꺼내올수있음.

## 파티션 && 오프셋
토픽들을 **파티션별로 분리**할 수 있음.
- 예를들어서 100개의 파티션으로 토픽 분리

분리된 파티션으로 메시지를 넣을 수 있음.

파티션 안의 메시지는 id값을 가지고 , 0부터 1씩 오르는데 이 값을 **오프셋**이라 함

Kafka안의 데이터는 지우거나 수정할수 없다.
- 불 가변성을 가진다.

## Producers
Kafka Topic에 메세지를 보내는 주체를 Producer 라 한다.

프로듀서는 LB를 통해서 특정 토픽 안에 파티션들에게 특정 알고리즘방식을 통해 메시지를 보내게 되기 때문에 , Kafka는 스케일링 한다.

### Producers : Message keys
프로듀서는 메시지에 키를 추가해서 토픽안의 여러 파티션에 값을 넣을 때 사용하게 된다.

key값이 null이라면 , 데이터는 라운드로빈 방식으로 메시지를 보낸다.

Key값이 null이 아니라면 , 프로듀서는 항상 같은 파티션에 메시지를 보내기 때문에 , 특정 필드에 대한 메시지 순서 설정을 해야 한다.

## Kafka Message Serializer
Kafka 메세지를 생성시키는 주체.

메시지를 보낼 때 , 프로듀서에서 평상시 객체를 byte 데이터 직렬화가 필요하다.
- 실제 123 , helloworld같은 문자나 숫자를 0101 인 2진 데이터로 직렬화하고 보내야 함.

ex )
- **KeySerializer=IntegerSerializer**로 int값을 byte로 변환하여 브로커로 전달
- **ValueSerializer=StringSerializer**로 String값을 byte로 변환하여 브로커로 전달

**Kafka는 이런 Message Serializer를 통해 byte 데이터 직렬화를 해서 Kafka에 보내기 때문에 , 어떤 데이터 포멧을 가진 값이 오더라도 토픽에 보낼 수 있는것이다.**

## Consumers
Kafka 토픽 파티션 안에있는 데이터를 읽어오는 주체

컨슈머는 Kafka 토픽 파티션에 있는 데이터를 Pull 모델을 통해 받아오고 , 파티션에 있는 데이터를 받아오는 순서는 low to high로 받아온다.
- ex ) offset 0부터 1 , 2 , 3 ,, 순서대로 받아옴

브로커가 fail 상태라면 , 컨슈머는 재해 복구를 어떻게 할지 미리 정한다.

### Consumer Deserializer
카프카에서 받은 데이터를 바이트 객체로 변환하는 역할을 한다.
- Programming 언어로 처리할 수 있는 객체로 변환시킨다.

ex ) 
- **KeyDeserializer=IntegerDeserializer**로 브로커에서 byte로 들어온 데이터를 int로 변환
- **ValueDeserializer=StringDeserializer**로 브로커에서 byte로 들어온 데이터를 String으로 변환

### Consumer Group
특정 application 내부에는 다수의 consumer들이 존재할 수 있고, 얘네들은 각각 파티션에서 데이터를 읽어올 수 있음. 이 집단을 **Consumer Group**이라 함

#### **Consumer Group 특성 1 - Consumer Group과 파티션의 관계**
***파티션 개수와 Consumer Group 내부 Consumer의 수가 1대1로 매핑될 때 || 파티션 개수가 Consumer의 수보다 더 많을때***
- 문제 없음.

***파티션 개수보다 Consumer Group 내부 Consumer의 수가 적을 때 - Consumer와 파티션이 1대1 매핑됬을 경우***
- 일단 1대1 매핑되고 , 남은 Consumer들은 inactive 되어 standby 상태로 남음.

***하나의 토픽(파티션) 에 여러 Consumer Group의 Consumer가 연결될 때***
- 가능 . 하나의 Consumer가 여러 파티션과 매핑될 수 있음.

#### **Consumer Group 특성 2 - Consumer Offsets**
Kafka는 Consumer Group에서 읽어온 offset을 ```__consumer_offset``` 이라는 이름으로 **Kafka 내부 Consumer Offset이라는 이름의 Topic에 커밋해 놓음.**

그리고 Consumer Group은 가끔씩 Consumer offset을 Kafka Consumer Offset Topic에 커밋함.

**그 이유는 , 만약 Consumer Group이 fail이 됐을 경우 , commit된 ```__consumer_offset```까지는 정상적으로 받아왔고 , 다시 시작할 때 그 다음부터 받으면 된다를 알릴 수 있기 때문**

- consumer offset commit 정책을 코드에서 지정할 수 있음 _ 아래 3가지 정책이 있음
    - **At least once**
        - 최소 한번은 메시지가 처리한 직후에 커밋됨.
    - **At most once**
        - 컨슈머가 메시지를 받자마자 오프셋을 커밋함.
    - **Exactly once**
        - 메시지를 딱 한번 처리함

## Kafka Brokers
Kafka Broker는 단순하게 카프카 서버다.

각 브로커들은 int 형식의 ID로 구분된다.
- ex ) Broker 101 , Broker 102 , Broker 103 ...

각 브로커는 고유의 토픽 파티션을 갖고 있으며 , 프로듀서가 브로커에게 데이터를 전달하면 , 모든 브로커에게 분산되어 저장된다.

Kafka Broker는 Bootstrap broker 메커니즘이 있어서 , client및 프로듀서 , 컨슈머가 Kafka cluster의 아무 broker에게만 연결해도 모든 broker에게 자동으로 연결된다.
- 이말은 broker가 수평 확장될 수 있다는 의미. 3개에서 4개로 늘어나도 client , 프로듀서 , 컨슈머가 모든 broker를 알 필요가 없다는 의미..
- Kafka의 Broker들은 Kafka cluster에 존재하는 모든 Broker의 metadata를 공유하기에, 이게 가능함.
- 기본적으로 3개의 Broker 구성이 기본 아키텍처라고들 함

### Broker with Topic
생성할 토픽 파티션 개수가 Broker 개수보다 모자라더라도 , 파티션은 모든 Broker에게 분산되어 저장된다.

많더라도 분산되어 저장된다.

## Topic replication factor - Kafka Topic의 재해복구 방법
Kafka Topic은 replication factor라는것을 가진다.

만약 각 토픽 설정이 아래와 같이 동일하다고 볼 때
- Topic 2개 
- 파티션 1개
- replication factor 1개
- Broker 3대

각 토픽과 파티션은 위에 Broker에서 설명한것 처럼 각 Broker에게 분산 저장되는데, **replication factor가 1개**로 설정되어 있기 때문에 **다른 Broker (내가 가진 파티션을 다른 브로커에게 복재시켜둠) 에게 파티션 1개를 복재 시킨다.**
- 이것은 특정 브로커가 죽어도, 데이터를 잃지 않고 계속 제공할수 있도록 한다.

### Topic replication factor 의 방법
**replication된 파티션은 ISR(In-sync replica)** 이라 함

각 파티션은 replica를 갖기에 리더가 있어야 하며 , **오직 1개의 브로커만 특정 파티션의 리더가 될 수 있음.**

**프로듀서는 리더 파티션에게만 데이터를 보낼 수 있음.. 똑같이 컨슈머도 리더 파티션에서만 데이터를 받을 수 있음..**
- 이말은 프로듀서와 컨슈머가 어떤 브로커의 파티션에게 데이터를 보내고 받는지를 알고 있다는 의미

만약 리더가 죽으면 ISR이 리더가 됨.

#### ETC 
#### **1. Kafka v2.4 이상에서 바뀐점..**
원래는 컨슈머가 리더 파티션에서만 읽을 수 있엇는데, 이제는 그냥 가까운 Broker에서 받아옴.
- 가까운 Broker의 파티션이 ISR이라도 , 그냥 갖고옴

따라서 **네트워크 cost나 latency을 더욱 줄일 수 있게 되었음**

#### **2. Kafka v3.x 이상에서 바뀐점..**
kafka Raft 메커니즘을 통해 Zookeeper 없이 Kafka를 실행할 수 있음.
- 주키퍼 없이 실행하는 카프카의 프로덕션 레디 상태의 버전은 **Kafka v3.3.1**

Kafka 2.x 버전까진 Zookeeper가 필수로 필요함. 없으면 실행안됨.

Kafka 4.x 버전엔 Zookeeper가 아예 삭제됨.
- 차후 피쳐로 주키퍼 없이 동작하게끔 Kafka는 계속 개발중

#### **3. Producer Acknowledgements (acks)**
프로듀서는 브로커에게 데이터가 정상적으로 보내졌는지를 acks 로 확인할 수 있음.

- ***1. acks = 0 :*** 
    - 프로듀서가 데이터 송신 확인을 기다리지 않음. 
        - 브로커가 다운됬을 경우 데이터 유실 가능성 있음.
- ***2. acks = 1 :*** 
    - 프로듀서가 리더 파티션에게만 송신 확인을 받음. 
        - 리더 파티션을 갖고 있는 브로커가 다운됬을 경우 데이터 유실 가능성 있음.
- ***3. acks = all :*** 
    - 리더 및 ISR 파티션에게 모두 송신 확인을 받음. 
        - 데이터 유실 가능성 없음



## Kafka 주요 특징
#### **1. 불가변성**

일단 Kafka Topic 파티션에 데이터가 들어가면 , 수정하거나 제거될 수 없다.

#### **2. 데이터 저장 시간**

Kafka 안에 들어간 데이터는 Default 1주일동안만 데이터를 갖고있으며 , 그 시간이 지나면 제거된다.

#### **3. offset 의미**

파티션마다 오프셋은 중복될 수 있기 때문에 , 파티션별로 구분된다.

메세지의 순서는 한 파티션에서만 유효하다.

#### **4. 파티션 갯수**

Topic마다 원하는대로 파티션 개수를 조절할 수 있다.

#### **5. serializer**
**프로듀서 -> 브로커 -> 컨슈머** 의 토픽 생애주기 중 데이터는 **객체 -> byte -> 객체** 로 변화함

프로듀서 측은 Serializer , 컨슈머 측은 Deserializer 를 사용
- **Serializer** 
    - int나 String 등 프로그래밍 언어에서 해석할 수 있는 객체를 브로커가 받을 수 있는 byte로 변환
- **Deserializer** 
    - 브로커에서 보낸 byte 코드를 프로그래밍 언어에서 해석할 수 있는 int나 String 객체로 변환

따라서 토픽이 생성되면 프로듀서가 전송하는 데이터 타입을 변경하면 안된다.
- 컨슈머는 받을 때 **Deserializer** 객체로 어떤 데이터가 올건지 생각하고 있는데 , 바뀌면 컨슈머에서 에러가 나기 때문

#### **6. 데이터 복구 - replication factor 최소값***
이론적으로 replication factor가 N개라면 , Kafka 데이터는 N-1개의 브로커가 다운됬을 때 까지 데이터 복구가 가능함.

## Zookeeper
Zookeeper는 소프트웨어로 Kafka broker들을 관리함.

**브로커가 다운될 때 마다 , 다운된 브로커의 리더 파티션을 다시 선출해야 하는데 , 이 과정을 Zookeeper가 담당함!!**

Zookeeper는 Kafka Broker의 metadata를 아주 많이 갖고있는 상태기 때문에 , 변동사항이 있을 때 마다 알림을 보냄.
- 토픽생성 , 브로커 다운 , 브로커 추가 , 토픽제거 등등의 이벤트들..

Zookeeper는 홀수 개수로 작동되게끔 디자인됐음.
- 1개, 3개 , 5개, 7개. 일반적으로 7개 이상은 없음

Zookeeper 또한 리더 계념이 있으며 , 나머진 팔로워라 함.
- 리더는 읽기 쓰기 전용 , 팔로워는 읽기 전용

**Kafka v0.10 이전까진 Consumer Offset을 Zookeeper에 저장했지만 , 이후엔 Kafka Broker에 Consumer Offset이라는 이름의 내부 Kafka Topic에 저장함 !!!!**

Kafka v4.0 이상이 릴리즈되고 프로덕션 레디가 되면 Zookeeper 사용 안해도 되는데 ,, 아직까진 써야됨.

예전엔 컨슈머 , 프로듀서 , 모두 Zookeeper에 연결됐는데 지금은 그러면 안됨. 왜냐면 Kafka 개발 방향에 맞지 않기에 Kafka 버전이 올라가고 버전 업그레이드시 마이그레이션에 문제가 있을 수 있으며 , Zookeeper가 Kafka보다 훨씬 보안성에 안좋기 때문.