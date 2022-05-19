# AWS
- 해당 문서는 AWS cloud의 기초 이론을 정리한 문서입니다.

## EC2

### AWS EC2 ?

**Elastic Compute Cloud(EC2)**
안전하고 크기 조정이 가능한 **컴퓨팅 용량을 클라우드에서 제공하는 웹 서비스**이다.
사용자는 간단한 **웹 서비스 인터페이스를 통해 간편하게 필요한 용량으로 서버를 구성**할 수 있다. 

( cpu 개수 등 .. )
컴퓨팅 리소스에 대한 **포괄적 제어권을 제공**하며 , 

**Amazon의 검증된 컴퓨팅 인프라에서 실행**할 수 있다.

### SSH

서버 접근 시 **사용되는 기본적인 것들 중 한가지**이다.
네트워크 프로토콜중 하나이며 , 

**클라이언트와 서버간 통신시 보안적으로 안전하게 통신하기위해 사용하는 프로토콜**이다.
보통 Password 인증과 RSA 공개키 암호화 방식으로 연결하는데 , 

AWS는 RSA 공개키 방식으로 연결이 된다.
**SSH의 기본적인 포트번호는 22번**이다.

### SSH로 EC2에 연결할 때 체크해야할 것들

1. **포트번호 확인**
2. **public ip 확인**
3. **보안그룹 확인 ( security group )**
VPC에 만들어지게 된다. **방화벽**임.
내 클라이언트 ip와 security group에 등록된 ip를 대조해서 등록이 되어있는지 확인한다.
4. **EC2 인스턴스가 public subnet에 있는지 확인한다.**
해당 서브넷이 연결된 라우팅 테이블을 확인한다.
internet gateway가 0.0.0.0/0 인지 확인한다.

### 서버에서 listen port 확인하는 명령어

```bash
$ netstat -lntp
```

---

## AWS Region

aws 데이터센터를 클러스터링하는 물리적 위치를 리전이라 한다.
각 **리전마다 서비스 가격이 상이하다**는 특징이 있다.

### 가용 영역 ( Availability Zone )

**리전 내부 실제 데이터센터의 위치**를 의미한다.
데이터센터는 물리적으로 멀리 떨어져잇는데 , 

그러한 이유는 자연재해등의 이유로 서비스 중단을 막기 위함이기 때문이다.

가용 영역은 **EC2 인스턴스를 생성할 때 서브넷으로 선택할 수 있다.**
**S3와같은 서비스는 가용 영역을 모두 사용하기때문에 서브넷을 선택하지 않는다.**

### 예시

서울 리전에는 물리적 데이터센터가 4개 있다.
서울 리전에는 Availability Zone이 4개 있다.
각 Availability Zone은 1개 이상의 데이터센터로 구성된다.

---

## AWS VPC

AWS 가상 네트워크로서 **AWS 사용자별 전용 네트워크를 구축**한다.
**VPC는 계정 생성시 자동으로 생성**되게 된다.
**VPC의 범위는 Region이다. 각 Region별로 존재하고 , Region 전체를 감싸게 된다.**

VPC는 만들어질때 **사설 네트워크 IP 대역폭이 할당**된다.
**B클래스의 사설 네트워크 대역**을 가진다.
해당 **네트워크 대역을 분리시킬 수 있는데 , 이때 CIDR 블록을 지정해서 분리**시킬 수 있다.

예를들어서 VPC 네트워크 대역이 **172.16.31.0/16** 이라면
CIDR 블록을 이용해

1. 172.16.0.0/24
2. 172.16.1.0/24
3. 172.16.2.0/24
4. 172.16.3.0/24

이렇게 분리시킬 수 있다.

또한 **분리된 서브넷들은 하나의 Availability Zone에서만 있을 수 있다.**
하나의 **Availability Zone에 하나의 서브넷만 존재**할 수 있다.

### VPC 구성 요소

1. **서브넷 ****: VPC의 IP 주소 범위

1. **라우팅 테이블 ****: 네트워크 트래픽을 전달할 위치를 결정하는 데 사용되는 라우팅 규칙 집합

1. **인터넷 게이트웨이** : VPC의 리소스와 인터넷 간의 통신을 활성화하기 위해 VPC에 연결하는 게이트웨이.
인터넷을 연결하기 위해 필요한 서비스

