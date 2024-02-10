# ./Images/Semaphore_Ansible UI
- [공식문서](https://docs.semui.co)

Semaphore란 Ansible Playbook UI 입니다.

Task별로 버전관리를 수행해 주거나 , Inventory , env , key 들을 잘 저장해서 편하게 사용할 수 있게끔 도와주는 Tool 입니다.

## Semaphore 설치
Semaphore는 4가지 설치방안을 지원합니다.
1. snap
2. package manager
3. docker
4. binary file

### 1. package manager 설치 방안
위 설치 방안중 package manager 방안을 통해 설치합니다.

먼저 wget으로 package manager deb 파일을 가져옵니다.

```bash
wget https://github.com/ansible-semaphore/semaphore/releases/download/v2.9.45/./Images/Semaphore_2.9.45_linux_amd64.deb
```

이후 semaphore의 config.json 파일을 성상하기 위한 셋업 과정을 진행합니다.
먼저 DB를 선택합니다.

```bash
# Setup Semaphore by using the following command
semaphore setup
1. Set up configuration for a MySQL/MariaDB database
2. Set up a path for your playbooks (auto-created)
3. Run database Migrations
4. Set up initial semaphore user & password

# 사용할 백엔드 database 선택
## 테스트할땐 내장 BoltDB 사용하는데, 실무에서 사용할때는 외부 DB 사용,
What database to use:
   1 - MySQL
   2 - BoltDB
   3 - PostgreSQL
 (default 1): 2
```

이후 Semaphore 관련 설정을 진행합니다.
- 관련 설정들의 다양한 옵션들은 다음 공식문서를 참고합니다.
    - [Semaphore Setup 관련 Docs](https://docs.semui.co/administration-guide/configuration#configuration-options)

```bash
# Semaphore가 사용할 db 파일의 경로와 이름 설정
db filename (default /home/ubuntu/database.boltdb): 

# Semaphore 작업 실행 시 사용될 Ansible 플레이북의 기본 저장 위치를 설정
Playbook path (default /tmp/semaphore): 

# Semaphore 서버에 접근하기 위한 public domain 설정. 설정하지 않으면 private network로만 접근이 가능함
Public URL (optional, example: https://example.com/semaphore): 

# email 알람 설정 
Enable email alerts? (yes/no) (default no): 

# telegram 알람 설정
Enable telegram alerts? (yes/no) (default no): 

# slack 알람 설정
Enable slack alerts? (yes/no) (default no): 

# LDAP 인증 설정
Enable LDAP authentication? (yes/no) (default no): 

# Semaphore 설정 완료 이후 작성되는 config.json 파일의 저장 위치 설정
Config output directory (default /home/ubuntu): 

...

> Username:    # 닉네임
> Email:  # 이메일
> Your name:    # 이름
> Password: # 비밀번호

...
```

설정 완료 시 config.json 파일이 생성되는것을 확인할 수 있습니다.

```bash
cat ./config.json 
```

Semaphore server를 아래 명령어로 실행합니다.
- config.json을 직접 생성해서 실행할 수 도 있습니다.

```bash
$ semaphore service --config=./config.json
Loading config
Validating config
BoltDB /home/ubuntu/database.boltdb
Tmp Path (projects home) /tmp/semaphore
Semaphore v2.9.45
Interface 
Port :3000
Server is running
```

Semaphore를 package manager 또는 binary file로 설치했을 경우, Semaphore Deamon service를 수동으로 생성할 수 있습니다.
- [관련 공식문서](https://docs.semui.co/administration-guide/installation#run-as-a-service)

먼저 systemd service file을 생성합니다.

```bash
# 아래 /path/to/config.json 설정부분을 실제 config.json 파일의 경로로 변경해야 합니다.
sudo cat > /etc/systemd/system/semaphore.service <<EOF
[Unit]
Description=Semaphore Ansible
Documentation=https://github.com/ansible-semaphore/semaphore
Wants=network-online.target
After=network-online.target

[Service]
Type=simple
ExecReload=/bin/kill -HUP $MAINPID
ExecStart=/path/to/semaphore service --config=/path/to/config.json
SyslogIdentifier=semaphore
Restart=always
RestartSec=10s

[Install]
WantedBy=multi-user.target
EOF
```

Semaphore service를 실행합니다.

```bash
sudo systemctl daemon-reload
sudo systemctl start semaphore
sudo systemctl enable semaphore
```

생성한 Semaphore service의 상태를 확인합니다.

```bash
sudo systemctl status semaphore
```

Semaphore를 설치한 서버에서, 아래 명령어를 수행하여 설치가 잘 되었는지 확인합니다.

```bash
# CLI 확인
semaphore -h
semaphore version
semaphore user list
```

Semaphore는 3000번 포트로 open됩니다. 웹에 접근하여 설치 결과를 확인합니다.
- Semaphore를 웹을 통해 이용할 수 도 있지만, CLI를 통해 사용하는것 또한 가능합니다.

![./Images/Semaphore_1](./Images/Semaphore_1)

설치 시 설정했던 ID , PWD 를 통해 웹에 로그인 합니다.

![./Images/Semaphore_2](./Images/Semaphore_2)

기본적으로 Semaphore는 프로젝트 단위의 격리를 지원합니다.

따라서 , 가장 먼저 프로젝트를 생성해야 합니다.

![./Images/Semaphore_3](./Images/Semaphore_3)

프로젝트를 생성하면 다음과 같은 페이지로 이동합니다.

![./Images/Semaphore_4](./Images/Semaphore_4)


## Semaphore 사용
***Semaphore 또한 Ansible Playbook을 편리하게 사용하기 위한 UI 이기 때문에, 대상이되는 서버에 ssh 연결이 무조건 되어야만 합니다.***

### 1. Key Store 설정
Semaphore dashboard의 좌측 버튼중 Key Store에 접근합니다.
- [Key Store 공식문서](https://docs.semui.co/user-guide/key-store)

![./Images/Semaphore_5](./Images/Semaphore_5)


여기서 Key를 설정하는것은 다음과같은 목적이 있습니다.

1. Playbook 대상 hosts들에게 ssh 연결을하기 위해 사용
2. Repository에 remote 연결하기 위해 사용
3. sudo 권한을 갖기위해 사용
4. Ansible vault로 암호화된 파일을 복호화하기위해 사용

따라서 Key 를 생성할 때, 다음 Type들을 지원합니다.

1. SSH Key
    - host ssh 연결 및 remote repository 에 ssh 연결 . 시사용
2. Login With Password
    - 로그인 계정에 대한 암호 또는 access token
    - remote hosts 로그인 시 암호
    - remote hosts 에 sudo 사용 시 암호
    - remote Repo over HTTPS 로그인 시 암호
    - Ansible vaults 의 Unlock
3. None
    - remote repository 에 암호가 없는 경우
    - local repository에 접근하거나, public repository에 접근할 때 사용. public 일 경우 인증정보가 없기에.


먼저 SSH Key file을 등록합니다.
- 각 host Node에 SSH 연결이 가능하다면, ansible server에 위치한 ssh private key가 있을것입니다. 이를 넣어줍니다.

```bash
# private key 확인
cat /home/ubuntu/.ssh/id_rsa
-----BEGIN OPENSSH PRIVATE KEY-----
.....
-----END OPENSSH PRIVATE KEY-----
```

key를 생성합니다.

![./Images/Semaphore_6](./Images/Semaphore_6)

sudo 권한을 위한 root 계정의 Login 정보도 등록합니다.

![./Images/Semaphore_7](./Images/Semaphore_7)

remote repository 에 연결하기 위한 정보도 등록합니다.
- repo에 접근할 때 정보가 필요하지않기에 none으로 생성

![./Images/Semaphore_8](./Images/Semaphore_8)


### 2. Repositories 생성
Semaphore dashboard의 좌측 버튼중 Repositories에 접근합니다.
- [Repositories 공식문서](https://docs.semui.co/user-guide/repositories)

![./Images/Semaphore_9](./Images/Semaphore_9)

Repositories 는 Playbook과 role들이 위치한곳을 등록합니다.

3가지 저장소를 지원합니다.

1. 로컬 Git Repo : git://
2. 로컬 파일시스템 : file://
3. 리모트 Git Repo : accessed over HTTPS(`*https://*`) , or SSH(`*ssh://*`)
    - 인증 필요 : SSH 사용 시 SSH Key store , 인증 없을 경우 None type Key

테스트를 위해 로컬 파일시스템에 role이나 playbook을 가져오도록 세팅합니다.

먼저 Semaphore가 설치된 서버에 아래 경로를 생성합니다.

```bash
mkdir /tmp/semaphore
```

이후 생성한 로컬 경로를 바라보는 Repository를 생성합니다.
- 이때 Branch가 자동으로 비활성화되고 URL or path 창의 구분이 자동으로 local path로 적용되는것을 확인할 수 있습니다.

![./Images/Semaphore_10](./Images/Semaphore_10)


### 3. Environment 생성
Semaphore dashboard의 좌측 버튼중 Environment에 접근합니다.
- [Environment 공식 문서](https://docs.semui.co/user-guide/environment)

인벤토리에 추가 변수를 저장하는 곳으로 JSON 포멧으로 작성합니다.

![./Images/Semaphore_11](./Images/Semaphore_11)

빈 값과 user 정보 2가지 변수를 생성합니다.

#### 1. 빈 값
변수 이름을 `Empty` 로 두고 `Extra variables` 에 아래 빈 값을 넣어줍니다.

```json
{}
```

#### 2. user 정보
변수 이름을 `User-cloudneta` 으로 두고 `Extra variables` 에 아래 값을 넣어줍니다.

```json
{
  "user" : "cloudneta"
}
```

이렇게 설정해두면, Playbook을 수행할 때 `User-cloudneta` 을 통해서 "user" : "cloudneta" 변수를 가져올 수 있을것 입니다.


### 4. Inventory 설정
Semaphore dashboard의 좌측 버튼중 Inventory에 접근합니다.
- [Inventory 공식 문서](https://docs.semui.co/user-guide/inventory)

Ansible Inventory를 설정하는곳으로, Yaml , Json , Toml 포멧을 지원합니다.

![./Images/Semaphore_12](./Images/Semaphore_12)

Inventory를 설정할 땐 , Static , Static Yaml , File Type을 지원하기 때문에 다양한 형태로 Inventory를 지정할 수 있습니다. 또한 해당 Host를 접근할 때 어떤 Credential을 사용해야할지 이전에 Key Store에서 생상한 Key를 선택하며 , Playbook 상황에 따라 Sudo가 필요할 경우 Sudo 권한을 위한 Credentials도 선택이 가능합니다.

![./Images/Semaphore_13](./Images/Semaphore_13)

먼저 Static Type으로 Host를 단순 나열하는 형태로 Inventory를 생성합니다.

![./Images/Semaphore_14](./Images/Semaphore_14)

두번째로 env라는 이름의 Inventory를 생성합니다.
- 특이점이 있는데, 해당 Inventory는 user=study 라는 변수를 가집니다. 위에 Environment 를 생성할 때도 변수를 만들어주었는데, 우선순위는 Environment가 최상위고, 그다음이 Inventory 입니다.

```Toml
[web]
tnode1
tnode2

[db]
tnode3

[all:children]
web
db

[all:vars]
user=study
```

![./Images/Semaphore_15](./Images/Semaphore_15)


### 5. Task Templates 설정
Semaphore dashboard의 좌측 버튼중 Task Templates에 접근합니다.
- [Task Templates 공식 문서](https://docs.semui.co/user-guide/task-templates)

실제 Playbook을 생성하는 부분입니다. 특장점으로 Semaphore는 Cron을 지원하기때문에 따로 스크립트를 만들지 않더라도 Playbook을 주기별 자동실행할 수 있습니다.

![./Images/Semaphore_16](./Images/Semaphore_16)

3가지 Type을 지원합니다.

#### 1. Task
단순하게 특정 playbook을 특정 파라미터들로 실행

#### 2. Build
Semaphore에서 각 Playbook Task 별 버전관리를 수행하는 타입

버전관리는 지원하지만 Semaphore가 기본적으로 빌드 결과물이나 실행 파일 같은 아티팩트를 생성하고 관리하는 기능을 내장하고 있지 않습니다. 따라서 사용자가 생성된 아티팩트들을 관리하기 위해서 추가적인 작업을 수행해야만 합니다.

![./Images/Semaphore_17](./Images/Semaphore_17)


#### 3. Deploy
Semaphore에서 Build 에서 생성된 특정 아티팩트 버전을 파라미터들로 실행하는 타입

![./Images/Semaphore_18](./Images/Semaphore_18)


간단한 Test를 위해서 Task Type으로 진행합니다.

[Repo 생성](#2-repositories-생성) 시 만들어준 ```/tmp/semaphore``` 경로에 아래 Playbook을 생성합니다.

- Facts 확인

```yaml
cat << EOT > /tmp/semaphore/fact.yml
---

- hosts: all

  tasks:
  - name: Print all facts
    ansible.builtin.debug:
      msg: >
        The default IPv4 address of {{ ansible_facts.fqdn }}
        is {{ ansible_facts.default_ipv4.address }}
EOT
```

- user 확인

```yaml
cat << EOT > /tmp/semaphore/user.yml
---

- hosts: web
  tasks:
  - name: Create User {{ user }}
    ansible.builtin.user:
      name: "{{ user }}"
      state: present
EOT
```

첫번째 ```fact.yml``` Playbook을 사용하는 Task를 생성합니다.
- 위에 생성했었던 정보들을 바탕으로 대상 Inventory , target Repository , Environment 를 설정하고 생성합니다.
    - CronTab 을 통해 주기를 설정합니다. (매 5분주기)

![./Images/Semaphore_19](./Images/Semaphore_19)

두번째 ```user.yml``` Playbook을 사용하는 Task를 생성합니다.
- ***특징으로 users Playbook에는 user 변수를 사용합니다. 그런데 , Environment , Inventory 모두 user 변수를 갖고있습니다. 이때 우선순위는 Environment 입니다.***

![./Images/Semaphore_20](./Images/Semaphore_20)

RUN 버튼을 클릭하여 생성한 Task를 실행합니다. Task Debug 창이 보이게 됩니다.

![./Images/Semaphore_21](./Images/Semaphore_21)

Task 기록또한 각 task를 클릭하여 확인할 수 있습니다.

![./Images/Semaphore_22](./Images/Semaphore_22)

또한 Task가 수행될 때 마다 Inventory 정보가 파일로 생성됩니다.

```bash
$ pwd
/tmp/semaphore

$ ls -l
total 20
-rw-rw-r-- 1 ubuntu ubuntu 207 Feb 11 01:52 fact.yml
-rw-rw-r-- 1 ubuntu ubuntu  32 Feb 11 02:00 inventory_2147483644
-rw-rw-r-- 1 ubuntu ubuntu  32 Feb 11 02:00 inventory_2147483645
-rw-rw-r-- 1 ubuntu ubuntu  78 Feb 11 01:59 inventory_2147483646
-rw-rw-r-- 1 ubuntu ubuntu 132 Feb 11 01:52 user.yml
```

두번째 ```user.yml``` Task를 수행합니다.
- ***Fail 되게 됩니다. 그러한 이유는 env를 생성할 때, root 유저에 대한 user명을 설정해주지 않았기 때문***

![./Images/Semaphore_23](./Images/Semaphore_23)

Key Store에 돌아가서, 설정한 login-root key를 수정합니다.
- Override 버튼 클릭하면 덮어씌워집니다. 


![./Images/Semaphore_24](./Images/Semaphore_24)

Task를 재 실행하면, **성공하고 변수는 Environment 가 더 높은 우선순위를 갖기 때문에 cloudneta 으로 등록된것을 확인할 수 있습니다.**

![./Images/Semaphore_25](./Images/Semaphore_25)

두번째 Task를 수정해서 , 추가변수를 생성해 봅니다.
- Semaphore에서는 추가변수를 설정할 때, 아래처럼 Allow CLI args in Tasks 버튼을 클릭하고 ```["-e", ~~]``` 형태로 추가 변수를 설정해야만 합니다.

![./Images/Semaphore_26](./Images/Semaphore_26)

Save 하고 재 실행하면, 가장높은 우선순위를 가지는 추가 변수 (-e) 가 변수로 들어가는것을 확인할 수 있습니다.

![./Images/Semaphore_27](./Images/Semaphore_27)