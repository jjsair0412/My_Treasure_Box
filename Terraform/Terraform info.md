# Terraform
- Terraform의 이론과 사용 방안을 정리한 문서입니다.

## 테라폼 전체 실습 예제

https://github.com/jjsair0412/Terraform

## what is Terraform ?

**infrastructure as Code**

**코드로써의 인프라** 이다.
코드로서의 인프라는 , **미들웨어 , 서버, 서비스 등 인프라 구성요소들을 코드를 통해 구축**하는것
iac는 코드로써의 장점 . 즉 , **작성 용이성 , 재사용성 , 유지보수 등의 장점을 가질 수 있다.**

테라폼은 현재 가장 많이 사용되고있는 Iac 도구이다.
**AWS, Azure, GCP 같은 퍼블릭 클라우드뿐만 아니라 다양한 서비스들 역시 지원**한다.

---

## Terraform 구성 요소

1. **provider** : 테라폼으로 생성할 인프라의 종류 ( aws , azure , 등 .. )
2. **resource** : 테라폼으로 실제로 생성할 인프라 자원을 의미
3. **state** : 테라폼을 통해 생성한 자원의 상태를 의미 , 파일형태로 남게 된다. 테라폼 명령을 실행한 결과물.
4. **output** : 테라폼으로 만든 자원을 변수형태로 state파일에 저장하는것을 의미한다.
5. **module** : 공통적으로 활용할 수 있는 코드를 문자 그대로 모듈형태로 정의하는것을 의미한다.
6. **remote** : 다른경로의 state를 참조하는것을 말한다. 보통 output 변수를 불러올 때 사용한다.
7. **backend** : terraform의 상태를 저장할 공간을 지정하는 부분. backend를 사용하면 현재 배포된 최신 상태를 외부에 저장하기 때문에 다른 사람과의 협업이 가능하다. 대표적으로 AWS S3 가 있다.

---

## Terraform 기본 명령어

1. **init** : 테라폼 명령어 사용을 위해 각종 설정을 진행한다.
최초의 테라폼 명령어를 실행할 때 해주어야하는 명령어.
2. **plan** : 테라폼으로 작성한 코드가 실제로 어떻게 만들어질지에 대한 예측결과를 보여준다.
3. **apply** : 테라폼 코드로 실제 인프라를 생성하는 명령어
4. **import** : 이미 만들어진 자원 ( aws 자원 등 ) 을 state 파일로 옮겨주는 명령어.
이미 만들어진 자원을 코드로 만들고 싶을 때 사용
5. **state** : 테라폼 state를 다루는 명령어 . 하위 명령어로 mv, push 와 같은 명령어가 있다.
6. **destroy** : 생성된 자원들 , state 파일을 삭제하는 명령어

---

## Terraform process

1. **Terraform code 작성**
2. **init**

      테라폼 설정 진행

1. **plan**

      어떻게 생성될지 확인

1. **apply**

      apply로 인프라 생성

**apply 시 실제 인프라에 영향을 미치기 때문에** **항상 plan하는 습관을 들여야 한다.**

---

## Install Terraform and AWS Cli

1. **AWS Cli 설치**

```bash
# Linux x86 (64-bit)
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# Linux ARM
curl "https://awscli.amazonaws.com/awscli-exe-linux-aarch64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install
```

1. **Terraform 설치**