1. **NAT 게이트웨이** : 네트워크 주소 변환을 통해 프라이빗 서브넷에서 인터넷 또는 기타 AWS 서비스에 연결하는 게이트웨이.
private subnet과 반대되는 계념은 public subnet.
라우팅 테이블에 외부 인터넷으로 나가는 서비스가 인터넷 게이트웨이를 통한다면 public subnet,
NAT 게이트웨이를 통한다면 private subnet이라 한다.
EC2에 공인 ip가 붙어있다면 public subnet, 공인 ip를 가지고있지 않지만 NAT gateway를 통한다면 private subnet

1. **씨큐리티 그룹** : 보안 그룹은 인스턴스에 대한 인바운드 및 아웃바운드 트래픽을 제어하는 가상 방화벽 역할을 하는 규칙 집합.

1. **VPC 엔드포인트 ****: VPC 내부의 여러 aws 서비스간 인터넷 게이트웨이나 nat를 사용하지 않고 바로 aws 서비스를 사용할 수 있는 aws 서비스

---

## public subnet & private subnet

VPC를 만들게 되면 , **default 라우팅 테이블이 생성**된다.
기본적으로 **같은 VPC끼리는 통신되도록 라우팅테이블이 생성**된다.

각 **VPC끼리 통신하기위해서는 꼭 라우팅테이블을 거쳐서 가야한다.**
이때 **라우팅테이블에 NAT gateway를 통해 외부와 통신한다면 private subnet**이고 ,

내가 알고있는 NAT이다.

**외부와 통신하기위해서 internet gateway를 라우팅테이블에 추가**해준다면 **public subnet**이다.

![public subnet & private subnet image][public subnet & private subnet image]

[public subnet & private subnet image]:./images/public subnet & private subnet image.PNG

**private subnet**의 라우팅테이블을 보면 **인터넷으로 가기 위해서 ( 0.0.0.0/0 ) nat-gateway를 거친다**는것을 볼 수 있고 , ( 위 ) 

**public subnet**의 라우팅테이블을 보면 **인터넷으로 가기 위해서 ( 0.0.0.0/0 )  igw ( internet gateway ) 를 거친다**는것을 볼 수 있다. ( 아래 )

---

## AWS s3

### what is s3 ?

simple storage service의 약자이다.
인터넷용 스토리지 서비스인데 , 개발자 친화적으로 되어있다.

aws s3는 정말 많은 데이터를 저장할 수 있고 빠르게 검색 , 데이터를 가져올 수 있다.

s3는 **bucket**이라는 개념으로 동작하게 된다.

또한 **s3 버킷에 저장된 데이터는 언제든 다운로드받을 수 있고 업로드**할 수 있다.

### Amazon S3의 장점

1. **버킷 만들기** : 데이터를 저장하는 버킷을 만들고 해당 버킷의 이름을 지정한다.
버킷은 데이터 스토리지를 위한 Amazon S3의 기본 컨테이너이다.

1. **데이터 저장** : 버킷에 데이터를 무한정으로 저장한다.
Amazon S3 버킷에 객체를 원하는 만큼 업로드할 수 있으며, 
    
    각 객체에 최대 5TB의 데이터를 포함할 수 있다.
    각 객체는 고유한 개발자 할당 키를 사용하여 저장 및 검색한다.
    

1. **데이터 다운로드** : 데이터를 직접 다운로드하거나 다른 사람이 다운로드할 수 있도록 한다.
언제든지 데이터를 직접 다운로드하거나 다른 사람이 다운로드하도록 허용할 수 있다.

1. **권한** : 데이터를 Amazon S3 버킷으로 업로드 또는 다운로드하려는 사용자에게 액세스 권한을 부여하거나 해당 권한을 거부한다.
3가지 유형의 사용자에게 업로드 및 다운로드 권한을 부여할 수 있다.
인증 메커니즘을 사용하면 데이터가 무단으로 액세스되지 않도록 보호하는 데 도움이 될 수 있다.

1. **표준 인터페이스** : 모든 인터넷 개발 도구 키트에서 사용할 수 있도록 설계된 표준 기반 REST 및 SOAP 인터페이스를 사용한다.

### bucket ?

버킷은 **Amazon S3에 저장된 객체에 대한 컨테이너**이다. **모든 객체는 어떤 버킷에 포함**된다.
**버킷은 region 종속적**이다. 리전마다 존재한다.

