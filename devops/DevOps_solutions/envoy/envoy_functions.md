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
장애 발생한 업스트림 클러스터에 문제가 생겼다고 판별될 경우, Envoy는 로드밸런싱 풀에서 Endpoint를 제거할 수 있음.

이를 바탕으로 서비스 안정성 유지와 가용성 확보 가능.

- Envoy 서킷브레이커 기본 계념
    - 업스트림의 상태 모니터링
    - 특정 에러 임계값 초과하면 요청 차단(Open 상태)
    - 일정 시간이 지나면 일부 요청 허용하여(Half-Open 상태) 서비스 복구 여부 확인
    - 응답이 정상적으로 돌아오면 서킷을 닫고(Normal 상태) 요청을 다시 보냄

### 6.1 Envoy Circuit Breaker 설정 예제
- 연결 개수 제한(Connection Limit)
    - 업스트림 서비스 별 최대 연결 개수를 제한하여, 해당 개수 초과 시 새로운 연결 차단
```yaml
## 최대 50개 대기 요청 허용 ##
## 초당 200개 요청 이상이면 차단 ##
## 최대 3회까지 재시도 ##
clusters:
  - name: my_backend
    connect_timeout: 0.25s
    type: STRICT_DNS
    lb_policy: ROUND_ROBIN
    load_assignment:
      cluster_name: my_backend
      endpoints:
        - lb_endpoints:
            - endpoint:
                address:
                  socket_address:
                    address: backend-service
                    port_value: 8080
    circuit_breakers:
      thresholds:
        - priority: DEFAULT
          max_connections: 100           # 최대 100개 연결 유지
          max_pending_requests: 50       # 최대 50개 대기 요청 허용
          max_requests: 200              # 초당 최대 200개 요청 허용
          max_retries: 3                 # 최대 3회 재시도 가능
```

- 에러 비율 기반 서킷 브레이커
    - 특정 업스트림 클러스터가 지속적으로 오류 반환 시 자동 제외
    - 일정시간 지나면 복구 여부 확인 후 다시 로드벨런싱 풀에 포함

```yaml
## 10초마다 상태 점검 후 다시 추가 여부 결정 ##
## 최소 30초 동안 해당 노드 차단 ##
## 최대 50% 인스턴스만 차단 가능(전체 차단되어 서비스 중단 방지 ##
clusters:
  - name: my_backend
    connect_timeout: 0.25s
    type: STRICT_DNS
    lb_policy: ROUND_ROBIN
    load_assignment:
      cluster_name: my_backend
      endpoints:
        - lb_endpoints:
            - endpoint:
                address:
                  socket_address:
                    address: backend-service
                    port_value: 8080
    outlier_detection:
      consecutive_5xx: 5                  # 5xx 응답이 5번 연속 발생하면 제거
      interval: 10s                        # 10초마다 상태 점검
      base_ejection_time: 30s              # 최소 30초 동안 서킷 오픈 (차단)
      max_ejection_percent: 50             # 최대 50%의 노드 차단 가능
      enforcing_consecutive_5xx: 100       # 100% 확률로 제거
```

## 7. Metric 수집을 통한 Observability
Envoy 주요 목표는 네트워크를 이해 가능하게 하고, 식별 가능하게 하는 것이 주요 목표임. 따라서 Envoy는 다양한 Metric을 수집함.

서버 호출하는 다운스트림 시스템, 서버, 서버가 요청을 보내는 업스트림 클러스터에 대한 여러 측면(Dimension)을 추적함.

다음은 수집하는 다양한 Metric 중 일부를 발췌한 표.

- ***Envoy 통계 항목***

| 항목                                         | 설명                                      |
|--------------------------------------------|-----------------------------------------|
| **downstream_cx_total**                    | 총 커넥션 개수                             |
| **downstream_cx_http1_active**             | 총 활성 HTTP/1.1 커넥션 개수                |
| **downstream_rq_http2_total**              | 총 HTTP/2 요청 개수                        |
| **cluster.<name>.upstream_cx_overflow**    | 클러스터의 커넥션 서킷 브레이커가 임계값을 넘겨 발동한 횟수 |
| **cluster.<name>.upstream_rq_retry**       | 총 요청 재시도 횟수                        |
| **cluster.<name>.ejections_detected_consecutive_5xx** | 5xx 오류가 계속돼 퇴출된 횟수 (시행되지 않은 경우도 포함) |

설정 가능한 어뎁터 또는 형태로 내보낼 수 있음.

## 8. 분산 트레이싱을 통한 Observability
Envoy는 트레이스 스팬(Span)을 OpenTracing 엔진에 보고함으로써 호출 그래프 내 트래픽 흐름, 홉, 지연 시간을 시각화 함.
-> 이는 특별한 오픈트레이싱 라이브러리를 설치할 필요가 없다는 의미

그러나 서비스 간 집킨 헤더를 전파하는 것은 Application 역할이며, 이는 가벼운 Wrapper 라이브러리로 수행할 수 있음.

