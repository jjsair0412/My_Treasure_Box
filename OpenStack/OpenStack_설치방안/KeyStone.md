# KeyStone 설치
- [Yoga_전체환경_KeyStone_설치방안_docs](https://docs.openstack.org/keystone/yoga/install/)
- [Yoga_ubuntu_KeyStone_설치방안_docs](https://docs.openstack.org/keystone/yoga/install/index-ubuntu.html)

## ETC
에러발생시 keystone의 로그파일 경로는 apache2 로그파일 경로와 동일합니다.
- conf파일 등이 바뀔경우 , bootstrap을 다시 진행하면 됩니다.

```bash
$ pwd
/var/log/apache2

$ ls
access.log  error.log  keystone_access.log  keystone.log  other_vhosts_access.log
```

## KeyStone Precondition
### 1. Precondition
- /etc/hosts 수정
    - 설치 대상이 되는 서버의 hosts파일을 수정합니다.

auth 서버로 외부에 노출될 domain이 없고 , 공부용으로 설치중이기 때문에 ip로 auth 서버를 노출시키거나 , 아래 문서대로 controller를 사용하려면 etc/hosts를 아래와 같이 수정합니다.

```bash
vi /etc/hosts
...
127.0.0.1 controller
```

- MySQL 설치 밎 구성

Identity 서비스를 설치 및 구성하기 전에 데이터베이스를 생성해야 합니다.

간단하게 apt로 설치합니다.
```bash
$ sudo apt-get update
$ sudo apt-get install mysql-server -y

# 데몬으로 mysql 실행
$ sudo systemctl enable mysql
$ sudo systemctl start mysql
```

설치한 MySQL에 root user로 접속한뒤 root 계정의 패스워드 타입을 변경합니다.
- auth_socket > mysql_native_password 으로 변경
```bash
$ sudo mysql -u root

mysql> use mysql;

# root의 plugin이 auth_socket으로 설정되어있는지 확인
mysql> SELECT User, Host, plugin FROM mysql.user;
+------------------+-----------+-----------------------+
| User             | Host      | plugin                |
+------------------+-----------+-----------------------+
| debian-sys-maint | localhost | caching_sha2_password |
| mysql.infoschema | localhost | caching_sha2_password |
| mysql.session    | localhost | caching_sha2_password |
| mysql.sys        | localhost | caching_sha2_password |
| root             | localhost | auth_socket           |
+------------------+-----------+-----------------------+

# mysql_native_password으로 변경
mysql> update user set plugin='mysql_native_password' where user='root';
Query OK, 1 row affected (0.01 sec)
Rows matched: 1  Changed: 1  Warnings: 0

mysql> flush privileges;
Query OK, 0 rows affected (0.00 sec)

# 변경상태 확인
mysql> select user, host, plugin from user;
+------------------+-----------+-----------------------+
| user             | host      | plugin                |
+------------------+-----------+-----------------------+
| debian-sys-maint | localhost | caching_sha2_password |
| mysql.infoschema | localhost | caching_sha2_password |
| mysql.session    | localhost | caching_sha2_password |
| mysql.sys        | localhost | caching_sha2_password |
| root             | localhost | mysql_native_password |
+------------------+-----------+-----------------------+
```

root 접속 확인합니다.
```bash
$ mysql -u root -p
mysql>
```

- keystone 데이터베이스 생성 및 데이터베이스 액세스 권한 부여
    - keystone
```bash
# keystone 이름의 데이터베이스 생성
mysql> CREATE DATABASE keystone;

# keystone localhost user 생성 (pwd 1234)
mysql> CREATE USER 'keystone'@'localhost' IDENTIFIED BY '1234';

# keystone user에게 localhost 권한부여
mysql> GRANT ALL PRIVILEGES ON *.* TO 'keystone'@'localhost' WITH GRANT OPTION;

# keystoen user에게 keystone 데이터베이스 localhost에서 관리 권한부여
mysql> GRANT ALL PRIVILEGES ON keystone.* TO 'keystone'@'localhost' WITH GRANT OPTION;

# keystone user 생성 (pwd 1234)
mysql> CREATE USER 'keystone'@'%' IDENTIFIED BY '1234';

# keystone user에게 권한부여
mysql> GRANT ALL PRIVILEGES ON *.* TO 'keystone'@'%' WITH GRANT OPTION;

# keystoen user에게 keystone 데이터베이스 관리 권한부여
mysql> GRANT ALL PRIVILEGES ON keystone.* TO 'keystone'@'%' WITH GRANT OPTION;
```

모든 쿼리가 실행됐다면 mysql에서 빠져나옵니다.
```bash
mysql> exit
Bye
```

- openstack command 설치
    - [설치_방안_docs](https://docs.openstack.org/ocata/admin-guide/common/cli-install-openstack-command-line-clients.html)
    - 얘도 os 환경마다 설치방안이 다르기 때문에 , 조심합니다.

먼저 python-pip와 python-dev를 설치합니다.
```bash
$ sudo add-apt-repository universe
$ sudo apt update
$ apt install python-dev python3-pip
```

pip로 OpenStack Client command를 설치합니다.
```bash
$ pip install python-openstackclient
```

openstack command 설치결과를 확인합니다.
```bash
$ openstack --version
openstack 6.2.0
```

## KeyStone 구성요소 설치 및 구성
### 설치시 주의사항
**Apache HTTP 서버를 사용하여 mod_wsgi포트 5000에서 Identity 서비스 요청을 제공**합니다. 
     
기본적으로 **keystone 서비스는 5000번 포트에서 수신 대기**합니다. 이 패키지는 모든 Apache 구성을 처리합니다(apache2 모듈 활성화 mod_wsgi및 Apache의 keystone 구성 포함).    

- keystone 패키지를 apt로 설치합니다.
```bash
$ sudo apt-get install keystone
```

### keystone.conf 설정 변경
keystone.conf파일을 활용하여 keystone을 설정합니다.
- 해당 문서는 기본 설치방안을 따릅니다.

conf파일 위치는 다음과 같습니다.
```bash
$ pwd
/etc/keystone/keystone.conf
```

[database]에서 아까 설치했던 MYSQL 액세스를 구성합니다.
```conf
[database]
# ...
connection = mysql+pymysql://keystone:KEYSTONE_DBPASS@controller/keystone

# 실 사용 명령어
connection = mysql+pymysql://keystone:1234@controller/keystone
```

[token]에서 fernet 토큰 공급자를 구성합니다.
```conf
[token]
# ...
provider = fernet
```

### Identity service database 구성
```bash
$ su -s /bin/sh -c "keystone-manage db_sync" keystone
```

### Fernet key store 초기화
```--keystore-user``` option과 ```---keystore-group``` 옵션은 , keystore를 실행하는데 사용될 os의 user / group을 의미합니다.

이들은 다른 OS user / group에게 실행하는것을 허용하게끔 제공됩니다.
- 아래 예에선 keystone 이름으로 user와 group 모두 통일합니다.
```bash
$ keystone-manage fernet_setup --keystone-user keystone --keystone-group keystone
$ keystone-manage credential_setup --keystone-user keystone --keystone-group keystone
```

### keystone ID Service bootstrap
KeyStone을 부트스트랩 합니다.
- ADMIN_PASS를 적절하게 바꿔줍니다.

```bash
$ keystone-manage bootstrap --bootstrap-password ADMIN_PASS \
  --bootstrap-admin-url http://controller:5000/v3/ \
  --bootstrap-internal-url http://controller:5000/v3/ \
  --bootstrap-public-url http://controller:5000/v3/ \
  --bootstrap-region-id RegionOne

# 실 사용 명령어
# controller엔 외부에 노출시킬 auth domain을 입력
$ keystone-manage bootstrap --bootstrap-password 1234 \
  --bootstrap-admin-url http://controller:5000/v3/ \
  --bootstrap-internal-url http://controller:5000/v3/ \
  --bootstrap-public-url http://controller:5000/v3/ \
  --bootstrap-region-id RegionOne
```
### DB sync
keystone과 연동할 mysql db를 동기화 합니다.
```bash
$ keystone-manage db_sync
```

**만약 이 과정을 생략한다면 , keystone에 500에러가 발생합니다.**

db 동기화가 완료되었는지 mysql db 접속하여 테이블 생성된것을 확인합니다.
- 동기화 하지 않는다면 테이블이 없습니다.
```bash
$  mysql -u root -p
```

테이블 생성된것을 확인
```bash
mysql> SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'keystone';
+------------------------------------+
| TABLE_NAME                         |
+------------------------------------+
| access_rule                        |
| access_token                       |
| application_credential             |
| application_credential_access_rule |
| application_credential_role        |
| assignment                         |
| config_register                    |
| consumer                           |
| credential                         |
| endpoint                           |
| endpoint_group                     |
| expiring_user_group_membership     |
| federated_user                     |
| federation_protocol                |
| group                              |
| id_mapping                         |
| identity_provider                  |
| idp_remote_ids                     |
| implied_role                       |
| limit                              |
| local_user                         |
| mapping                            |
| migrate_version                    |
| nonlocal_user                      |
| password                           |
| policy                             |
| policy_association                 |
| project                            |
| project_endpoint                   |
| project_endpoint_group             |
| project_option                     |
| project_tag                        |
| region                             |
| registered_limit                   |
| request_token                      |
| revocation_event                   |
| role                               |
| role_option                        |
| sensitive_config                   |
| service                            |
| service_provider                   |
| system_assignment                  |
| token                              |
| trust                              |
| trust_role                         |
| user                               |
| user_group_membership              |
| user_option                        |
| whitelisted_config                 |
+------------------------------------+
49 rows in set (0.00 sec)
```


## Apache Web Server 구성
### apache.conf파일 구성
apache web server에서 servername을 controller로 변경함으로써 apache가 controller 노드를 바라보게끔 옵션을 변경합니다.

이때 SSL 인증이 필요하다면 , apache에 구성합니다.

```bash
vi /etc/apache2/apache2.conf
```

ServerName 변경
- 만약 ServerName 이 없다면 , 추가
```conf
...
ServerName controller
...
```

apache restart
```bash
$ service apache2 restart
```

## 환경변수 세팅
환경변수를 export로 세팅하여 KeyStone의 Admin 계정을 구성합니다.
- 여기에 생성된 변수들은 keystone-manage로 부트스트랩할때 설정한 설정값으로 변경합니다.
- ADMIN_PASS를 이전에 keystone-manage로 부트스트랩 할 때 설정한 ADMIN_PASS로 변경합니다.

```bash
$ export OS_USERNAME=admin
$ export OS_PASSWORD=ADMIN_PASS
$ export OS_PROJECT_NAME=admin
$ export OS_USER_DOMAIN_NAME=Default
$ export OS_PROJECT_DOMAIN_NAME=Default
$ export OS_AUTH_URL=http://controller:5000/v3
$ export OS_IDENTITY_API_VERSION=3

# 실사용 명령어
# controller는 외부에 노출시킬 URL
$ export OS_USERNAME=admin
$ export OS_PASSWORD=1234
$ export OS_PROJECT_NAME=admin
$ export OS_USER_DOMAIN_NAME=Default
$ export OS_PROJECT_DOMAIN_NAME=Default
$ export OS_AUTH_URL=http://controller:5000/v3
$ export OS_IDENTITY_API_VERSION=3
```

## keystone에 curl 요청하여 확인
설정한 AUTH_URL에 curl요청하여 정상적으로 keystone이 배포되었는지 확인합니다.
```bash
$ curl http://controller:5000/v3
{"version": {"id": "v3.14", "status": "stable", "updated": "2020-04-07T00:00:00Z", "links": [{"rel": "self", "href": "http://controller:5000/v3/"}], "media-types": [{"base": "application/json", "type": "application/vnd.openstack.identity-v3+json"}]}}
```

## 도메인 , 프로젝트 , 사용자 역할 및 생성
keystone에선 각 OpenStack 프로젝트 별(nova , cinder 등 ..) 사용자 RBAC을 제공합니다.

이때 인증 방식으로 도메인 , 프로젝트 , USER 및 ROLE 조합을 사용합니다.
- 도메인별로 프로젝트를 논리적 그룹으로 나눕니다.
- 각 도메인은 독립적 사용자 및 프로젝트를 가질 수 있습니다.

### 1. 새로운 도메인을 만드는 방법
An Example Domain을 생성해 봅시다.
- 만약 설치만 진행한다면 해당 섹션 (새로운 도메인을 만드는방법) 은 skip해도 괜찮습니다.

1. 도메인 생성
    - --os-password 에는 bootstrap 시 설정한 비밀번호를 입력합니다.
```bash
$ openstack --os-auth-url http://controller:5000/v3 \
  --os-project-domain-name Default --os-user-domain-name Default \
  --os-project-name admin --os-username admin token issue

# 실사용 명령어
$ openstack domain create --description "An Example Domain" example \
    --os-password '1234' \
    --os-auth-url http://controller:5000/v3
+-------------+----------------------------------+
| Field       | Value                            |
+-------------+----------------------------------+
| description | An Example Domain                |
| enabled     | True                             |
| id          | 6ac93984cf5c481296b5b2fd4184e736 |
| name        | example                          |
| options     | {}                               |
| tags        | []                               |
+-------------+----------------------------------+
```

2. 프로젝트 생성
    - 각 서비스에 대해 고유한 사용자가 포함된 서비스 프로젝트를 사용합니다.
    - service project를 생성합니다.
```bash
$ openstack project create --domain default \
  --description "Service Project" service

# usecase
$ openstack project create --domain default \
    --description "Service Project" service \
    --os-password '1234' \
    --os-auth-url http://controller:5000/v3
+-------------+----------------------------------+
| Field       | Value                            |
+-------------+----------------------------------+
| description | Service Project                  |
| domain_id   | default                          |
| enabled     | True                             |
| id          | 5420b97c1d5d4ed18766c329ad45d78e |
| is_domain   | False                            |
| name        | service                          |
| options     | {}                               |
| parent_id   | default                          |
| tags        | []                               |
+-------------+----------------------------------+
```
### 2. 새로운 유저를 생성하고 권한을 부여하는 방법 
1. 사용자 생성

일반(비관리자) 작업은 권한이 없는 프로젝트 및 사용자를 사용해야 합니다. 따라서 프로젝트 및 일반유저를 생성하고 권한을 부여해봅니다.

- ```myproject``` 이름으로 프로젝트를 생성합니다.
```bash
$ openstack project create --domain default \
  --description "Demo Project" myproject \
  --os-password '1234' \
  --os-auth-url http://controller:5000/v3
+-------------+----------------------------------+
| Field       | Value                            |
+-------------+----------------------------------+
| description | Demo Project                     |
| domain_id   | default                          |
| enabled     | True                             |
| id          | 2402d4386874442796d8cbd4ab6482ae |
| is_domain   | False                            |
| name        | myproject                        |
| options     | {}                               |
| parent_id   | default                          |
| tags        | []                               |
+-------------+----------------------------------+
```

2. ```myuser``` 사용자를 생성합니다.
    - user password는 12345 로 설정합니다. (테스트기 때문에)
```bash
$ openstack user create --domain default \
  --password-prompt myuser \
  --os-password '1234' \
  --os-auth-url http://controller:5000/v3
User Password: 12345
Repeat User Password: 12345
+---------------------+----------------------------------+
| Field               | Value                            |
+---------------------+----------------------------------+
| domain_id           | default                          |
| enabled             | True                             |
| id                  | c3094f0af59149cc8b117a3e023ed104 |
| name                | myuser                           |
| options             | {}                               |
| password_expires_at | None                             |
+---------------------+----------------------------------+
```

3. ```myrole``` 이름을 가진 role을 생성합니다.
```bash
$ openstack role create myrole \
  --os-password '1234' \
  --os-auth-url http://controller:5000/v3
+-------------+----------------------------------+
| Field       | Value                            |
+-------------+----------------------------------+
| description | None                             |
| domain_id   | None                             |
| id          | 244871d84e034fe1a8633cf7fbbf641e |
| name        | myrole                           |
| options     | {}                               |
+-------------+----------------------------------+
```

4. 생성한 ```myuser``` 라는 유저에게 ```myrole``` 역할을 부여합니다.
    - 해당 명령은 output이 없습니다.
```bash
$  openstack role add --project myproject --user myuser myrole \
  --os-password '1234' \
  --os-auth-url http://controller:5000/v3
```

## 설치 결과확인
실제로 keystone이 정상 작동하는지 확인합니다.
1. 임시 설정해제

    - 임시로 OS_AUTH_URL 및 OS_PASSWORD 환경 변수를 설정 해제합니다.
```bash
$ unset OS_AUTH_URL OS_PASSWORD
```

2. admin 토큰 요청
openstack 명령어로 admin 인증 토큰을 요청합니다.
- 이때 각 옵션값을 keystone 부트스트랩 및 user/group을 생성할 때 사용한 옵션으로 변경합니다.
```bash
$ openstack --os-auth-url http://controller:5000/v3 \
  --os-project-domain-name Default --os-user-domain-name Default \
  --os-project-name admin --os-username admin token issue

# usecase
$ openstack --os-auth-url http://controller:5000/v3 \
  --os-project-domain-name Default --os-user-domain-name Default \
  --os-project-name admin --os-username admin token issue \
  --os-password '1234'
+------------+-----------------------------------------------------------------+
| Field      | Value                                                           |
+------------+-----------------------------------------------------------------+
| expires    | 2016-02-12T20:14:07.056119Z                                     |
| id         | gAAAAABWvi7_B8kKQD9wdXac8MoZiQldmjEO643d-e_j-XXq9AmIegIbA7UHGPv |
|            | atnN21qtOMjCFWX7BReJEQnVOAj3nclRQgAYRsfSU_MrsuWb4EDtnjU7HEpoBb4 |
|            | o6ozsA_NmFWEpLeKy0uNn_WeKbAhYygrsmQGA49dclHVnz-OMVLiyM9ws       |
| project_id | 343d245e850143a096806dfaefa9afdc                                |
| user_id    | ac3377633149401296f6c0d92d79dc16                                |
+------------+-----------------------------------------------------------------+
```

3. myuser 토큰 요청

이전에 만들었던 [새로운 유저를 생성하고 권한을 부여하는 방법](#1-새로운-도메인을-만드는-방법) ```myuser``` 의 인증 토큰을 요청해 봅니다.

  - ```myproject``` 또한 생성해야 합니다. 

```bash
$ openstack --os-auth-url http://controller:5000/v3 \
  --os-project-domain-name Default --os-user-domain-name Default \
  --os-project-name myproject --os-username myuser token issue \
  --os-password '12345'
+------------+-----------------------------------------------------------------+
| Field      | Value                                                           |
+------------+-----------------------------------------------------------------+
| expires    | 2016-02-12T20:15:39.014479Z                                     |
| id         | gAAAAABWvi9bsh7vkiby5BpCCnc-JkbGhm9wH3fabS_cY7uabOubesi-Me6IGWW |
|            | yQqNegDDZ5jw7grI26vvgy1J5nCVwZ_zFRqPiz_qhbq29mgbQLglbkq6FQvzBRQ |
|            | JcOzq3uwhzNxszJWmzGC7rJE_H0A_a3UFhqv8M4zMRYSbS2YF0MyFmp_U       |
| project_id | ed0b60bf607743088218b0a533d5943f                                |
| user_id    | 58126687cbcc4888bfa9ab73a2256f27                                |
+------------+-----------------------------------------------------------------+
```