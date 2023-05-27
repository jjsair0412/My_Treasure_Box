# OpenStack with Vagrant
## Precondition
해당 문서는 OpenStack의 각 모듈을 수동으로 설치하는 방안에 대해 기술합니다.
- 모든 설치과정은 공식 문서를 참고하여 진행합니다.
- **Yoga** version으로 설치합니다.
    - [OpenStack_docs](https://docs.openstack.org/install-guide/openstack-services.html)

설치대상 서비스는 다음과 같습니다.
- 자세한 설명은 아래 링크에 있습니다.
- [Openstack_이론](/OpenStack/%EC%9D%B4%EB%A1%A0/README.md)

|name | Explanation| ETC |
|--|--|--|
|keystone | 인증 서비스||
|glance | 이미지관리 서비스 ||
|placement | 가상머신 컨테이너 인스턴스 배치 서비스 ||
|nova | 컴퓨팅 리소스 관리 서비스 ||
|neutron | neutron installation for Yoga||
|horizon | horizon installation for Yoga||
|cinder | cinder installation for Yoga||

## 설치 환경
|name | version| 
|--|--|
| ubuntu | 20.04 |

## Yoga 버전
2016년 10월 6일출시되고 , 