Envoy는 서비스 간 호출을 연관시킬 목적으로 x-request-id 헤더를 생성하며, 트레이싱이 시작될 때 첫 x-b3* 헤더를 만들 수 있음.

Application이 전파해야 하는 헤더는 다음과 같음.

1. ***x-b3-traceid*** : 트랜잭션 고유 식별자, 요청이 시작될 때 생성되며, 전체 요청 흐름을 추적하는 데 사용
2. ***x-b3-spanid*** : 현재 호출에 대한 고유 식별자. 하나의 트랜잭션 내에서 여러 서비스가 호출되면, 각 호출별 고유한 span 생성
3. ***x-b3-parentspanid*** : 현재 span의 부모 span 지정. 트랜잭션 내에서 호출이 계층적으로 연결될 수 있게 함.
4. ***x-b3-sampled*** : 트래킹을 샘플링할지 말지 결정. 0이면 트래킹 x, 1이면 트래킹 수행
5. ***x-b3-flags*** : 플래그 값으로, 트랜잭션을 어떻게 처리할지에 대한 추가 정보 포함. 예를 들어 디버깅을 위한 플래그 설정 가능

## 9. TLS Termination
Envoy 계층에서 TLS 트래픽을 Termination 할 수 있음.

이는 클러스터의 에지와 서비스 메시 내부 프록시 모두에서 가능함.

Termination 뿐만 아니라, Envoy가 직접 TLS 트래픽을 시작할 수 도 있음. 이는 개발자와 운영자가 언어별 설정과 키스토어, 트러스트 스토어를 조정하지 않아도 된다는 의미임.

요청 경로에 Envoy가 있을 경우 TLS, 상호 TLS까지 자동 적용 가능

### 9.1 TLS Termination 예제
- Envoy가 클라이언트 연결을 TLS로 암호화해서 처리하고, 백엔드 서버로는 암호화되지 않은 HTTP 연결 사용하는 예시
```yaml
## cluster이름 my_service로 http 통신 ##
## Envoy로 들어오는 443 트래픽을 Envoy가 직접 TLS Termination 함 ##
static_resources:
  listeners:
    - name: main_listener
      address:
        socket_address: { address: 0.0.0.0, port_value: 443 }
      filter_chains:
        - filters:
            - name: envoy.filters.network.http_connection_manager
              config:
                codec_type: AUTO
                stat_prefix: ingress_http
                route_config:
                  name: local_route
                  virtual_hosts:
                    - name: backend
                      domains: ["*"]
                      routes:
                        - match: { prefix: "/" }
                          route:
                            cluster: my_service
        transport_socket:
          name: envoy.transport_sockets.tls
          config:
            common_tls_context:
              tls_params:
                tls_minimum_protocol_version: TLSv1_2
                tls_maximum_protocol_version: TLSv1_3
              certificate_chain:
                filename: "/etc/envoy/certs/server-cert.pem"
              private_key:
                filename: "/etc/envoy/certs/server-key.pem"
```

### 9.2 Envoy로 TLS 트래픽 시작
- Envoy와 업스트림 클러스터(백엔드 서버)간의 연결도 TLS로 처리
```yaml
static_resources:
  clusters:
    - name: my_service
      connect_timeout: 0.25s
      type: STRICT_DNS
      lb_policy: ROUND_ROBIN
      load_assignment:
        cluster_name: my_service
        endpoints:
          - lb_endpoints:
              - endpoint:
                  address:
                    socket_address:
                      address: backend-service
                      port_value: 443
      transport_socket:
        name: envoy.transport_sockets.tls
        config:
          common_tls_context:
            tls_params:
              tls_minimum_protocol_version: TLSv1_2
              tls_maximum_protocol_version: TLSv1_3
            certificate_chain:
              filename: "/etc/envoy/certs/client-cert.pem"
            private_key:
              filename: "/etc/envoy/certs/client-key.pem"
            verification_context:
              trusted_ca:
                filename: "/etc/envoy/certs/ca-cert.pem"
```

### 9.3 상호 TLS (Mutual TLS, mTLS 설정)
- Envoy가 클라이언트 서버 간 상호 인증을 처리하도록 설정. 클라이언트 인증서와 서버 인증서 모두 확인하여 트래픽 보호
```yaml
## require_client_certificate: true 설정으로 클라이언트 인증서 필수로 요구 ##
## CA 인증서로 클라이언트 인증서 검증 ##
static_resources:
  listeners:
    - name: main_listener
      address:
        socket_address: { address: 0.0.0.0, port_value: 443 }
      filter_chains:
        - filters:
            - name: envoy.filters.network.http_connection_manager
              config:
                codec_type: AUTO
                stat_prefix: ingress_http
                route_config:
                  name: local_route
                  virtual_hosts:
                    - name: backend
                      domains: ["*"]
                      routes:
                        - match: { prefix: "/" }
                          route:
                            cluster: my_service
        transport_socket:
          name: envoy.transport_sockets.tls
          config:
            common_tls_context:
              tls_params:
                tls_minimum_protocol_version: TLSv1_2
                tls_maximum_protocol_version: TLSv1_3
              certificate_chain:
                filename: "/etc/envoy/certs/server-cert.pem"
              private_key:
                filename: "/etc/envoy/certs/server-key.pem"
              verification_context:
                trusted_ca:
                  filename: "/etc/envoy/certs/ca-cert.pem"
              require_client_certificate: true
```

