# envoy 기능 문서
## 0. OverView
Envoy Proxy의 다양한 기능들을 명시한 문서입니다.

## 1. Service Discovery
클라이언트 측의 서비스 디스커버리를 구현하기 위해, 다른 런타임별로 전용 라이브러리를 사용할 필요 없이 Envoy로만 서비스 디스커버리 수행 가능. 이는 Envoy가 디스커버리 API를 찾기만 하면, Application은 서비스 엔드포인트 찾는 방법을 몰라도 됨.

Envoy는 자체적으로 서비스 디스커버리 기능을 수행하는 것이 아니라, **기존 서비스 디스커버리 솔루션과 통신하는 중간 계층 역할을 수행**할 수 있음. 

1.	일반적인 서비스 디스커버리 방식
- 클라이언트(애플리케이션)가 직접 Consul, Zookeeper, Eureka 등의 API를 호출하여 서비스 엔드포인트를 조회함.
- 각 애플리케이션이 사용하는 프로그래밍 언어에 맞는 전용 라이브러리를 사용해야 할 수도 있음.
2.	Envoy를 활용한 서비스 디스커버리 방식
- 클라이언트는 서비스 엔드포인트를 몰라도 되고, Envoy가 대신 서비스 디스커버리를 수행함.
- Envoy는 Consul, Zookeeper, Eureka 등의 API를 감싸는 디스커버리 API(EDS, xDS)를 사용하여 엔드포인트 정보를 자동으로 업데이트함.

Envoy가 서비스 디스커버리 API를 찾을 수 있으면, Application은 서비스 엔드포인트를 찾는 방법을 몰라도 됨.

Envoy의 디스커버리 API는 기존 디스커버리 솔루션(Consul, Zookeeper, Eureka 등)과 직접 통신이 가능하며, 이를 통해 엔드포인트를 동적으로 업데이트 할 수 있음.

Envoy를 사용하면 런타임 별 전용 라이브러리를 사용할 필요 없이, 서비스 디스커버리를 표준화 할 수 있음.

## 2. Load Balancing
Envoy는 고급 로드벨런싱 알그리즘을 구현하고 있음.

1. Random
2. RR(Round Robbin)
3. 가중치를 적용한 최소 요청
4. 일관된 Hash(Sticky)

가장 큰 특징은, 지역 인식(Locality-aware) 로드벨런싱 기능임. Envoy는 특정 조건을 충족하지 않으면 트래픽이 지역 경계를 넘지 않게 해 트래픽을 더 잘 분산시킬 수 있음. 

예를들어 장애상황으로 이어지는 것이 아닌 이상, 서비스 간 트래픽을 동일한 지역의 인스턴스로 라우팅되도록 함.

클라우드의 AZ와 같이 생각하면 좋음. 

기본적으로 우선순위가 높은 us-east-1a AZ Instance로 트래픽을 보내다가, 단순 장애 혹은 네트워크 지연시간, 리소스 사용량(CPU, Memory), 사용자 위치, 비용 최적화 기반으로 us-east-1b AZ로 트래픽 이전할 수 있음.

### 2.1 지역 인식(Locality-aware) 로드벨런싱 예제
- 지연 시간 기반 라우팅
  - **Outlier Detection** 기능 사용 : 지연 시간이 높은 인스턴스를 자동으로 감지하고, 일정 시간 동안 트래픽을 줄이거나 차단

```yaml
## 조건 1 : 특정 인스턴스에서 연속 5번 이상 5xx에러 발생하면 30초동안 트래픽 제외 ##
## 조건 2 : 평균 성공률이 낮은 인스턴스도 제외
## 조건 3 : 지연 시간이 너무 긴 인스턴스 제외, 빠른 인스턴스로 트래픽 보내도록 유도
clusters:
  - name: my_service
    connect_timeout: 5s  # 연결 제한 시간
    type: EDS
    eds_cluster_config:
      eds_config:
        api_config_source:
          api_type: GRPC
          grpc_services:
            envoy_grpc:
              cluster_name: xds_cluster
    outlier_detection:
      consecutive_5xx: 5  # 5xx 오류가 연속 5번 발생하면 제거
      interval: 10s  # 10초마다 헬스체크 수행
      base_ejection_time: 30s  # 문제가 발생한 인스턴스를 30초 동안 제외
      max_ejection_percent: 50  # 최대 50% 인스턴스를 제외 가능
      success_rate_minimum_hosts: 2  # 최소 2개 이상 인스턴스가 있어야 동작
      success_rate_request_volume: 5  # 최소 5개 요청이 있어야 동작
      success_rate_stdev_factor: 1900  # 평균 성공률 대비 표준편차 기준값
```

