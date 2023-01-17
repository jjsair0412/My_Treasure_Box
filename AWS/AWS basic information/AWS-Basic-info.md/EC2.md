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
EC2 User data는 EC2 Instance를 실행할 때, 특정 작업을 부트스트랩하여 인스턴스를 실행할 수 있다.

스크립트는 단 한번만 실행되며 , Root user에서 진행되게 된다.
- bootstrap ? : 부팅 작업을 자동화 한다.
    - update
    - install software
    - 일반적 파일을 인터넷에서 다운로드

    등 ..

EC2 USER Data는 EC2 Instance가 실행될 떄 단 한번만 실행되는 스크립트를 정의할 수 있다.

만약 아래와 같이 스크립트를 정의한다면 ,EC2 Instance가 실행될 때 apt-get update를 실행한 뒤 , httpd servie를 설치하고 실행할 것이다.

그리고 index.html파일에 hellow world from ~ 이 적힐 것이다.

```bash
#!/bin/bash
# Use this for your user data (script from top to bottom)
# install httpd (Linux 2 version)
yum update -y
yum install -y httpd
systemctl start httpd
systemctl enable httpd
echo "<h1>Hello World from $(hostname -f)</h1>" > /var/www/html/index.html
```

## EC2 Instance Type
- [EC2 Instance Type 종류](https://aws.amazon.com/ko/ec2/instance-types/)

위 URL을 따라가면 , 다양한 EC2의 Instance Type들이 있고 사용 목적에 따라 어떤 Instance를 사용할지 정할 수 있다.

AWS에서 EC2 Instance type 명명 규칙은은 다음과 같다.

만약 m5.2xlarge라는 Instance가 있다면
- m : instance class
범용의 인스턴스라는 뜻이다.
- 5 : instance의 세대를 의미한다.
- 2xlarge : instance의 크기를 의미한다. 크기가 클수록 더 많은 메모리와 CPU를 가지게 된다.

따라서 위 url에서 instance type들을 확인할 수 있다.


### EC2 Instance class 종류
- 범용
- 컴퓨팅 최적화
- 메모리 최적화
- 가속화된 컴퓨팅
- 스토리지 최적화
- HPC 최적화

EC2 Instance의 비용 및 사양 비교할 때 좋은 사이트
- https://instances.vantage.sh/

## Security Group
aws에서 네트워크 보안을 수행하는 애다.

EC2에서는 트래픽을 조절하는데 , EC2에 출입이 가능한 대역과 나가는 그룹을 설정할 수 있다.

IP를 지정할 수 도 있고 , security group끼리 묶을 수 도 있다.

EC2 인스턴스의 방화벽이다.
- port 허용
- ipv4 또는 ipv6의 ip 범위 지정 가능
- inbound network 지정
- outbound network 지정

### Security Group에서 알아야 할 점들
1. 하나의 보안 그룹을 여러 인스턴스에 연결할 수 있다.
2. VPC 결합으로 통제되어 있기에 VPC마다 생성해 주어야 한다. ( 리전별로 공유 x )
3. EC2 외부에 존재한다. -> EC2에서 확인할 수 없다.
4. SSH를 위해 security group을 별도로 분리하는게 좋다.
5. Timeout이 생긴다면 , Security Group이 잘못된것이다. 그러나 connection refused가 생기면 security group은 뚫린거다.
6. 기본적으로 outbound는 모두 뚫려있다.



### Classic Ports to know ( saa 시험을 위해 알아야 할 ports 번호들 )
- 22 = SSH (Secure Shell) - log into a Linux instance
- 21 = FTP (File Transfer Protocol) – upload files into a file share
- 22 = SFTP (Secure File Transfer Protocol) – upload files using SSH
- 80 = HTTP – access unsecured websites
- 443 = HTTPS – access secured websites
- 3389 = RDP (Remote Desktop Protocol) – log into a Windows instance

## EC2 IAM Role
iam role을 지정한 후 , EC2에 등록하게 되면 aws 자격 증명을 제공할 수 있다.

예를 들어 DemoIamRole이라는 Iam Role이 있다고 하자.
DemoIamRole에는 aws Iam ReadOnly policy가 정의되어 있다.

이때 해당 iam role을 EC2에 지정하게 되면
ec2 instance에 ssh 연결 후 아래 aws 명령을 통해 iam list를 read하면 결과가 출력된다.
```bash
$ aws iam list-users
```

## EC2 Instance Options
1. **On-Demand Instance**

    예측 가능한 가격을 가진 단기 워크로드용 인스턴스

    사용한만큼 비용을 지불함.
    - 1분지난뒤 1초당 비용 나오기 시작함.

    사용해제 , 중지 , 시작이 자유로움

    연속적 단기 워크로드에 적합함.

2. **Reserved Instance**

    예약 인스턴스. 약속된 기간 ( 1년 또는 3년 )동안 사용될 때 사용하는 인스턴스.
    최소 1년으로 지정해야 함.
    type은 세가지가 있다.
    - Reserved Instance : 긴 워크로드에 사용되는 기본적인 예약 인스턴스
    - convertible Reserved Instance : instance type을 유연하게 바꿀 수 있음
    - Scheduled Reserved Instance : batch job처럼 특정 기간 ( 매주 목요일 , 매주 월요일 3시에서 6시 등 ) 을 지정해서 사용하는 인스턴스

    온디멘드에 비해 75% 절약 가능

    54%가 최저 할인

    aws에 이러한 ec2를 몇년동안 쓸거야 라고 하고 먼저 돈내는거라 할인율 높음.
    선결제 , 부분 선결제 , 매달 요금지불 세가지옵션 가능.

    애플리케이션이 안정된 상태로 있어야하는 DB같은곳에 적합

3. Spot Instance

    저렴한 단기 워크로드용 인스턴스. 저렴하지만 손실 가능성이 있고 신뢰성이 낮다.

    할인율이 가장 높은 Instance

    스팟 가격은 점진적으로 변화하는데 , 내가 낼 비용보다 낮아지면 서버가 멈춤.

    따라서 인스턴스가 언제든 중지될 수 있으니 이러한 상황이라도 괜찮은 프로그램을 쓸 때 적합함.

4.  Dedicated Hosts

    물리 서버 전체를 예약하고 인스턴스 배치를 제어함.

    AWS IDC 내부의 물리 서버자체를 예약하는것이다.
    
    3년동안 사용해야함. 전체 서버를 사용하니 비용이 올라간다.

    IDC 자체를 빌리니까 다른사람은 그곳을 못쓴다.

    라이센스같은 문제가 있을 경우 , 클라우드를 못쓰니까 요걸 써서 aws idc 서버 자체를 빌린다.

5. Dedicated Instance

    Host와 동일하게 물리 IDC를 사용하지만 , 얘는 Instance별로 사용된다.

## EC2 Spot Instance
작동 방식은 다음과 같다.

어떤 스팟 인스턴스에 대해 지불할 의향이 있는 비용을 지정한 후 , 그 가격보다 낮은동안 사용한다.

시간당 지정비용은 바뀔수 있으며 , 내가 지정한 가격이 해당 가격보다 높아지면 2분동안 유예 기간을 준다. 
- 인스턴스 종료하거나 인스턴스를 중지해서 스팟 비용이 내가 지정한 가격보다 낮아질 때 다시 인스턴스를 실행할 수 있다.

spot instance를 종료하기 위해선 , spot request를 종료하여 instance 실행 요청을 aws에 보내지 않도록 하고 , spot instance를 종료해야 한다.


## EC2 public ip and Elastic IP address
EC2 instance를 정지햇다가 다시 실행하면 , public ip가 변경된다.

Elastic IP를 사용하면 변경되지 않는다 .
- Elastic IP는 기본적으로 계정당 5개를 허용한다.

또한 Elastic IP는 EC2 Instance에 연결되어 있지 않다면 요금이 부과된다.
- 연결되어 있는 상태더라도 EC2 Instance가 running상태가 아니라면, 요금이 부과된다.

## EC2 배치 그룹 ( EC2 Placement Group )
EC2 Placement Group은 EC2 Instance가 aws 인프라에 배치되는 방식을 컨트롤하기 위해 사용한다.

배치그룹을 사용하면 AWS 하드웨어와 직접 상호 작용은 하지 않지만 , EC2 Instance가 어떻게 배치되기를 원하는지 aws에 요청할 수 있다.

배치 그룹을 사용하기 위해서 , EC2 Instance를 실행할 때 세부 정보에서 미리 설정으로 생성해둔 EC2 Placement Group 을 선택하기만 하면 된다.

### EC2 배치 그룹 ( EC2 Placement Group ) 의 종류
1. cluster
    - 단일 AZ 내에서 지연 시간이 짧은 하드웨어 설정 .
    - high performance , high risk
    - 모든 인스턴스가 동일한 하드웨어 랙에 존재함 . 
      동일한 하드웨어와 같은 AZ에 존재함 .
      latency가 제일 낮음 .
      그러나 하드웨어 랙에 에러가 나면 모든 EC2 인스턴스가 고장남 .
    - 네트워크 성능이 제일 좋아서 , 빅데이터 작업이나 짧은 지연시간 ( latency )가 필요할 때 사용 .
2. spread ( 분산 배치 그룹 )
    - EC2 인스턴스가 다른 하드웨어에 분산되어 배치 .
    - AZ 당 인스턴스가 최대 7개까지만 가질 수 있음 .
    - 크리티컬 application인 경우 적합 .
    - 실패 위험을 최소화함.
      모든 EC2 인스턴스가 다른 하드웨어에 존재함 .
      여러 AZ에 걸쳐 잇으니까 실패 위험이 낮음..
      그러나 배치 그룹의 개수가 7개로 제한되는 단점이 있음. ( 7개 )
3. Partition ( 분할 배치 그룹 )
    - 인스턴스 분산
    - 여러 파티션에 인스턴스가 분산되어 있는데 , 서로 다른 하드웨어 랙에 존재함 
      따라서 에러 확률이 낮음 .
    - 수백개의 EC2 인스턴스를 통해 확장 가능
    - Hadoop , Cassandra , kafka application 수행 가능
    - 여러 하드웨어 랙에 존재하여 실패 확률이 낮으며 , az당 7개가 있을수 있어서 설정을 통해 수백개의 인스턴스를 얻을 수 있음.


## EC2 Hibernate mode
EC2 인스턴스를 중지하면 디스크 데이터는 그대로 유지된다.

그러나 삭제하면 다 삭제된다.

EC2 인스턴스를 시작하면 , os를 키고 , user data 스크립트를 수행하고 , 시작하는데까지 시간이 다소 걸린다.

Hibernate 인스턴스는 ..
- RAM에 잇는 메모리 데이터는 남아 있는다. 따라서 더 빠르게 시작된다.
- RAM data는 root EBS 볼륨에 저장되기에 EBS 볼륨 크기가 커야한다.
    - 다시 시작될때 RAM data가 EBS에 덤프되어 있다가 , 다시 시작할 때 EBS에 있는 덤프 데이터를 가지고와서 시작된다.
- 또한 root EBS 볼륨이 암호화되어있는지 확인해야 한다.
    - EC2 인스턴스를 생성할 때 EBS volume의 Encrypted 옵션을 Encrypted 모드로 변경해야 한다.
    default로 암호화 모드는 꺼져있다.

Hibernate mode는 60일까지 사용이 가능하다.