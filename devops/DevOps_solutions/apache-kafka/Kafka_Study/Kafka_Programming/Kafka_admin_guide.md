# kafka Admin Guide
카프카를 prod 환경에서 사용하기 위해 고려해야할 사항들에 대해 기술하였습니다.

## 1. Kafka Cluster를 생성할 때 고려해야될 사항들
Zookeeper와 Kafka Broker를 분리시켜야 합니다.
- 만약 주키퍼와 카프카 브로커가 같이 있다면 , 어느한쪽에 문제가 발생했을 경우 , 다른쪽에도 영향을 미치기에 분리시켜야 합니다.
- DNS 관리 , 스케일링 등 관리 포인트가 많습니다.
- Kafka Monitoring , Kafka 기능을 완벽히 숙달해야 합니다.

### Broker의 개수를 몇개 두어야 할까 ?
스루풋 , 데이터 리텐션 기간 , replication factor 등 고려해야할 사항이 많습니다.

따라서 사례를 들어 , 테스트하며 결정해야 하고, HA를 통해 최소 2개를 구성해야하기 때문에 , 전체 인프라는 자동화시키는것이 좋습니다.
- 어떤 절차 , 흐름 등을 이해한 다음 작업자체는 간소화 시키는것이 효율적이기 때문

## 2. Kafka Monitoring
Kafka Monitoring은 JMX에서 볼 수 있음.

**- Kafka 모니터링 지표들**
### 1. URP ( Under Replicated Partitions )
ISR 에 문제가 있는 파티션 개수를 나타냄

URP 지표가 크다면 , 시스템 부하 크다는것을 알 수 있음

### 2. Request Handlers
IO , network에 사용되는 스레드 관련 지표

Kafka 브로커의 사용량을 체크할 수 있음

해당 지표가 높다면 , 더 나은 브로커나 브로커 개수가 더 필요하다는 의미

### 3. Request Timing
요청에 응답하는데 소요되는 시간을 의미함

값이 낮을수록 레이턴시가 적기 때문에 , 낮은것이 좋음

## 3. Kafka 운영 수행사항
1. 브로커 롤링 restart
2. 클러스터 전반에 걸친 파티션 균형 맞추기
3. topic의 replication factor 높이거나 낮추기
4. 브로커 추가하기 , 브로커 대체하기 , 브로커 제거하기
5. 다운타임 없이 Kafka cluster 업그레이드 하기

## 4. Kafka security
Kafka cluster를 구축 , 운영하면서 , 카프카 클러스터에 접근할 수 있는 클라이언트들은 **인증 인가 과정**이 꼭 필요합니다.

또한 , Kafka cluster와 클라이언트 사이에 데이터는 꼭 **암호화** 되어야 합니다.
- 암호화되지 않으면 , 네트워크상에서 데이터를 탈취하거나 위변조될 가능성이 있기 때문

> ETC) kafka 보안을 가장 잘 활용할 수 있는 언어는 , Java.

**- Kafka 보안 모델들**
> Kafka를 운영상에서 사용하기 위해선 , 아래 암호 모델을 모두 적용하는것이 좋습니다.
### 1. in-flight 암호화
해당 암호화 모델은 https와 같이 클라이언트와 Kafka 클러스터 사이에 이동하는 데이터 통신을 모두 암호화하는것.

해당 암호화방안을 하지 않는다면 , **PLANTEXT://** 가 사용되기 떄문에 , 암호화없이 그대로 모든 데이터가 노출됨

### 2. 인증 절차 - SSL 또는 SASL 기반 인증
기존 인증인가 방식과 동일하게 , 클라이언트가 인증 데이터를 암호화해서 Kafka에게 보내면 , Kafka는 인증 정보를 검토함. 그 후 정보가 맞다면 클라이언트는 인증됨.

- 여러 인증 방식
#### 1. SSL 인증
클라이언트가 SSL 인증서를 가지고 kafka에게 인증요청

#### 2. SASL/PLANTEST
클라이언트가 ID , password를 통해 kafka에 인증함.
- 꼭 브로커에게 SSL 암호화를 적용해 두어야 함
- 클라이언트의 암호를 바꾸기 위해선 , 브로커를 재시작해야하기에 개발 환경에서만 좋음

