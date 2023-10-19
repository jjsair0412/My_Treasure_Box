# Amazon EKS ( Elastic Kubernetes Service )
- [EKS Terraform github](https://github.com/terraform-aws-modules/terraform-aws-eks#important-note)
## Overview

EKS는 자체 Kubernetes control plane 노드를 설치 운영할 필요 없이, Kubernetes 실행에 사용할 수 있는 Amazon 관리형 Kubernetes 서비스 입니다.

AWS의 여러 가용영역에 걸쳐 Kubernetes control plane을 구성할 수 있으며, 비정상 인스턴스를 감지 및 교체하고 자동화된 버전업데이트 및 패치를 제공합니다.

AWS의 여러 서비스들과 통합하여 운영할 수 있습니다.
- Amazon ECR 
- ELB
- IAM
- VPC
등

### EKS 지원 버전
4개의 마이너 버전을 지원 (2023.10.19 현재 버전으로 1.24 ~ 1.28) 하며, 평균 3개월마다 새로운 버전을 제공하고, 각 버전은 12개월간 지원합니다.
- [관련 docs](https://docs.aws.amazon.com/eks/latest/userguide/kubernetes-versions.html)

### AWS EKS 배포시 알아야할점
- Kubernetes API Server 및 etcd를 AWS에서 관리하게 됩니다.
- 3개의 AZ에서 Kubernetes control plane을 실행합니다.
- 클러스터에 노드를 추가하게 되면 , control plane이 스케일업됩니다.

EKS를 자동화 배포하기 위해 아래 방법들이 추천됩니다.
1. AWS 웹 인터페이스
2. eksctl
3. Terraform

해당 문서에선 Terraform을 통해 EKS를 배포합니다.

## Deploy Amazon EKS with Terraform
Terraform을 통해 EKS를 배포합니다.
- [Terraform 정리해둔 문서](https://github.com/jjsair0412/My_Treasure_Box/blob/main/Terraform/Terraform_basic_info.md)

먼저 Terraform이 정상적으로 설치되어있는지 확인합니다.

```bash
$ terraform version
Terraform v1.5.7
```

aws-cli 를 통해 aws 계정이 정상적으로 등록되어있는지를 확인합니다.
- 아래명령어를 호출하면, 현재 등록된 AWS 자격 증명과 관련된 ID값을 반환합니다.
- 반환된다면 정상처리된것.
```bash
aws sts get-caller-identity
```



### 1. main.tf 정의
main.tf파일을 정의합니다.
```tf
provider "aws" {
  region = "ap-northeast-2"
}

data "aws_availability_zones" "available" {}

data "aws_eks_cluster" "cluster" {
  name = module.eks.cluster_id
}

data "aws_eks_cluster_auth" "cluster" {
  name = module.eks.cluster_id
}

locals {
  cluster_name = "basick8s"
}

provider "kubernetes" {
  host                   = data.aws_eks_cluster.cluster.endpoint
  cluster_ca_certificate = base64decode(data.aws_eks_cluster.cluster.certificate_authority.0.data)
  token                  = data.aws_eks_cluster_auth.cluster.token
}

module "eks-kubeconfig" {
  source     = "hyperbadger/eks-kubeconfig/aws"
  version    = "1.0.0"

  depends_on = [module.eks]
  cluster_id =  module.eks.cluster_id
  }

resource "local_file" "kubeconfig" {
  content  = module.eks-kubeconfig.kubeconfig
  filename = "kubeconfig_${local.cluster_name}"
}

module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "3.18.1"

  name                 = "k8s-vpc"
  cidr                 = "172.16.0.0/16"
  azs                  = data.aws_availability_zones.available.names
  private_subnets      = ["172.16.1.0/24", "172.16.2.0/24", "172.16.3.0/24"]
  public_subnets       = ["172.16.4.0/24", "172.16.5.0/24", "172.16.6.0/24"]
  enable_nat_gateway   = true
  single_nat_gateway   = true
  enable_dns_hostnames = true

  public_subnet_tags = {
    "kubernetes.io/cluster/${local.cluster_name}" = "shared"
    "kubernetes.io/role/elb"                      = "1"
  }

  private_subnet_tags = {
    "kubernetes.io/cluster/${local.cluster_name}" = "shared"
    "kubernetes.io/role/internal-elb"             = "1"
  }
}

module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "18.30.3"

  cluster_name    = "${local.cluster_name}"
  cluster_version = "1.24"
  subnet_ids      = module.vpc.private_subnets

  vpc_id = module.vpc.vpc_id

  eks_managed_node_groups = {
    first = {
      desired_capacity = 1
      max_capacity     = 10
      min_capacity     = 1

      instance_type = "m5.large"
    }
  }
}
```

#### main.tf 섹션별 주석
EKS cluster를 생성할 때 사용한 main.tf파일에 대한 설명입니다.

main.tf파일의 주요 부분은 모듈별로 확인할 수 있는데, 각각 아래와 같습니다.
1. **module "vpc"**
2. **module "eks"**

#### 1. **module "vpc"**
다음을 aws에 생성하라고 명령합니다.
1. VPC
2. private subnet 3개 , public subnet 3개
3. 단일 NAT Gateway
4. [EKS Cluster에서 사용하는 내부 LB 및 public NLB를 자동 프로비저닝하기 위한 subnet의 TAG](https://docs.aws.amazon.com/eks/latest/userguide/network-load-balancing.html)

#### 2. **module "eks"**
다음을 aws에 생성하라고 명령합니다.
1. control plane
2. worker node
	- worker node들은 EKS에서 private subnet에 생성되야하기 때문에 , ```subnet_ids      = module.vpc.private_subnets``` 섹션을 통하여 VPC 모듈에서 생성한 private subnet을 할당합니다.
3. security group 설정
4. Auto Scalling group 설정

#### 3. **모듈을 제외한 나머지**
다음을 설정합니다.
1. cluster에 대한 올바른 IAM 권한설정
2. cluster health check
3. kubeconfig 파일 생성


### 2. EKS 배포
Terraform init 명령어를 통해 .tfstate 설정파일을 생성해줍니다.

```bash
$ terraform init
```

terraform validate 명령어로 오류가없는지 검증합니다.
```bash
$ terraform validate
```

아래와같은 에러가 발생하는데,, 해당 main.tf파일 들어가서 아래 line들을 제거해줍니다.
- enable_classiclink
- enable_classiclink_dns_support
- enable_classiclink

```bash
╷
│ Error: Unsupported argument
│ 
│   on .terraform/modules/vpc/main.tf line 35, in resource "aws_vpc" "this":
│   35:   enable_classiclink             = null # https://github.com/hashicorp/terraform/issues/31730
│ 
│ An argument named "enable_classiclink" is not expected here.
╵
╷
│ Error: Unsupported argument
│ 
│   on .terraform/modules/vpc/main.tf line 36, in resource "aws_vpc" "this":
│   36:   enable_classiclink_dns_support = null # https://github.com/hashicorp/terraform/issues/31730
│ 
│ An argument named "enable_classiclink_dns_support" is not expected here.
╵
╷
│ Error: Unsupported argument
│ 
│   on .terraform/modules/vpc/main.tf line 1244, in resource "aws_default_vpc" "this":
│ 1244:   enable_classiclink   = null # https://github.com/hashicorp/terraform/issues/31730
│ 
│ An argument named "enable_classiclink" is not expected here.
```

정상수행되면 아래와같은 Success 문구가 출력됩니다.
```bash
Success! The configuration is valid, but there were some validation warnings as shown above.
```

plan 명령을 실행하여, 어떤 인프라를 구축할지 예측 결과를 확인합니다.
```bash
terraform plan
```

plan 수행결과, 테스트가 완료되었다고 판단된다면 apply 명령어로 EKS Cluster를 배포합니다.
- 배포시간은 20분정도 소요됩니다.
- ```Apply complete! Resources: 49 added, 0 changed, 0 destroyed.``` 메세지가 출력되면 성공
```bash
terraform apply
...
Apply complete! Resources: 49 added, 0 changed, 0 destroyed.
```

### 3. 배포결과 확인
tree 명령어로 , main.tf 파일 위치에 어떤파일들이 생성되었는지 확인합니다.
```
tree . -a -L 2   
.
├── .terraform
│   ├── modules
│   └── providers
├── .terraform.lock.hcl
├── README.md
├── kubeconfig_basick8s
├── main.tf
└── terraform.tfstate
```

만들어진 kubeconfig파일 ```kubeconfig_basick8s``` 로 kubectl 명령어를 수행하여 워커노드의 상태를 확인합니다.

