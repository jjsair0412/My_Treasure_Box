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

