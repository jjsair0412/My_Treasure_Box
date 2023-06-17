# Horizon 설치 방안
- [Yoga_전체환경_Horizon_설치방안_docs](https://docs.openstack.org/horizon/xena/install/)
- [Yoga_ubuntu_Horizon_설치방안_docs](https://docs.openstack.org/horizon/xena/install/install-ubuntu.html)
- [Yoga_ubuntu_Horizon_Manual_설치](https://docs.openstack.org/horizon/latest/install/from-source.html#deployment)

## ETC
Horizon을 구성한 뒤 , apache2를 restart 했는데도 404나 500 에러가 발생한다면 , 아래 명령어로 트러블슈팅합니다 .

Horizon은 python application이기에 다음 명령어로 확인해야 합니다.
```bash
$ python -c "import py_compile; py_compile.compile(r'/etc/openstack-dashboard/local_settings.py')"
```

실제 openstack-dashboard python code의 경로는 다음과 같습니다.
```bash
$ pwd
/usr/share/openstack-dashboard/
```

python 명령어로 강제 작동시켜서 디버깅이 가능합니다.
```bash
$ pwd
python manage.py runserver
```

## ENV
- endpoint API : 
- Domain : 
- Port :  

## OverView
해당 문서에선 OpenStack의 데시보드 서비스인 Horizon을 ubuntu os에 설치하는 방안에 대해 기술한 문서 입니다.

로컬 환경에 설치하고있기 때문에 , access

## ***설치 진행***
## 1. 구성요소 설치 및 구성
apt 명령어로 먼저 필수 패키지를 설치 합니다.
```bash
$ sudo apt-get install openstack-dashboard
```

## 2. local 환경 세팅
```local_settings.py``` 파일을 수정해야 합니다. 해당 파일 경로는 다음과 같습니다.

```bash
$ pwd
/etc/openstack-dashboard/local_settings.py 
```

Controller 노드에서 OpenStack 서비스를 사용하도록 , 대시보드를 다음과 같이 수정합니다.
```py
# before
OPENSTACK_HOST = "127.0.0.1"

# 수정 후 반영사항
OPENSTACK_HOST = "controller"
```

특정 도메인에 대해서 접근을 허용하도록 구성합니다.
- 여기에 호라이즌 대시보드의 도메인을 넣어주면 되는데 , 아래처럼 하이어 라키로 모든 호스트의 접근을 허용할 수 있습니다.
- 그러나 전체 허용은 보안상 좋지 않기에 , 실제 운영을 목표로한다면 
아래 문서를 참고하여 구성해야만 합니다.
    - [ALLOWED_HOSTS_섹션_허용가능변수확인](https://docs.djangoproject.com/en/dev/ref/settings/#allowed-hosts)
```py
# 모든 호스트 허용
# 해당 구성으로 설정함
ALLOWED_HOSTS = ['*']

# 여러 호스트 허용
ALLOWED_HOSTS = ['one.example.com', 'two.example.com']
```

```memcached``` 세션 스토리지 서비스를 구성합니다.
- 이전에 설치했었던 memcached 를 바라보면 됩니다.
    - [memcached_설치방안](/OpenStack/OpenStack_%EC%84%A4%EC%B9%98%EB%B0%A9%EC%95%88/Memcached.md)
```py
SESSION_ENGINE = 'django.contrib.sessions.backends.cache'

CACHES = {
    'default': {
         'BACKEND': 'django.core.cache.backends.memcached.MemcachedCache',
         'LOCATION': 'controller:11211',
    }
}
```

identity API version 3를 활성화 합니다.
- 만약 keystone 서비스에 접근하기 위해 포트번호가 필요하다면 , 포트번호도 추가로 기입해주어야만 합니다.
```py
# usecase
OPENSTACK_KEYSTONE_URL = "http://%s/identity/v3" % OPENSTACK_HOST

# 실 사용 명령어
# LB , 프록시 없이 구성하고있기 때문에 keystone의 5000번 포트를 추가합니다.
OPENSTACK_KEYSTONE_URL = "http://%s:5000/identity/v3" % OPENSTACK_HOST
```

도메인 support 기능을 활성화 합니다.
- 원본파일에 없기 때문에 , 그냥 추가해 줍니다.
```py
OPENSTACK_KEYSTONE_MULTIDOMAIN_SUPPORT = True
```

API 버전을 구성합니다.
- 원본파일에 없기 때문에 , 그냥 추가해 줍니다.
```py
OPENSTACK_API_VERSIONS = {
    "identity": 3,
    "image": 2,
    "volume": 3,
}
```

대시보드를 통해서 사용자를 생성한다면 , 해당 사용자가 Default로 사용되는 옵션을 추가해 줍니다.
- 원본파일에 없기 때문에 , 그냥 추가해 줍니다.
```py
OPENSTACK_KEYSTONE_DEFAULT_DOMAIN = "Default"
```

네트워킹 옵션 (이전 neutron 을 설치할 때 , 1번 혹은 2번 아키텍처 ,  Provider Network ) 중  Provider Network로 구성하였다면 , 아래 옵션을 추가하여 3계층 네트워킹 서비스에 대한 지원을 disable 합니다.
- 원본파일에 없기 때문에 , 그냥 추가해 줍니다.
```py
OPENSTACK_NEUTRON_NETWORK = {
    ...
    'enable_router': False,
    'enable_quotas': False,
    'enable_ipv6': False,
    'enable_distributed_router': False,
    'enable_ha_router': False,
    'enable_fip_topology_check': False,
}
```

Timezone을 구성합니다.
- [default_timezone_확인](https://en.wikipedia.org/wiki/List_of_tz_database_time_zones)
```py
TIME_ZONE = "KR"
```

만약 아래 경로에 위치한 ```openstack-dashboard.conf``` 파일에 아래 설정이 없다면 추가합니다.
```bash
$ pwd
/etc/apache2/conf-available/openstack-dashboard.conf

# 추가
$ vi /etc/apache2/conf-available/openstack-dashboard.conf
WSGIApplicationGroup %{GLOBAL}
```

## 3. web restart
web server를 재 시작 합니다.
```bash
$ systemctl reload apache2.service
```