[Downloads | Terraform by HashiCorp](https://www.terraform.io/downloads.html)

---

## AWS Configure 세팅

AWS IAM에서 access key와 secret key를 발급받아서 작성한다.

**절대 깃허브나 다른공간에 key값을 공유하면 안됀다.**

```bash
$ aws configure
AWS Access Key ID [None]: <액세스 키 ID>
AWS Secret Access Key [None]: <비밀 액세스 키>
Default region name [ap-northeast-2]: <리전 정보>
Default output format [json]: <json>
```

세팅 이후 제대로 설정되었는지 아래 명령어를 통해 확인한다.

```bash
$ cat ~/.aws/credentials
```

현재 설정한 사용자가 누구인지 확인하려면 아래 명령어를 입력한다.

```bash
$ aws sts get-caller-identity
```

---

## Terraform 작동 원리

Terraform에는 3가지 형상이 존재한다.

1. **Local 코드** : 현재 개발자가 작성 / 수정하고 있는 코드
2. **AWS 실제 인프라** : 실제로 AWS에 배포되어 있는 인프라
3. **Backend에 저장된 상태** : 가장 최근에 배포한 테라폼 코드 형상

여기서 가장 중요한것은 AWS 실제 인프라와 Backend에 저장된 상태가 100% 일치하도록 만드는 것.

일치하지 않고 달라질 수 있는데 , 
달라지는 경우는 , 

1. 로컬에서 사용자가 코드를 작성해서 인프라를 올리고 콘솔에서 한번 더 바꾸는 경우
2. state파일을 다르게 변경시켜서 코드와 달라지는경우

등 이 있다.

테라폼에서는 이를 막기 위해서 **import, state 등 여러 명령어를 제공**한다.

---

## Terraform으로 인프라 만드는 순서

### 참고

**tf 리소스파일은 여러개가 같이 있어도 무관하다.**

1. **provider 생성**
    
    ```bash
    provider "aws"{
      region = "us-east-2"
    }
    ```
    
    .tf의 확장자를 갖고있는 provider를 생성
    리전정보 등의 설정값이 들어감
    

1. **Terraform init**
    
    ```bash
    $ terraform init
    ```
    
    지정 **backend에 상태 저장을 위한 .tfstate 파일을 생성**한다. 
    
    여기엔 **가장 마지막에 적용한 테라폼 내역이 저장**된다.
    
    init을 완료하게 되면 , local에 .tfstate에 정의된 내용을 담은 .terraform 파일이 생성된다.
    
    기존에 다른 개발자가 .tfstate에 인프라를 정의해 놓은 것이 있다면, 
    
    다른 개발자는 init 작업을 통해 local에 동기화를 맞출 수 있다.
    

1. **resource 파일 생성**
    
    ```bash
    resource "aws_s3_bucket" "test" {
      bucket = "terraform101-jinsoung"
    }
    ```
    
    .tf의 확장자를 갖고있는 리소스파일을 생성한다.
    여기에는 어떤 리소스를 만들것인가가 들어가게 된다.
    
    그리고 테라폼 내부에서 마치 변수처럼 사용하게될 이름을 지정한다. 
    
    여기서는 test
    

1. **Terraform plan**
    
    ```bash
    $ terraform plan
    ```
    
    정의한 코드가 어떤 인프라를 만들게 되는지 예측 결과를 알려준다.
    
    그러나 plan시 에러가 없다고 출력됐다 하더라도 , 실제 적용했을 때 에러가 발생할 가능성이 있다.
    
    plan 명령어는 자주 실행해서 어떤 결과가 나올지 계속 확인하는 습관을 들여야 한다.
    

1. **Terraform apply**
    
    ```bash
    $ terraform apply
    ```
    
    실제로 코드로만든 인프라를 배포한다.
    
    apply를 완료하면 , AWS상에 실제로 해당 인프라가 생성되고 
    
    작업 결과가 backend의 .tfstate파일에 저장된다.
    
    해당 결과는 local에 .terraform 파일에도 저장된다.
    

1. **Terraform import**
AWS에 배포된 리소스를 terraform state로 옮겨주는 작업
    
     
    이는 local의 .terraform에 해당 리소스의 상태 정보를 저장해주는 역할을 함.
    
    코드를 생성시켜주는것은 아니다.
    
    Apply 전까지는 backend에 저장되지 않음.
    
    Import 이후에 plan을 하면 로컬에 해당 코드가 없기 때문에 리소스가 삭제 또는 변경된다는 결과 출력
    
    이 결과를 바탕으로 코드를 작성하면 됌.
    
    만약 기존에 인프라를 AWS에 배포한 상태에서 테라폼을 적용하고 싶으면 모든 리소스를 terraform import로 옮겨야 함.
    
    번거로운 경우에는 처음부터 다시 작업해서 리소스를 올릴 수 있지만, 실제 서비스가 되는 인프라를 내리는 건 위험할 수 있다.
    
    ---
    
    ## Terraform 변수 참조 방법
    
    아래 예처럼 , .tf 파일 내부에서 다른 변수를 참조할 수 있다.
    
    리소스를 만들 때 변수명을 만들어줬엇는데, 그걸 이용해서 참조할 수 있다.
    
    public subnet resource를 보면 , 맨위 resource인 jinseong의 id를 참조해서 id값을 만들고 잇는것을 볼 수 있다.
    
    ```bash
    resource "aws_vpc" "jinseong" {
      cidr_block = "10.0.0.0/16"
    
      tags = {
        Name = "terraform-101"
      }
    }
    
    resource "aws_subnet" "public_subnet" {
      vpc_id = aws_vpc.jinseong.id # aws_vpc 리소스 jinseong의 id를 참조한다.
      cidr_block = "10.0.0.0/24"
    
      availability_zone = "us-east-2a"
    
      tags = {
        Name = "terraform-101-public-subnet"
      }
    }
    
    resource "aws_subnet" "private_subnet" {
      vpc_id = aws_vpc.jinseong.id # aws_vpc 리소스 jinseong의 id를 참조한다.
      cidr_block = "10.0.10.0/24"
    
      tags = {
        Name = "terraform-101-private-subnet"
      }
    }
    ```
    
    ---
    
    ## Terraform state
    
    Terraform state는 **Terraform apply 명령어를 실행하면 리소스가 생성이 되고 난 후에 terraform.tfstate 파일이 생긴다.**
    
    얘는 **내가 실행한 이 apply의 결과를 저장해놓은 상태**라고 보면 된다.
    
    그런데 **주의할점은 현재 인프라의 상태를 의미하는것은 아니다.**
    **내가 적용한 시점의 상태이지 , 현재 인프라의 상태가 아닐 수 있기 때문**이다.
    
    이러한 **state는 원격 저장소인 backend에도 저장할 수 있다.**
    
    ---
    

## Terraform backend

**Terraform state파일을 어디에 저장하고 가져올지에 대한 설정**이다.

기본적으론 **local storage에 저장되는것이 기본**인데 , 

이걸 **s3나 consul, etcd와 같은곳에 저장하면서 다양한 backend type을 사용**할 수 있다.

**공식문서**

[Backend Overview - Configuration Language | Terraform by HashiCorp](https://www.terraform.io/language/settings/backends)

### **Terraform Backend를 왜 쓸까 ?**

1. **Locking**
보통 테라폼 코드를 혼자 작성하지는 않는다. 그래서 한부분을 여려명이 접근할 수 있다.
인프라를 변경하는것은 굉장히 민감한 작업이 될 수 있는데, **동시에 같은 리소스를 접근하는것을 막아서 의도치 않은 변경을 막을 수 있다.**
2. **Backup**
s3와같은 원격 저장소를 사용함으로써 **state파일의 유실을 방지**한다.

### Terraform Backend의 실행 방식

state파일이 s3에 저장되고 , **s3파일의 경로와 lock을 할 수 있는 파일들이 DynamoDB에 저장되어 현재 이 파일이 사용중인지 아닌지를 판단**한다.

Terraform Backend를 사용하면 , **local에 init하면 생성되는 state와 backend파일은 지워도 정상적으로 작동하게 된다.**

### Terraform Backend 설정 방법

Backend를 설정하고싶은 tf 파일들이 있는곳에서 진행한다.

1. **backend type 결정 및 리소스 생성하기**

결정한 type에 맞게 리소스와 DynamoDB를 terraform 파일을 통해 생성한다.

아래의 예는 s3를 사용했다.

```bash
provider "aws" {
  region = "us-east-2" # Please use the default region ID
  version = "~> 2.49.0" # Please choose any version or delete this line if you want the latest version
}

# S3 bucket for backend
resource "aws_s3_bucket" "tfstate" {
  bucket = "tf101-jinseong22-apne2-tfstate"

  versioning {
    enabled = true # Prevent from deleting tfstate file
  }
}

# DynamoDB for terraform state lock
resource "aws_dynamodb_table" "terraform_state_lock" {
  name           = "terraform-lock"
  hash_key       = "LockID"
  billing_mode   = "PAY_PER_REQUEST"

  attribute {
    name = "LockID"
    type = "S"
  }
}
```

1. **backend 설정하기**

type과 key ( backend에서 저장될 경로 ), 리전 , 암호화 여부등을 설정한 tf파일을 작성한다.

```bash
# 아래 backend 설정파일은 따로 backend.tf 파일을 생성해서 작성해주자.
# 해당 리포지토리에서의 backend 실습은 iam state파일을 backend s3에 저장해주었기 때문에
# iam 폴더에 있다.
terraform {
    backend "s3" { # backend type은 s3라는것
      bucket         = "tf101-jinseong22-apne2-tfstate" # state파일을 저장할 backend s3 bucket 이름
      key            = "terraform/own-your-path/terraform.tfstate" # s3 내에서 저장되는 경로를 의미. key값은 실제 파일의 경로와 같으면 좋다.
      region         = "us-east-2"  # 리전
      encrypt        = true # 암호화 여부
      dynamodb_table = "terraform-lock" # dynamodb_table 이름
    }
}
```

이 두가지 파일은 서로 같이 있어도 무관하지만 , 확장성을 위해 따로 작성해주는것이 좋다.

1. **terraform init**

init 명령어를 실행해준다.

```bash
$ terraform init
```

그러면 state파일과 backend 파일을 복사할것이냐는 문구가 출력되는데 , yes 입력하면 끝이다.

기존 local에 있던 state파일과 backend파일을 삭제해도 backend에 존재하기때문에 각종 명령어가 정상적으로 동작하는것을 볼 수 있다.

---

## Terraform variables

terraform 은 HCL Syntax를 가진 언어 이다. 

**언어적 특성을 가지고 있기 때문에 당연히 변수를 정의하고 주입해서 사용할 수 있다.**

**Variable type**

1. string
2. number
3. bool

**Complex variable types**

1. list()
2. set()
3. map()
4. object({ = , ... })
5. tuple([, ...])

공식문서 참고

[Input Variables - Configuration Language | Terraform by HashiCorp](https://www.terraform.io/language/values/variables)

### 변수 정의하기

변수를 정의 ( 선언 ) 하는 방법은 tf파일 어디서는 가능하다.
그러나 보통 [variables.tf](http://variables.tf/) 같은 파일을 따로 만들어서 해당 파일에 정의하는것을 주로 사용한다.

```bash
# 변수를 선언하는 부분
variable "image_id" { 
  type = string # 타입을 지정해준다.
}

variable "availability_zone_names" {
  type    = list(string)
  default = ["us-west-1a"] # 변수를 선언하면서 이렇게 바로 값을 주입시켜줄 수 도 있다.
}

variable "ami_id_maps" {
  type = map
  default = {}
}

# 사용하고 싶을때는 var.ami_id_mpas 이렇게 사용한다.
# image_id를 쓰고싶다면 var.image_id 이렇게 사용한다.
```

### 변수에 값 주입하기

변수를 정의하고 값을 주입하기 위해서는 정의하면서 바로 넣어줄 수 도 있지만 

( 위 코드 default 처럼 )
아래처럼 따로 terraform.tfvars라는 파일을 만들어서 주입시키는것이 일반적이다.

```bash
# 변수에 값을 주입하는부분
image_id = "ami-064c81ce3a290fde1" # String
availability_zone_names = ["us-west-1a","us-west-1b","us-west-1c"] # list
ami_id_maps = { # map
    ap-northeast-2 = {
      amazon_linux2 = "ami-010bf43fe22f847ed"
      ubuntu_18_04  = "ami-061b0ee20654981ab"
    }

    us-east-1 = {
      amazon_linux2 = "ami-0d29b48622869dfd9"
      ubuntu_18_04  = "ami-0d324124b7b7eec66"
    }
}
```

### 주입한 값 확인하기

변수를 할당한걸 보고만 싶다면 . output을 활용한다.
**output.tf파일을 따로 만들어서 apply를 진행**한다.

```bash
# 값을 주입한걸 확인만 하고싶다면 ( 실제 리소스에 변경을 주지 않고 확인만 )
# output을 사용한다.
output "tf101_image_id" {
  value = var.image_id
}

output "tf101_availability_zone_names" {
  value = var.availability_zone_names
}

output "tf101_ami_id_maps" {
  value = var.ami_id_maps
}

# 이렇게 output파일을 만들어주고 apply를 진행하면, state파일에만 저장되고 리소스에는 반영되지 않는다.
# 출력만 되게 된다.
```

### **사용 예**

**provider.tf**

```bash
provider "aws" {
  region = var.aws_region
}
```

**resource.tf**

```bash
resource "aws_iam_group_membership" "devops-jinseong"{
  name = aws_iam_group.devops_group.name

  users = var.iam_user_list # devops-jinseong 그룹에 gildong.hong user 등록

  group = aws_iam_group.devops_group.name # group 정보가 필요하다. group은 위에 만들어준 devops_group의 이름을 설정한다.
}
```

**variables.tf**

변수 선언부

```bash
variable "aws_region" {
  description = "region for aws"
}

variable "iam_user_list" {
  type = list(string)
}

variable "image_id" {
  type = string # 타입을 지정해준다.
}

variable "availability_zone_names" {
  type    = list(string)
  default = ["us-west-1a"] # 변수를 선언하면서 이렇게 바로 값을 주입시켜줄 수 도 있다.
}

variable "ami_id_maps" {
  type = map
  default = {}
}

```

**terraform.tfvars**

변수 주입부

```bash
aws_region = "us-east-2"

iam_user_list = ["gildong.hong"]

image_id = "ami-064c81ce3a290fde1" # String
availability_zone_names = ["us-west-1a","us-west-1b","us-west-1c"] # list
ami_id_maps = { # map
    ap-northeast-2 = {
      amazon_linux2 = "ami-010bf43fe22f847ed"
      ubuntu_18_04  = "ami-061b0ee20654981ab"
    }

    us-east-1 = {
      amazon_linux2 = "ami-0d29b48622869dfd9"
      ubuntu_18_04  = "ami-0d324124b7b7eec66"
    }
}
```

---

## Terraform function

terraform은 다양한 함수를 내장한다.
**사용자는 function을 사용해서 리소스를 보다 효율적으로 생성하거나** 

**간략하게 코드를 만들 수 있다.**

현재는 사용자가 직접 함수를만들수는 없다.
( 곧 될듯 )

### Function의 종류

1. Numeric functions
2. String functions
3. Collection functions
4. Encoding functions
5. Filesystem functions
6. Date and Time functions
7. Hash and Crypto functions
8. IP Network functions
9. Type Conversion Functions

**공식 문서 참조**

[Functions - Configuration Language | Terraform by HashiCorp](https://www.terraform.io/language/functions)

**예제 코드**

[](https://github.com/jjsair0412/Terraform/tree/main/Terraform_function)

**예제 코드의 count() 함수 ?**
기본적으로 모든 리소스가 가지고 있는 count 파라미터를 이용하 반복되는 리소스를 간단하게 생성할 수 있다.

count 에 부여한 숫자만큼, 리소스는 반복되어 생성되고 자동으로 테라폼내에서 resource_name[0] 처럼 리스트화 된다.