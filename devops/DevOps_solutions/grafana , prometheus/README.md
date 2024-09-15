# introduce
해당 디렉터리는 grafana와 prometheus를 설치 및 연동하는 방안에 대해 기술합니다.

## prometheus information
prometheus는 k8s에서 metric값을 수집할 때 많이 사용됩니다.

다른 DB와 다르게 pull 방식으로 수집합니다.

예를들어 ELK , EFK는 push 방식으로 컨테이너 로그를 elasticsearch에 전송하지만 ,
prometheus는 수집 대상의 데이터를 직접 수집합니다. 
이러한 방식을 pull 방식이라 하며 , 설정값의 변경으로 push방식 또한 사용이 가능하지만 prometheus는 pull 방식을 사용합니다.


### prometheus component
- exporter
    - prometheus가 수집할 대상,  수집정보. 
    - exporter를 설치해야 대상에게서 metric을 pull 할 수 잇습니다.
- prometheus server
    - prometheus 데이터베이스
- promql
    - prometheus sql. 해당 sql문을 쿼리하여 메트릭값을 대상에게서 pull 합니다.
- alertmanager
    - prometheus에서 설정한 경계값이 트리거되면 알림을 전송