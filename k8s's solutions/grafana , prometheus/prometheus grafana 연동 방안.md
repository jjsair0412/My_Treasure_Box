# prometheus and grafana 연동
## prerequire
동일 디렉토리에 작성한 설치 방안을 토대로 설치한 이후 , grafana와 prometheus를 연동하는 방안에 대해 기술합니다.

[grafana , prometheus 설치 방안](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/grafana%20%2C%20prometheus/grafana%20%EB%B0%8F%20prometheus%20%EC%84%A4%EC%B9%98%EB%B0%A9%EC%95%88.md)

사용할 grafana dashboard는 multi cluster dashbaord를 사용합니다.
[multi cluster dashboard](https://github.com/jjsair0412/kubernetes_info/blob/main/grafana/grafana%20dashboards/k8s%20grafana%20multi%20cluster%20dashboard.json)

### 1. prometheus와 grafana 연동
먼저 설치한 prometheus를 grafana에 연동합니다.

#### 1.1 grafana admin 계정으로 접속합니다.

![gra1][gra1]

[gra1]:./images/gra1.png

#### 1.2 좌측 하단의 톱니바퀴 버튼에 , Data sources를 클릭하여 접속합니다.

![gra2][gra2]

[gra2]:./images/gra2.png

![gra3][gra3]

[gra3]:./images/gra3.png

#### 1.3 Add datasource
Add datasource를 클릭합니다. 
문서작성 환경은 datasource가 없기때문에 아래 화면처럼 출력됩니다.

![gra4][gra4]

[gra4]:./images/gra4.png

#### 1.4 datasource 선택
prometheus 선택합니다.
다른 datasource를 사용한다면 여기서 분기하면 됩니다.

![gra5][gra5]

[gra5]:./images/gra5.png

#### 1.5 datasource 등록
prometheus 접근 url정보를 기입합니다.
Default datasource로 등록할것인지 확인합니다.

![gra6][gra6]

[gra6]:./images/gra6.png

맨 밑으로 이동한뒤 Save & test 버튼으로 잘 등록되었는지 확인 후 Explore 버튼으로 등록합니다.

![gra7][gra7]

[gra7]:./images/gra7.png

쿼리가 잘 작동하는지 테스트 합니다.

![gra8][gra8]

[gra8]:./images/gra8.png

#### 1.6 datasource uid 확인

### 2. dashboard 설정
grafana dashboard에 multi cluster dashboard를 import 합니다.

#### 2.1 dashboard import 페이지 이동
좌측 정 사각형 네개 모형을 클릭하거나 하단의 import를 클릭합니다.

사진과 같이 클릭하였을 경우 new -> import를 클릭합니다.
이동 페이지는 동일합니다.

![gra9][gra9]

[gra9]:./images/gra9.png

#### 2.2 dashboard json값 수정
사용할 json 파일 내용의 datasource uid를 전부 다 변경해 주어야 합니다.
```
...
        "datasource": {
          "type": "prometheus", 
          "uid": "SxwVNiO4k" # grafana에 등록한 datasource의 uid를 넣어줍니다.
        },
...
```

#### 2.3 dashboard import

해당 페이지로 이동 후 Upload JSON file을 클릭합니다.

파일 내용 전체를 복사하여 붙여넣기해도 무관합니다.

![gra10][gra10]

[gra10]:./images/gra10.png

![gra11][gra11]

[gra11]:./images/gra11.png

dashboard의 이름을 특정한 뒤 , import 버튼을 클릭합니다.
이때 동일 dashboard json값을 import 한다면 , uid값이 겹치기에 에러가 발생하기 때문에 uid값을 변경해 주어야 합니다.

import 결과 확인하여 우측 최 상단의 save 버튼으로 dashboard를 저장합니다.