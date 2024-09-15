# AWS Container Services
## 1. EKS - Elastic Kubernetes Service
AWS 공식 홈페이지의 EKS 정의는 다음과 같습니다.

>Amazon Elastic Kubernetes Service(Amazon EKS)는 AWS 클라우드와 온프레미스 데이터 센터에서 Kubernetes를 실행하는 데 사용되는 관리형 Kubernetes 서비스입니다. 클라우드에서 Amazon EKS는 컨테이너 예약, 애플리케이션 가용성 관리, 클러스터 데이터 저장 및 다른 주요 작업을 담당하는 Kubernetes 컨트롤 플레인의 가용성과 확장성을 관리합니다. Amazon EKS를 사용하면 AWS 네트워킹 및 보안 서비스와의 통합뿐만 아니라 AWS 인프라의 모든 성능, 규모, 신뢰성 및 가용성을 활용할 수 있습니다 온프레미스에서 EKS는 완벽하게 지원되는 일관된 Kubernetes 솔루션을 제공합니다. 통합된 도구를 사용하여 AWS Outposts, 가상 머신 또는 베어 메탈 서버에 간편하게 배포할 수 있습니다.

AWS에서 Kubernetes 를 사용하는 서비스 입니다.

다른 AWS 서비스와 연동이 가능합니다.
- ALB
- ECR
- CloudWatch

## 2. ECS - Elastic Container Service
AWS 공식 홈페이지의 ECS 정의는 다음과 같습니다.

>Amazon Elastic Container Service(Amazon ECS)는 컨테이너식 애플리케이션의 배포, 관리 및 크기 조정을 간소화하는 완전관리형 컨테이너 오케스트레이션 서비스입니다. 애플리케이션과 필요한 리소스를 설명하기만 하면 Amazon ECS가 유연한 컴퓨팅 옵션 전반에서 애플리케이션을 시작, 모니터링 및 확장하여 애플리케이션에 필요한 다른 지원 AWS 서비스와 자동으로 통합합니다. 사용자 지정 크기 조정 및 용량 규칙 생성과 같은 시스템 작업을 수행하고 애플리케이션 로그 및 원격 분석의 데이터를 관찰하고 쿼리합니다.

## ECS는 .. 
AWS 컨테이너 오케이스레이션 서비스

컨테이너를 실행하고 배포 관리하는 서비스

두가지 모드가 있습니다.
1. EC2 Mode
- EC2를 활용한 컨테이너 서비스

2. AWS Fargate
- Serverless 컨테이너 서비스

대규모 어플리케이션을 구축하고 배포하는데 좋습니다.

### 1. EC2 Mode
직접 관리하는것이 필요합니다.
- 스케줄링 등

vpc 안에 생성됩니다.

주로 대규모 어플리케이션에 사용 됩니다.

비용이 많이 소모됨

### 2. AWS Fargate
Serverless 서비스 입니다.

vpc 밖에 생성되어 vpc 안에 접근할 수 있습니다.

단기적 작업에 주로 사용 됩니다.

비용관리가 비교적 쉬움

#### 2.1 AWS App Runner
AWS 공식 홈페이지의 App Runner 정의는 다음과 같습니다.

>AWS App Runner는 인프라나 컨테이너와 관련한 경험이 없더라도 컨테이너화된 웹 애플리케이션과 API 서비스를 구축, 배포 및 실행할 수 있는 완전관리형 컨테이너 애플리케이션 서비스입니다.

aws 경량 컨테이너기반 애플리케이션 실행 서비스 입니다.

cicd , LB , vpc 생성 등의 추가 작업이 필요없이 , 클릭몇번으로 애플리케이션을 구축할 수 있씁니다.

vpc 지원하기에 vpc 자원에 접근할 수 있습니다.

**임시 스토리지기반**
- 저장공간이 영구적이지 않기 때문에 , 끝나면 저장공간이 날아갑니다.
- 따라서 vpc를 지원하기에 외부 RDS 등과 같이 실행시켜서 데이터를 저장해야 합니다.

## 3. Lambda를 통한 Container 실행
람다에서 ECR에 들어가있는 이미지를 람다를 통해 배포할 수 있습니다.

AWS Lambda의 Base Image를 기반으로 이미지를 생성한 이후 , ECR에 push한 뒤 , 람다에서 실행하면 됩니다.
- Base Image 종류 : Node.js , Python , Java , .Net , Go , Ruby

aws base image를 사용하지 않고 , 커스텀해서 이미지를 생성하는것 또한 가능합니다.
- **Lambda Runtime Api를 구현해야만 사용할 수 있습니다.**

용량 지원은 최대 10GB 까지 가능합니다

### 3.1 사용 사례
AWS Lambda의 트리거 기능들을 활용하여 컨테이너 기반 코드를 실행하고 싶을 때..

Lambda의 250mb 용량 제한을 우회하고 싶을 때 ..
- 직접코드 작성 시 250mb 용량 제한이 있는데, 이걸 이미지기반으로 바꾸면 10GB 까지 늘어납니다.