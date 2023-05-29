# Placement 설치
- [Yoga_전체환경_Placement_설치방안_docs](https://docs.openstack.org/placement/yoga/install/)
    - 환경별 설치방안 섹션으로 바로가기 하려면 , 아래링크를 클릭합니다.
    - [환경별_설치방안](https://docs.openstack.org/placement/yoga/install/#installation-packages)
- [Yoga_ubuntu_Placement_설치방안_docs](https://docs.openstack.org/placement/yoga/install/install-ubuntu.html)

## ENV
- endpoint API : http://controller:8778 
- Domain : controller
- Port : 8778 

## ETC
Glance 설치 및 운영시 에러를 확인할 수 있는 로그파일 위치는 다음과 같습니다.
```bash
$ pwd
/var/log/placement
```


## KeyStone Precondition
### 1. Precondition
#### MySQL USER 구성
- DB 구성
이전에 생성해뒀던 MySQL DB에 placement Database 생성

```bash
$ mysql -u root -p
```
- placement 데이터베이스 생성 및 데이터베이스 액세스 권한 부여
    - placement 계정에 적절한 비밀번호를 지정합니다. **해당 문서는 가이드이기에 1234로 설정**
```bash
# placement 이름의 데이터베이스 생성
mysql> CREATE DATABASE placement;

# placement localhost user 생성 (pwd 1234)
mysql> CREATE USER 'placement'@'localhost' IDENTIFIED BY '1234';

# placement user에게 localhost 권한부여
mysql> GRANT ALL PRIVILEGES ON *.* TO 'placement'@'localhost' WITH GRANT OPTION;

# placement user에게 placement 데이터베이스 localhost에서 관리 권한부여
mysql> GRANT ALL PRIVILEGES ON placement.* TO 'placement'@'localhost' WITH GRANT OPTION;

# placement user 생성 (pwd 1234)
mysql> CREATE USER 'placement'@'%' IDENTIFIED BY '1234';

# placement user에게 권한부여
mysql> GRANT ALL PRIVILEGES ON *.* TO 'placement'@'%' WITH GRANT OPTION;

# placement user에게 placement 데이터베이스 관리 권한부여
mysql> GRANT ALL PRIVILEGES ON placement.* TO 'placement'@'%' WITH GRANT OPTION;
```

모든 쿼리가 실행됐다면 mysql에서 빠져나옵니다.
```bash
mysql> exit
Bye
```
### 2. 사용자 및 엔드포인트 구성
#### admin-openrc 설정 (환경변수 소싱)
- Glance를 설치하며 진행했던 소싱 스크립트를 그대로 사용합니다.
```bash
$ cat admin-openrc 
export OS_USERNAME=admin
export OS_PASSWORD=1234
export OS_PROJECT_NAME=admin
export OS_USER_DOMAIN_NAME=Default
export OS_PROJECT_DOMAIN_NAME=Default
export OS_AUTH_URL=http://controller:5000/v3
export OS_IDENTITY_API_VERSION=3

$ . admin-openrc 
```

#### openstack Placement 설정
소싱한 사용자 정보를 가지고 openstack의 Placement 사용자를 생성합니다.
- 사용자 이름 : ```placement```
- 비밀번호 : ```12345```

```bash
$ openstack user create --domain default --password-prompt placement
User Password: 12345
Repeat User Password: 12345
+---------------------+----------------------------------+
| Field               | Value                            |
+---------------------+----------------------------------+
| domain_id           | default                          |
| enabled             | True                             |
| id                  | cad1723f8e964439bdae1f1bb1940846 |
| name                | placement                        |
| options             | {}                               |
| password_expires_at | None                             |
+---------------------+----------------------------------+
```

만들어진 placement 서비스 사용 유저에 admin role을 부여 합니다.
- output이 안나오는게 정상
```bash
$ openstack role add --project service --user placement admin
```

openstack 서비스 카탈로그에서 , Placement API 항목을 생성합니다.

```bash
$ openstack service create --name placement \
  --description "Placement API" placement
+-------------+----------------------------------+
| Field       | Value                            |
+-------------+----------------------------------+
| description | Placement API                    |
| enabled     | True                             |
| id          | fefce34c5c4647718c955ed8701e9157 |
| name        | placement                        |
| type        | placement                        |
+-------------+----------------------------------+
```

Placement API endpoint를 생성합니다.
- 8778 포트로 open합니다.
- endpoint는 3가지 생성하며 , interface로 구분됩니다.
    - public
    - internal
    - admin
```bash
# interface public
$ openstack endpoint create --region RegionOne \
  placement public http://controller:8778
+--------------+----------------------------------+
| Field        | Value                            |
+--------------+----------------------------------+
| enabled      | True                             |
| id           | 856d2d0d25914497916c361bd4113b0a |
| interface    | public                           |
| region       | RegionOne                        |
| region_id    | RegionOne                        |
| service_id   | fefce34c5c4647718c955ed8701e9157 |
| service_name | placement                        |
| service_type | placement                        |
| url          | http://controller:8778           |
+--------------+----------------------------------+

# interface internal
$ openstack endpoint create --region RegionOne \
>   placement internal http://controller:8778
+--------------+----------------------------------+
| Field        | Value                            |
+--------------+----------------------------------+
| enabled      | True                             |
| id           | a767b592a61040ffb1e2b3f451f0dfc8 |
| interface    | internal                         |
| region       | RegionOne                        |
| region_id    | RegionOne                        |
| service_id   | fefce34c5c4647718c955ed8701e9157 |
| service_name | placement                        |
| service_type | placement                        |
| url          | http://controller:8778           |
+--------------+----------------------------------+

# interface admin
$  openstack endpoint create --region RegionOne \
>   placement admin http://controller:8778
+--------------+----------------------------------+
| Field        | Value                            |
+--------------+----------------------------------+
| enabled      | True                             |
| id           | 18b6fd6f332c40d59c36721620becbe9 |
| interface    | admin                            |
| region       | RegionOne                        |
| region_id    | RegionOne                        |
| service_id   | fefce34c5c4647718c955ed8701e9157 |
| service_name | placement                        |
| service_type | placement                        |
| url          | http://controller:8778           |
+--------------+----------------------------------+
```

### 3. Placement conf 설정 및 구성
Placement의 conf파일을 환경에 맞게 바꾸고 , 설치를 진행합니다.
- 해당 문서는 테스트 목적으로 설치하기때문에 , 기본 설정으로 설치합니다.

#### 1. placement-api 설치
apt 명령어로 설치합니다.
```bash
$ sudo apt-get install placement-api
```

#### 2. placement conf파일 구성
placement의 conf파일을 구성합니다.
- 환경에 맞게끔 설정합니다.

placement의 conf파일 위치는 다음과 같습니다.
```bash
vi  /etc/placement/placement.conf
```

[placement_database] 섹션에서 DB 엑세스 정보를 기입합니다.
```conf
[placement_database]
# ...
# placement 데이터베이스 password 입력
connection = mysql+pymysql://placement:PLACEMENT_DBPASS@controller/placement

# 실 사용 명령어
connection = mysql+pymysql://placement:1234@controller/placement
```

[api] 섹션과 [keystone_authtoken]에서 이전에 설치했던 keystone 서비스 엑세스 정보를 기입합니다.
- Glance를 설치할 때와 동일하게 구성합니다.
- user_name, password 값은 keystone의 project_domain_name, user_domain_name 구성과 동기화되어야 합니다.
- 각 정보는 , 위에 소싱해둔 정보와 매핑시킵니다.
    - [admin_cli_소싱](#admin-openrc-설정-환경변수-소싱)

```conf
[api]
auth_strategy = keystone

[keystone_authtoken]
# keystone auth 정보 입력
# source로 소싱해둔 admin-openrc 스크립트확인
auth_url = http://controller:5000/v3
memcached_servers = controller:11211
auth_type = password
project_domain_name = Default
user_domain_name = Default
project_name = admin
username = admin
password = 1234
```

#### 3. placement db와 동기화 합니다.

```bash
$ su -s /bin/sh -c "placement-manage db sync" placement
```

#### 4. 설치 마무리
설정한 placement의 구성 정보를 apache가 등록하게끔 apache를 재 시작 합니다.
```bash
$ service apache2 restart
```

### 4. 설치결과 확인
#### curl 명령 수행
placement api endpoint에 curl을 날려서 , json 값이 정상적으로 출력되는지 확인합니다.
```bash
$ curl http://controller:8778
{"versions": [{"id": "v1.0", "max_version": "1.36", "min_version": "1.0", "status": "CURRENT", "links": [{"rel": "self", "href": ""}]}]}
```

#### 자격증명 소싱
위에 진행했던 자격증명 소싱 작업을 ssh 접근한 유저에서 진행하지 않았다면 , 진행합니다.
- [자격증명_소싱](#admin-openrc-설정-환경변수-소싱)
```bash
$ . admin-openrc
```

#### placement health check
placement-status 명령어를 통해 헬스체크 진행합니다.
- 주의 ! : 해당 명령어는 placement.conf파일을 참조합니다. 따라서 root권한이 없는 linux 유저로 해당 명령어를 수행하면 , 아래와같은 에러가 발생합니다.
- 꼭 sudo su로 root권한을 얻거나 , sudo 명령어를 추가하여 수행해야 합니다.
    - [관련이슈](https://storyboard.openstack.org/#!/story/2008969)

```bash
$ placement-status upgrade check
Traceback (most recent call last):
  File "/usr/bin/placement-status", line 10, in <module>
    sys.exit(main())
  File "/usr/lib/python3/dist-packages/placement/cmd/status.py", line 123, in main
    config(args=sys.argv[1:], project='placement')
  File "/usr/lib/python3/dist-packages/oslo_config/cfg.py", line 2141, in __call__
    self._check_required_opts()
  File "/usr/lib/python3/dist-packages/oslo_config/cfg.py", line 2879, in _check_required_opts        
    raise RequiredOptError(opt.name, group)
oslo_config.cfg.RequiredOptError: value required for option connection in group [placement_database]
```

헬스체크 진행
- 아래 공식 docs를 참고해서 , placement의 상태를 검사합니다.
- [upgrade check 반환 코드별 설명](https://docs.openstack.org/placement/yoga/cli/placement-status.html#upgrade)
```bash
$ sudo placement-status upgrade check
+----------------------------------+
| Upgrade Check Results            |
+----------------------------------+
| Check: Missing Root Provider IDs |
| Result: Success                  |
| Details: None                    |
+----------------------------------+
| Check: Incomplete Consumers      |
| Result: Success                  |
| Details: None                    |
+----------------------------------+
```

#### Placement API를 대상으로 명령어 실행
osc placment plugin을 설치하여 , 사용가능 리소스 목록을 확인해 봅니다.
- [osc placement plugin ?](https://docs.openstack.org/osc-placement/latest/)

osc placement plugin을 pip3로 설치합니다.
- pip3 명령어로 설치합니다.

```bash
$ pip3 install osc-placement
.....
.4->cliff>=3.2.0->osc-lib>=1.2.0->osc-placement) (1.0.0)
Installing collected packages: osc-placement
Successfully installed osc-placement-4.1.0
```

osc placement 명령어로 사용 가능한 리소스 클래스 및 특성을 list up 해봅니다.
```bash
# 사용가능 리소스 클래스 반환
$ openstack --os-placement-api-version 1.2 resource class list --sort-column name
+----------------------------+
| name                       |
+----------------------------+
| DISK_GB                    |
| FPGA                       |
| IPV4_ADDRESS               |
| MEMORY_MB                  |
| MEM_ENCRYPTION_CONTEXT     |
| NET_BW_EGR_KILOBIT_PER_SEC |
| NET_BW_IGR_KILOBIT_PER_SEC |
| NUMA_CORE                  |
| NUMA_MEMORY_MB             |
| NUMA_SOCKET                |
| NUMA_THREAD                |
| PCI_DEVICE                 |
| PCPU                       |
| PGPU                       |
| SRIOV_NET_VF               |
| VCPU                       |
| VGPU                       |
| VGPU_DISPLAY_HEAD          |
+----------------------------+

# 사용가능 리소스 특성 반환
$ openstack --os-placement-api-version 1.6 trait list --sort-column name
+---------------------------------------+
| name                                  |
+---------------------------------------+
| COMPUTE_ACCELERATORS                  |
| COMPUTE_DEVICE_TAGGING                |
| COMPUTE_GRAPHICS_MODEL_CIRRUS         |
| COMPUTE_GRAPHICS_MODEL_GOP            |
| COMPUTE_GRAPHICS_MODEL_NONE           |
| COMPUTE_GRAPHICS_MODEL_QXL            |
.....
```
