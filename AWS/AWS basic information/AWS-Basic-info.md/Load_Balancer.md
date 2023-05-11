# AWS'S_LoadBalancer_TYPE_AND_What_is_LB_?
AWS의 로드 벨런서 종류에 대해 학습
## LB health check
aws lb에서는 backend ec2에 health check를 하여 application 상태를 주기적으로 체크함.

/health path로 체크하며 , 200으로 응답하면 정상.
## AWS의 LB 종류
1.  Classic Load Balancer ( CLD - 2009 )
- http , https , tcp , ssl (secure TCP) 지원
- 권장하지 않거니와 , 이제 아예 사용 못함
2. Application Load Balancer ( ALB - 2016 )
- http , https , websokect 지원
3. Network Load Balancer ( NLE - 2017 )
- tcp , tls (secure TCP) , UDP 지원
4. Gateway Load Balancer ( GWLB - 2020 )
- 네트워크층에서 작동, 3계층에서 작동

## LB 보안
LB는 모든 ip에 대해 (0.0.0.0/0) 80 . 443으로 접근 가능 , ec2에서는 LB 보안그룹의 80번 포트만 가능하게끔 하여 보안을 강화할 수 있음
- ex ) NLB -> ALB -> EC2

## 1. ALB (Application Load Balancer)
- **7계층에서 작동하는 load balancer.** target group으로 묶인 muchine 간의 http 라우팅에 사용됨.
- 동일 ec2 인스턴스 상의 여러 application에 부하를 분산함
    - 이떄 컨테이너 , ecs가 사용됨
- HTTP2와 WEBSOCAT 지원 , 리다이렉트 또한 지원
- 파라미터 기반 라우팅 지원
    - ex ) Platform parameter value가 mobile일 경우 , desktop일 경우 서로 다른 target group으로 라우팅 가능
    - ?Platform=mobile
    - ?Platform=desktop
- URL 기반 라우팅 지원
    - /example/user와 /example/posts 를 다른 target group으로 라우팅 가능
    - 쿼리문자열이나 헤더로 다른 target group으로 라우팅 가능
- MSA나 컨테이너 기반 application에 가장 좋은 LB
- ECS 인스턴스 동적 포트로 리다이렉팅 가능
- ALB의 헬스체크는 TARGET GROUP별로 진행됨

### 1.1 ALB의 target group이 될 수 있는것들
- EC2 인스턴스
- ECS tasks들
- Lambda functions
- private ip들
    - 온프레미스 서버 private ip를 target group으로 지정해서 , alb를 두고 하이브리드 클라우드를 구성할 수 있음

### 1.2 ALB 통신 사례
1. Client -> ALB 까지 client ip로 ALB에 닿는데 , 여기서 ALB는 Connection termination을 수행
2. ALB는 private ip로 ec2와 통신함.
    - EC2가 client ip를 보려면 , 주의사항칸에 적어둔 X-Forwarded-~ 헤더로 볼 수 있음

### 1.3 ALB 주의사항
- ALB는 client 실제 ip를 볼 수 없음.
    - X-Forwarded-For 라는 헤더에 client ip가 삽입됨.
    - X-Forwarded-Port 헤더에 port , X-Forwarded-Proto 헤더에 protocol 들어감.

## 2. NLB (Network Load Balancer)
- TCP, UDP 전송을 지원하는 **L4계층 Load Balancer .**
- 성능이 굉장히 좋음
    - 초당 수백만건 요청 통과 , 지연시간 alb보다 4배더 빠름
- **AZ별로 1개의 static ip를 가지기 때문에 , 여러개 elatic ip를 가진 app을 배포할때 유용**

### 2.1 NLB의 target group이 될 수 있는것들
- ec2 인스턴스들
    - tcp 또는 udp로 트래픽 리다이렉트 가능
- IP Address
    - private ip여야만 하며 , ip는 하드코딩되어야 함
