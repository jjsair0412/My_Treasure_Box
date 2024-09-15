# Terraform lockfile
## 관련 테스트 소스코드

## Overview
Terraform init을 수행하면, .terraform.lock.hcl 파일이 하나 생성됩니다.

이는 사용중인 provider와 모듈들의 버전을 담고 있습니다.

해당 파일에 대해 기술한 문서입니다.

## .terraform.lock.hcl file 관리
기본적으로 lock file은 , Git과 같은 코드 관리 툴에 커밋되어야만 합니다.

팀의 다른 구성원이 해당 lock.hcl 파일을 가져와서 terraform을 실행하거나 자동화 할 때, 해당 lock file을 사용할것이고, 이는 이전 타 팀원이 개발한 Terraform 코드와 동일한 모듈 버전과 provider를 사용한다는것을 보장하기 때문입니다.

terraform의 provider의 요구사항(버전 등) 을 변경해야 할 경우에 혹은 변경했다면 Terraform은 lock file을 업데이트 합니다.

## provider의 예
provider 요구사항의 예시

```bash
terraform {
  required_providers {
    aws = {
        version = ">=3.20.0"
    }
  }
  required_version = ">=0.14"
}
```

위와 같은 terraform 코드가 있을 경우, 해석은 다음과 같습니다.

1. 요구되는 테라폼 버전은 0.14 이상이어야 함.
2. aws provider 버전은 3.20.0 보다 같거나 높아야 함.

### provider와 lockfile을 통한 terraform 버전관리
만약 해당 lock 파일이 구성되어있는 상태에서 , ```terraform init```을 수행하면, lockfile에 지정된버전만 다운로드 합니다.

3.20.0 버전보단 높을 수 있지만, lockfile에 정의된 버전이 다운로드 되기에, 해당 버전이 최신이아니라면 최신버전이 아니라는 의미 입니다.

따라서 최신버전을 다운로드 하기 위해선, lock file을 제거하고 다시  ```terraform init``` 을 수행하면 됩니다.



