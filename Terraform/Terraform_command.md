# Terraform Command List
## Overview
Terraform은 인프라 상태를 코드로써 관리할 수 있게끔 해주는 툴.

따라서 상태관리를 위해 다양한 명령어를 제공함.

## 명령어 리스트
    [OPTION] 부분이 붙은부분은 없어도 동작한다는 의미임.
|||||
|--|--|--|--|
|**No**|**명령어**|**사용 사례**|**비고**|
|0|```terraform apply```|```state``` 를 인프라에 반영함.||
|1|```terraform destroy```|모든 ```terraform state``` 를 파괴함. 제거||
|2|```terraform fmt```|작성한 ```.tf``` 코드들을 terraform 표준 형식 및 스타일로 변환함. 구문에러 찾을때 유용||
|3|```terraform get```|모듈을 다운로드하거나 업데이트 할때 사용.||
|4|```terraform graph```|실행 계획이나 구성을 계층구조 시각적 형태로 보여줌. ```.tf``` 파일이 많고 초기 분석이 필요할 때 사용하면 좋음.||
|5|```terraform import [options] ADDRESS_ID```|리소스 아이디값을 통해 인프라 리소스의 상태를 ```state``` 파일로 가져욤. 실행중 인스턴스가 있는데 ```.tf``` 파일로 아직 안만들었을때, 실행중 인스턴스만 골라서 import 시킬 수 있음.(AWS라면 해당 인스턴스 ID가 ```ADDRESS_ID``` 부분에 들어가주면 됨.|```state``` 파일로 가져왔다 하더라도, ```.tf``` 파일로 직접변환은 안해주기 때문에, 사용자가 ```state``` 를 분석해서 직접 옮겨야 함.|
|6|```terraform output [OPTION] [NAME]```|특정 리소스를 출력시킴.|```[NAME]``` 을 지정하면 해당 리소스만 출력할 수 있음.|
|7|```terraform plan```|테라폼을 state를 반영하기 전 , 반영결과를 미리 출력함.|***필수***|
|8|```terraform taint [resource_name]```|인프라에 반영되어있는 특정 테라폼 리소스를 재 생성하는 등의 작업을 수행하기 위해 사용함. 예를들어 특정 인스턴스를 지정해서, 다시 프로비저닝되게끔 할 수 있음.||
|9|```terraform untaint [resource_name]```|```taint``` 시킨 리소스를 다시 정상상태로 되돌림.||
|10|```terraform push```|아틀라스(테라폼 엔터프라이즈 툴) 에 변경사항을 푸쉬함.|유료|