- 특정 지연 시간 이상이면 다른 지역(Locality)로 트래픽 이동
  - **Overload Manager** 기능 사용 : 특정 지연 시간 이상이면 트래픽을 다른 지역(Locality)으로 우선 전송

```yaml
## 네트워크 부하가 95% 이상이면 Timout 줄여 인스턴스 자동 제외 ##
## 결과적으로 빠른 응답을 보이는 인스턴스(지역)로 트래픽이 자동 이동 ##
## RAM 사용량이 2GB를 초과하면 부하를 줄이는 방식으로 동작 ##
overload_manager:
  refresh_interval: 0.25s  # 0.25초마다 상태 갱신
  resource_monitors:
    - name: "envoy.resource_monitors.fixed_heap"
      config:
        max_heap_size_bytes: 2147483648  # 2GB RAM 사용 제한
    - name: "envoy.resource_monitors.network"
      config:
        max_concurrent_requests: 1000  # 네트워크 요청이 1000개 이상이면 오버로드 처리 시작
  actions:
    - name: "envoy.overload_actions.reduce_timeouts"
      triggers:
        - name: "envoy.resource_monitors.network"
          threshold:
            value: 0.95  # 네트워크 부하가 95% 이상이면 타임아웃 감소
```

- 네트워크 지연 시간 기반 로드벨런싱 기능 정리

| 방법                         | 설명                                                      |
|------------------------------|-----------------------------------------------------------|
| **Outlier Detection**        | 지연 시간이 긴 인스턴스를 자동으로 감지하고 트래픽에서 제외 |
| **Overload Manager**         | 네트워크 부하가 높으면 빠른 인스턴스로 트래픽 자동 이동   |
| **Locality-aware Load Balancing** | 기본적으로 지연 시간이 짧은 지역(AZ)으로 트래픽 우선 전송 |

## 3. Traffic/Request Routing
HTTP 1.1, HTTP 2와 같은 L7 Application Protocol 이해할 수 있으므로 정교한 라우팅 Rule로 트래픽을 특정 BE Application으로 보낼 수 있음.

이를 통해 ***Virtual Host Maapping***, ***Context-Path Routing***과 같은 ***기본적 리버스 프록시 라우팅 처리***가 가능하며, ***헤더 및 우선순위 기반 라우팅***, ***라우팅 재시도 및 타임아웃***, ***오류 주입***까지 가능

## 4. Traffic Splitting / Shifting / Shadowing
가중치 적용 Traffic Splitting(분할), Shifting(전환) 지원. 이 기능으로 카나리기반 배포와 같은 기능 적용 가능.

- 가중치 기반 트래픽 분기. 카나리아 배포 구현 가능 
```yaml
load_assignment:
  cluster_name: my_service
  endpoints:
    - locality:
        region: us-east-1
        zone: us-east-1a
      lb_endpoints:
        - endpoint:
            address:
              socket_address:
                address: 10.0.0.1
                port_value: 8080
      load_balancing_weight: 80  # 트래픽 80% 할당
    - locality:
        region: us-east-1
        zone: us-east-1b
      lb_endpoints:
        - endpoint:
            address:
              socket_address:
                address: 10.0.1.1
                port_value: 8080
      load_balancing_weight: 20  # 트래픽 20% 할당
```

이뿐만 아니라, Traffic을 보내고 망각하는 Traffic Shadowing 기능을 지원함. Traffic Splitting과 같이 생각할 수 있지만, 그렇지 않음. 해당 기능을 사용하면 업스트림 클러스터가 보는 트래픽은 실제 라이브 트래픽의 복사본으로, 라이브 운영 환경에 영향을 주지 않고 섀도잉한 트래픽을 통해 서비스 변경 사항을 테스트 할 수 있음.

다음과 같은 이점이 있음.
1. 실제 데이터 기반 테스트로 신뢰성 확보
2. 사용자 경험에 영향이 없음
3. 신규 버전 App에 대한 성능 및 에러 모니터링 가능

### 4.1 Envoy Traffic Shadowing Example
- 라이브 트래픽을 primary_service로 보내면서, 같은 요청을 shadow_service에 100% 복제하는 예시
    - primary_service(원래 서비스)로 모든 요청을 보냄
    - 동일한 요청을 shadow_service(테스트 서비스)로 100% 복사하여 보내도록 설정
    - request_mirror_policies를 사용하여 트래픽을 복제 (Traffic Shadowing)
    - runtime_fraction을 이용해 복사 비율을 조절 가능 (ex: 50/100이면 50%만 복사)

