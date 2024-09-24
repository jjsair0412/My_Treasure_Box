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

## Route53 Policy
Route53이 DNS 쿼리에 응답하는 방법들
- Route53의 Hosted Zone에 등록된 Record의 정책들


트래픽이 DNS에서 클라이언트로 라우팅하는 정책
>이때 트래픽 라우팅이란 , LB가 백엔드 EC2에 트래픽을 전달해주는 이런 의미가 아니라, Route53이 DNS 쿼리에 응답하는 방식에 대한것이라 보면 됨.
### 1. Simple - 단순
트래픽을 단일 리소스로 보냅니다.

하나의 도메인에 IPv4나 IPv6 주소를 반환시킬 수도 있습니다.
- 이렇게 하면 , 클라이언트는 반환받은 여러개 중에 **임의로** 하나를 선택해서 사용
### 2. Weighted - 가중치 기반
가중치를 활용해, 일부 요청의 비율을 특정 리소스로 보내는 식의 제어가 가능

***전체 가중치는 100이 아니여도 됩니다.***

#### 사용 방안
가중치 기반을 사용할 때, 레코드를 여러개 만들고 전체 가중치를 만들어줍니다.

각 레코드는 모두 같은 Record Name을 가져야만 합니다.
- 이때 모든 레코드의 가중치를 합한 전체 가중치는 100이 아니여도 됩니다.

또한 각 레코드를 식별할 고유값인 Record ID 값을 지정해주어야 합니다.

#### 이걸 사용하는 이유는 ,, 

적은 양의 트래픽을 보내 새 어플리케이션을 테스트 하거나,
- 카냐리아 배포

서로 다른 지역의 걸쳐 로드벨런싱 하거나 할 때 사용합니다.
### 3. Latency based - 지연 시간 기반
지연시간이 가장 짧은 , 가장 가까운 리소스로 리다이렉팅 시키는 라우팅 정책.
- 이때 지연시간 측정 기준은 , 가장 가까운 식별된 AWS 리전에 연결하기까지 걸리는 시간을 기반으로 측정됨.

#### 사용 방안
Alias를 없이 Value에 IPv4, IPv6와 같은 IP를 그대로 하드코딩해서 넣엇을 경우, 해당 IP가 어디의 IP인지 모르기 때문에 해당 IP가 등록되어있는 리전을 지정해주어야 함.

각 레코드는 모두 같은 Record Name을 가져야만 합니다.

또한 각 레코드를 식별할 고유값인 Record ID 값을 지정해주어야 합니다.

### 4. Failover - 장애조치 
만약 재해복구용 EC2와 , 기본 EC2 두가지가 있을 때, Route53의 Health Check를 기본 EC2 로 연결합니다.

이후 기본 EC2가 비정상으로 간주되었을 때 , 재해복구용 Ec2로 장애 조치하여 클라이언트는 정상으로 판단되는 리소스를 자동으로 얻게 됩니다.
- 당연히 기본 EC2가 정상이라면 , 기본 EC2가 응답합니다.

**중요 : 기본과 재해 복구용 각각 한가지씩만 가질 수 있습니다.**

#### 사용 방안
사용할 때, 두개 레코드를 같은 도메인 이름으로 작성합니다. 그리고 하나는 기본 , 하나는 재해 복구로 두고 레코드를 두개 생성하여 사용합니다.

### 5. Geolocation - 지리적
사용자의 실제 위치를 기반으로 합니다.

사용자의 실제 위치를 가장 정확히 선택되어, 해당 IP로 라우팅되는 기법
- 만약 일치하는 위치가 없는 경우엔 , 미리 선택해둔 Default Record가 응답합니다.

#### 사용 사례
나라별로 웹 사이트의 언어를 변경하거나 , 보여줄 컨텐츠를 분리시킬 때 용이합니다.

#### 사용 방안
레코드를 생성할 때 , Value를 작성한 뒤 Location을 지정하면 됩니다.
- Default Location도 등록해야 합니다.

만약 아래처럼 등록했다면,

1. 대한민국이고 , 대한민국은 value로 1.1.1.1
2. 미국이고, 미국은 value로 2.2.2.2
3. Default , value로 3.3.3.3

실제 사용자 위치가 미국이면 2.2.2.2 반환 , 대한민국이면 1.1.1.1 반환됩니다. 만약 둘다 아니라면 3.3.3.3 반환됩니다.


### 6. Geoproximity - 지리 근접 라우팅 
사용자와 리소스의 지리적 위치를 기반으로 , 트래픽을 리소스로 라우팅하는 정책입니다.

리소스 (트래픽 라우트 대상 리소스) 들에게 Bias(편향값) 을 주어, 높은 편향값을 가진 리소스에게 트래픽이 더 몰리게끔 하는것 입니다.
- 정확히는 양수와 음수로 구분됩니다. 양수 편향값이 높고, 음수 편향값은 낮습니다.

