# ceph information
## ceph info
ceph는 storage management tool 입니다.

dashboard는 존재 하지만 , grafana데시보드에 출력되는 promethues 메트릭을 보여주는 역할만 합니다.
따라서 하드웨어 계층이라 생각하면 이해가 편합니다.

dashboard에서 파일을 관리 ( upload , download , share 등 .. ) 할 수 없기 때문에 , minio와 같이 연동하여 사용하는편이
사용에 있어 편리합니다.

## deploy k8s ceph ( rook-ceph ) vs deploy cephadm 
ceph는 kubernetes의 pod형태로 배포하여 볼륨을 mount시켜 storageclass로 사용할 수 있으며 ,
cephadm으로 베어메탈 환경에서 설치한 이후 kubernetes의 storageclass와 연동하여 사용할 수 도 있습니다.

전자인 k8s에 배포할 경우 , 메모리 소모량이 크기때문에 공식 docs에는 6core & 16G memory를 권장합니다. 
- 테스트 결과 , 8core에 16G memory에 3node 인 경우 가장 안정적으로 배포됩니다.
그러나 ceph의 object gateway등의 기능이 전부 open되어 배포되기에 , 메모리나 코어가 넉넉하다면 helm 배포가 적합합니다.

하지만 가볍게 사용하기 위해선 , cephadm으로 베어메탈에 ceph를 설치한 이후 사용하는편이 메모리 관리에 있어 유리합니다.
그러나 해당 방법은 , object gateway등의 기능을 일일히 open시켜야하는것이 단점입니다.