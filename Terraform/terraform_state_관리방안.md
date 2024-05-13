# Terraform State
## OverView
Terraform은 인프라의 상태를 저장합니다.

인프라 상태 관리를 어떻게 진행하는지에 대한 내용을 기술하였습니다.

## 1. terraform.tfstate
Terraform은 ```terraform.tfstate``` , ```terraform.tfstate.backup``` 두가지 파일로 인프라 상태를 저장하는데, 명령어를 수행한 루트경로에 생성되게 됩니다.

해당 파일들로 terraform 원격 상태를 저장하고, state를 팀과 공유하여 협업할 수 있게 합니다.

### terraform.tfstate
terraform apply 명령어로 프로비저닝한 현재 인프라 상태를 저장함.

### terraform.tfstate.backup
현재 수행한 상태의 이전 상태를 저장함.


## 2. state remote 저장 방안
state 파일을 관리하는 방안은 여러가지가 있습니다.

state 파일을 저장하는곳을 백엔드라 합니다.

### 2.0 local backend
local에 state 파일을 두고 관리하는 방법입니다.

### 2.1 Git backend
단순한 관리 방안으로 Git Repostiory에 state 파일을 관리하는것이 있습니다.

하지만 이 방법은, state파일에 대해 코드 충돌이 발생할 가능성이 높으며 항상 git pull , push 명령어를 수행해야한다는 단점이 있습니다.

### 2.2 S3 backend
잠금 메커니즘을 가지는 S3에 state를 저장하는 방법.

이는 같은 시간대에 오직 한 사람만이 state 파일을 업데이트할 수 있기 때문에 충돌이 일어날 가능성이 낮습니다.

### 2.3 consol backend
잠금 메커니즘을 가지는 consol에 state를 저장하는 방법.

이는 같은 시간대에 오직 한 사람만이 state 파일을 업데이트할 수 있기 때문에 충돌이 일어날 가능성이 낮습니다.


## 3. remote state 구성 방안
remote state를 구성하는 단계는 두가지로 이루어집니다.

1. .tf 파일에 백엔드 코드 추가
2. init 과정 수행

### 3.1 backend.tf 파일 생성
먼저 어떤 remote backend storage를 사용할 것인지 backend.tf 파일을 생성하여 정의합니다.
- 아래 예시는 mybucket 이라는 s3 버킷을 backend 로 구성하는 예제 입니다.

```json
terraform {
    backend "s3" {
        bucket = "mybucket" // bucket Name
        key = "terraform/myproject" // state 저장 경로
        region = "eu-west-1" // 버킷 리전
    }
}
```

해당 코드를 실행하기 위해선 S3에 접근할 자격이 있는 IAM 계정으로 자격증명이 이루어져야 하는데, 이 과정은 ```aws configure``` 로 수행하는것이 좋습니다.

이유는 ```variable``` 을 사용한다면, ```terraform init``` , 초기화 단계 이후에 사용되게 되는데, 이 backend 구성 과정은 ```terraform init``` , 즉 초기화 단계에서 AWS S3에 접근해야 하기 때문입니다.
- backend 가 초기화되는 단계에서는 variable을 사용하지 못함.

만약 코드로 넣는다면 , ```ACCESS_KEY``` , ```SECRET_ACCESS_KEY``` 값이 그대로 노출되게 됩니다.

```bash
$ aws configure
AWS Access Key ID [None]: 
AWS Secret Access Key [None]: 
Default region name [None]: 
Default output format [None]:
```

### 3.2 terraform init , 초기화
bakend.tf 파일을 구성하고 (S3일 경우) 자격증명또한 완료되었다면 ```terraform init``` 으로 초기화 합니다.

```bash
terraform init
```

이렇게 초기화과정만 거치면, 만들어준 backend 정보를 가지고 AWS 자격증명을 수행하고 S3를 terraform state backend로 사용되게 됩니다.

## 4. remote state 사용범위 확장 - 출력
terraform state를 remote state 저장소로 관리하게 되면, 충돌 방지뿐만 아니라 많은 장점이 있습니다.

예를들어 만약 state에 저장된 값을 읽기만한다면, remote 저장소에있는 state를 통해 저장된 값을 간단하게 출력하여 읽기 전용으로도 사용이 가능합니다.

```json
date "terraform_remote_state" "aws-state" {
    backend = "s3"
    config {
        bucket = "terraform-state"
        key = "terraform.state"
        access_key = "${var.AWS_ACCESS_KEY}"
        secret_key = "${var.AWS_SECRET_KEY"
        region = "${var.AWS_REGION}"
    }
}
```