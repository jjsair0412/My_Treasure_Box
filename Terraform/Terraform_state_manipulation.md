# Terraform State Manipulation (Terraform 상태 조작)
## Overview
Terraform state 명령를 위해, Terraform의 state file이나 원격 상태를 조작할 수 있습니다.

## terraform state command list
|||
|--|--|
|**Command**|**Description**|
|```terraform state list```|state를 나열합니다. 단순하게 state 안에 정의된 리소스 이름들을 나열합니다.|
|```terraform state mv```|state를 이동하거나 이름을 변경합니다.|
|```terraform state pull```|현재 state 파일을 표준출력으로 가져옵니다. state 파일을 백엔드에서 로컬로 가져와 JSON으로 출력합니다.|
|```terraform state push```|로컬 state 파일의 변경 사항을 백엔드에 업로드하여 원격 상태 파일을 재정의합니다.|
|```terraform state replace-provider```|provider 또는 provider 버전을 재 정의(변경) 합니다.|
|```terraform state rm```|state 파일에서 해당 리소스를 제거합니다. 예를들어 테라폼에서 관리하고 싶지 않은 리소스를 state에서 제거하여, 인프라에 반영된 리소스에 영향이 가지 않도록 합니다.|
|```terraform state show```|state에 있는 항목을 보여줍니다.|

## ETC
### 1. terraform state mv command
기본적으로 state에서 리소스를 이동하기 위해 사용됩니다.

예를들어 state에서 리소스 이름을 바꾸거나, root 모듈에서 자신이 생성한 모듈로 이동시킬 때 사용합니다.

해당 명령어는 인프라상태를 변화시키지 않으며, 순전히 state만 변경합니다.

***만일 이름을 변경했지만 리소스를 다시 생성하고싶지 않다면, 해당 명령어를 사용합니다.***

해당 명령어는 테라폼코드 자체를 리펙토링(결과는 똑같은데, 코드만 변경) 할 때 많이 사용됩니다.

예를들어 아래 terraform ssm resource를 생성합니다.
```bash
resource "aws_ssm_parameter" "myparameter" {
  name  = "/myapp/myparameter"
  type  = "String"
  value = "myvalue"
}
```

이때 terraform apply를 수행하고, state 파일을 생성한 뒤 , state는 다음과 같이 정의되어 있습니다.
```bash
$ terraform state list                                                           
aws_ssm_parameter.myparameter

$ terraform state show aws_ssm_parameter.myparameter
aws_ssm_parameter.myparameter:
resource "aws_ssm_parameter" "myparameter" {
    allowed_pattern = null
    arn             = "arn:aws:ssm:ap-northeast-2:982414933550:parameter/myapp/myparameter"
    data_type       = "text"
    description     = null
    id              = "/myapp/myparameter"
    key_id          = null
    name            = "/myapp/myparameter"
    tags            = {}
    tags_all        = {}
    tier            = "Standard"
    type            = "String"
    value           = (sensitive value)
    version         = 1
}
```

***이때 정의된 테라폼코드의 리소스이름 자체를 변경하면, Terraform은 apply 이후 인프라에 반영되는 결과가 같더라도, 이를 인식하지 못하고 다른 리소스로 보기에, 기존의 것을 제거하고 새로 생성하려 합니다.***
```bash
resource "aws_ssm_parameter" "myparameter2" { # 리소스 이름 변경 , myparameter -> myparameter2
  name  = "/myapp/myparameter"
  type  = "String"
  value = "myvalue"
}

# terraform apply 수행
$ terraform apply
aws_ssm_parameter.myparameter: Refreshing state... [id=/myapp/myparameter]

Terraform used the selected providers to generate the following execution plan. Resource actions are indicated with the following symbols:
  + create
  - destroy

Terraform will perform the following actions:

  # aws_ssm_parameter.myparameter will be destroyed
  # (because aws_ssm_parameter.myparameter is not in configuration)
  - resource "aws_ssm_parameter" "myparameter" {
      - arn             = "arn:aws:ssm:ap-northeast-2:982414933550:parameter/myapp/myparameter" -> null
      - data_type       = "text" -> null
      - id              = "/myapp/myparameter" -> null
      - name            = "/myapp/myparameter" -> null
      - tags            = {} -> null
      - tags_all        = {} -> null
      - tier            = "Standard" -> null
      - type            = "String" -> null
      - value           = (sensitive value) -> null
      - version         = 1 -> null
        # (3 unchanged attributes hidden)
    }

  # aws_ssm_parameter.myparameter2 will be created
  + resource "aws_ssm_parameter" "myparameter2" {
      + arn            = (known after apply)
      + data_type      = (known after apply)
      + id             = (known after apply)
      + insecure_value = (known after apply)
      + key_id         = (known after apply)
      + name           = "/myapp/myparameter"
      + tags_all       = (known after apply)
      + tier           = (known after apply)
      + type           = "String"
      + value          = (sensitive value)
      + version        = (known after apply)
    }

Plan: 1 to add, 0 to change, 1 to destroy.

Do you want to perform these actions?
  Terraform will perform the actions described above.
  Only 'yes' will be accepted to approve.

  Enter a value: 
```

이러한 결과는 terraform 코드를 리펙토링하는데에 있어 취약합니다. 예를들어 운영중인 인프라를 테라폼코드가 변경되었다 해서, 제거되면 안되기 때문입니다.

***이때 terraform mv 명령어를 사용하게 됩니다.***

terraform mv 명령어로, state 파일만 업데이트함으로써 실제 인프라 상태를 바꾸지 않고, 백엔드에 저장한 state만 바꿀수 있습니다.
- 이는 직역하자면 테라폼 코드의 리소스 이름은 변경되는데, 인프라는 영향을 받지 않는다는 의미입니다.
```bash
# terraform state mv로 aws_ssm_parameter 리소스 이름을 myparameter2로 변경
$ terraform state mv aws_ssm_parameter.myparameter aws_ssm_parameter.myparameter2                             
Move "aws_ssm_parameter.myparameter" to "aws_ssm_parameter.myparameter2"
Successfully moved 1 object(s).

# state list 확인
$ terraform state list                                                           
aws_ssm_parameter.myparameter2

# terraform apply 수행 , 별도의 리소스 교체(Destory -> Apply) 없이 변경 완료
$ terraform apply
```