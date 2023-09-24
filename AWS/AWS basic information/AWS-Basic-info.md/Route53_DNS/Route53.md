# Route53 
## Route53 이란 ?
Route53은 AWS의 고 가용성, 확장성, 관리되고 권한이 있는 DNS Registrar  입니다.
- AWS의 고객, 즉 Route53을 사용하는 사용자들이 DNS 레코드를 업데이트 할 수 있기 때문에 권한이 있다는 표현을 사용

또한 SLA 가용성을 제공하는 유일한 AWS 서비스 입니다.

## Route53의 기능
DNS 여러 레코드 타입을 지원합니다.

**ex)**
1. A
2. AAAA
3. CNAME
4. NS

### **각 레코드는 다음 정보를 포함합니다.**
1. 레코드를 통해 특정 도메인으로 라우팅하는 방법을 정의합니다.
    - 서브도메인 같은 이름과 정보를 포함시킬 수 있습니다.

2. (Time To Live) DNS 리졸버에서 레코드가 캐싱 되는 시간인 TTL

3. Domain이나 Sub Domain 이름을 포함합니다.
    - ex) example.com

4. 레코드의 값을 포함합니다.
    - ex) example.com -> 1.2.3.4


## TTL 이란 ?
Time To Live의 약자입니다.

클라이언트가 DNS를 Route53에 질의했을 경우, 응답이 옵니다.
- ex) request : myapp.example.com -> response : 1.2.3.4

이때 TTL은 , 클라이언트가 응답받은 결과 (IPv4 등) 를 캐시해 두고있는 시간입니다.
>클라이언트가 Route53에 너무 많이 질의하지 않기 위해서 사용


### TTL 설정 기준
레코드가 자주 바뀌는경우, TTL을 짧은 시간으로 두어 클라이언트에 캐싱된 정보를 빠르게 갱신하는게 좋습니다.
- 그러나 단점은, Route53은 요금정책이 Route53에 얼마나 많이 질의했느냐에 따라 요금이 발생하기에 돈이 많이나올수있음.

레코드가 자주 바뀌지 않는경우, TTL을 24시간 이상의 긴 시간으로 설정하는것이 요금 절약에 유리합니다.

### TTL 전략
TTL을 정적으로 고정해두고 사용하는것 보단 , 새로운 레코드가 생성되었고, 해당 결과가 클라이언트에게 퍼져야된다면, TTL을 낮춰서 클라이언트에 캐시된 정보를 새로 바꾸고, 다시 TTL을 올리는 전략을 세울 수 도 있을것 입니다.

## CNAME VS Alias 레코드의 차이
이 둘을 사용하는 이유는 , 모두 Route53에 등록된 Domain과 다른 HostName을 연결한다는것에 의의가 있습니다.

예를들어 CloudFront나 LB에 생성된 DNS에 Route53 을 붙일 때 사용합니다.

또한 가장 큰 차이점으론 , 

**CNAME은 루트 도메인이 아닌 경우에만 작동하지만 , Alias는 루트 도메인일 경우에도 작동한다는 것 입니다.**
- ex) Root Domain이 example.com 이라면, CNAME은 example.com 으로는 작동을 하지 않고, *.example.com 처럼 앞에 서브도메인이 꼭 들어가야 합니다.
  그러나 Alias는 모든 경우에 다 작동합니다.

- **이것을 CNAME은 apex에 접근할 수 없다라고 합니다.**
    - apex는 Hosted Zone의 대상인 Root Domain을 이야기합니다.
    - ex) example.com 대상으로 Hosted Zone을 설정했다면 , example.com 이 apex

**요금에도 차이가 있습니다. Alias는 무료이고, CNAME은 유료입니다.**

**Alias는 특정 AWS 리소스를 바라보게끔 구성할 수 있습니다.**

### Alias 레코드
오직 AWS 리소스에만 매핑되기 때문에, 외부 호스트와 연결될 수 없습니다.

또한 Alias 레코드는 TTL을 지정할 수 없으며 Route53이 자동으로 설정해줍니다.
#### 주의
- Alias type의 record를 생성할 때 , Record type으로 Alias record의 target이 될 resource가 어떤 방식을 지원하고잇는지 확인한 후, 그거에 맞게 설정해야 합니다.
>예를 들어 target이 ALB이고, ALB가 IPv4 만을 지원하고 있다면 , Record Type을 A로 설정해주어야 합니다.

#### Alias 레코드의 target이 될 수 있는 resources 종류
1. ELB
2. CloudFront
3. API Gateway
4. Elastic Beanstalk
5. S3
6. VPC endpoint
5. Global Accelerator
6. Route53 Record (같은 Hosted Zone일 경우에만)

#### Alias 레코드의 target이 될 수 없는 resources 종류
1. EC2의 DNS