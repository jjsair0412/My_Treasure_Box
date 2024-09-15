# Terraform module 
## Overview 
해당 폴더는 Terraform 코드를 모듈화하는 방안에 대해 기술합니다.

VPC를 하나 생성한 뒤, 해당 vpc에 public subnet을 생성해서, 생성한 public subnet에 nginx web server를 user_data로 프로비저닝하는 테스트를 진행하며 모듈화에 대해 기술합니다.
- [테스트 코드](../Terraform_module/)

테스트 코드 구조
```bash
$ tree
.
├── main.tf
├── modules
│   ├── 01_vpc
│   │   ├── 01-01_subnet
│   │   │   ├── main.tf
│   │   │   ├── output.tf
│   │   │   └── variable.tf
│   │   ├── main.tf
│   │   └── variable.tf
│   ├── 02_routetable
│   │   ├── main.tf
│   │   ├── output.tf
│   │   └── variable.tf
│   ├── 03_securitygroup
│   │   ├── main.tf
│   │   ├── output.tf
│   │   └── variable.tf
│   └── 04_ec2
│       ├── main.tf
│       ├── output.tf
│       └── variable.tf
├── terraform.tfvars
└── variable.tf
```

## 1. Terrafrom module 이란 ?
기본적으로 테라폼은 module 화 하여 리소스나 목적성에 따라 코드를 한데 묶어서 관리할 수 있습니다.

코드를 모듈별로 나눠 관리함으로써, 코드의 복잡성이나 특정 부분을 변경했을 경우 다른 인프라 리소스가 영향을 미치는 상황을 미리 방지할 수 있습니다.

### 1.1 장점
Terraform 코드를 모듈로 관리함으로써 , 다음과 같은 장점을 얻을 수 있습니다.

1. 재 사용성
    - 모듈의 공통 구성요소를 캡슐화 함으로써, 다양한 환경에 재 사용할 수 있습니다.
2. 캡슐화
    - 관련있는 모듈끼리 모아놓고 관리함으로써 휴먼이슈를 줄일 수 있습니다.
3. 버전 관리와 협업
    - 모듈들은 독립적으로 작동하기에, 많은 사람들이 협업할 때 모듈을 공유하고 협업함에 있어서 편리함을 가질 수 있습니다.

## 2. 모듈화 방안
기본적으로 Terraform의 module은 크게 **root module** 과 **child module** 로 나뉩니다.

- **root module**
    - terraform command가 실행되고 있는 최 상단 root 경로의 module
    - root module은 다른 child module의 진입점이 됩니다.
- **child module**
    - 다른 module (root module 포함) 에서 호출하여 사용되는 module

또한 각 모듈들은 각각의 ```main.tf``` , ```variable.tf``` , ```output.tf``` 등의 terraform resource들을 가질 수 있으며, provider를 제각각 두는것 또한 가능합니다.
- default 설정으론, root module의 provider를 child module에서 상속받습니다.

### 2.1 root module의 설정
child module을 정의하기 위해선, root module의 main.tf 파일에서 module을 정의해야 합니다.

```tf
terraform {
  required_providers { # child module은 해당 provider를 상속받습니다.
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.region
  access_key = var.access_key
  secret_key = var.secret_key
}

module vpc {
  source = "./modules/01_vpc" # module의 경로를 작성합니다.
}
```

**그리고 child module에서 사용할 변수를 정의합니다.**

child module들의 모든 변수들은 root module에서 관리한다고 생각하면 됩니다.
따라서 root module의 ```main.tf``` 파일을 정의할 때, root module의 변수를 넣어줍니다.

**가장 중요한 부분은.. root module에 child module을 추가할 때 마다 terraform init 명령어로 terraform.tfstate 에 module 정보를 주입해야 합니다.**

### 2.2 child module의 설정
01_vpc module의 ```main.tf``` 입니다.

```tf
resource "aws_vpc" "demo_vpc" {

  cidr_block = var.vpc_cidr_block

  tags = {
    Name = "Demo VPC"
  }
}
```

01_vpc module의 ```variable.tf``` 입니다.

