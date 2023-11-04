# AWS Load Balancer Controller
## 배포기준
***1. Ingress인 경우***
    - Application LoadBalancer 프로비저닝됨

***2. Service인 경우***
    - Network LoadBalancer 프로비저닝됨

### 왜 ALB , NLB로 다르게 프로비저닝될까?
Ingress는 L7계층의 라우팅을 하게됩니다.

따라서 URL경로로 라우팅되야 Kubernetes Backend Service로 트래픽을 전달할 수 있기 때문에, Network LoadBalacner가 프로비저닝됩니다.
- NLB는 7계층이기 때문에 Http, Https 프로토콜을 지원합니다. 따라서 URL 경로기반으로 백엔드에 트래픽을 전달합니다.

또한 Service는 백엔드 파드에 트래픽을 전달하게 됩니다.

따라서 IP:Port 로 라우팅되어야 Kubernetes Backend Pod로 트래픽을 전달할 수 있기 때문에, Application LoadBalancer가 프로비저닝 됩니다.
- ALB는 4계층이기 때문에 Tcp, Udp 프로토콜을 지원하고 IP:port 를 기준으로 라우팅되게 됩니다. 또한 NLB는 Static Public Ip를 할당받기에 서비스앞단에 붙기에 적합합니다.

## AWS Load Balancer Controller에서 지원하는 트래픽 모드들

***1. Instance(default)***
    - 클러스터 내 노드를 ALB의 대상으로 등록합니다. ALB에 도달하는 트래픽은 NodePort로 라우팅된 다음 파드로 프록시됩니다.
***2. IP*** 
    - 파드를 ALB 대상으로 등록합니다. ALB에 도달하는 트래픽은 파드로 직접 라우팅됩니다. 해당 트래픽 모드를 사용하기 위해선 ingress.yaml 파일에 주석을 사용하여 명시적으로 지정해야 합니다.