- ALB - 중요한 설정 . Client -> NLB -> ALB -> EC2 ~ ECS 등
    - ALB앞에 NLB를 사용할 수 있음
    - NLB로 고정 ip를 얻을 수 있으며 , alb로 http 리다이렉팅 기능을 얻을 수 있음
    - 또한 NLB는 4계층에서 작동하는 lb기 때문에 , NLB를 통과하자마자 곧장 백엔드 (ec2 등) 으로 달려감. 만약 ec2 인스턴스를 target group으로 가졌다면. ip차단 등은 ec2의 보안그룹 규칙을 따르게 되기에 , 앞에 ALB를 두어서 곧장 ec2로 붙지 않도록 막는게 좋음 

### 2.2 health check protocols
- tcp , http , https protocol들을 health check로 사용할 수 있음

### 2.3 NLB 주의사항
- NLB는 4계층에서 작동하는 lb기 때문에 , NLB를 통과하자마자 곧장 백엔드 (ec2 등) 으로 달려감. 만약 ec2 인스턴스를 target group으로 가졌다면. ip차단 등은 ec2의 보안그룹 규칙을 따르게 되기에 , 앞에 ALB를 두어서 곧장 ec2로 붙지 않도록 막는게 좋음 

## 3. GWLB (Gateway Load Balancer)
- **GWLB는 네트워크 계층인 3계층에서 동작하는 LB**
- 배포 , 확장 , aws 타사 app의 (서드파티) 가상 어플라이언스의 플릿 관리에 사용됨
- 네트워크의 모든 트래픽이 방화벽을 거치거나 , 침입 탐지 및 방지 시스템에 사용함
    - **동작 예**
    - Client -> ALB -> EC2 구성일 때 . 모든 트래픽은 검사받지 않고 ec2로 곧장감. 이때 GWLB를 중간에 두면
    - Client -> GWLB -> 서드파티 -> GWLB -> ALB -> EC2 VPC의 라우팅 테이블이 수정되어 GWLB로 트래픽이 전달되고 , GWLB는 트래픽을 관리할 서드파티로 트래픽을보냄 . 서드파티에서 트래픽을 검사하고 , 정상적이면 GWLB로 return한뒤 ALB로 감

### 3.1 GWLB의 기능
- Transparent Network Gateway
    - 3계층에서 동작하기에 vcp의 모든 트래픽이 gwlb를 통과하게 됨.
- Load Balancer
    - GWLB의 target group에 묶인 애들에게 모두 트래픽을 분산시킴.

### 3.2 GWLB의 target group이 될 수 있는 것들
- ec2 인스턴스들
    - 인스턴스 id로 등록하거나 ec2 인스턴스가 될 수 있음
- ip address
    - private ip여야만 하며 , hard coding 해야함.


## Sticky Session
- 요청에 응답하기 위해 처음 요청한 사람이 응답받은 단 한개의 백엔드에 붙이는것
- 처음 응답한 인스턴스와 요청보낸 client가 붙음
- **CLB와 ALB가 해당 기능을 가질 수 있음**
    - 쿠키로 해당기능 지원

### 1. Cookie 종류
- 1. Application-based Cookies
    - custom cookie
        - target을 기반으로 생성한 custom cookie.
        - 이름을 지어주어야 하는데 , AWS, AWSALB, AWSALBTG 같은 이름은 사용할 수 없음
    - Application cookie
        - LB 자체에서 생성
        - cookie 이름은 AWSALBAPP
- 2. Duration-based Cookie
    - 특정 기간으로 만료됨
    - LB에 의해서 생성되는 Cookie
    - 기간 지정가능
    - cookie 이름은 ALB에선 AWSALB , CLB에선 AWSELB

## Cross-zone Load Balancing or without Cross-zone Load Balancing
- with Corss-zone Load Balancing
    - 서로 다른 AZ에 있는 ALB라 하더라도 , 각각의 AZ의 백엔드 ec2 등으로 트래픽을 골고루 부하분산 하는 방법.
- without Cross-zone Load Balancing
    - 서로 다른 AZ에 있는 ALB들이 , 각각 트래픽을 각각 AZ의 백엔드 ec2 등으로 트래픽을 부하분산 하는 방법.
- **ALB는 기본적으로 cross-zone lb가 활성화 되어 있습니다.**
    - target group 계층에서 막을 수 있습니다.
    - AZ들 끼리 데이터교환은 비용이들지 않습니다.
- **NLB와 GWLB는 기본적으로 cross-zone lb가 비활성화 되어 있습니다.**
    - 활성화하려면 돈내야됨

