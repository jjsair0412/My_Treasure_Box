# EC2
## EC2 ?
EC2 = Elastic Compute Cloud = aws에서 제공하는 service형 infrastructure

가상 머신을 EC2에 임대가 가능하다.
- EC2 Instance라 함.

데이터를 EBS 볼륨 또는 가상 드라이브에 저장할 수 있다.

ELB (Elastic LoadBalancer) 를 통해서 데이터를 부하분산 할 수 있다. 

ASG (auto-scaling group)를 통해서 서비스를 확장할 수 있다.

## EC2 Sizing & configuration options
### EC2에서 대여 가능한 운영 체제 목록
1. linux
2. window
3. Mac Os

### EC2 configuration options
1. CPU 갯수
2. RAM 용량
3. Storage 저장 공간의 양 
4. Network card
    - 속도가 빠른 network card를 원하는지 , 어떤 종류의 public ip address를 등록할지 지정 가능
5. firewall rule
    - security group으로 방화벽 rule 지정 가능
6. EC2 User data : Bootstrap script 
    - 인스턴스를 구성하기 위한 Bootstrap script

## EC2 User Data
EC2 User 
     