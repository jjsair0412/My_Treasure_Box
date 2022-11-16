# information
grafana dashboard를 모아놓은 directory 입니다.

사용할 때에는 , json 파일 내용의 datasource uid를 전부 다 변경해 주어야 합니다.

아래 처럼 사용합니다.
```json
...
        "datasource": {
          "type": "prometheus", 
          "uid": "SxwVNiO4k" # grafana에 등록한 datasource의 uid를 넣어줍니다.
        },
...
```

# 주의사항
grafana dashboard를 import하여 사용 할 때에는 , grafana의 variables부터 확인합니다.
- variables에 있는 변수값들이 prometheus에서 메트릭으로 등록되어있지 않다면 , exporter등을 설치하여 해결합니다.

그 후 prometheus의 메트릭값이 정상적으로 뿌려지는지 확인하고 , grafana 쿼리를 맞춥니다.

# reference
## 1. multi cluster dashboard
아래 링크 grafana dashboard를 참고하여 제작하였습니다.

[kubernetes all in one cluster monitoring dashboard](https://grafana.com/grafana/dashboards/13770-1-kubernetes-all-in-one-cluster-monitoring-kr/)