## SSL/TLS
- SSL : Secure Socket Layer : 연결을 암호화하는데 사용
- TLS : Transport Layer Security : 최근에 많이 사용됨.
- SSL 인증서는 , client와 LB 사이에 트래픽이 이동하는 동안 암호화해줌, -> 전송중 암호화라 함
- CA를 통해 인증서발급해서 , LB에 등록하는 형태로 진행
- SSL 인증서는 만료기간이 있음
- AWS는 ACM에서 인증서를 관리함
- SSL 인증을 진행할 때, HTTPS 로 구성해야 함
    - 기본 인증서 등록해야함
    - 클라이언트는 SNI (Server name indication) 을 써서 접속할 호스트 이름을 알릴 수 있음.

### **1. SNI ( SERVER NAME indication )**
- 여러개 의 ssl 인증서를 하나의 웹서버에 등록시켜서 , 하나의 웹 서버가 여러개의 웹 사이트를 지원할 수 있게 함
    - nginx , tomcat 처럼
- 최초 ssl 핸드쉐이크 시 , client가 target server의 호스트 이름을 지정하도록 함
    - www.example.com 과 www.hello.com 인증서 두개를 ALB가 갖고있다면 , ALB는 SNI를 통해 client request url로 인증서를 로드하고 , 백엔드 타겟그룹으로 트래픽을 부하분산한다.
- **NLB , ALB , CloudFront에서만 작동함**

## Connection Draining
- EC2 인스턴스 등 LB뒤에있는애가 중지됐을때 , 정상 작동하기까지 LB가 기다리는 시간을 의미한다.
- 기본값은 5분 , 600초이며 , 1~3,600 초 사이로 등록할 수 있다.

## ASG ( Auto Scaling Group )
- 요청많아지면 ec2 서버 scale out (ec2 add) 이나 scale in (ec2 remove) 를 자동화하는것이 ASG 이다.
- 최소 / 희망 / 최대 ec2 개수를 지정할 수 있다.
    - 최대 용량보다 낮고 희망보다 높은값을 주면 , 최대 용량만큼 ec2가 생성된다.
- ASG에 속한 LB는 늘어나거나 줄어들어도 LB와 연결된다.
- 인스턴스 헬스체크 하여 비정상이면 다시만들어준다.

### 1. ASG 생성 방법
- Launch Template을 생성해야 한다. 아래와 같은 정보가 기입됨.
- ec2를 생성할때와 거의 유사함
    - AMI + Instance Type
    - EC2 User Data
    - EBS Volume
    - Security Group
    - SSH Key Pair
    - IAM Roles for your EC2 Instance
    - Network + Subnets information
    - Load Balanacer information
    - 등 ..
- min size , max size , 초기용량 지정해야 함
- 스케일링 정책또한 지정해야 함
- CloudWatch 알람과 연동해서 스케일 정책을 자동 생성하여 스케일아웃되거나 인되는 행동을 발생시킬 수 있음
    - ASG cpu 사용량과 같은 metric 등을 기반으로 알람 발생 

### 2. Auto Scaling Groups 스케일링 정책들 -  Dynamic Scaling Polices 
- 1. Target Tracking Scaling
    - 가장쉬움
    - 예를들어 , 모든 ec2가 특정 수치의 40%대에 유지될수 있도록 함.
- 2. Simple / Step Scaling
    - cloudwatch 알람 설정 후 , 전체 ASG에 대한 cpu사용률을 감시해서 ,. 70% 이상되면 2개 추가해라 등의 상세설정 가능 
- 3. Scheduled Actions
    - 특정 순간에 ASG 최소 용량을 늘리거나 줄일 수 있게끔 예약하는 기능
    - 10시부터 12시 사이 ASG 최소 용량을 3개로 늘려줘 등 가능

### 3. Auto Scaling Groups 스케일링 정책들 -  Predictive Scaling
- Predictive Scaling
    - 로드를 보고 다음 스케일링 예측하여 해당 예측을 기반으로 스케일링 정책 생성

### 4. Scaling Cooldwon
- 스케일링 작업이 끝날 때 마다 5분 혹은 300초의 쉬는시간을 갖는것
- 휴식시간에는 ec2가 생성되거나 제거될 수 없음