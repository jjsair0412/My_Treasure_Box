# IGW - Internet Gateway
## OverView
VPC와 서브넷을 생성했다 해서, 인터넷에 연결할 수 있는것은 아닙니다.

따라서 IGW ( Internet Gateway ) 를 생성해서 AWS 리소스 (람다, EC2 등) 를 인터넷과 연결합니다.

## 작동방식
IGW 자체로는 인터넷과 연결할 수 없으며, VPC의 Route Table을 생성한 이후, 해당 라우팅테이블에 서브넷 할당 후 , Route를 생성한 IGW로 가게끔 해야만 이 인터넷에 연결할 수 있습니다.