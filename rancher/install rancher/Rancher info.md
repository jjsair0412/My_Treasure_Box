# Rancher info
- 해당 문서는 rancher 사용 방안과 , 각종 정보가 정리되어 있습니다.
- helm , Terraform 등을 사용하여 rancher를 설치하는 방법은 같은 위치에 정리되어 있습니다.
- [rancher 공식문서](https://rancher.com/docs/)
## Introduce rancher

### 1. rancher ? 
 - rancher는 suse에서 만든 상용 솔루션
 
 -  k8s cluster를 유지 보수하고 유저에 대해 권한을 부여하며, k8s 환경을 provisioning 한다.
 - k8s 1.17버전부터 provisioning이 가능하다.  이전 버전은 rancher로 새로 찍어내야 한다. 
#
### 2. why rancher ?
- 만약 multi cloud 환경 ( aks , eks , gcp .. ) 이거나 , 온프레미스도 같이 사용하여 k8s 클러스터들이 각각 여러개 존재한다고 생각해보자 . 
- 물론 이런 환경에서 , 모든 k8s에 각각 접속하여 관리해도 무관 할 것이지만 , 많아지면 많아질수록 관리point는 늘어날 것이며 , 사용자에 실수로인한 에러 발생 확률도 올라갈 것이다. 따라서 이러한 k8s 클러스터들을 중앙에서 관리해줄 통합 관리 툴이 필요한데 , 이때 사용할 관리 툴이 rancher가 될 수 있다.


 - 기본 k8s에는 사용자 계정 관리 기능이 없지만 , rancher는 있다. 
  -- 각 cluster 별로 사용자 권한을 부여할 수 있다. ( RBAC )
 - identity 서비스와 연계해서 , 여러 클러스터에 사용자 id관리를 통합관리 할 수 있게끔 도와준다. 
  -- oauth , LDAP 등 
 - runtime 솔루션 ( promethus , grafana , longhorn ... ) 얘네들을 통합해서 쉽게 운영할 수 있다. ( 마켓플레이스 )

 - RBAC을 중앙 ( RANCHER )에서 관리가 가능하다.
 
#
### 3. rancher vs tanzu
 - 기본적으로 제공하는 기능은 비슷하다.
 - rancher 2.6에서 kubernetes management에 필요한 기능은 다 갖추었다고 볼 수 있다.
#
### 4. rancher 설치 시 관련 정보
- etcd 3개 , control plane nodes 2개 , worker nodes 2개 설정
-- etcd는 3개 필요 . 
-- etcd server clustering을 위해 항상 홀수개로 설정해야 함 . 

 - 권장 사항은 etcd , control plane , worker 모두 분리시켜야한다.
 - Downstream cluster는 rancher가 관리하고있는 k8s를 의미한다.
 - rancher 관리 클러스터는 독립적으로 사용 하는 것을 권장
#
### 5. rancher vs RKE
 - rancher는 k8s 관리 툴
 - rke는 suse rancher에서 만든 k8s 런타임 이다.
#
### 6. rke vs rke2
 - rke2는 rke와 비교해서 보안이 강하다.
 - 나중에 둘이 통합 될 예정이다.
#
### 7. rancher 동작과정
 - user가 kubectl 명령어를 수행하면 , 모든 인증 및 관리절차는 rancher server의 authentication proxy와 cluster agent를 통한다.
 
 - k8s 클러스터를 직접 접근하지는 않는다.
 #

### 2. rancher menu bar
- rancher가 처음실행하면 , **cluster local**이라는게 보이는데 , 얘가 rancher가 배포된 k8s환경을 의미한다.


- rancher cluster 이름변경하면 에러가 발생하는데 솔루션 자체 에러.

- cicd gitops형태로 구성할 때 , Continuous Delivery 사용하면 된다. 
 -- rancher는 ci부분은 없고 , cd만 들어있다. gitops 기반. 

- user & authentication 
-- 사용자 연동 처리방안 

- global setting 
-- rancher 설정방안
# 
### rancher 구성요소
1. cattle-cluster-agent
 -- kube-apiserver와 연결되어 user의 명령어를 전달해주는 rancher 교신용 파드
2. node-agent
 -- 각 node에 node-agent가 damonset type으로 생성됨. 평상시에는 얘가 동작안하고 cluster-agent가 동작하는데, cluster-agent가 동작하지 않을 때 얘가 cluster-agnet 역할을 대신한다.
 #
### rancher snapshot ?  
 - rancher에서 snapshot 찍는 것과 같은 기능들은 , rancher에서 k8s 환경을 provisioning 한 경우만 가능하다.
 - 기존 cluster를 import 한 경우 , 특정 rancher 기능을 사용하지 못한다. 
#
### rancher의 project ?
 - k8s namespace의 집합
 - namespace를 groupping 한 상위 폴더라고 보면 된다.
#
### user 권한관리 
rancher는 keycloak , git 등 다양한 상용 솔루션과 연동이 가능하다.

 - 계정은 rancher만가지고 id만들어서 사용해도 무관하다.
 - LADP등 환경이 있다면 , 거기서 만들어도 무관
 - global permission 종류( 아래 p-110 LECTURE_MANUAL-RAN201v2.6 ... ) 
 1. admin 
  --전체 접근 가능  
 2. restricted admin 
  -- rancher cluster를 접근하지 못함. 
  -- local cluster에 접근하지 못한다.
 3. standard 
  -- rancher cluster를 접근하지 못한다.
  -- 사용자 소유의 cluster만 접근 가능.
  -- 로그인후 계정 자신이 provisioning & import한 cluster만 접근이 가능하다.
 
 - Cluster Roles의 종류
1. Cluster Owner
-- 모든 resource와 cluster에 접근이 가능하다 .
2. Cluster Member
 -- 대부분의 cluster를 볼 수 있고 새로운 project를 생성할 수 있다.
 
**Project Role의 종류**
1. Project Owner
-- 모든 리소스와 project에 접근이 가능하다.
2. Project Member
-- 자기 자신의 project만 접근이 가능하다.
3. Read Only
-- project를 보는것만 허용되며 , create , delete, update 등의 작업은 허용되지 않는다.
 
**LDAP 연동시 참고사항**
LDAP 연동 시 , 각 USER들을 설정할 수 있고 , GROUP별로 USER를 묶어서 LDAP으로 관리할 수 도 있다.
  #
  ### more info..
**rancher 2.6 버전 이상을 사용해야 한다.**
만약 클라우드의 k8s 환경을 import 했을 경우 ( aks, gcp , eks 등 ) rancher는 k8s 환경 관리를 양쪽 둘다 가능하다. 
 -- rancher 수정 , k8s 수정 둘 다 가능
 
그러나 2.6버전 전에는 k8s 환경에서 수정했을 경우 , 싱크가 맞지 않았었다.

 - rancher가 직접 프로비저닝 ( k8s 생성 ) 한 k8s는 기능이 더 많다.
 - 그런데 클라우드에서 만들어진 k8s ( eks , aks .. ) 얘네들은 지원하는 기능이 제한되는경우가 있다.
 
 -    user관리같은건 가능한데 , 클러스터를 직접 관리하는것들은 기능제한이 걸린다.
 - k8s import했을 경우 또한 기능에 제한이 있을 수 있다.