#### 3. SASL/SCRAM
SSL 암호화를 사용하게 되는데 , 인증 정보를 주키퍼에 저장함.
- 주키퍼가 제거되기 전까지..
- 유저를 추가 / 제거한다고 해서 Kafka 브로커를 재 시작하지 않아도 됨. ( 주키퍼에 저장되기에 )

#### 4. SASL/GSSAPI (Kerberos)
보안상 튼튼하지만 , 구성하기 어려움
- 매우 안전해야 할 때 기업에서 사용중

#### 5. SASL/OAUTHBEARER
OAUTH2 토큰을 이용하는 방식
- kafka docs에 나와있음

Kafka 클러스터에 인증 요청이 성공적으로 처리되면 , 토큰으로 접근함.

### 3. 인가 절차
인증절차에서 Kafka 클러스터에 인증됐다면 , 이 정보를 이용해서 어떤 클라이언트가 뭘 할수 있는지 알 수 있음.

#### 1. ACL - 접근제어목록
Kafka Admin이 관리함.
- ACL을 사용해서 어떤 유저가 어떤 토픽에 접근할 수 있는지 관리할 수 있음.

## 2. Kafka Multi Cluster && replication factor
만약 데이터가 세계 곳곳에 분산저장되어 있다면 , 다른 지역의 레플리카를 사용할 수 도 있기 때문에 데이터 분산저장 프로세스를 확립하는것이 중요합니다.

따라서 복제 도구를 사용하게 되는데 , 가장 많이 사용되는것은 **Mirror Maker 2** 입니다.

이러한 도구등으로 생성된 복제들은 offset을 보존하지는 않습니다.

데이터만 복제하는데 , 이러한 이유는 데이터가 같다고 해서 offset까지 같을필요는 없기 때문입니다.

복제 방식은 , active-acvite , active-passive 두가지로 나뉘게 됩니다.


## 3. Kafka와 client간의 통신 규약 - 중요 ( Advertised listener )
실제 상황에선, client와 kafka는 다른 머신에 잇을 것이고 , kafka broker는 public IP, private IP를 갖고있다고 가졍했을 때 ,
Kafka broker에선 **Advertised Listener** 라는것을 구성해야 합니다.

만약 Kafka broker에 client가 public IP로 접근한다 했을 경우 ,프로세스는 아래처럼 동작합니다.

### Advertised Listener가 private IP일 경우 프로세스
1. client publie ip로 접근 요청
2. 클라이언트는 브로커에게 접근은 되는데 , 브로커가 아래 응답을 보냄
   - 나랑 계속 통신하기 위해선 , Advertised Listener로 요청보내! public은 쓰지 마
   - Advertised Listener 는 ip 값 _ private IP일 경우
3. client는 받아온 Advertised Listener로 요청 보냄
   - 이떄 같은 네트워크에잇다면 연결성공
   - 같은네트워크가아니라면 당연히 연결 실패

### Advertised Listener가 localhost일 경우
localhost일 경우 , 응답이 localhost로 오기 때문에 내 local pc에 설치된 kafka로 연결될 수 있음

### Advertised Listener가 public IP일 경우
public IP일 경우 , 연결은 성공하겠지만 만약 브로커 머신이 재 실행되거나해서 IP가 변동된다면 ,

Advertised Listener는 변경되기 이전의 IP로 응답을 보내기 때문에 연결 실패 가능성 있음

***따라서 Advertised Listener가 등장***

- 클라이언트가 프라이빗 네트워크, 즉 broker와 통신이 가능한 상황이라면 ,

**브로커에 내부용 private IP를 설정하거나 , private DNS 호스트 이름을 설정해야 함**

- 클라이언트가 public 네트워크에 있다면,

**브로커에 public IP를 설정하거나 , public IP를 가리키는 public DNS 이름을 설정해야 함**
>그러나 브로커를 public network에 두는것은 , 보안상 취약 - 공개적으로 노출되게 됨