# Terraform 
해당 폴더는 Terraform 관련 정보를 저장해둔 폴더 입니다.

## Repository Map
### 1. Terraform basic info
Terraform에 대한 기초적인 이론을 정리해 두었습니다.
- [Terraform basic info 링크](./Terraform_basic_info.md)

### 2. Terraform module
Terraform module화에 대한 내용 및 사용방안에 대해서 정리해 두었습니다.
- [Terraform module 링크](./Terraform_module/)

### 3. Terraform user data
Terraform을 통해 vm instance를 프로비저닝 할 때, user_data 를 스크립트로 등록하는 방안과, Terraform resource의 변수를 user_data script에 전달하는 방안에 대해 정리해 두었습니다.
- [Terraform user data 링크](./Terraform_user_data.md)

### 4. Terraform test code
Terraform을 study 하면서 생성한 test code
- [test code](./terraform_test_code/)

### 5. Terraform remoget state 관리방안
Terraform의 state 파일들을 원격으로 관리하여 충돌을 방치하는 방안에 대해 기술
- [terraform state 관리방안](./terraform_state_관리방안.md)

### 6. Terraform Datasource
Terraform에서 AWS와 같은 특정 프로바이더를 사용했을 경우, API로 제공되는 다양한 데이터 소스를 사용하는 방법에 대해 기술
- [terraform data source](./Terraform_data_resource.md)