편향값이 모두 0으로 동일하다면, 실제 사용자의 위치를 기반으로 트래픽이 분산됩니다. 
- 미국의 서, 동 에 위치한 리소스가 트래픽 라우팅 대상이라면 , 사용자들은 미국을 반으로 쪼게서 반반 나눠서 트래픽이 분산될것 입니다.
  **그러나 한쪽의 리소스에 편향값을 양수로 높게 둔다면 , 높은 편향값을 가진 리소스로 트래픽이 몰리게 됩니다.**

#### 사용 사례
지리 근접 라우팅 (Geoproximity) 는 , **Bias(편향값) 을 증가시켜 한 리전에서 다른 리전으로 트래픽을 보낼 때 유용합니다.**


### 7. IP-based Routing - IP 기반 라우팅
클라이언트 IP주소를 기반으로 라우팅 정의.

Route53에서 CIDR 목록을 정의해서 (클라이언트 IP 범위) , 특정 CIDR에 따라 트래픽을 어느 리소스 위치로 보낼지를 결정함.

#### 사용 사례
특정 ISP가 특정 CIDR를 사용한다는것을 알면, 특정 ISP에서 들어오는 트래픽은 특정 리소스로 트래픽을 라우팅 할 수 있습니다.

### 8. Multi-Value Answer - 다중 값 응답
트래픽을 다중 리소스로 라우팅할 때 사용.

Route53은 다중 값과 리소스를 반환합니다.
- ELB와 비슷해 보이지만 , 클라이언트 측면 (백엔드에 가는 트래픽을 분산시키는것이 아닌) 의 로드벨런싱이기 때문에 ELB를 대체할 수 없습니다.

근데, 기본 단순라우팅 정책에서도 여러 값을 응답할 수 있는데 , 왜 이걸 쓸까 ?

#### Multi-Value Answer의 사용 사례
단순 라우팅 정책은 Health Check를 지원하지 않기 때문에 , 반환되는 여러 값중 정상적이지 않은 리소스의 엔드포인트를 반환할 수 있는데,

**Multi-Value Answer 는 Health Check를 지원하기 때문에 ,Health Check와 결합하여 레코드를 생성하면, 비정상 레코드는 제외되고 반환되는 여러 값들은 정상적인것만 오기 때문에 사용합니다.**


## Route53 Health Checks
Route53에 등록된 리소스에 대해 Health Check를 진행할 수 있습니다.

만약 고 가용성을 목표로 서로다른 AZ에 각기 다른 ALB가 구축되어 있고, Route53은 라우팅 정책 가중치 기반으로 작동하고 있다 생각 했을 때,

특정 ALB에 문제가 발생한다면 그 LB로는 트래픽을 보내면 안될것입니다.

**이때 사용하는것이 Route53 Health Check** 

### 사용 방안
위 예에서 Route53 Health Check를 사용하고 싶다면 , 헬스체크 타겟이 되는 각 ALB들 모두에게 Health Check를 생성해야 합니다.

#### 사용에 주의할 점
Health Check를 사용하기 위해선 , 헬스체크 타겟이 되는 리소스가 Route53의 상태 확인용 IP 주소 범위에 들어오는 모든 요청을 허용해야 합니다.
- [주소 범위](https://ip-ranges.amazonaws.com/ip-ranges.json)

위 링크를 타고 들어가면 주소 범위를 확인할 수 있습니다.

### Route53 - Calculated Health Check
여러 개의 상태 확인 결과를 하나로 합쳐주는 기능

**Health Check들이 여러개 만들어질 수 있는데, 이때 각 Health Check들의 부모 Health Check를 생성해줄 수 있습니다.**

이때 합칠 수 있는 조건은 , ***OR , AND , NOT*** 입니다.

하위 Health Check는 256개까지 모니터링하는것이 가능하며 , Health Check를 위해 몇개의 Health Check를 통과해야 하는지에 대한 조건도 지정해 줄 수 있습니다.

### ETC - Private Hosted Zone 리소스에 대한 Health Check 방법
만약 개인 subnet 내부의 리소스를 헬스체크 하고 싶다면 , cloud watch를 사용할 수 있을것 입니다.

해당 리소스의 cloud watch 메트릭을 만들어서 , 해당 메트릭이 침해되는 경우 cloud watch가 알람을 생성하게 되는데 , 

**이때 Health Check가 private subnet 내부의 cloud watch를 모니터링 하고 있다가 알람이 일어나면 Health Check가 자동 비활성화 된 것이기 때문에, 이렇게 모니터링할 수 있습니다.**

## ETC - 외부 DNS Registrar에서 Domain 사서 Route53으로 관리하기
가능합니다.

1. 일단 도메인을 가비아 및 타 사이트에서 구매한 다음 , 

2. 이후 , Route53에 public Hosted Zone을 생성한 뒤 ,

3. 도메인을 구매한 타사 사이트에서 NS 또는 Nameserver를 Route53의 nameserver로 업데이트 하면 , Route53의 Nameserver를 가르키게 되면서 Route53으로 관리가 가능해집니다.