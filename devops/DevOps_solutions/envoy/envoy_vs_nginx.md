# Envoy VS Nginx
## Oviervew
해당 문서는 Nginx Proxy와 Envoy Proxy의 차이점을 설명합니다.

이를 바탕으로 둘의 차이점을 명확히 이해하여 환경에 맞는 Proxy 솔루션을 선택할 수 있습니다.

둘을 비교하는데 좋은 자료는 다음과 같습니다.
- [Envoy vs NGINX vs HAProxy: Why the Edge Stack API Gateway chose Envoy](https://blog.getambassador.io/envoy-vs-nginx-vs-haproxy-why-the-open-source-ambassador-api-gateway-chose-envoy-23826aed79ef)
- [youtube_Nginx vs. Envoy performance benchmark](https://www.youtube.com/watch?v=0Q9I-x--np4&t=145s)

## 0. 목차
1. What is Proxy?
2. Nginx
3. Envoy
4. Nginx VS Envoy
4.1 기능
4.2 성능
4.3 사례
5. 결론

## 1. What is Proxy?
Proxy는 

## 2. Nginx

## 3. Envoy
Envoy는 분산 시스템에서 발생하는 어려운 Application 네트워킹 문제를 해결하기 위한 목적으로 리프트 업체에서 만들게 되었습니다. 2016년 9월 오픈소스로 발표되었으며, 2017년 9월에는 CNCF에 합류됩니다.

Envoy는 C++로 작성되었는데, 성능향상과 많은 부하량에서도 안정적이고 결정론적으로 동작할 수 있도록 함에 있습니다.

Envoy는 2가지 중요 원칙을 따라 만들어집니다.

1. Application에게 네트워크는 투명해야 한다.
2. 네트워크 및 Application 문제가 발생한다면, 원인 파악이 쉬워야 한다.

또한 Envoy는 결과론적으로 Proxy입니다. 그러한 이유는 서버간 통신의 중간에 위치하여, 통신과정의 중개자로 구성되기 때문입니다. Istio 구성에서 Envoy 위치를 파악해보면, 각 Pod의 Sidcar로 동작하며 모든 네트워크 트래픽을 중개합니다.

## 4. Nginx VS Envoy

### 4.0 기능

### 4.1 성능

### 4.2 사용 사례

## 5. 결론
