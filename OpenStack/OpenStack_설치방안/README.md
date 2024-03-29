# OpenStack with Vagrant
## Precondition
해당 문서는 OpenStack의 각 모듈을 수동으로 설치하는 방안에 대해 기술합니다.
- 모든 설치과정은 공식 문서를 참고하여 진행합니다.
- **Yoga** version으로 설치합니다.
    - [OpenStack_Install_Guide](https://docs.openstack.org/install-guide/)
    - [OpenStack_docs](https://docs.openstack.org/install-guide/openstack-services.html)
    - [OpenStack_version별_최소설치방안](https://docs.openstack.org/install-guide/openstack-services.html#minimal-deployment-for-yoga)
    - [OpenStack_용어집](https://docs.openstack.org/install-guide/common/glossary.html#term-message-queue)

설치대상 프로젝트는 다음과 같습니다.
- 각 프로젝트별 자세한 설명은 아래 링크에 있습니다.
    - [Openstack_이론](/OpenStack/%EC%9D%B4%EB%A1%A0/README.md)
- **각 프로젝트는 유기적으로 동작하기 때문에 , 설치 순번을 꼭 지켜야만 합니다.**

|Name | Explanation| 설치 순번 |
|--|--|--|
|KeyStone | 인증 인가 및 OpenStack 전체 RBAC관리 프로젝트|1|
|Glance | 가상머신 이미지 및 도커 이미지 관리 프로젝트 |2|
|Placement | 가상머신 컨테이너 인스턴스 배치 프로젝트 |3|
|Nova | 컴퓨팅 리소스 관리 프로젝트 |4|
|Neutron | 가상 네트워크 관리 프로젝트|5|
|Horizon | OpenStack dashboard 프로젝트|6|
|Cinder | 블록 스토리지 연동 프로젝트|7|

### 설치시 주의 사항
- **각 프로젝트는 유기적으로 동작하기 때문에 , 설치 순번을 꼭 지켜야만 합니다.**
- **각 프로젝트를 설치할 때 , 설치할 환경이 suse linux인지 , centos인지, ubuntu인지 꼭 확인하고 각 환경에 맞는 설치방안을 따라야 합니다. (다다름)**
- **서비스별로 유저를 생성하게 되는데 , 이때 프로젝트 권한을 줄 때 잘 보고 프로젝트 권한을 주어야 합니다. 안그러면 keystone에서 권한없다고 401에러 발생합니다.**
    - ```openstack role add --project service --user nova admin``` 요 명령어 날릴때 , project 확인하는 습관을 들이자. 401에러 발생한다면 , 테스트니까 모든 프로젝트에 생성한 user에게 admin 권한을 부여해보자.
    - 생성된 모든 프로젝트 확인 command : ```openstack project list```

## 설치 환경
- vagrant를 통해 VM을 설치하고 , 고 위에 OpenStack을 설치합니다.
- ubuntu 위에 설치합니다.

|name | 버전 및 용량|비고|
|--|--|--|
| ubuntu | 20.04 | |
| python | 3.6 | Horizon 설치시 , python 3.6 or 3.7 버전 필요 |
| Django | 3.2 | Horizon 설치시 , Django 3.2 필요 |
| memory | 16,384 | |
| cpu | 12 | |


## 설치 완료 후 구성
- 외부 Open되는 enpoint API와 포트입니다.
    - 해당 문서에서 설치결과로 나온 endpoint API는 테스트이기 때문에 , ```http://controller```로 모두 동일합니다.

| 서비스 명 | API | 포트 |
|--|--|--|
|  |  | |

## Install Memcached
- [Memcached_설치_방안](./Memcached.md)

## Install Message Queue
- [Message_Queue_설치_방안](./Message_Queue.md)

## Install KeyStone
- [KeyStone_설치_방안](./KeyStone.md)

## Install Glance
- [Glance_설치_방안](./Glance.md)

## Install Placement
- [Placement_설치_방안](./Placement.md)

## Install Nova
- [Nova_설치_방안](./Nova.md)


## Install Neutron
- [Neutron_설치_방안](./Neutron.md)

## Install horizon
- [horizon_설치_방안](./horizon.md)