# Terraform user data 방안
## OverView
Terraform에서 ec2 instance를 프로비저닝 할 때, 미리 정의해 둔 shell script를 user_data로 등록하여 인프라를 프로비저닝할 수 있습니다.

이때 shell script 상에서, Terraform apply 시 생성된 id 값 등을 shell script에서 참조하여 사용할 수 있습니다.

해당 문서는 overview에 작성한 내용들에 대해 기술합니다.

## 1. 사용 방안
aws provider 기준, aws_instance Resource에 user_data 필드가 있습니다.
- [Terraform user_data 공식문서](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/instance#user_data)

공식문서를 참고하여, aws_instance Resource를 작성할 때 user_data의 경로를 작성해 줍니다.
- 만약 child module에서 root module에 있는 script를 참조한다 하더라도, root module 경로를 작성해주어야 합니다.

    예를들어 child module 입장에서 scripts 폴더 접근 경로가, ```../../scripts/nginx_init_sc.sh``` 이라도, root module 입장에서 ```./scripts/nginx_init_sc.sh``` 로 작성해야 합니다.

```
...
resource "aws_instance" "demo" {
  ami           = var.ubuntu_ami_id
  instance_type     = var.instance_type
  availability_zone = var.availability_zone
  user_data = "./scripts/nginx_init_sc.sh"

  network_interface {
    network_interface_id = aws_network_interface.demo_network_interface.id
    device_index         = 0
  }
  
  tags = {
    Name = "demo ec2 instance"
  }
}
...
```

만약 Terraform apply 시 생성된 Infra resource의 Id값 등이 필요하다면, user_data 에 templatefile 함수를 사용합니다.

templatefile 함수는, 파라미터로 ```("user-data에 사용할 스크립트파일", "스트립트에 전달할 변수나 Terraform resource 1", "스트립트에 전달할 변수나 Terraform resource 2", ...)``` 를 가집니다.
변수는 여러개가 올 수 있습니다.
- [Terraform template file 공식문서](https://developer.hashicorp.com/terraform/language/functions/templatefile)

```
...
resource "aws_instance" "demo" {
  ami           = var.ubuntu_ami_id
  instance_type     = var.instance_type
  availability_zone = var.availability_zone
  user_data = templatefile("./scripts/nginx_init_sc.sh", {TEST=var.test})
  ## 변수 여러개할당 ##
  # user_data = templatefile("./scripts/nginx_init_sc.sh", {
    TEST=var.test, 
    TEST2=var.test2
  })

  network_interface {
    network_interface_id = aws_network_interface.demo_network_interface.id
    device_index         = 0
  }
  
  tags = {
    Name = "demo ec2 instance"
  }
}
...
```

nginx_init_sc.sh 파일은 다음과 같습니다.

```bash
#!/bin/bash
sudo apt-get update -y
sudo apt-get install nginx -y

## 받아온 테라폼 변수명을 환경변수로 할당합니다.
test = ${TEST}

## nginx restart ##
sudo systemctl restart nginx
```

생성된 instance에 ssh 접근하여 환경변수를 확인해 봅니다.
```bash
$ echo test
test
```