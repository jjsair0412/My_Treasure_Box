# Nova
- [Yoga_전체환경_Nova_설치방안_docs](https://docs.openstack.org/nova/yoga/install/)
- [Yoga_ubuntu_Nova_Controller_설치방안_docs](https://docs.openstack.org/nova/yoga/install/controller-install-ubuntu.html)
- [Yoga_ubuntu_Nova_Compute_설치방안_docs](https://docs.openstack.org/nova/yoga/install/compute-install.html)

Nova를 설치하기 전 , 아키텍처 요구사항에 관련한 docs를 꼭 읽어보아야 합니다.

다음 문서는 Block Storage 및 Object Storage를 제외하고 , Controller Node와 compute Node만을 설치한 문서 입니다,, 
- [Yoga_Nova_Architecture](https://docs.openstack.org/nova/yoga/install/overview.html#example-architecture)

## ENV
- endpoint API : http://controller:8774/v2.1
- Domain : controller
- Port : 8774 


## Nova Precondition
OpenStack의 Nova를 설치하기 위해선 , 최소 2개의 노드가 필요합니다.
- 다음 아키텍처는 OpenStack의 최소 요구사항에 부합하는 아키텍처 입니다.
    - Prod용 시스템 아키텍쳐가 아니기 때문에 , Prod용 시스템 아키텍쳐는 다음 링크를 참고합니다.
        - [OpenStack Architecture Design Guide](https://docs.openstack.org/arch-design/)
        - [Openstack test Architecture](https://docs.openstack.org/nova/yoga/install/overview.html#example-architecture)


최소 요구사항에 부합하는 아키텍처를 따릅니다.
- 
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