```tf
variable vpc_cidr_block {
  type        = string
  description = "vpc cidr block"
}
```

여기서 주목할 부분이 있는데, child module에서 var 함수를 사용하여 ```vpc_cidr_block``` 을 정의하고 있습니다.

***그런데 해당 값을 실제로 주입시켜주는 부분은 root module의 main.tf에서 진행하게 됩니다. 해당 테스트 코드를 예시로 들어서 설명하면, 01_vpc module에서 사용할 변수는 root module의 main.tf 에서 module을 할당할 때 정의해주어야 합니다.***

root module의 ```main.tf``` 파일입니다.
- vpc variable로 설정한 부분에 또 var로 변수로 할당하였는데, 이것은 root module의 variable.tf를 의미하게 됩니다.

  **결과적으로 아래 예시처럼 테라폼 코드를 관리하게 되면, 모든 module들의 변수를 root module의 terraform.tfvars 또는 variable.tf 에서 공통관리할 수 있게됩니다**

```tf
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.region
  access_key = var.access_key
  secret_key = var.secret_key
}

module vpc {
  source = "./modules/01_vpc"

  ## vpc variable ##
  vpc_cidr_block = var.vpc_cidr_block
}
```

root module의 ```variable.tf``` 입니다.

```tf
variable access_key {
  type        = string
  description = "aws IAM user access key"
}

variable secret_key {
  type        = string
  description = "aws IAM user secret key"
}

variable region {
  type        = string
  description = "region name"
}

## 01_vpc module variable ## 
variable vpc_cidr_block {
  type        = string
  description = "vpc cidr block"
}
```

### 2.3 child module에서 다른 child module의 값 주입하기
child module에서 다른 child module의 값을 참조해야할 일이 많습니다.

해당 예시는 01_vpc module 에서 생성한 vpc의 id 값을, 02_routetable 에서 참조합니다.

이럴땐 아래 순서대로 작업합니다.

#### 2.3.1 참조 대상이 되는(01_vpc module의 vpc id) module에서 output.tf 파일에 참조 대상 값(vpc id)을 생성
- 먼저, 1번의 내용대로 ```output.tf``` 를 정의합니다.

```tf
## 01_vpc module의 전문 내용
### main.tf ###
resource "aws_vpc" "demo_vpc" {
  cidr_block = var.vpc_cidr_block

  tags = {
    Name = "Demo VPC"
  }
}


### output.tf ###
output demo_vpc_id {
  value       = aws_vpc.demo_vpc.id ## output으로 value를 root module로 내보냅니다.
  description = "description"
}
```

#### 2.3.2 참조 할 child module(02_routetable) 을 정의한 root module의 main.tf module에서 output.tf 값을 꺼내옴
- 2번의 내용대로 root module에서 02_routetable child module을 정의한 ```main.tf``` 에서 , output.tf로 꺼낸 값을 참조합니다.

  참조할 땐, ```module.{module_name}.{output_name}``` 으로 참조합니다.

```tf
## root module의 main.tf
### main.tf ###
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.region
  access_key = var.access_key
  secret_key = var.secret_key
}

module vpc {
  source = "./modules/01_vpc"

  ## vpc variable ##
  vpc_cidr_block = var.vpc_cidr_block
}

module routetable {
  source = "./modules/02_routetable"
  
  ## vpc id ##
  ## vpc module에서 output으로 꺼낸 demo_vpc_id 를 참조합니다.
  demo_vpc_id = module.vpc.demo_vpc_id
}
```

#### 2.3.3 참조 할 child module(02_routetable) 에서, variable로 가져온 값을 변수로 할당
- 3번의 내용대로 child module 에서, ```module.{module_name}.{output_name}``` 로 가져온 다른 child module의 값을 변수로 할당하여 사용합니다.