## 10. 속도 제한
Envoy는 네트워크(커넥션별)와 HTTP(요청별)수준 모두에서 속도제한 서비스와 통합될 수 있음.

1. **호출 비용이 비쌈**
- 특정 서비스나 호출 비용이 많이 드는 작업을 수행할 때, 과도하게 수행되지 않도록 하여 리소스 낭비 방지
2. **지연 시간이 길고 예측 불가능할 경우**
- 네트워크 과부하를 방지하고 서비스 응답속도를 일정하게 유지함.
3. **기아 상태를 방지하기 위해 공정성 알고리즘이 필요**
- 시스템 일부 리소스가 과도하게 사용되어 기아 상태에 빠지지 않도록 요청을 공정히 분배하는 알고리즘을 사용하여 모든 서비스가 일정 비율로 리소스를 할당받을 수 있게 함.

위와 같은 경우 속도를 제한하여 DB, 캐시, 공유 서비스와 같은 리소스를 보호할 수 있음.

이는 서비스 안정성을 높이고 리소스를 보호하는 데 유용함.

Envoy는 rate limiting 필터를 통해 이를 구현함.

### 10.1 속도 제한의 예시. rate limiting
- Http Method 별 속도 제한. 클라이언트 IP 별로 제한하는것 또한 가능하며, HTTP뿐만아니라 gRPC에도 적용 가능
```yaml
static_resources:
  clusters:
    - name: my_service
      connect_timeout: 0.25s
      type: STATIC
      lb_policy: ROUND_ROBIN
      load_assignment:
        cluster_name: my_service
        endpoints:
          - lb_endpoints:
              - endpoint:
                  address:
                    socket_address:
                      address: backend-service
                      port_value: 8080

  listeners:
    - name: main_listener
      address:
        socket_address: { address: 0.0.0.0, port_value: 80 }
      filter_chains:
        - filters:
            - name: envoy.filters.network.http_connection_manager
              config:
                codec_type: AUTO
                stat_prefix: ingress_http
                route_config:
                  name: local_route
                  virtual_hosts:
                    - name: backend
                      domains: ["*"]
                      routes:
                        - match: { prefix: "/" }
                          route:
                            cluster: my_service
                            rate_limits:
                              - actions:
                                  - request_headers: # HTTP Request Method를 기준으로 속도 제한 설정
                                      header_name: ":method"
                                      descriptor_key: "method"
                                  - remote_address: {}  # 클라이언트 IP 기반으로 제한
            - name: envoy.filters.http.rate_limit
              config:
                domain: rate_limit_example
                rate_limit_service:
                  grpc_service:
                    envoy_grpc:
                      cluster_name: rate_limit_service


--
## GET, POST별 다른 요청 수를 제한함. ##
## 192.168.1.1에 대해 추가적인 제한을 설정함. ##
domain: rate_limit_example
descriptors:
  - key: "method"
    value: "GET"
    rate_limit:
      unit: minute
      requests_per_unit: 100 ## 1분에 100번
  - key: "method"
    value: "POST"
    rate_limit:
      unit: minute
      requests_per_unit: 50 ## 1분에 50번
  - key: "remote_address"
    value: "192.168.1.1"
    rate_limit:
      unit: minute
      requests_per_unit: 20 ## 1분에 20번
```

## 11. 다양한 프로토콜 지원
HTTP 1.1, HTTP 2와 같은 Application 프로토콜을 이해할 수 있기 때문에 정교한 라우팅 규칙으로 특정 업스트림 클러스터로 요청을 전송하는 것이 가능함.

또한 HTTP/1.1, HTTP/2 프록시로 개발되었기 때문에 다운스트림, 업스트림 모두에서 각 프로토콜을 프록시 할 수 있음.

Envoy는 **HTTP/1.1 커넥션을 받아 HTTP/2로 프록시** 하거나 반대도 가능하며, gRPC또한 지원하기 때문에 다른 서비스 프록시와 차별화 되어 있음.

## 12. 확장성
Envoy는 필터를 추가로 구축하여 기능을 확장하는 것이 주요 사용 사례로 삼고 있음.

이는 자신의 Application 환경 필요에 맞게 직접 확장할 수 있는 흥미로운 방법임.

Envoy Filter는 C++로 작성되어 Envoy 바이너리 파일로 컴파일 됨. 또한 Lua 스크립트와 웹 어셈블리를 지원하므로 확장성에 뛰어남.