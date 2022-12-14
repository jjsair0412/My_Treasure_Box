# prometheus and grafana 연동
## prerequire
동일 디렉토리에 작성한 설치 방안을 토대로 설치한 이후 , grafana와 prometheus를 연동하는 방안에 대해 기술합니다.

[grafana , prometheus 설치 방안](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/grafana%20%2C%20prometheus/grafana%20%EB%B0%8F%20prometheus%20%EC%84%A4%EC%B9%98%EB%B0%A9%EC%95%88.md)

사용할 grafana dashboard는 multi cluster dashbaord를 사용합니다.
[multi cluster dashboard](https://github.com/jjsair0412/kubernetes_info/blob/main/grafana/grafana%20dashboards/k8s%20grafana%20multi%20cluster%20dashboard.json)

### 1. prometheus와 grafana 연동
먼저 설치한 prometheus를 grafana에 연동합니다.

#### 1.1 grafana 접근
grafana admin 계정으로 접속합니다.

![gra1][gra1]

[gra1]:./images/gra1.png

#### 1.2 datasource 접근
좌측 하단의 톱니바퀴 버튼에 , Data sources를 클릭하여 접속합니다.

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
dashboard를 import하기전에 추가한 datasource의 uid를 확인해야 합니다.

확인 방법은 grafana의 RestAPI로 Get 요청을 보내어 확인 합니다.

```http
GET /api/datasources HTTP/1.1
Accept: application/json
Content-Type: application/json
Authorization: Bearer eyJrIjoiT0tTcG1pUlY2RnVKZTFVaDFsNFZXdE9ZWmNrMkZYbk
```

Get 요청을 보내기 위해 Bearer 토큰값을 만들어야 합니다.

좌측 하단 톱니바퀴 버튼의 API keys를 클릭하여 토큰값을 생성하는 페이지로 이동합니다.

![key1][key1]

[key1]:./images/key1.png

Key name을 Bearer로 설정한 뒤 , Role과 API Key 유효 기간을 지정합니다.

![key2][key2]

[key2]:./images/key2.png

생성된 Bearer key를 Copy하여 저장해 둡니다.

![key3][key3]

[key3]:./images/key3.png

postman 또는 curl 명령을 통해 restcall 합니다.
```bash
# ex
curl -L -X GET -H 'Accept: application/json' -H 'Authorization: Bearer {token}' 'http://grafa_url/api/datasources'

# usecase
curl -L -X GET -H 'Accept: application/json' -H 'Authorization: Bearer eyJrIjoiV1RUbTluMVV6R25QVnJjOHpWdWw4c1k1bkltV0syRGYiLCJuIjoiQmVhcmVyIiwiaWQiOjF9' 'http://gra.jjs.com/api/datasources'
```

결과에는 call 대상인 grafana에 등록된 모든 datasources 정보가 출력되며 , 필요한 datasources의 uid값을 Copy하여 저장해 둡니다.
```json
# output
[
    {
        "id": 1,
        "uid": "nGWH6D54z",
        "orgId": 1,
        "name": "Prometheus",
        "type": "prometheus",
        "typeName": "Prometheus",
        "typeLogoUrl": "public/app/plugins/datasource/prometheus/img/prometheus_logo.svg",
        "access": "proxy",
        "url": "http://pro.jjs.com:30325/",
        "user": "",
        "database": "",
        "basicAuth": false,
        "isDefault": true,
        "jsonData": {
            "httpMethod": "POST"
        },
        "readOnly": false
    }
]
```

### 2. dashboard 설정
grafana dashboard에 multi cluster dashboard를 import 합니다.

#### 2.1 dashboard import 페이지 이동
좌측 정 사각형 네개 모형을 클릭하거나 하단의 import를 클릭합니다.

사진과 같이 클릭하였을 경우 new -> import를 클릭합니다.
이동 페이지는 동일합니다.

![gra9][gra9]

[gra9]:./images/gra9.png

#### 2.2 dashboard json값 수정
사용할 json 파일 내용의 datasource uid를 이전에 저장해두었던 대상 datasource의 uid로 전부 다 변경시켜 주어야 합니다.
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

만약 데이터를 받아오지 못한다면 , 페널별로 sql문을 수정하여 값을 맞춰줍니다.

import 결과 확인하여 우측 최 상단의 save 버튼으로 dashboard를 저장합니다.