```tf
## 02_routetable child module ##
### variable.tf에서 변수로 할당 ###
variable demo_vpc_id {
  type        = string
  description = "demo vpc id"
}


### main.tf에서 해당 변수를 사용 ###
resource "aws_route_table" "demo_route_table" {
  vpc_id = var.demo_vpc_id ## 01_vpc child module에서 꺼낸값

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw.id
  }

  tags = {
    Name = "demo route table"
  }
}

resource "aws_internet_gateway" "igw" {
  vpc_id = var.demo_vpc_id ## 01_vpc child module에서 꺼낸값

  tags = {
    Name = "demo igw"
  }
}
```

### 2.4 중첩 child module의 관리
child module 내부에 child module이 위치할 수 있습니다.

아래 구조처럼, 01_vpc child module 내부에 01-01_subnet child module이 위치합니다. 이것이 중첩 module 입니다.
```bash
$ tree
.
├── main.tf
├── modules
│   ├── 01_vpc
│   │   ├── 01-01_subnet
│   │   │   ├── main.tf
│   │   │   ├── output.tf
│   │   │   └── variable.tf
│   │   ├── main.tf
│   │   └── variable.tf
...
```

이럴땐, root module에서 해당 중첩 module을 정의할 때, ```source``` 부분에 정확한 디렉토리 레벨을 나타내주기만 한 뒤, ```output.tf```, ```variable.tf``` 등을 통해 기존 child module과 동일하게 사용하면 됩니다.


중첩 child module 인 01-01-subnet module을 root module의 ```main.tf``` 에선 아래처럼 정의해 사용하면 됩니다.

```
...
module subnet {
  source = "./modules/01_vpc/01-01_subnet"

  ## subnet variable ##
  public_subnet_cidr = var.public_subnet_cidr

  ## vpc id ##
  demo_vpc_id = module.vpc.demo_vpc_id
  
  ## route table id ##
  demo_route_table_id = module.routetable.demo_route_table_id
}
...
```

### 2.5 root module의 data.tf 값을 가져와서 사용하기
root module에 존재하는 ```data.tf``` 를, child module 에서 사용할 수 있습니다.

에를들어, data.tf에서 aws_ami 를 선언합니다.

```
## root module의 data.tf 전문 ##
data "aws_ami" "ubuntu" {
  most_recent = true
  
  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-focal-20.04-amd64-server-*"]
  }
  
  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  owners = ["099720109477"] # Canonical
}
```

그리고, root module에서 data.tf 값을 사용할 대상 child module를 선언할 때, ```data.{data_resource_name}.{data_name}``` 을 사용해서 선언해 줍니다.
- 해당 예시에선 , 04_ec2 child module이 ```data.aws_ami.ubuntu.id``` 값을 사용합니다.
```
...
module ec2_instance {
  source = "./modules/04_ec2"

  availability_zone = var.availability_zone
  instance_type = var.instance_type
  public_subent_id = module.subnet.public_subent_id
  ubuntu_ami_id = data.aws_ami.ubuntu.id
  allow_http_sg_id = module.sg.allow_http_sg_id
}
...
```

사용할 child module의 ```variable.tf``` 파일에, root module에서 선언한 변수 값(예시에선 ```allow_http_sg_id```) 을 선언하여 child module 에서 사용합니다.

```
## 04_ec2 child module ##
### variable.tf ###
variable instance_type {
  type        = string
}

variable availability_zone {
  type        = string
}

variable public_subent_id {
  type        = string
}

variable ubuntu_ami_id { # root module에서 가져온 변수 선언
  type        = string
}

variable allow_http_sg_id {
  type        = string
}
...

## main.tf ##
resource "aws_instance" "demo" {
  ami           = var.ubuntu_ami_id # 사용
  instance_type     = var.instance_type
  availability_zone = var.availability_zone
  
  network_interface {
    network_interface_id = aws_network_interface.demo_network_interface.id
    device_index         = 0
  }
  
  tags = {
    Name = "demo ec2 instance"
  }
}

resource "aws_network_interface" "demo_network_interface" {
  subnet_id   = var.public_subent_id
  security_groups = [var.allow_http_sg_id]

  tags = {
    Name = "public_network_interface"
  }
}

resource "aws_eip" "demo_eip" {
  instance = aws_instance.demo.id
  network_interface = aws_network_interface.demo_network_interface.id
  domain   = "vpc"
}
```