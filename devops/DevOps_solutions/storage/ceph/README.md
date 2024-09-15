# ceph information
## ceph info
ceph는 storage management tool 입니다.

dashboard는 존재 하지만 , grafana데시보드에 출력되는 promethues 메트릭을 보여주는 역할만 합니다.
따라서 하드웨어 계층이라 생각하면 이해가 편합니다.

dashboard에서 파일을 관리 ( upload , download , share 등 .. ) 할 수 없기 때문에 , minio와 같이 연동하여 사용하는편이
사용에 있어 편리합니다.

## ceph cluster 구성 방안
ceph은 설치하기위해 세 가지 방안으로 나뉩니다.
1. cephadm
2. ansible-cephadm
3. rook-ceph

이전에는 ansible-cephadm을 많이 사용했지만 , 최근 cephadm을 설치할 때 docker container기반으로 설치되기에 속도도 향상됐으며 더 빠르고 간편합니다.
따라서 cephadm으로 설치하는 방안이 더 좋습니다.

또한 , k8s에서는 ceph을 편리하게 관리하기 위해서 rook-ceph을 사용합니다.

rook-ceph은 helm chart로 kubernetes pod의 형태로 ceph cluster가 동작하게 됩니다.
간편하게 사용할 수 있고 , object gateway등 ceph의 모든 기능이 open되어서 설치된다는 장점이 있지만 ,
k8s worker node가 ceph의 osd가 되기에 ceph이 k8s cluster에 종속된다는 단점이 있습니다.

## deploy k8s ceph ( rook-ceph ) vs deploy cephadm 
ceph는 kubernetes의 pod형태로 배포하여 볼륨을 mount시켜 storageclass로 사용할 수 있으며 ,
cephadm으로 베어메탈 환경에서 설치한 이후 kubernetes의 storageclass와 연동하여 사용할 수 도 있습니다.

전자인 k8s에 배포할 경우 , 메모리 소모량이 크기때문에 공식 docs에는 6core & 16G memory를 권장합니다. 
- 테스트 결과 , 8core에 16G memory에 3node 인 경우 가장 안정적으로 배포됩니다.
그러나 ceph의 object gateway등의 기능이 전부 open되어 배포되기에 , 메모리나 코어가 넉넉하다면 helm 배포가 적합합니다.

하지만 가볍게 사용하기 위해선 , cephadm으로 베어메탈에 ceph를 설치한 이후 사용하는편이 메모리 관리에 있어 유리합니다.
그러나 해당 방법은 , object gateway등의 기능을 일일히 open시켜야하는것이 단점입니다.

## management ceph storage files
s3cmd 명령어를 object gateway로 보내서 bucket내부 파일들을 관리할 수 있습니다.

여기서 관리란 , upload , delete , download , 폴더 생성 등을 이야기 합니다.

ceph object gateway bucket 사용법 문서를 참조하여 object gateway를 사용하고 , 
s3cmd와 object gateway의 user를 연결시켜주면 s3cmd명령어로 관리할 수 있습니다.

ceph object gateway bucket 사용법 문서에 해당 방법은 기술해 두었습니다.