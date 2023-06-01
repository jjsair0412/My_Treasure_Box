# Glance
- [Yoga_전체환경_Glance_설치방안_docs](https://docs.openstack.org/glance/yoga/install/)
- [Yoga_ubuntu_Glance_설치방안_docs](https://docs.openstack.org/glance/yoga/install/install-ubuntu.html)

## ENV
- endpoint API : http://controller:9292
- Domain : controller
- Port : 9292 

## ETC
Glance 설치 및 운영시 에러를 확인할 수 있는 로그파일 위치는 다음과 같습니다.
```bash
$ pwd
/var/log/glance/glance-api.log
```

glance 명령어들은 KeyStone과 연동하여 인증 과정이 필요합니다. 그러나 소싱작업을 통해 생략하였기에 , 명령어엔 env로 들어가지 않습니다.

따라서 꼭 소싱작업을 진행 하고 작업합니다.

***소싱이란 ?*** 

    환경 변수를 정의하고 설정하기 위해 외부 스크립트나 파일을 사용하는 것을 의미

## Glance Precondition
### 1. Precondition
Glance 이미지 프로젝트를 설치 구성하기 전 , 서비스 인증정보 및 API Endpoint를 설정해야 합니다.

#### MySQL USER 구성
- MySQL을 설치하는것은 , 이전 문서인 KeyStone에서 진행하였기 때문에 생략

```bash
$ mysql -u root -p
mysql>
```

- glance 데이터베이스 생성 및 데이터베이스 액세스 권한 부여
    - glance 계정에 적절한 비밀번호를 지정합니다. **해당 문서는 가이드이기에 1234로 설정**
```bash
# glance 이름의 데이터베이스 생성
mysql> CREATE DATABASE glance;

# glance localhost user 생성 (pwd 1234)
mysql> CREATE USER 'glance'@'localhost' IDENTIFIED BY '1234';

# glance user에게 localhost 권한부여
mysql> GRANT ALL PRIVILEGES ON *.* TO 'glance'@'localhost' WITH GRANT OPTION;

# glance user에게 glance 데이터베이스 localhost에서 관리 권한부여
mysql> GRANT ALL PRIVILEGES ON glance.* TO 'glance'@'localhost' WITH GRANT OPTION;

# glance user 생성 (pwd 1234)
mysql> CREATE USER 'glance'@'%' IDENTIFIED BY '1234';

# glance user에게 권한부여
mysql> GRANT ALL PRIVILEGES ON *.* TO 'glance'@'%' WITH GRANT OPTION;

# glance user에게 glance 데이터베이스 관리 권한부여
mysql> GRANT ALL PRIVILEGES ON glance.* TO 'glance'@'%' WITH GRANT OPTION;
```

모든 쿼리가 실행됐다면 mysql에서 빠져나옵니다.
```bash
mysql> exit
Bye
```

#### admin-openrc 설정 (환경변수 소싱)
- admin 관리자 전용 CLI 명령에 접근하기 위해서 , 아래 명령어로 자격 증명을 얻습니다.
- 소싱이란 , source 명령어로 환경변수를 정의한다는걸 의미합니다.
    - 실 예제는 admin 계정과 Default로 생성되는 프로젝트와 도메인을 사용합니다.
    - [admin_토큰_요청_명령어에서_확인](./KeyStone.md/#설치-결과확인)
        - **OS_USERNAME** : KeyStone에서 생성한정보
        - **OS_PASSWORD** : KeyStone에서 생성한정보
        - **OS_PROJECT_NAME** : KeyStone에서 생성한정보
        - **OS_USER_DOMAIN_NAME** : KeyStone에서 생성한정보
        - **OS_PROJECT_DOMAIN_NAME** : KeyStone에서 생성한정보
        - **OS_AUTH_URL** : KeyStone endpoint
        - **OS_IDENTITY_API_VERSION** : version
```bash
$ cat <<EOF > admin-openrc
export OS_USERNAME=admin
export OS_PASSWORD=1234
export OS_PROJECT_NAME=admin
export OS_USER_DOMAIN_NAME=Default
export OS_PROJECT_DOMAIN_NAME=Default
export OS_AUTH_URL=http://controller:5000/v3
export OS_IDENTITY_API_VERSION=3
EOF

$ source admin-openrc

$ . admin-openrc
```

#### openstack keystone 설정
먼저 , ```glance``` 사용자를 생성합니다.
- 비밀번호는 12345 로 세팅합니다. (테스트니까)
```bash
$ openstack user create --domain default --password-prompt glance \
    --os-password '1234' \
    --os-auth-url http://controller:5000/v3
User Password:12345
Repeat User Password:12345
+---------------------+----------------------------------+
| Field               | Value                            |
+---------------------+----------------------------------+
| domain_id           | default                          |
| enabled             | True                             |
| id                  | b3d673177a4a4aee84b538559bbd895d |
| name                | glance                           |
| options             | {}                               |
| password_expires_at | None                             |
+---------------------+----------------------------------+
```

service 프로젝트에 ```glance``` user admin 권한을 부여합니다.
- 해당 명령은 output이 없습니다.
```bash
$ openstack role add --project service --user glance admin \
    --os-password '1234' \
    --os-auth-url http://controller:5000/v3
```

OpenStack image 서비스를 생성합니다.
```bash
$ openstack service create --name glance \
    --description "OpenStack Image" image \
    --os-password '1234' \
    --os-auth-url http://controller:5000/v3
+-------------+----------------------------------+
| Field       | Value                            |
+-------------+----------------------------------+
| description | OpenStack Image                  |
| enabled     | True                             |
| id          | c21b9906d0494d1584f225d06b26c2a7 |
| name        | glance                           |
| type        | image                            |
+-------------+----------------------------------+
```

생성한 image 서비스의 API Endpoint를 생성합니다.
- 9292 포트로 open합니다.
- endpoint는 3가지 생성하며 , interface로 구분됩니다.
    - public
    - internal
    - admin
```bash
# image 서비스 endpoint : http://controller:9292
# interface : public
$ openstack endpoint create --region RegionOne \
  image public http://controller:9292
+--------------+----------------------------------+
| Field        | Value                            |
+--------------+----------------------------------+
| enabled      | True                             |
| id           | 80fc820c562340a8ac6046a5cefa56d3 |
| interface    | public                           |
| region       | RegionOne                        |
| region_id    | RegionOne                        |
| service_id   | c21b9906d0494d1584f225d06b26c2a7 |
| service_name | glance                           |
| service_type | image                            |
| url          | http://controller:9292           |
+--------------+----------------------------------+

# interface : internal
$ openstack endpoint create --region RegionOne \
  image internal http://controller:9292
+--------------+----------------------------------+
| Field        | Value                            |
+--------------+----------------------------------+
| enabled      | True                             |
| id           | e0cf05a73a7247adb1fdc0f0f5e7ff6e |
| interface    | internal                         |
| region       | RegionOne                        |
| region_id    | RegionOne                        |
| service_id   | c21b9906d0494d1584f225d06b26c2a7 |
| service_name | glance                           |
| service_type | image                            |
| url          | http://controller:9292           |
+--------------+----------------------------------+

# interface : admin
$ openstack endpoint create --region RegionOne \
  image admin http://controller:9292
+--------------+----------------------------------+
| Field        | Value                            |
+--------------+----------------------------------+
| enabled      | True                             |
| id           | 7f731fa728a143fa859664931aa1534a |
| interface    | admin                            |
| region       | RegionOne                        |
| region_id    | RegionOne                        |
| service_id   | c21b9906d0494d1584f225d06b26c2a7 |
| service_name | glance                           |
| service_type | image                            |
| url          | http://controller:9292           |
+--------------+----------------------------------+
```

## 테넌트별 ENDPOINT limit 지정방안
또한 Glance에서 테넌트별로 할당량을 지정해 줄 수 도 있습니다.
- [Glance 서비스 접근 limit 생성하기](https://docs.openstack.org/glance/yoga/install/install-ubuntu.html#prerequisites)

## Glance 구성요소 설치 및 구성
Glance 기본 설치 구성본으로 설치를 진행합니다.
- 해당 문서는 기본 설치방안을 따릅니다.

### Glance 설치 및 conf구성
- Glance 패키지를 apt로 설치합니다.
```bash
$ sudo apt-get install glance
```

glance의 conf파일 위치는 다음과 같습니다.
```bash
vi /etc/glance/glance-api.conf
```

[database] 에서 MYSQL 액세스를 구성합니다.
- 처음 설정했던 MYSQL 설정을 넣어줍니다.
```conf
[database]
# glance 데이터베이스 password 입력
connection = mysql+pymysql://glance:GLANCE_DBPASS@controller/glance

# 실 사용 명령어
connection = mysql+pymysql://glance:1234@controller/glance
```

[keystone_authtoken] 및 [paste_deploy] 칸에 ID 서비스(keystone) 액세스 설정을 구성해 줍니다.
- 최 하단 username 및 password는 , 위에 keystone을 통해 생성해준 glance user정보를 기입합니다.
- 각 정보는 , 위에 소싱해둔 정보와 매핑시킵니다.
    - [admin_cli_소싱](#1-admin-cli-소싱)
```conf
[keystone_authtoken]
# keystone auth 정보 입력
# source로 소싱해둔 admin-openrc 스크립트확인
www_authenticate_uri = http://controller:5000
auth_url = http://controller:5000
memcached_servers = controller:11211
auth_type = password
project_domain_name = Default
user_domain_name = Default
project_name = admin
username = glance
password = 12345


[paste_deploy]
# ...
flavor = keystone
```

[glance_store] 섹션에 로컬 폴더 어디에 이미지파일을 저장할것인지 위치를 지정합니다.
- /var/lib/glance/images 에 저장합니다.
```bash
$ ls /var/lib/glance/ 
image-cache  images
```

conf파일 설정
- filesystem_store_datadir 에 저장경로 입력합니다.
```conf
[glance_store]
# ...
stores = file,http
default_store = file
filesystem_store_datadir = /var/lib/glance/images/
```

[oslo_limit] 섹션에 keystone 액세스 정보를 구성합니다.
- 이떄 MY_SERVICE 계정에 시스템 범위 리소스에대한 접근 권한이 있는지 체크해야합니다.
- [통합 제한 클라이언트 구성에 대한 자세한 내용](https://docs.openstack.org/oslo.limit/latest/user/usage.html#configuration)
```bash
$ openstack role add --user MY_SERVICE --user-domain Default --system all reader

# 실 사용 명령어 (output 없음)
$ openstack role add --user glance --user-domain Default --system all reader
```

conf파일 설정
```conf
[oslo_limit]
auth_url = http://controller:5000 # keystone auth url
auth_type = password
user_domain_id = default
username = MY_SERVICE
system_scope = all
# password = MY_PASSWORD , keystone password
password = 1234  
endpoint_id = ENDPOINT_ID
region_name = RegionOne
```

[DEFAULT] 섹션에서 테넌트당 할당량을 활성화 합니다.
- 이 설정은 , 이전에 [테넌트별_limit_할당](#테넌트별-endpoint-limit-지정방안) 을 진행했을 경우에만 설정합니다.

```conf
[DEFAULT]
use_keystone_quotas = True
```

## Glance 설치
Glance image 서비스를 재 시작합니다.
```bash
$ service glance-api restart
```

Glance daemon 로그 확인합니다.
```bash
systemctl status glance-api
● glance-api.service - OpenStack Image Service API
     Loaded: loaded (/lib/systemd/system/glance-api.service; enabled; vendor preset: enabled)
     Active: active (running) since Sat 2023-05-27 15:18:29 UTC; 7s ago
   Main PID: 28444 (glance-api)
      Tasks: 9 (limit: 19100)
     Memory: 118.1M
     CGroup: /system.slice/glance-api.service
             ├─28444 /usr/bin/python3 /usr/bin/glance-api --config-file=/etc/glance/glance-api.conf --log-file=/var/log/glance/glance-api.log
             ├─28467 /usr/bin/python3 /usr/bin/glance-api --config-file=/etc/glance/glance-api.conf --log-file=/var/log/glance/glance-api.log
             ├─28468 /usr/bin/python3 /usr/bin/glance-api --config-file=/etc/glance/glance-api.conf --log-file=/var/log/glance/glance-api.log
             ├─28469 /usr/bin/python3 /usr/bin/glance-api --config-file=/etc/glance/glance-api.conf --log-file=/var/log/glance/glance-api.log
             ├─28470 /usr/bin/python3 /usr/bin/glance-api --config-file=/etc/glance/glance-api.conf --log-file=/var/log/glance/glance-api.log
             ├─28471 /usr/bin/python3 /usr/bin/glance-api --config-file=/etc/glance/glance-api.conf --log-file=/var/log/glance/glance-api.log
             ├─28472 /usr/bin/python3 /usr/bin/glance-api --config-file=/etc/glance/glance-api.conf --log-file=/var/log/glance/glance-api.log
             ├─28473 /usr/bin/python3 /usr/bin/glance-api --config-file=/etc/glance/glance-api.conf --log-file=/var/log/glance/glance-api.log
             └─28474 /usr/bin/python3 /usr/bin/glance-api --config-file=/etc/glance/glance-api.conf --log-file=/var/log/glance/glance-api.log

May 27 15:18:29 master systemd[1]: Started OpenStack Image Service API.
```

로그파일 확인
```bash
$ cat /var/log/glance/glance-api.log
...
2023-05-27 15:18:31.858 28472 INFO eventlet.wsgi.server [-] (28472) wsgi starting up on http://0.0.0.0:9292
2023-05-27 15:18:31.859 28444 INFO glance.common.wsgi [-] Started child 28473
2023-05-27 15:18:31.861 28473 INFO eventlet.wsgi.server [-] (28473) wsgi starting up on http://0.0.0.0:9292
2023-05-27 15:18:31.862 28444 INFO glance.common.wsgi [-] Started child 28474
2023-05-27 15:18:31.864 28474 INFO eventlet.wsgi.server [-] (28474) wsgi starting up on http://0.0.0.0:9292
```

curl 요청하여 json return 확인
```bash
curl http://controller:9292
{"versions": [{"id": "v2.9", "status": "CURRENT", "links": [{"rel": "self", "href": "http://controller:9292/v2/"}]}, {"id": "v2.7", "status": "SUPPORTED", "links": [{"rel": "self", "href": "http://controller:9292/v2/"}]}, {"id": "v2.6", "status": "SUPPORTED", "links": [{"rel": "self", "href": "http://controller:9292/v2/"}]}, {"id": "v2.5", "status": "SUPPORTED", "links": [{"rel": "self", "href": "http://controller:9292/v2/"}]}, {"id": "v2.4", "status": "SUPPORTED", "links": [{"rel": "self", "href": "http://controller:9292/v2/"}]}, {"id": "v2.3", "status": "SUPPORTED", "links": [{"rel": "self", "href": "http://controller:9292/v2/"}]}, {"id": "v2.2", "status": "SUPPORTED", "links": [{"rel": "self", "href": "http://controller:9292/v2/"}]}, {"id": "v2.1", "status": "SUPPORTED", "links": [{"rel": "self", "href": "http://controller:9292/v2/"}]}, {"id": "v2.0", "status": "SUPPORTED", "links": [{"rel": "self", "href": "http://controller:9292/v2/"}]}]}
```

## Glance with DB Sync
Glance api와 연동할 mysql db를 동기화 합니다.

위 명령어를 수행하지 않으면 , 생성한 glance Database에 테이블이 생성되지 않습니다.
```bash
mysql> SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'glance';
Empty set (0.00 sec)
```

수행하면 다음과 같은 결과를 볼 수 있습니다.
- db 동기화 수행

```bash
$ glance-manage db_sync
...
INFO  [alembic.runtime.migration] Running upgrade train_contract01 -> ussuri_contract01
INFO  [alembic.runtime.migration] Context impl MySQLImpl.
INFO  [alembic.runtime.migration] Will assume non-transactional DDL.
Upgraded database to: ussuri_contract01, current revision(s): ussuri_contract01
INFO  [alembic.runtime.migration] Context impl MySQLImpl.
INFO  [alembic.runtime.migration] Will assume non-transactional DDL.
Database is synced successfully.
```

테이블 생성되는것을 확인
```bash
mysql> SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'glance';
+----------------------------------+
| TABLE_NAME                       |
+----------------------------------+
| alembic_version                  |
| image_locations                  |
| image_members                    |
| image_properties                 |
| image_tags                       |
| images                           |
| metadef_namespace_resource_types |
| metadef_namespaces               |
| metadef_objects                  |
| metadef_properties               |
| metadef_resource_types           |
| metadef_tags                     |
| migrate_version                  |
| task_info                        |
| tasks                            |
+----------------------------------+
15 rows in set (0.00 sec)
```

테이블 확인되면 mysql에서 나옵니다.
```bash
mysql> exit
Bye
```

## Glance 작동 확인
Glance를 배포한 뒤 , Linux 이미지인 CirrOS를 이용하여 작동 여부를 테스트 합니다.

#### 1. admin CLI 소싱
admin-openrc 명령어를 통해 위에서 작업했던것 처럼 admin 관리자 전용 CLI에 대한 명령어 엑세스 권한을 얻습니다.
- [admin-openrc 설정](#admin-openrc-설정-환경변수-소싱)

### 2. 테스트용 원본 이미지 설치
wget으로 테스트할 대상인 CirrOS 이미지를 설치합니다.
```bash
$ wget http://download.cirros-cloud.net/0.4.0/cirros-0.4.0-x86_64-disk.img
```

### 3. image upload 테스트
모든 OpenStack 프로젝트에서 접근할 수 있게끔 QCOW2 디스크 형식, 베어 컨테이너 형식 및 공용 가시성을 사용하여 이미지 서비스인 Glance에 이미지를 upload 합니다.
- glance 프로젝트의 cmd 옵션 정보는 다음 docs를 보고 선택합니다.
    - [glance_cmd_options](https://docs.openstack.org/python-glanceclient/latest/cli/details.html)
- 아래 명령어로 image를 upload하면 , qcow2 디스크 형식으로 이미지를 업로드 합니다.
    - [QCOW2_방식_?_docs](https://docs.openstack.org/glance/yoga/glossary.html#term-QEMU-Copy-On-Write-2-QCOW2)
    - [베어_컨테이너_방식_?_docs](https://docs.openstack.org/glance/yoga/glossary.html#term-bare)
- auth서버의 인증은 admin-openrc 를 source로 소싱하여 유저 환경변수로 설정해두고 사용하기 때문에 , env 옵션으로 쓰지 않습니다.
    - [admin_cli_소싱](#admin-openrc-설정-환경변수-소싱)

```bash
$ glance image-create --name "cirros" \
    --file cirros-0.4.0-x86_64-disk.img \
    --disk-format qcow2 --container-format bare \
    --visibility=public
+------------------+------------------------------------------------------+
| Field            | Value                                                |
+------------------+------------------------------------------------------+
| checksum         | 443b7623e27ecf03dc9e01ee93f67afe                     |
| container_format | bare                                                 |
| created_at       | 2023-05-27T16:28:32Z                                 |
| disk_format      | qcow2                                                |
| file             | /v2/images/cc5c6982-4910-471e-b864-1098015901b5/file |
| id               | cc5c6982-4910-471e-b864-1098015901b5                 |
| min_disk         | 0                                                    |
| min_ram          | 0                                                    |
| name             | cirros                                               |
| owner            | bb091fdd1a6346df80e04dabbc0c4989                     |
| protected        | False                                                |
| schema           | /v2/schemas/image                                    |
| size             | 12716032                                             |
| status           | active                                               |
| tags             |                                                      |
| updated_at       | 2023-05-27T16:28:33Z                                 |
| virtual_size     | None                                                 |
| visibility       | public                                               |
+------------------+------------------------------------------------------+
```

### 4. 이미지 속성 확인
이미지 업로드가 정상 처리됐는지 확인하고 , 속성을 확인합니다.
```bash
$ glance image-list
+--------------------------------------+--------+
| ID                                   | Name   |
+--------------------------------------+--------+
| aabfd5d2-85dc-454d-84d8-34cf7051d602 | cirros |
+--------------------------------------+--------+
```