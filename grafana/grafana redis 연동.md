# Redis-cluster grafana 연동 과정
## 1. Prerequisites
- 본 문서는 redis-cluster와 grafana를 연동 시키는 과정을 설명합니다.
- 해당 문서를 응용해서 , grafana에 솔루션들을 연동시킨 후 모니터링할 수 있습니다.
## 2. prometheus 연동 확인
### 2.1 serviceMonitor 확인
- redis에서 values.yaml파일을 변경하여 serviceMonitor를 자동으로 생성 되게끔 하였기 때문에 , serviceMonitor가 정상적으로 생성되었는지 확인합니다.
- prometheus가 메트릭값을 수집하는 방법은 여러가지가 잇지만 , serviceMonitor를 생성하는것이 가장 간편합니다. servicemonitor pod가 떠있는 상태로 메트릭을 수집하게 됩니다.
```
$ kubectl get servicemonitor -n monitoring
...
redis-cluster                                        35m
...
```
### 2.2 prometheus ui 확인
- serviceMonitor가 동작하고있다면 , prometheus에 접속하여 redis-cluster 메트릭이 수집되고 있는지 확인합니다.
- 파란색 글씨로 설정해둔 이름값 ( redis-cluster ) 을 가진 url처럼생긴게 있다면 , 연동이 완료된것입니다.


### 2.3 grafana dashborad json파일 설치 및 연동
- redis-cluster에 대한 grafana dashborad json파일을 설치합니다.
- json 파일은 직접 커스터마이징해도 무관하지만 , 구글에 검색하여 template을 꺼내온 뒤 , 입맛에 맞게 변화시키는것이 더 효율적입니다. 
- grafana dashboard라고 구글에 검색하면 template이 많이 나옵니다.
- [json파일 다운로드한곳](https://grafana.com/grafana/dashboards/763)
```
{
  "__inputs": [
    {
      "name": "DS_PROM",
      "label": "prom",
      "description": "",
      "type": "datasource",
      "pluginId": "prometheus",
      "pluginName": "Prometheus"
    }
  ],
  "__requires": [
    {
      "type": "panel",
      "id": "singlestat",
      "name": "Singlestat",
      "version": ""
    },
    {
      "type": "panel",
      "id": "graph",
      "name": "Graph",
      "version": ""
    },
    {
      "type": "grafana",
      "id": "grafana",
      "name": "Grafana",
      "version": "3.1.1"
    },
    {
      "type": "datasource",
...

```
- grafana에 접속하여 , 좌측 메뉴중 Dashboards -> Browse 페이지로 이동 후 , import 버튼을 클릭합니다.

![das-1][das-1]

[das-1]:./images/das-1.jpg

- 이전 설치해두었던 json 값들 전체를 import via panel json 박스에 붙여 넣은 후 Load버튼을 클릭합니다.

![das-2][das-2]

[das-2]:./images/das-2.PNG

- name칸에 dashboard 이름을 지정해 줍니다. 그 후 prom 박스에서 Prometheus 를 선택한 뒤 import 버튼을 클릭합니다.

![das-3][das-3]

[das-3]:./images/das-3.PNG

### 2.4 연동 완료 페이지 확인
- dashboard를 확인합니다.

![das-4][das-4]

[das-4]:./images/das-4.PNG