예를 들어 photos/puppy.jpg로 명명된 객체는 미국 서부(오레곤) 리전의 awsexamplebucket1 버킷에 저장되며
URL [https://awsexamplebucket1.s3.us-west-2.amazonaws.com/photos/puppy.jpg를](https://awsexamplebucket1.s3.us-west-2.amazonaws.com/photos/puppy.jpg%EB%A5%BC) 사용하여 주소를 지정할 수 있다.

해당 uri를 통해서 객체로 접근할 수 있으며 , html파일을 버킷에 올려두고 해당 파일의 권한을 public으로 주면 , 마치 웹 페이지처럼 동작한다.

### 객체 ?

객체는 **Amazon S3에 저장되는 기본 개체이다**. 객체는 객체 데이터와 메타데이터로 구성된다.
메타데이터는 객체를 설명하는 이름-값 페어의 집합이다.

 

### s3 사용법

ubuntu나 linux에서 파일을 다운로드 / 업로드하고싶다면 **먼저 aws access key와 secret key를 aws configure에 등록**시켜야 한다.

올린 파일의 권한을 콘솔에서 public으로 준다면 , **파일 uri를 통해서 누구나 접근해서 볼 수 있다.**
이걸이용해서 html파일을 버킷에 올려서 웹페이지처럼 사용할 수 도 있다.

***업로드***

```bash
# 구조
$ aws s3 cp <file_path> s3://<bucket_name> 

# 실제 사용 예
$ aws s3 cp index.html s3://devopsart-terraform-101-jinseong/
```

index.html이라는 파일을 devopsart-terraform-101-jinseong 이라는 bucket에 업로드
복사해서 넣는다는뜻이다.

***다운로드***

```bash
# 구조
$ aws s3 cp s3://<bucket_name>/<file_name> .

# 실제 사용 예
$ aws s3 cp s3://devopsart-terraform-101-jinseong/testfile .
```

bucket에 존재하는 testfile을 다운로드

---

## AWS IAM

aws iam은 identity and access management의 약자이다.
iam을 사용한다면 **리소스를 사용하도록 인증(로그인) 및 권한을 부여하는 대상을 제어**한다.
**특정 리소스를 사용하도록 허가하거나 접근하지 못하도록 막는것**이다.

IAM은 리전에 종속적이지 않다. global resource이다.

### IAM 구성요소

1. **IAM user**
aws 내에서 생성하는 사용자 및 애플리케이션을 의미한다.
2. **IAM group**
iam user의 집합이다. 다수 사용자들에대해서 권한 제어를 보다 쉽게 할 수 있다.
3. **IAM role**
특정 권한을 가진 IAM 자격 증명이다. 이 Role을 사용함으로써 특정 사용자 혹은 애플리케이션 , aws 서비스에 접근 권한을
위임할 수 있다.
4. **IAM policy**
AWS에 접근하는 해당 권한을 정의하는 개체, aws IAM 리소스들과 연결하여 사용할 수 있다.
user, group, role은 policy를 갖는다.

### IAM Policy structure

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "iam:ChangePassword"
            ],
            "Resource": [
                "arn:aws:iam::*:user/${aws:username}"
            ]
        },
        {
            "Effect": "Allow",
            "Action": [
                "iam:GetAccountPasswordPolicy"
            ],
            "Resource": "*"
        }
    ]
}
```

json 형태를 띈다.

### 각 key에대한 설명

1. **Effect**
"allow" 또는 "deny" 일 수 있다. 기본적으로 IAM 사용자에게는 리소스 및 API 작업을 사용할 권한이 없기에 모든 요청이 거부된다.
2. **Action**
권한을 부여하거나 거부할 특정 API 작업.
3. **Resource**
작업에 영향을 받는 리소스. Amazon 리소스 이름(ARN) 을 사용하거나
명령문이 모든 리소스에 적용됨을 표시하는 와일드카드 (*) 를 사용한다.
4. **Condition**
선택사항이다.
다양한 조건문 ( if문 같은 ) 을 넣을 수 있다.
대표적으로 ip가 잇을 수 있다. 특정 ip일 경우에만 허용.

policy는 내가 만들 수도 있고 , aws에서 미리 만들어놓은 policy를 사용할 수 도 있다.

### 참고

만약 aws group에 어떤 user가 등록됐다고 생각했을 때 ,
group에 policy와 user의 policy 둘다 있다고 생각하자.
그렇다면 동작할때는 or의 형태로 동작하게 된다.
그니까 두개의 policy 모두 합쳐서 동작한다.

그런데 allow는 둘다 or로 되지만 ,
deny가 있다면 deny가 우선된다.

---

## AWS DynamoDB

DynamoDB는 aws에서 서비스하고있는 **Nosql key-value db**이다.
**확장성이 좋고 , 큰 테이블을 저장**할 수 있다.