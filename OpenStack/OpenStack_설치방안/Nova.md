# Nova
- [Yoga_전체환경_Nova_설치방안_docs](https://docs.openstack.org/nova/yoga/install/)
- [Yoga_ubuntu_Nova_Controller_설치방안_docs](https://docs.openstack.org/nova/yoga/install/controller-install-ubuntu.html)
- [Yoga_ubuntu_Nova_Compute_설치방안_docs](https://docs.openstack.org/nova/yoga/install/compute-install.html)

Nova를 설치하기 전 , 아키텍처 요구사항에 관련한 docs를 꼭 읽어보아야 합니다.

다음 문서는 Block Storage 및 Object Storage를 제외하고 , Controller Node와 compute Node만을 설치한 문서 입니다,, 
- [Yoga_Nova_Architecture](https://docs.openstack.org/nova/yoga/install/overview.html#example-architecture)

## ENV
- Controller ENV
  - endpoint API : http://controller:8774/v2.1
  - Domain : controller
  - Port : 8774 

- Compute ENV
  - endpoint API : http://controller:5672
  - Domain : http://controller:5672
  - Port :  5672

## Nova Precondition
OpenStack의 Nova를 설치하기 위해선 , 최소 2개의 노드가 필요합니다.
  - 그러나 해당 문서는 테스트기 때문에 , 1개 노드에서 둘다 설치합니다.
- 다음 아키텍처는 OpenStack의 최소 요구사항에 부합하는 아키텍처 입니다.
    - Prod용 시스템 아키텍쳐가 아니기 때문에 , Prod용 시스템 아키텍쳐는 다음 링크를 참고합니다.
        - [OpenStack Architecture Design Guide](https://docs.openstack.org/arch-design/)
        - [Openstack test Architecture](https://docs.openstack.org/nova/yoga/install/overview.html#example-architecture)


최소 요구사항에 부합하는 아키텍처를 따릅니다.

![hwreqs][hwreqs]


[hwreqs]:./images/hwreqs.png

***1. Controller Node***

    컨트롤러 노드는 ID 서비스, 이미지 서비스, 컴퓨팅의 관리 부분, 네트워킹의 관리 부분, 다양한 네트워킹 에이전트 및 대시보드를 실행합니다. 
    
    또한 SQL 데이터베이스, 메시지 대기열 및 NTP(Network Time Protocol)와 같은 지원 서비스도 포함됩니다.

    선택적으로 컨트롤러 노드는 Block Storage, Object Storage, Orchestration 및 Telemetry 서비스의 일부를 실행합니다.

    컨트롤러 노드에는 최소 두 개의 네트워크 인터페이스가 필요합니다.

    기존에 Placement와 Glance , KeyStore를 설치한 노드에 구성

***2. Compute Node***

    컴퓨팅 노드는 인스턴스의 하이퍼바이저 부분을 실행합니다. 
    
    기본적으로 Compute는 커널 기반 VM(KVM) 하이퍼바이저를 사용합니다. 
     
    컴퓨팅 노드는 인스턴스를 가상 네트워크에 연결하고 보안 그룹을 통해 인스턴스에 방화벽 서비스를 제공하는 네트워킹 서비스 에이전트도 실행합니다.

    둘 이상의 컴퓨팅 노드를 배포할 수 있습니다. 
    
    각 노드에는 최소 두 개의 네트워크 인터페이스가 필요합니다.

## Nova 구성
**먼저 Controller Node부터 구성합니다.**

## Controller Node 구성
### 1. Precondition
#### Database 구성
총 3개의 Database가 구성됩니다.
1. nova_api
2. nova
3. nova_cell0

```bash
$ mysql -u root -p
mysql>
```

-  데이터베이스 생성 및 데이터베이스 액세스 권한 부여
    - 각 Database에 대해 nova 계정에 적절한 비밀번호를 부여합니다. **해당 문서는 가이드이기에 모든 비밀번호 1234로 설정**

- 1. nova_api database 작업
    - 유저를 생성하는 부분은 , 모두같은 Database를 바라보고 있다면 에러가발생합니다.
    - 생략하고 넘어갑니다.
```bash
# nova_api 이름의 데이터베이스 생성
mysql> CREATE DATABASE nova_api;

# nova localhost user 생성 (pwd 1234)
mysql> CREATE USER 'nova'@'localhost' IDENTIFIED BY '1234';

# nova user에게 localhost 권한부여
mysql> GRANT ALL PRIVILEGES ON *.* TO 'nova'@'localhost' WITH GRANT OPTION;

# nova user에게 nova_api 데이터베이스 localhost에서 관리 권한부여
mysql> GRANT ALL PRIVILEGES ON nova_api.* TO 'nova'@'localhost' WITH GRANT OPTION;

# nova user 생성 (pwd 1234)
mysql> CREATE USER 'nova'@'%' IDENTIFIED BY '1234';

# nova user에게 권한부여
mysql> GRANT ALL PRIVILEGES ON *.* TO 'nova'@'%' WITH GRANT OPTION;

# nova user에게 nova_api 데이터베이스 관리 권한부여
mysql> GRANT ALL PRIVILEGES ON nova_api.* TO 'nova'@'%' WITH GRANT OPTION;
```

- 2. nova database 작업
```bash
# nova 이름의 데이터베이스 생성
mysql> CREATE DATABASE nova;

# nova localhost user 생성 (pwd 1234)
mysql> CREATE USER 'nova'@'localhost' IDENTIFIED BY '1234';

# nova user에게 localhost 권한부여
mysql> GRANT ALL PRIVILEGES ON *.* TO 'nova'@'localhost' WITH GRANT OPTION;

# nova user에게 nova 데이터베이스 localhost에서 관리 권한부여
mysql> GRANT ALL PRIVILEGES ON nova.* TO 'nova'@'localhost' WITH GRANT OPTION;

# nova user 생성 (pwd 1234)
mysql> CREATE USER 'nova'@'%' IDENTIFIED BY '1234';

# nova user에게 권한부여
mysql> GRANT ALL PRIVILEGES ON *.* TO 'nova'@'%' WITH GRANT OPTION;

# nova user에게 nova 데이터베이스 관리 권한부여
mysql> GRANT ALL PRIVILEGES ON nova.* TO 'nova'@'%' WITH GRANT OPTION;
```

- 3. nova_cell0 database 작업
```bash
# nova 이름의 데이터베이스 생성
mysql> CREATE DATABASE nova_cell0;

# nova localhost user 생성 (pwd 1234)
mysql> CREATE USER 'nova'@'localhost' IDENTIFIED BY '1234';

# nova user에게 localhost 권한부여
mysql> GRANT ALL PRIVILEGES ON *.* TO 'nova'@'localhost' WITH GRANT OPTION;

# nova user에게 nova 데이터베이스 localhost에서 관리 권한부여
mysql> GRANT ALL PRIVILEGES ON nova_cell0.* TO 'nova'@'localhost' WITH GRANT OPTION;

# nova user 생성 (pwd 1234)
mysql> CREATE USER 'nova'@'%' IDENTIFIED BY '1234';

# nova user에게 권한부여
mysql> GRANT ALL PRIVILEGES ON *.* TO 'nova'@'%' WITH GRANT OPTION;

# nova user에게 nova_cell0 데이터베이스 관리 권한부여
mysql> GRANT ALL PRIVILEGES ON nova_cell0.* TO 'nova'@'%' WITH GRANT OPTION;
```

모든 쿼리가 실행됐다면 mysql에서 빠져나옵니다.
```bash
mysql> exit
Bye
```

#### 자격증명 소싱
이전에 계속 진행했던 소싱작업을 동일하게 진행합니다.
- 동일한 파일을 사용합니다.
```bash
$ cat admin-openrc 
export OS_USERNAME=admin
export OS_PASSWORD=1234
export OS_PROJECT_NAME=admin
export OS_USER_DOMAIN_NAME=Default
export OS_PROJECT_DOMAIN_NAME=Default
export OS_AUTH_URL=http://controller:5000/v3
export OS_IDENTITY_API_VERSION=3

$ source admin-openrc

$ . admin-openrc
```

#### 자격증명 생성
compute 서비스의 자격 증명을 생성합니다.

```nova``` 사용자를 생성합니다.
- 비밀번호는 12345 로 세팅합니다. (테스트니까)

```bash
$ openstack user create --domain default --password-prompt nova
User Password: 12345
Repeat User Password: 12345
+---------------------+----------------------------------+
| Field               | Value                            |
+---------------------+----------------------------------+
| domain_id           | default                          |
| enabled             | True                             |
| id                  | e6dd0b1d5cf045bc8c777ce927e30ac9 |
| name                | nova                             |
| options             | {}                               |
| password_expires_at | None                             |
+---------------------+----------------------------------+
```

생성한 nova 사용자에게 admin role을 부여합니다.
- 해당 명령은 output이 나오지 않습니다.
```bash
$ openstack role add --project service --user nova admin
```

nova 서비스 항목을 생성합니다.
```bash
$ openstack service create --name nova \
  --description "OpenStack Compute" compute
+-------------+----------------------------------+
| Field       | Value                            |
+-------------+----------------------------------+
| description | OpenStack Compute                |
| enabled     | True                             |
| id          | da32d05013814688955c1f959064af14 |
| name        | nova                             |
| type        | compute                          |
+-------------+----------------------------------+
```

compute 서비스의 API Endpoint를 생성합니다.
- 8774 포트로 open합니다.
- /v2.1 로 접근합니다.
- endpoint는 3가지 생성하며 , interface로 구분됩니다.
    - public
    - internal
    - admin
```bash
# interface : public
$ openstack endpoint create --region RegionOne \
  compute public http://controller:8774/v2.1
+--------------+----------------------------------+
| Field        | Value                            |
+--------------+----------------------------------+
| enabled      | True                             |
| id           | 60e8d934ff594740a243854a034c928f |
| interface    | public                           |
| region       | RegionOne                        |
| region_id    | RegionOne                        |
| service_id   | da32d05013814688955c1f959064af14 |
| service_name | nova                             |
| service_type | compute                          |
| url          | http://controller:8774/v2.1      |
+--------------+----------------------------------+

# interface : internal
$ openstack endpoint create --region RegionOne \
  compute internal http://controller:8774/v2.1
+--------------+----------------------------------+
| Field        | Value                            |
+--------------+----------------------------------+
| enabled      | True                             |
| id           | 732f79f4c0664232a1d1dd450e25074c |
| interface    | internal                         |
| region       | RegionOne                        |
| region_id    | RegionOne                        |
| service_id   | da32d05013814688955c1f959064af14 |
| service_name | nova                             |
| service_type | compute                          |
| url          | http://controller:8774/v2.1      |
+--------------+----------------------------------+

# interface : admin
$ openstack endpoint create --region RegionOne \
  compute admin http://controller:8774/v2.1
+--------------+----------------------------------+
| Field        | Value                            |
+--------------+----------------------------------+
| enabled      | True                             |
| id           | e6534748f8a2409bbc48dd71bf2b6ca8 |
| interface    | admin                            |
| region       | RegionOne                        |
| region_id    | RegionOne                        |
| service_id   | da32d05013814688955c1f959064af14 |
| service_name | nova                             |
| service_type | compute                          |
| url          | http://controller:8774/v2.1      |
+--------------+----------------------------------+
```


#### RabbitMQ 설치 및 구성
Nova의 controller node에선 RabbitMQ 서버가 필요합니다.

### Nova controller 설치 및 conf구성
#### 패키지 설치
controller 패키지를 먼저 apt 명령어로 설치합니다.
- 해당 문서는 기본 설치방안을 따릅니다.
```bash
$ sudo apt-get install nova-api nova-conductor nova-novncproxy nova-scheduler
```

#### Nova controller conf파일 구성
Nova의 conf파일 위치는 다음과 같습니다.
```bash
vi /etc/nova/nova.conf
```

[api_database] 및 [database] 섹션에서 database 엑세스 구성을 기입해줍니다.

```conf
[api_database]
# ...
connection = mysql+pymysql://nova:NOVA_DBPASS@controller/nova_api

# 실사용 명령어
connection = mysql+pymysql://nova:1234@controller/nova_api

[database]
# ...
connection = mysql+pymysql://nova:NOVA_DBPASS@controller/nova

# 실사용 명령어
connection = mysql+pymysql://nova:1234@controller/nova
```

또한 이전에 설치했던 RabbitMQ인 메세지 큐 정보를 [DEFAULT] 섹션에 기입합니다.
- DEFAULT 섹션은 nova.conf 파일의 맨 위에 있습니다.
- ```RABBIT_PASS``` 에 설치한 메시지 큐 비밀번호를 기입합니다.
  - ```1234``` 로 설정했기 때문에 , 기입합니다.
```conf
[DEFAULT]
# ...
transport_url = rabbit://openstack:RABBIT_PASS@controller:5672/

# 실 사용 명령어
transport_url = rabbit://openstack:1234@controller:5672/
```

[keystone_authtoken] 및 [api] 섹션에서 KeyStone 엑세스 정보를 기입합니다.
- 각 auth 정보는 위에 소싱해둔 정보와 매칭시킵니다.
- 또한 username 및 password 칸에는 keystone을 통해 생성한 nova username 및 password를 기입합니다.
  - [nova 자격증명 생성](#자격증명-생성)
  - [admin_cli_소싱](#자격증명-소싱)
```conf
[api]
# ...
auth_strategy = keystone

[keystone_authtoken]
# 실 사용 keystone authtoken 섹션
# keystone auth 정보 입력
# source로 소싱해둔 admin-openrc 스크립트확인
www_authenticate_uri = http://controller:5000
auth_url = http://controller:5000
memcached_servers = controller:11211
auth_type = password
project_domain_name = Default
user_domain_name = Default
project_name = admin
username = nova
password = 12345
```

[service_user] 섹션에 서비스 사용자토큰 정보를 기입합니다.

***- 서비스 사용자 토큰 ?***

    openstack에서 다른 서비스에 REST API 호출할 때 , 일반 사용자 토큰과 함께 서비스 사용자 토큰을 같이 반환하도록 구성해야만 합니다.
      
    KeyStone은 일반 사용자 토큰이 만료될 경우에 , 서비스 사용자 토큰을 사용해서 요청을 인증하게 됩니다.
      
    이렇게 만든 이유는 , 만약 스냅샷 및 실시간 마이그레이션 작업 등의 오래 실행되는 작업을 수행할 때 , 사용자 토큰이 만료될 수 있기 때문입니다.

    만약 실행하고 있던 작업 수행시간이 사용자 토큰 유효시간보다 더 길어서 사용자 토큰이 만료됐을 경우에 , Nova가 블록 스토리지(Cinder) 또는 네트워킹(Neutron) 과 같은 다른 서비스 API를 호출할 때 실행하고있던 작업이 실패할 수 있기 때문입니다.

    또한 서비스 토큰으로 API 호출자가 서비스인지 식별하는데 쓰이기도 합니다.
    이는 일부 서비스 API를 사용자별로 제한하는데 사용되게 됩니다.

서비스 사용자토큰을 설정할 때 유의해야할 사항으론 , 
- 오픈스택(OpenStack)의 구성 요소 중 일부인 블록 스토리지 (Cinder), 이미지 관리 서비스 (Glance), 네트워킹 (Neutron)과 같은 다른 서비스들의 keystone_authtoken.service_token_roles에 service_user에 할당된 역할(role)이 포함되어야 합니다.

서비스 사용자 토큰을 [service_user] 섹션에 아래와 같이 기입합니다.
- 동일하게 username , password엔 nova user정보를 기입하며 , 나머지 정보는 keystone_authtoken 에 적용한 정보와 동일하게 기입합니다.
```conf
[service_user]
send_service_user_token = true
auth_url = http://controller:5000/identity
auth_strategy = keystone
auth_type = password
project_domain_name = Default
project_name = admin
user_domain_name = Default
username = nova
password = 12345
```

[DEFAULT] 섹션에 컨트롤러 노드의 private ip주소를 기입합니다.
- 이때 기입된 주소로 노드끼리 통신하기 때문에 , 통신 가능한 private ip를 기입해야 합니다.
- vagrantfile을 보면 , 192.168.50.10 으로 controller 노드 ip를 설정하였습니다.
```conf
[DEFAULT]
# ...
my_ip = 192.168.50.10
```

[vnc] 섹션에서 , VNC (Virtual Network Computer) 가 방금 my_ip 에 설정한 private IP 주소를 사용하도록 VNC 프록시를 구성합니다.
```conf
[vnc]
enabled = true
# ...
server_listen = $my_ip
server_proxyclient_address = $my_ip

# 실사용 명령어
[vnc]
enabled = true
server_listen = 192.168.50.10
server_proxyclient_address = 192.168.50.10
```

[glance] 섹션에서 이미지 서비스인 glance의 API endpoint를 기입해 줍니다.
- [Glance_설치_md](./Glance.md)
```conf
[glance]
# ...
api_servers = http://controller:9292
```

[oslo_concurrency] 섹션에 lock path를 구성합니다.
***- lock path란?***

    Nova는 동시성을 관리하기 위해 lock 파일을 사용하게 됩니다.

    이떄 lock 파일은 , 여러 프로세스나 스레드가 동시에 접근하지 못하도록 특정 작업을 독점하게끔 합니다.

    이를 통해서 데이터 일관성 및 안정성을 지킬 수 있습니다.

아래처럼 lock 파일이 저장될 경로를 구성합니다.
```conf
[oslo_concurrency]
# ...
lock_path = /var/lib/nova/tmp
```

OpenStack의 패키징 버그가 있기 떄문에 , [Default] 섹션에서 ```log_dir``` 구성을 제거해야 합니다.
```conf
# 작업 전 DEFAULT
[DEFAULT]
log_dir = /var/log/nova
lock_path = /var/lock/nova
state_path = /var/lib/nova
transport_url = rabbit://openstack:1234@controller:5672/
my_ip = 192.168.50.10

# 작업 후 DEFAULT
[DEFAULT]
lock_path = /var/lock/nova
state_path = /var/lib/nova
transport_url = rabbit://openstack:1234@controller:5672/
my_ip = 192.168.50.10
```

[placement] 섹션에 placement 접근 정보를 기입합니다.
- 이전에 설치해두었던 정보 및 소싱 정보를 조합해서 해당 섹션을 채워줍니다.
  - username 및 password에 placement 접근 정보를 기입합니다.
- [placement 설치 방안](./Placement.md)
```conf
[placement]
# ...
region_name = RegionOne
project_domain_name = Default
project_name = service
auth_type = password
user_domain_name = Default
auth_url = http://controller:5000/v3
username = placement
password = PLACEMENT_PASS

# 실사용 명령어
[placement]
# ...
region_name = RegionOne
project_domain_name = Default
project_name = admin
auth_type = password
user_domain_name = Default
auth_url = http://controller:5000/v3
username = placement
password = 12345
```

#### Nova 데이터베이스 작업
nova-api database를 동기화 합니다.
- 여기에 지원중단 메시지가 출력되는데 , 무시하면 됩니다.
```bash
$ su -s /bin/sh -c "nova-manage api_db sync" nova
```

cell0 데이터베이스를 등록합니다.
```bash
$ su -s /bin/sh -c "nova-manage cell_v2 map_cell0" nova
 result = self._query(query)
```

celll 셀을 만들어 줍니다.
```bash
$ su -s /bin/sh -c "nova-manage cell_v2 create_cell --name=cell1 --verbose" nova
  result = self._query(query)
85f331b7-27cd-4357-8b4f-793dbf00722b
```

nova 데이터베이스를 동기화 합니다.
```bash
$ su -s /bin/sh -c "nova-manage db sync" nova
```

nova cell0 및 cell1이 올바르게 등록되었는지 확인 합니다.
```bash
$ su -s /bin/sh -c "nova-manage cell_v2 list_cells" nova
+-------+--------------------------------------+----------------------------------------------------+--------------------------------------------------------------+----------+
|  Name |                 UUID                 |                   Transport URL                    |                     Database Connection                      | Disabled |
+-------+--------------------------------------+----------------------------------------------------+--------------------------------------------------------------+----------+
| cell0 | 00000000-0000-0000-0000-000000000000 |                       none:/                       | mysql+pymysql://nova:****@controller/nova_cell0?charset=utf8 |  False   |
| cell1 | f690f4fd-2bc5-4f15-8145-db561a7b9d3d | rabbit://openstack:****@controller:5672/nova_cell1 | mysql+pymysql://nova:****@controller/nova_cell1?charset=utf8 |  False   |
+-------+--------------------------------------+----------------------------------------------------+--------------------------------------------------------------+----------+
```

실제 output은 이렇게 출력됐습니다.
- 문제생기면 트러블슈팅할 예정
```bash
  result = self._query(query)
+-------+--------------------------------------+------------------------------------------+-------------------------------------------------+----------+
|  Name |                 UUID                 |              Transport URL               |               Database Connection               | Disabled |
+-------+--------------------------------------+------------------------------------------+-------------------------------------------------+----------+
| cell0 | 00000000-0000-0000-0000-000000000000 |                  none:/                  | mysql+pymysql://nova:****@controller/nova_cell0 |  False   |
| cell1 | 85f331b7-27cd-4357-8b4f-793dbf00722b | rabbit://openstack:****@controller:5672/ |    mysql+pymysql://nova:****@controller/nova    |  False   |
+-------+--------------------------------------+------------------------------------------+-------------------------------------------------+----------+
```

### Controller node 설치 마무리
- Nova compute 서비스를 다시 시작합니다.

```bash
$ service nova-api restart
$ service nova-scheduler restart
$ service nova-conductor restart
$ service nova-novncproxy restart
```

- curl명령 한번 날려봅니다.
```bash
curl http://controller:8774/v2.1
{"version": {"id": "v2.1", "status": "CURRENT", "version": "2.87", "min_version": "2.1", "updated": "2013-07-23T11:33:21Z", "links": [{"rel": "self", "href": "http://controller:8774/v2.1/"}, {"rel": "describedby", "type": "text/html", "href": "http://docs.openstack.org/"}], "media-types": [{"base": "application/json", "type": "application/vnd.openstack.compute+json;version=2.1"}]}}
```

## Compute Node 구성
- [Yoga_ubuntu_compute_node_설치방안_공식_docs](https://docs.openstack.org/nova/yoga/install/compute-install-ubuntu.html)

### ETC
compute 서비스의 로그파일 경로는 다음과 같습니다.
- compute 서비스가 실패하면 해당 로그를 확인하고 트러블슈팅
```bash
$ pwd
/var/log/nova/nova-compute.log
```

### OverView
    compute 서비스는 인스턴스 또는 VM을 배포하기 위해서 , 여러 하이퍼바이저를 지원합니다.

    그러나 해당 문서는 테스트기 때문에 , 가상 머신에 대한 하드웨어 가속을 지원하는 컴퓨팅 노드에서 커널 기반 VM(KVM) 확장과 함께 QEMU(Quick EMUlator) 하이퍼바이저를 사용합니다.

    해당 가이드 방안은 처음 compute 노드를 구성하는것을 가정하고 설치를 진행하게 됩니다. 따라서 추가적으로 노드를 구성하기 위해선 , [예시_아키텍쳐](https://docs.openstack.org/nova/yoga/install/overview.html#overview-example-architectures) 링크를 참고해서 구성합니다.

    추가적인 Compute Node는 고유 IP 주소가 필요합니다.

### Compute Node 설치 및 conf구성
    해당 설치 방안은 기본설치 입니다. 따라서 Prod에 설치하게 된다면 , 여기서 옵션을 추가하면 됩니다.

#### 패키지 설치
nova-compute 패키지를 apt 명령어로 설치합니다.
```bash
$ sudo apt-get install nova-compute
```

#### conf 구성
nova-compute 구성은 nova.conf파일에 설정합니다.
```bash
$ pwd
/etc/nova/nova.conf
```

[DEFAULT] 섹션에서 메세지 큐 정보를 기입합니다.
- RabbitMQ를 설치했기 때문에 , RabbitMQ 정보로 기입합니다.
```conf
[DEFAULT]
# ...
transport_url = rabbit://openstack:RABBIT_PASS@controller

# 실 사용 명령어
[DEFAULT]
# ...
transport_url = rabbit://openstack:1234@controller
```

[api] 섹션 및 [keystone_authtoken] 섹션에 KeyStone 서비스 엑세스 정보를 기입합니다.
```conf
[api]
# ...
auth_strategy = keystone

[keystone_authtoken]
# ...
www_authenticate_uri = http://controller:5000/
auth_url = http://controller:5000/
memcached_servers = controller:11211
auth_type = password
project_domain_name = Default
user_domain_name = Default
project_name = admin
username = nova
password = NOVA_PASS # 12345
```

[service_user] 섹션에 서비스 사용자 토큰 정보를 기입합니다.
```conf
send_service_user_token = true
auth_url = http://controller:5000/identity
auth_strategy = keystone
auth_type = password
project_domain_name = Default
project_name = admin
user_domain_name = Default
username = nova
password = NOVA_PASS # 12345
```

[DEFAULT] 섹션에 my_ip 를 구성합니다.
- MANAGEMENT_INTERFACE_IP_ADDRESS 를 compute 노드의 private IP로 변경합니다.
- 물론 controller node와 통신할수 있어야 합니다.
  - 같은 private IP 대역이여야만 함
```conf
[DEFAULT]
# ...
my_ip = MANAGEMENT_INTERFACE_IP_ADDRESS

[DEFAULT]
# ...
my_ip = 192.168.50.11
```

[neutron] 섹션을 구성합니다. [vnc] 원격 콘설 엑세스를 활성화 하고 구성합니다.
- 여기서 compute node는 모든 ip에 대해 listen하고는 있지만 , 프록시는 자신의 ip 주소만 허용하는것을 알 수 있습니다.
- novncproxy_base_url 은 , 웹 브라우저를 통해 해당 compute node에 있는 인스턴스의 원격 콘솔에 접근 할 수 있는 도메인주소를 나타냅니다.
  - controller나 다른 도메인으로 base_url을 설정했는데, 만약 사용자가 해당 도메인에 접근할 수 없는 환경이라면 , 해당 도메인을 controller node의 private ip로 변경해야만 합니다.
```conf
[vnc]
# ...
enabled = true
server_listen = 0.0.0.0
server_proxyclient_address = $my_ip
novncproxy_base_url = http://controller:6080/vnc_auto.html
```

[glance] 섹션에 glance 서비스 endpoint API를 구성합니다.
```conf
[glance]
# ...
api_servers = http://controller:9292
```

[placement] 섹션에 Placement API를 구성합니다.
- 이때 username과 password는 controller 노드에 구성한것과 동일하게 , placement의 user 정보를 기입합니다.
```conf
[placement]
# ...
region_name = RegionOne
project_domain_name = Default
project_name = service
auth_type = password
user_domain_name = Default
auth_url = http://controller:5000/v3
username = placement
password = PLACEMENT_PASS # 12345
```

### Compute Node 설치 마무리
#### 하드웨어 가속 지원여부 확인
- compute node가 가상 머신 하드웨어 가속을 지원하는지 아래 명령어로 확인합니다.
```bash
$ egrep -c '(vmx|svm)' /proc/cpuinfo
```
해당 명령어의 결과로 0이 아닌 값을 반환하게 된다면 , 하드웨어 가속을 지원하기때문에 추가 구성이 필요하지 않습니다.
- 따라서 해당 섹션을 넘어갑니다.

그러나 아래처럼 0을 반환한다면 , 노드가 가속을 지원하지 않기 때문에 , [libvirt] 섹션의 KVM 대신에 QEMU 를 사용하도록 구성을 변경해야 합니다.

```bash
# 하드웨어 가속 지원안함 (Vagrant by Virtualbox)
$ egrep -c '(vmx|svm)' /proc/cpuinfo
0
```

지원 안할경우 [libvirt] 섹션을 다음과 같이 변경합니다.
```conf
[libvirt]
# ...
virt_type = qemu
```
### Compute service 재시작
컴퓨팅 서비스를 재 시작 합니다.
- 하드웨어 가속을 지원하더라도 여기서부터 합니다.
```bash
$ service nova-compute restart
```

만약 아래와 같은 에러로그와 함께 실패한다면 ,
```bash
$ cat /var/log/nova/nova-compute.log
... 
AMQP server on controller:5672 is unreachablenova-compute
```

컨트롤러 노드의 방화벽이 포트 5672에 대한 액세스를 차단하고 있는것이기 때문에, 컨트롤러 노드에서 포트 5672를 열도록 방화벽을 구성하고 컴퓨팅 노드에서 서비스를 다시 시작합니다.

#### cell database 구성
cell database에 compute node를 추가합니다.

**- 해당 작업은 controller node에서 진행해야만 합니다.** 

1. compute host 확인
자격증명 소싱 후 , openstack 명령어로 CLI command를 활성화 시킨 이후에 , database에 compute node가 들어가있는지 확인합니다.

```bash
# 소싱
$ . admin-openrc

$ openstack compute service list --service nova-compute
+--------------------------------------+--------------+--------+------+---------+-------+----------------------------+
| ID                                   | Binary       | Host   | Zone | Status  | State | Updated At                 |       
+--------------------------------------+--------------+--------+------+---------+-------+----------------------------+       
| 86fe49bb-3388-4d8a-bbf3-1f6ad425fd09 | nova-compute | master | nova | enabled | up    | 2023-06-01T16:24:22.000000 |       
+--------------------------------------+--------------+--------+------+---------+-------+----------------------------+ 
```

2. 컴퓨팅 호스트 검색
- 아래처럼 매핑되었다면 정상 처리된것입니다.
```bash
$ sudo su

$ su -s /bin/sh -c "nova-manage cell_v2 discover_hosts --verbose" nova
... 
  result = self._query(query)
Checking host mapping for compute host 'master': d7e690dd-73af-4e65-99d6-0f5825d3c24f
Creating host mapping for compute host 'master': d7e690dd-73af-4e65-99d6-0f5825d3c24f
Found 1 unmapped computes in cell: 85f331b7-27cd-4357-8b4f-793dbf00722b
```

### ETC
만약 새로운 compute node를 추가하고 싶다면 , controller node에서 ```nova-manage cell_v2 discover_hosts``` 를 실행해야만 합니다.

또는 nova.conf의 [scheduler] 섹션에서 다음과 같이 인터벌 옵션을 줄 수 도 있습니다.
```conf
[scheduler]
discover_hosts_in_cells_interval = 300
```