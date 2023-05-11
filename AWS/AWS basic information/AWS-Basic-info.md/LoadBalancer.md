# LoadBalancer
## LB health check
aws lb에서는 backend ec2에 health check를 하여 application 상태를 주기적으로 체크함.

/health path로 체크하며 , 200으로 응답하면 정상.
## AWS의 LB 종류
1.  Classic Load Balancer ( CLD - 2009 )
- http , https , tcp , ssl (secure TCP) 지원
- 권장하지 않아 사용 못함
2. Application Load Balancer ( ALB - 2016 )
- http , https , websokect 지원
3. Network Load Balancer ( NLE - 2017 )
- tcp , tls (secure TCP) , UDP 지원
4. Gateway Load Balancer ( GWLB - 2020 )
- 네트워크층에서 작동, 3계층에서 작동

## LB 보안
LB는 모든 ip에 대해 (0.0.0.0/0) 80 . 443으로 접근 가능 , ec2에서는 LB 보안그룹의 80번 포트만 가능하게끔 하여 보안을 강화할 수 있음

## 1. ALB
- 7계층에서 작동하는 load balancer. target group으로 묶인 muchine 간의 http 라우팅에 사용됨.
- 동일 ec2 인스턴스 상의 여러 application에 부하를 분산함
    - 이떄 컨테이너 , ecs가 사용됨
- HTTP2와 WEBSOCAT 지원 , 리다이렉트 또한 지원
- URL 기반 라우팅 지원
    - /example/user와 /example/posts 를 다른 target group으로 라우팅 가능
    - 쿼리문자열이나 헤더로 다른 target group으로 라우팅 가능
- MSA나 컨테이너 기반 application에 가장 좋은 LB
