# EFK Index Lifecycle 설정
## 1. Prerequisites
- [참고 문서](https://www.elastic.co/guide/en/elasticsearch/reference/current/set-up-lifecycle-policy.html)
- EFK의 index 자동 삭제 기능을 설정하는 방법과 , Lifecycle 설정 방법을 설명하는 문서입니다.
- 해당 설정은 kibana ui로 진행합니다. 
## 2. Index Lifecycle 설정
### 2.1 Kibana ui 확인
- kibana에 접속합니다.
![Lifecycle_1][Lifecycle_1]

[Lifecycle_1]:./images/Lifecycle_1.PNG

### 2.2 Lifecycle polices 접속
- 좌측 메뉴바에서 stack Management를 클릭한 후 , index Lifecycle Policies를 클릭합니다.
- 그 후 해당 페이지에서 Create Policies 버튼을 클릭합니다.

![Lifecycle_2][Lifecycle_2]

[Lifecycle_2]:./images/Lifecycle_2.PNG
### 2.3 Police info
- index lifecycle polices를 생성합니다.
- 최 상단 텍스트박스에 policy 이름을 지정해줍니다.
- index lifecycle은 hot , warm , cold 영역으로 구분됩니다.
1. hot
   - 최근에 가장 많이 검색한 데이터를 hot 계층에 저장합니다. 
      인덱싱 성능과 검색 성능 모두 최고 수준으로 가지고 있습니다. 
      필수 옵션이기 때문에 끄거나 킬 수 없습니다.
2. warm
   - 검색할 가능성은 있지만 , 자주 업데이트해야 하는 경우 그러한 데이터들을 warm 계층으로 이동시킵니다.
     warm 계층은 인덱싱 성능보다 검색 성능에 더 최적화되어 있습니다.
3. cold
   - 검색 빈도가 낮고 업데이트할 필요가 없는 경우 그러한 데이터들을 cold 계층으로 이동시킵니다.
     검색 성능보다는 비용 절감에 최적화되어 있습니다.
     
![Lifecycle_3][Lifecycle_3]

[Lifecycle_3]:./images/Lifecycle_3.PNG

### 2.4 Lifecycle setting
- 해당 테스트에서는 hot 영역과 cold 영역을 사용하며 ,  cold 영역에서 180일 ( 6개월 ) 이 지나간다면 index를 삭제하는 정책을 구성합니다.
- cold 영역을 enable 시켜준 뒤 , 해당 영역으로 데이터를 이동시킬 날짜를 지정합니다. ( 180일 )

![Lifecycle_4][Lifecycle_4]

[Lifecycle_4]:./images/Lifecycle_4.PNG

- 그 후 , cold 영역의 우측 하단에 위치한 박스에 휴지통 버튼을 클릭하여 delete 영역을 생성합니다.
- 인덱스를 삭제하기 전 , 특정 snapshot을 지정하여 snapshot을 생성할 수 있습니다. snapshot은 이름으로 등록합니다.

![Lifecycle_5][Lifecycle_5]

[Lifecycle_5]:./images/Lifecycle_5.PNG

- save 버튼을 클릭해서 index lifecycle을 생성합니다.

## 3. Index 등록 절차  
- 생성시킨 index lifecycle policy를 index에 등록시키는것은 , 기존의 index에 추가하는 방법과 , index Template 단계 에서 부터 등록시켜주는 방법 두가지로 나뉘게 됩니다.

### 3.1 index Tempate 에서 등록
#### 3.1.1 template 생성
- index management로 이동 후 , index Templates의 create template 버튼을 클릭합니다.

![index_template_1][index_template_1]

[index_template_1]:./images/index_template_1.PNG

- 그 후 , template의 이름과 pattern을 지정해줍니다.

![index_template_2][index_template_2]

[index_template_2]:./images/index_template_2.PNG

- 상단의 index setting 버튼을 클릭하고 , 적용할 lifecycle과 rollover_alias, 복제되는 샤드 수, refresh_interval 등을 설정하고 next를 클릭한뒤 세부 구성을 설정하고 create template을 클릭해 구성을 마무리합니다.

```
{
  "index": {
    "lifecycle": {
      "name": "Policy_test",
      "rollover_alias": "test"
    },
    "number_of_shards": "1",
    "refresh_interval": "5s"
  }
}
```

![index_template_3][index_template_3]

[index_template_3]:./images/index_template_3.PNG

#### 3.1.2 template 구성 완료
- 생성한 template에 정상적으로 lifecycle이 구성된것을 확인할 수 있습니다.

![index_template_4][index_template_4]

[index_template_4]:./images/index_template_4.PNG

- Manage -> Edit 버튼을 클릭해 세부 구성 설정 화면으로 되돌아가서 setting을 다시 진행할 수 도 있습니다.

![index_template_5][index_template_5]

[index_template_5]:./images/index_template_5.PNG

### 3.2 기존 index에 등록
#### 3.2.1 index 설정
- index management로 이동 후 , lifecycle을 등록시킬 index를 체크합니다.
- 해당 문서에서는 carwash-2022.05.18 index에 lifecycle을 등록합니다.

![index_add_1][index_add_1]

[index_add_1]:./images/index_add_1.PNG

- 그 후 , Manage index 버튼을 클릭한 뒤 Add lifecycle policy를 선택합니다.

![index_add_2][index_add_2]

[index_add_2]:./images/index_add_2.PNG

- 만들어둔 lifecycle police를 선택한 뒤 , Add policy를 클릭합니다.

![index_add_3][index_add_3]

[index_add_3]:./images/index_add_3.PNG

- 해당 index를 클릭하여 정상적으로 index lifecycle이 등록된것을 확인할 수 있습니다.

![index_add_4][index_add_4]

[index_add_4]:./images/index_add_4.PNG

#### 3.2.2 index lifecycle 제거
- index 정보에 Manage -> Remove lifecycle policy 를 클릭하여 등록했던 index를 제거할 수 있습니다.

![index_add_5][index_add_5]

[index_add_5]:./images/index_add_5.PNG

![index_add_6][index_add_6]

[index_add_6]:./images/index_add_6.PNG