```yaml
static_resources:
  listeners:
    - name: listener_0
      address:
        socket_address:
          address: 0.0.0.0
          port_value: 10000
      filter_chains:
        - filters:
            - name: envoy.filters.network.http_connection_manager
              typed_config:
                "@type": type.googleapis.com/envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager
                codec_type: AUTO
                stat_prefix: ingress_http
                route_config:
                  name: local_route
                  virtual_hosts:
                    - name: local_service
                      domains: ["*"]
                      routes:
                        - match:
                            prefix: "/"
                          route:
                            cluster: primary_service
                            request_mirror_policies:
                              - cluster: shadow_service  # 트래픽을 섀도 서비스로 복사
                                runtime_fraction:
                                  default_value:
                                    numerator: 100
                                    denominator: HUNDRED  # 100% 트래픽 복사
                http_filters:
                  - name: envoy.filters.http.router

  clusters:
    - name: primary_service
      connect_timeout: 1s
      type: STATIC
      load_assignment:
        cluster_name: primary_service
        endpoints:
          - lb_endpoints:
              - endpoint:
                  address:
                    socket_address:
                      address: primary-service
                      port_value: 8080

    - name: shadow_service
      connect_timeout: 1s
      type: STATIC
      load_assignment:
        cluster_name: shadow_service
        endpoints:
          - lb_endpoints:
              - endpoint:
                  address:
                    socket_address:
                      address: shadow-service
                      port_value: 8081
```

## 5. Network Recovery
Envoy는 Request Timeout과 재시도(재시도 별 타임아웃 포함)를 자동으로 수행할 수 있음.

이런 재시도 동작은 네트워크 불안정이 간간이 요청에 영향을 줄때 매우 유용하지만, 재시도가 너무 많아지면 장애로 이어질 수 있기에 조심해야 함. 이를 방지하기위해 재시도 동작을 제한할 수 있음.

Envoy는 이상값 감지를 수행해 서킷 브레이커처럼 동작하여 Endpoint를 로드 밸런싱 풀에서 제거할 수 있으며, 업스트림 커넥션 혹은 요청 개수를 제한하고 임계값을 넘어서는 것은 빠르게 실패시키도록 설정할 수 있음.

그러나, Application 수준의 재시도는 여전히 필요할 수 있으며, Envoy가 완전하게 대체할 수는 없다는점을 명시해야 함.

### 5.1 Envoy Level 재시도 VS Application Level 재시도
Envoy는 Proxy Level에서 요청을 제어하지만, Application Level의 재시도는 Envoy가 감지하지 못하는 고 수준의 Application 오류 탐지가 가능한 재 시도를 의미함.

#### 5.1.1 Envoy Level 재시도
클라이언트와 서버 간의 트래픽을 중개하며 자동으로 재시도 수행

- **네트워크 오류**(5xx 응답, Timeout, 연결 끊김 등)에 대해 재시도
- 클라이언트는 한번만 요청을 보내면, **Envoy가 내부적으로 재시도 수행**
- 빠르고 일관된 정책을 적용하는 것이 가능하지만, Application 비즈니스 로직의 에러에 대한 재시도 불가능

```yaml
## 5xx 응답, Gateway 오류, 연결 실패가 발생하면 2초 간격으로 최대 3번 재시도 ##
route:
  retry_policy:
    retry_on: "5xx,gateway-error,connect-failure,refused-stream"
    num_retries: 3
    per_try_timeout: 2s
```

#### 5.1.2 Application Level 재시도
애플리케이션 코드 자체에서 재시도 수행

- **비즈니스 로직상 에러 검출하여 재시도** 가능
- API에 대한 Request Body, Param 등의 값이 잘못된 경우에도 재시도 가능
- **비즈니스 로직을 반영한 고 수준 재시도** 가능
- 정밀한 제어 가능하지만, 클라이언트마다 구현해야 하므로 일관성 부족

```python
## 5xx 에러 발생 시 2초 간격마다 최대 3번까지 재시도하는 Python Code ##
import requests
from retrying import retry

@retry(stop_max_attempt_number=3, wait_fixed=2000)  # 최대 3회, 2초 대기
def make_request():
    response = requests.get("http://my-service/api")
    if response.status_code >= 500:  # 5xx 에러 발생 시 재시도
        raise Exception("Server Error")
    return response

make_request()
```

## 6. Circuit Breaker

## 7. Metric 수집을 통한 Observability

## 8. 분산 트레이싱을 통한 Observability

## 9. TLS Termination

## 10. 속도 제한

## 11. 다양한 프로토콜 지원

## 12. 확장성
