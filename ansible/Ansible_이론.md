# Ansible의 소개 및 기본 사용

## 참고 링크
- [Ansible 공식 GitHub](https://github.com/ansible)
- [Ansible 공식 문서](https://docs.ansible.com/)
- [Ansible 공식 블로그](https://www.ansible.com/blog)

## Ansible이란 ?
Ansible은 오픈소스 IT 자동화 도구.

코드를 기반으로 여러 환경에 특정작업을 동일하게 적용될 수 있도록 도와주는 역할을 합니다.

### Ansible 특징
#### 1. Agentless
기존 자동화 도구들처럼 관리대상 Linux 노드에 Agent를 설치할 필요가 없습니다. Ansible은 Agnet 없이 SSH 기반으로 관리 노드에 접근하여 서버들을 관리합니다.

데몬 형식의 Agent를 통해 관리 노드를 관리햇을 때, 복잡한 추가 구성이나 패키지 모듈등을 설치하는 과정이 필요 없기에 편리합니다.

#### 2. 멱등성
동일한 연산을 여러번수행해도 결과가 달라지지 않는다는 성질을 갖고 있습니다.

#### 3. 쉬운 사용법과 다양한 모듈 제공
다른 자동화 도구에 비해 간단하고 복잡하지 않아 자동화 절차 및 과정을 이해하기가 쉽습니다.

yaml 기반의 문법을 갖고있기에 쉽게 작성하고 읽을 수 있습니다.

또한 파일 복사와 같은 일반시스템 관리부터 다양한 환경의 퍼블릭 클라우드 관련 모듈컬렉션까지 제공하기에, 쉽게 플레이북 예제를 찾아보고 자동화를 수행할 수 있습니다.

## Ansible 아키텍처
- Control Node (제어 노드)
    - 앤서블이 설치되는 노드로 운영체제가 리눅스라면 제어 노드가 될 수 있음.
    - 앤서블은 파이썬 모듈을 사용하기에 파이썬 설치가 필요

- Manage Node (관리 노드)
    - 앤서블이 제어하는 원격 시스템 또는 호스트
    - 리눅스가 설치된 노드일 수도 있고, 윈도우가 설치되어있을 수도 있음. 또는 퍼블릿 클라우드일경우, 프라이빗 클라우드일경우일수도 있음
    - 앤서블은 SSH 기반으로 작동하기에, 제어 노드와 SSH 연결이 되어야 하며 파이썬이 설치되어있어야 함.

- Inventory (인벤토리)
    - 제어 노드가 관리하는 관리 노드를 나열해둔 파일
    - 앤서블은 인벤토리에 사전 정의된 노드에만 접근할 수 있음
    - 인벤토리 목록은 관리 노드 성격별로 그룹화도 가능
```bash
# Inventory 예시
$ vi inventory
192.168.10.101

[WebServer]
web1.example.com
web2.example.com

[DBServer]
db1.example.com
db2.example.com
```

- Modules (모듈)
    - 앤서블은 관리 노드의 작업을 수행할 때 , SSH 연결 후 'Ansible Modules' 라는 스크립트를 푸시하여 작동
    - 대부분의 모듈은 시스템 상태를 설명하는 매게 변수를 허용하고, 모듈 실행이 완료되면 제거됨

- Plugin (플러그인)
    - 앤서블 핵심 기능 (데이터변환, 로그 출력, 인벤토리 연결 등) 을 강화하는 역할
    - **모듈이 대상 시스템에서 별도 프로세스로 실행되는 동안 플러그인은 제어 노드에서 실행됨**

- Playbook (플레이북)
    - 관리 노드에서 수행할 작업들을 Yaml 문법을 통해 순서대로 작성해둔 파일
    - 플레이북을 활용하여 관리 노드에 SSH 로 접근해 작업을 수행함
    - **플레이북은 사용자가 직접 작성하고 자동화를 완성하는 가장 중요한 파일**
```bash
# Playbook 예시
---
- hosts: webservers
  serial: 5  # 한 번에 5대의 머신을 업데이트하라는 의미
  roles:
  - common
  - webapp

- hosts: content_servers
  roles:
  - common
  - content
```

# Ansible 사용 방안
해당 문서의 Ansible 실습은, 아래 환경에서 수행합니다.

| Node | OS | vCPU | Memory | Disk | NIC IP | 관리자 계정 | (기본) 일반 계정 | Private IP |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| server | Ubuntu 22.04 | 2 | 4GB | 30GB | 10.10.1.10 | root / qwe123 | ubuntu / qwe123 | 192.168.100.4 |
| tnode1 | 상동 | 2 | 4GB | 30GB | 10.10.1.11 | root / qwe123 | ubuntu / qwe123 | 192.168.100.5 |
| tnode2 | 상동 | 2 | 4GB | 30GB | 10.10.1.12 | root / qwe123 | ubuntu / qwe123 | 192.168.100.6 |
| tnode3 | 상동 | 2 | 4GB | 30GB | 10.10.1.13 | root / qwe123 | ubuntu / qwe123 | 192.168.100.7 |

## Ansible 설치
### 1. Ansible 설치
***Ansible은 모든 노드(제어노드, 관리노드) 에 파이썬이 설치되어있어야 합니다.***

설치 방법은 호스트 시스템별로 상이하기에, 공식문서를 참고해서 설치합니다.
- [Ansible 설치 방안](https://docs.ansible.com/ansible/latest/installation_guide/installation_distros.html#installing-ansible-on-ubuntu)

아래 설치 과정은 Ubuntu:22.04 환경에서 앤서블을 설치하는 방법 입니다.

```bash
# 파이썬 버전 확인
python3 --version
Python 3.10.12

# 설치
apt install software-properties-common -y
add-apt-repository --yes --update ppa:ansible/ansible
apt install ansible -y

# 확인 : 책 버전(파이썬 3.11.2, jinja 진자 3.1.2)
ansible --version
ansible [core 2.15.8]
  config file = /etc/ansible/ansible.cfg
  configured module search path = ['/root/.ansible/plugins/modules', '/usr/share/ansible/plugins/modules']
  ansible python module location = /usr/lib/python3/dist-packages/ansible
  ansible collection location = /root/.ansible/collections:/usr/share/ansible/collections
  executable location = /usr/bin/ansible
  python version = 3.10.12 (main, Nov 20 2023, 15:14:05) [GCC 11.4.0] (/usr/bin/python3)
  jinja version = 3.0.3
  libyaml = True

cat /etc/ansible/ansible.cfg
which ansible
```

### 2. 관리 노드 SSH 인증 구성
앤서블은 제어 노드에서 관리 노드로 SSH 접근이 가능해야만 합니다.
- SSH Keyfile 방식으로 로그인할 수 있도록 세팅

1. ssh-keygen 명령어로 SSH 키 생성
```bash
# 모니터링
tree ~/.ssh
watch -d 'tree ~/.ssh'

# Create SSH Keypair
ssh-keygen -t rsa -N "" -f /root/.ssh/id_rsa
```

2. Public Key를 관리 노드에 복사
```bash
# 공개 키를 관리 노드에 복사
for i in {1..3}; do ssh-copy-id root@tnode$i; done

# 복사 확인
for i in {1..3}; do echo ">> tnode$i <<"; ssh tnode$i cat ~/.ssh/authorized_keys; echo; done

# ssh 접속 테스트
ssh tnode1
whoami
exit

ssh tnode2
exit

ssh tnode3
exit
```


# Ansible 사용
## Ansible 초기 구성 및 기초사용방법
앤서블을 사용하기 위해 먼저 Host를 설정합니다.


## Host 설정
### 1. inventory 작성
inventory 파일은 텍스트 파일이며, 앤서블이 자동화 대상으로 하는 관리 호스트를 지정합니다.

- ip 기반으로 inventory 파일 작성하기
```bash
# inventory 파일 생성
cat <<EOT > inventory
10.10.1.11
10.10.1.12
10.10.1.13
EOT

# inventory 검증 : -i 특정 인벤토리 지정
## ansible-inventory 명령어로 특정 인벤토리가 잘 작성되었는지 검증 할 수 있습니다.
ansible-inventory -i ./inventory --list | jq
{
  "_meta": {
    "hostvars": {}
  },
  "all": {
    "children": [
      "ungrouped"
    ]
  },
  "ungrouped": { # 관리 hosts들이 그룹화되어 있지 않다는 의미,
    "hosts": [
      "10.10.1.11",
      "10.10.1.12",
      "10.10.1.13"
    ]
  }
}
```

- 호스트 명으로 통신 가능할 경우, 호스트 명으로 inventory 파일 작성
```bash
# /etc/hosts 파일 확인
cat /etc/hosts

# inventory 파일 생성
cat <<EOT > inventory
tnode1
tnode2
tnode3
EOT

# inventory 검증
ansible-inventory -i ./inventory --list | jq
{
  "_meta": {
    "hostvars": {}
  },
  "all": {
    "children": [
      "ungrouped"
    ]
  },
  "ungrouped": {
    "hosts": [
      "tnode1",
      "tnode2",
      "tnode3"
    ]
  }
}
```

- 그룹별 호스트 설정
    - 하나의 inventory 파일에 여러 호스트들을 그룹별로 나눌 수 있습니다.
    - 이는 각 그룹별로 다른 자동화 작업을 수행할 수 있게끔 도와줍니다.
    - 예) web-server 그룹만 재시작을 수행하세요.
    - 또한 앤서블 인벤토리는 호스트 그룹에 기존에 정의한 호스트 그룹을 포함할 수도 있습니다.
      이 경우 호스트 그룹 이름 생성 시 :children 이라는 접미사를 추가하면 됩니다.

```bash
# inventory 그룹 구성
## 기존에 정의한 호스트 그룹 포함 ([all:children])
cat <<EOT > inventory
[web]
tnode1
tnode2

[db]
tnode3

[all:children]
web
db
EOT

# inventory 검증
ansible-inventory -i ./inventory --list | jq
{
  "_meta": {
    "hostvars": {}
  },
  "all": { # 모든 그룹 확인
    "children": [
      "ungrouped",
      "web",
      "db"
    ]
  },
  "db": { # db 그룹
    "hosts": [
      "tnode3"
    ]
  },
  "web": { # web 그룹
    "hosts": [
      "tnode1",
      "tnode2"
    ]
  }
}

ansible-inventory -i ./inventory --graph
@all:
  |--@ungrouped:
  |--@web:
  |  |--tnode1
  |  |--tnode2
  |--@db:
  |  |--tnode3
```

### 2. ansible.cfg 파일 작성
프로젝트 디렉터리 내에 앤서블 환경 설정 파일인 ansible.cfg 파일을 구성하면, -i 옵션을 사용하지 않아도 ansible.cfg 파일에 정의된 인벤토리의 호스트 정보를 확인할 수 있습니다.

```bash
# ansible.cfg 파일 생성
## defaults 값으로 현재 디렉토리의 inventory 파일을 사용한다, 를 세팅함
cat <<EOT > ansible.cfg
[defaults]
inventory = ./inventory
EOT

# inventory 목록 확인
ansible-inventory --list | jq
{
  "_meta": {
    "hostvars": {}
  },
  "all": {
    "children": [
      "ungrouped",
      "web",
      "db"
    ]
  },
  "db": {
    "hosts": [
      "tnode3"
    ]
  },
  "web": {
    "hosts": [
      "tnode1",
      "tnode2"
    ]
  }
}
```

### ETC - ansible.cfg의 적용 우선순위
ansible.cfg 파일은 경로나 환경변수에 따른 적용 우선순위가 있는데 , 이는 다음과 같습니다.

1. `ANSIBLE_CONFIG` (environment variable if set)
2. `ansible.cfg` (in the current directory)
3. `~/.ansible.cfg` (in the home directory)
4. `/etc/ansible/ansible.cfg`


## Playbook 작성
인벤토리를 이용하여 대상 호스트를 세팅한 뒤 , 대상 호스트에 수행될 작업들을 정의하기 위한 플레이북을 세팅합니다.

### 1. Playbook 환경설정
Playbook을 작성하고 실행하기 위해 여러가지 설정을 미리 해야 합니다.

이때 ```ansible.cfg``` 파일을 사용하게 되는데, 여기에 다양한 설정값을 통해 앤서블 설정을 하게 됩니다.
```ansible.cfg``` 파일은 역할을 기반으로 섹션별로 나뉘게 됩니다.

아래 예시 ansible.cfg 파일은 두 섹션으로 나뉩니다.
- [defaults]
- [privilege_escalation]
 
```bash
[defaults]
inventory = ./inventory
remote_user = root
ask_pass = false

[privilege_escalation]
become = true
become_method = sudo
become_user = root
become_ask_pass = false
```

**[defaults] 섹션** 
- 앤서블 작업을 위한 기본값 설정

| 매개 변수 | 설명 |
| --- | --- |
| inventory | 인벤토리 파일의 경로를 지정함. |
| remote_user | 앤서블이 관리 호스트에 연결할 때 사용하는 사용자 이름을 지정함. 이때, 사용자 이름을 지정하지 않으면 현재 사용자 이름으로 지정됨. |
| ask_pass | SSH 암호를 묻는 메시지 표시 여부를 지정함. SSH 공개 키 인증을 사용하는 경우 기본값은 false임. |

**[privilege_escalation] 섹션** 
- 보안과 감사로 인해 앤서블을 원격 호스트에 권한 없는 사용자로 먼저 연결한 후, 관리 액세스 권한을 에스컬레이션 하여 루트 사용자로 가져와야 할 때 사용

| 매개 변수 | 설명 |
| --- | --- |
| become | 기본적으로 권한 에스컬레이션을 활성화할 때 사용하며, 연결 후 관리 호스트에서 자동으로 사용자를 전환할지 여부를 지정함. 일반적으로 root로 전환되며, 플레이북에서도 지정할 수 있음. |
| become_method | 권한을 에스컬레이션하는 사용자 전환 방식을 의미함. 일반적으로 기본값은 sudo를 사용하며, su는 옵션으로 설정할 수 있음. |
| become_user | 관리 호스트에서 전환할 사용자를 지정함. 일반적으로 기본값은 root임. |
| become_ask_pass | become_method 매개 변수에 대한 암호를 묻는 메시지 표시 여부를 지정함. 기본값은 false임. 권한을 에스컬레이션하기 위해 사용자가 암호를 입력해야 하는 경우, 구성 파일에 become_ask_pass = true 매개 변수를 설정하면 됨. |

### 2. Playbook 수행
Playbook을 작성하거나 미리 정의된 모듈을 사용해서 자동화 작업을 수행해 봅니다.

#### 2.1 Playbook 모듈 실행해보기
ansible modules 을 통해 자동화 작업을 수행해 봅니다.

ansible의 ping 이라는 모듈을 사용해서 , 위에 설정한 inventory 그룹 중 web 그룹에 ping 테스트로 ansible이 관리가 가능한 상태인지를 체크합니다.
- **이는 ICMP 프로토콜 Ping이랑 다릅니다.**
```bash
ansible -m ping web
tnode1 | SUCCESS => {
    "ansible_facts": {
        "discovered_interpreter_python": "/usr/bin/python3"
    },
    "changed": false,
    "ping": "pong"
}
tnode2 | SUCCESS => {
    "ansible_facts": {
        "discovered_interpreter_python": "/usr/bin/python3"
    },
    "changed": false,
    "ping": "pong"
}

# db도 확인
ansible -m ping db 
tnode3 | SUCCESS => {
    "ansible_facts": {
        "discovered_interpreter_python": "/usr/bin/python3"
    },
    "changed": false,
    "ping": "pong"
}
```

파라미터값을 사용해서 실행할 수도 있습니다.
```bash
# 암호 입력 후 실행
ansible -m ping --ask-pass web
SSH password: <암호입력>
tnode2 | SUCCESS => {
    "ansible_facts": {
        "discovered_interpreter_python": "/usr/bin/python3"
    },
    "changed": false,
    "ping": "pong"
}
tnode1 | SUCCESS => {
    "ansible_facts": {
        "discovered_interpreter_python": "/usr/bin/python3"
    },
    "changed": false,
    "ping": "pong"
}
```

다른 사용자 계정으로 실행하기
- 파라미터값 사용
  - 해당 예제 중 첫번째는 실패합니다. 그 이유는 ubuntu 계정으로 관리 노드에 SSH 연결이 실패하기 때문입니다.
```bash
# root 계정 대신 ubnutu 계정으로 실행
## 실패
ansible -m ping web -u ubuntu
tnode1 | UNREACHABLE! => {
    "changed": false,
    "msg": "Failed to connect to the host via ssh: ubuntu@tnode1: Permission denied (publickey,password).",
    "unreachable": true
}
tnode2 | UNREACHABLE! => {
    "changed": false,
    "msg": "Failed to connect to the host via ssh: ubuntu@tnode2: Permission denied (publickey,password).",
    "unreachable": true
}

# ubuntu 계정으로 하는데, 비밀번호 입력하여 SSH 연결 성공 후 명령어 수행
ansible -m ping web -u ubuntu --ask-pass
tnode1 | SUCCESS => {
    "ansible_facts": {
        "discovered_interpreter_python": "/usr/bin/python3"
    },
    "changed": false,
    "ping": "pong"
}
tnode2 | SUCCESS => {
    "ansible_facts": {
        "discovered_interpreter_python": "/usr/bin/python3"
    },
    "changed": false,
    "ping": "pong"
}

ansible -m ping db -u ubuntu --ask-pass
tnode3 | SUCCESS => {
    "ansible_facts": {
        "discovered_interpreter_python": "/usr/bin/python3"
    },
    "changed": false,
    "ping": "pong"
}
```

shell 이라는 모듈도 실행해 봅니다.
```bash
#
ansible -m shell -a uptime all
tnode1 | CHANGED | rc=0 >>
 18:59:56 up 51 min,  1 user,  load average: 0.01, 0.02, 0.00
tnode2 | CHANGED | rc=0 >>
 18:59:56 up 51 min,  1 user,  load average: 0.08, 0.02, 0.01
tnode3 | CHANGED | rc=0 >>
 18:59:56 up 51 min,  1 user,  load average: 0.01, 0.01, 0.00

#
ansible -m shell -a "free -h" web
tnode1 | CHANGED | rc=0 >>
               total        used        free      shared  buff/cache   available
Mem:           3.8Gi       188Mi       2.9Gi       0.0Ki       641Mi       3.3Gi
Swap:             0B          0B          0B
tnode2 | CHANGED | rc=0 >>
               total        used        free      shared  buff/cache   available
Mem:           3.8Gi       199Mi       2.9Gi       0.0Ki       640Mi       3.3Gi
Swap:             0B          0B          0B

#
ansible -m shell -a "tail -n 3 /etc/passwd" db
tnode3 | CHANGED | rc=0 >>
_chrony:x:114:121:Chrony daemon,,,:/var/lib/chrony:/usr/sbin/nologin
ubuntu:x:1000:1000:Ubuntu:/home/ubuntu:/bin/bash
lxd:x:999:100::/var/snap/lxd/common/lxd:/bin/false
```

#### 2.2 Playbook 작성하여 수행
Playbook은 YAML 포멧으로 작성된 텍스트 파일입니다. 따라서 .yml 확장자를 사용합니다.

Playbook은 대상 호스트나 호스트 그룹에 수행할 작업을 정의하고 이를 실행하게 됩니다. 이때 특정 작업 단위를수행하기 위해 모듈을 적용합니다.
- Playbook을 작성할 땐 공백 문자(스페이스) 만 허용되므로 탭 문자는 사용하지 않는것이 좋습니다.

***또한 playbook은 host를 정의한 뒤, 해당 host에 전달할 task들을 배열의 형태로 작성합니다.***
이때 작업 이름, 수행할 모듈명, 모듈 파라미터 순으로 작성합니다.

아래 처럼 첫번째 Playbook을 작성합니다.

- 에러 없는버전
```yaml
cat first-playbook.yml
---
- hosts: all # inventory에 all 그룹 대상으로 수행
  tasks:
    - name: Print message # 첫번째 task의 이름
      debug: # 모듈 들어감. debug 모듈 사용
        msg: Hello CloudNet@ Ansible Study # debug 모듈에 전달할 파라미터
```

- 에러 있는버전
  - 들여쓰기가 잘못됨
```yaml
cat first-playbook-with-error.yml
---
- hosts: all
  tasks:
    - name: Print message
      debug:
      msg: Hello CloudNet@ Ansible Study
```

문법 확인 명령어를 통해 Playbook.yml 파일에 에러가 있는지 확인할 수 있습니다.

```bash
# 정상수행
ansible-playbook --syntax-check first-playbook.yml 
playbook: first-playbook.yml

# 문법 에러발생
ansible-playbook --syntax-check first-playbook-with-error.yml 
ERROR! conflicting action statements: debug, msg

The error appears to be in '/root/my-ansible/first-playbook-with-error.yml': line 4, column 7, but may
be elsewhere in the file depending on the exact syntax problem.

The offending line appears to be:

  tasks:
    - name: Print message
      ^ here
```

플레이북을 수행할 땐 , ```ansible-playbook``` 명령어를 사용합니다. 환경 설정 파일인 ansible.cfg가 존재하는 프로젝트 디렉터리 내에서 실행할 경우에는 ansible-playbook 명령어와 함께 실행하고자 하는 플레이북 파일명을 입력하면 됩니다.

```bash
ansible-playbook first-playbook.yml

PLAY [all] *************************************************************************************************************************************************************************************************************************

TASK [Gathering Facts] *************************************************************************************************************************************************************************************************************
ok: [tnode1]
ok: [tnode2]
ok: [tnode3]

TASK [Print message] ***************************************************************************************************************************************************************************************************************
ok: [tnode1] => {
    "msg": "Hello CloudNet@ Ansible Study"
}
ok: [tnode2] => {
    "msg": "Hello CloudNet@ Ansible Study"
}
ok: [tnode3] => {
    "msg": "Hello CloudNet@ Ansible Study"
}

PLAY RECAP *************************************************************************************************************************************************************************************************************************
tnode1                     : ok=2    changed=0    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
tnode2                     : ok=2    changed=0    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
tnode3                     : ok=2    changed=0    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
```

서비스 상태를 변경하는 Playbook을 생성해 봅니다.
- service 모듈은 리눅스의 데몬 시스템의 상태를 변경합니다.

아래 4가지 상태를 파라미터로 넣어서 정의할 수 있습니다.
- **started** : `Start service httpd, if not started`
- **stooped** : `Stop service httpd, if started`
- **restarted** : `Restart service httpd, in all cases`
- **reloaded** : `Reload service httpd, in all cases`

아래 예시 playbook은 모든 호스트를 대상으로 service 모듈을 사용해서 sshd 데몬 시스템을 재시작 하라는 자동화 스크립트를 갖고 있습니다.

```yaml
cat restart-service.yml
---
- hosts: all
  tasks:
    - name: Restart sshd service
      ansible.builtin.service: # service 모듈 사용 ,
        name: sshd # sshd 데몬시스템을
        state: restarted # 재시작 해라
```

작성한 playbook을 실행합니다.
```bash
# 문법검사
ansible-playbook --check restart-service.yml 

# playbook 실행
ansible-playbook restart-service.yml
```

재시작 대상 노드에서 sshd 데몬을 아래 명령어로 확인해보면, 실제로 playbook이 작동하면서 재시작 시키는것을 확인할 수 있습니다.
```bash
# (신규터미널) 모니터링 : 서비스 재시작 실행 여부 확인
ssh tnode1 tail -f /var/log/syslog
...
arted daemon_reload=False daemon_reexec=False scope=system no_block=False enabled=None force=None masked=None
Jan 13 10:15:10 ip-10-10-1-11 python3[3128]: ansible-ansible.legacy.setup Invoked with gather_subset=['all'] gather_timeout=10 filter=[] fact_path=/etc/ansible/facts.d
Jan 13 10:15:12 ip-10-10-1-11 python3[3196]: ansible-ansible.legacy.systemd Invoked with name=sshd state=restarted daemon_reload=False daemon_reexec=False scope=system no_block=False enabled=None force=None masked=None
Jan 13 10:15:12 ip-10-10-1-11 systemd[1]: Stopping OpenBSD Secure Shell server...
Jan 13 10:15:12 ip-10-10-1-11 systemd[1]: ssh.service: Deactivated successfully.
Jan 13 10:15:12 ip-10-10-1-11 systemd[1]: Stopped OpenBSD Secure Shell server.
Jan 13 10:15:12 ip-10-10-1-11 systemd[1]: Starting OpenBSD Secure Shell server...
Jan 13 10:15:12 ip-10-10-1-11 systemd[1]: Started OpenBSD Secure Shell server.
```

## 변수
앤서블 또한 변수를 사용하여 코드를 작성할 수 있습니다. 앤서블은 변수를 사용하여 사용자, 설치하고자 하는 패키지, 재시작할 서비스, 생성 또는 삭제할 파일명 등 시스템 작업 시 사용되는 다양한 값을 저장할 수 있습니다.

변수를 사용하게 되면, 사용자로부터 받은 값을 사용할 수 도 있으며 플레이북을 재 사용할 수 있습니다.

### 변수 이론
앤서블에서 사용되는 변수는 다음 종류들이 있습니다.
1. 그룹 변수
  - 인벤토리에 정의된 호스트 그룹에 적용되는 변수, 따라서 인벤토리에 선언해아 하며, ```:vars``` 라는 문자열을 추가해 추가 변수를 선언

2. 호스트 변수
  - 말 그대로 변수를 해당 호스트에서만 사용할 수 있는 변수

3. 플레이 변수
  - 플레이북 내에서 선언되는 변수

4. 추가 변수
  - 외부에서 ```ansible-playbook``` 을 실행할 때 함께 파라미터로 넘겨주는 변수

5. 작업 변수
  - 플레이북의 테스크 수행 결과를 저장한 것을 의미, 특정 작업과 수행 후 그 결과를 후속 작업에서 사용할 때 주로 사용됨.

### 변수 실습
아래 예시에는 ```ansible.builtin.user``` 라는 모듈을 사용합니다.
- [해당 모듈 공식문서 링크](https://docs.ansible.com/ansible/latest/collections/ansible/builtin/user_module.html)

#### 1. 그룹변수
인벤토리에 정의된 호스트 그룹에 적용되는 변수, 인벤토리에 선언되야 하고, 선언하고자 하는 그룹명과 함께, ```:vars``` 라는 문자열을 추가해 추가 변수를 선언. 그룹명에 ```:vars``` 추가하면 얘는 변수


- 아래 inventory 파일은 ```[all:children]``` 섹션을 선언하고 , 해당 섹션 아래에 ```user=ansible``` 이라는 변수와 값을 선언 합니다.
    
  이렇게 하면 ```all``` 이라는 그룹에서 ```user``` 라는 변수를 사용할 수 있으며, 다음 예제에서 ```all``` 그룹에는 ```web``` 그룹과 ```db``` 그룹이, ```web``` 그룹에는 ```tnode1``` , ```tnode2``` 가 포함되며, ```db```그룹에는 tnode3 호스트가 포함됩니다.

```bash
cat inventory
[web]
tnode1
tnode2

[db]
tnode3

[all:children]
web
db

[all:vars]
user=ansible
```


이제 그룹변수를 사용하는 Playbook을 생성합니다.

이때 Playbook 파일에서 변수를 사용하기 위해선 , 반드시 **중괄호를 넣고 앞뒤로 한칸씩 스페이스로 띄어야 합니다.**

- 아래 Playbook은 유저가 있는지 확인하고 없다면 생성하는 모듈을 사용합니다.
```yaml
cat create-user.yml
---

- hosts: all
  tasks:
  - name: Create User {{ user }} # 변수를 사용할 때 겹 중괄호를 넣고 반드시 앞뒤로 한칸씩 스페이스로 띄어야함
    ansible.builtin.user:
      name: "{{ user }}"
      state: present
```

Playbook을 실행합니다.
- inventory 파일에 생성된 user 변수의 값인 ansible 로 없다면 각 노드에 유저가 생성되고, 있다면 ok로 task 가 성공하게 됩니다.

```bash
ansible-playbook create-user.yml

PLAY [all] *************************************************************************************************

TASK [Gathering Facts] *************************************************************************************
ok: [tnode1]
ok: [tnode2]
ok: [tnode3]

TASK [Create User ansible] *********************************************************************************
changed: [tnode1]
changed: [tnode2]
changed: [tnode3]

PLAY RECAP *************************************************************************************************
tnode1                     : ok=2    changed=1    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
tnode2                     : ok=2    changed=1    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
tnode3                     : ok=2    changed=1    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
```

실제 대상 호스트에서 ansible 사용자가 생성되었는지 확인합니다.
```bash
for i in {1..3}; do echo ">> tnode$i <<"; ssh tnode$i tail -n 3 /etc/passwd; echo; done
for i in {1..3}; do echo ">> tnode$i <<"; ssh tnode$i ls -l /home; echo; done
```

수동으로 첫번째 관리 노드에서만 유저를 제거하고 다시 플레이북을 실행해 봅니다.
- 제거된 tnode1 번 노드에서만 유저가 생성되기에 changed 로 체크되고, 나머지 두 노드는 유저가 존재하기에 ok 로 상태가 넘어갑니다.
```bash
# tnode1 에 ansible 사용자 삭제 후 확인
ssh tnode1 userdel -r ansible
ssh tnode1 tail -n 2 /etc/passwd

# 실행
ansible-playbook create-user.yml
PLAY [all] **********************************************************************************************************************************

TASK [Gathering Facts] **********************************************************************************************************************
ok: [tnode1]
ok: [tnode2]
ok: [tnode3]

TASK [Create User ansible] ******************************************************************************************************************
ok: [tnode2]
ok: [tnode3]
changed: [tnode1]

PLAY RECAP **********************************************************************************************************************************
tnode1                     : ok=2    changed=1    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
tnode2                     : ok=2    changed=0    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
tnode3                     : ok=2    changed=0    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
```


#### 2. 호스트 변수
inventory의 해당 호스트에서만 사용할 수 있는 변수

- 아래 inventory 파일은 [db] 그룹의 호스트들에게만 사용이 가능한 변수 ```user``` 를 선언한 예시 입니다.
```bash
[web]
tnode1
tnode2

[db]
tnode3 user=ansible1

[all:children]
web
db

[all:vars]
user=ansible
```

[db] 그룹 호스트에게만 사용이 가능한 변수기때문에, Playbook의 host를 [db] 그룹으로 변경해 줍니다.
- 변수 사용방법은 동일합니다.

```yaml
cat create-user1.yml
---

- hosts: db # db 그룹의 hosts들 대상으로 하는 tasks 선언
  tasks:
  - name: Create User {{ user }} # 변수를 사용할 때 겹 중괄호를 넣고 반드시 앞뒤로 한칸씩 스페이스로 띄어야함
    ansible.builtin.user:
      name: "{{ user }}"
      state: present
```

Playbook을 실행합니다.
```bash
ansible-playbook create-user1.yml

PLAY [db] ***************************************************************************************************************************************************

TASK [Gathering Facts] **************************************************************************************************************************************
ok: [tnode3]

TASK [Create User ansible1] *********************************************************************************************************************************
changed: [tnode3]

PLAY RECAP **************************************************************************************************************************************************
tnode3                     : ok=2    changed=1    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
```


실행 결과 [db] 그룹 대상으로 user 모듈이 수행된것을 확인할 수 있습니다.
```bash
# 확인
for i in {1..3}; do echo ">> tnode$i <<"; ssh tnode$i tail -n 3 /etc/passwd; echo; done
```


#### 3. 플레이 변수
플레이북 내에서 선언되는 변수

플레이북 작성 시 변수를 선언하여, 플레이북내부에서만 사용되는 변수입니다.

사용 방법은, ```hosts``` 아래에 ```vars:``` 를 추가하고 그 아래에 ```변수명: 값``` 을 두면서 변수를 할당합니다.
- 아래 예제는 [all] 그룹의 호스트들을 대상으로 ```user: ansible2``` 라는 변수를 할당합니다.

```yaml
cat create-user2.yml
---

- hosts: all # all 그룹대상
  vars:
    user: ansible2 # user 변수 생성
  
  tasks:
  - name: Create User {{ user }}
    ansible.builtin.user:
      name: "{{ user }}"
      state: present
```

playbook을 수행합니다.
- 인벤토리에 선언한 그룹 변수, 호스트 변수, 플레이 변수가 셋다 같은 변수명으로 선언되어있을 경우, 플레이 변수가 가장 높은 우선순위로 할당됩니다. 그 다음 인벤토리에 선언된 그룹변수, 호스트 변수 순으로 높습니다.

```bash
# (터미널2) 모니터링
watch -d "ssh tnode3 tail -n 3 /etc/passwd"

#  인벤토리에 선언한 그룹 변수와 호스트 변수 확인
cat inventory

# 실행
ansible-playbook create-user2.yml
...
PLAY RECAP ***********************************************************************************************************
tnode1                     : ok=2    changed=1    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
tnode2                     : ok=2    changed=1    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
tnode3                     : ok=2    changed=1    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   

# 확인
for i in {1..3}; do echo ">> tnode$i <<"; ssh tnode$i tail -n 3 /etc/passwd; echo; done
```

플레이 변수는 변수 파일을 외부에 따로 정의해 두고, 해당 파일을 갖고오는 형태로도 사용이 가능합니다.

변수 모음 파일 생성
```yaml
cat ~/my-ansible/users.yaml
user: ansible3
```

플레이북 생성
- ```vars_files``` 모듈을 사용합니다.

```yaml
---

- hosts: all
  vars_files: # 해당 모듈 사용하면 파일 가져올 수 있음
    - vars/users.yml # vars/users.yml 파일 참조


  tasks:
  - name: Create User {{ user }}
    ansible.builtin.user:
      name: "{{ user }}"
      state: present
```

플레이북을 수행해 봅니다.
- ansible3 유저가 생성됩니다. 이는 변수를 따로 파일로 선언해두어도 플레이북 내부에 꺼내왓기에 플레이 변수로 할당되어 가장 높은 우선순위를 가지기 때문입니다.

```bash
# (터미널2) 모니터링
watch -d "ssh tnode3 tail -n 3 /etc/passwd"

# 플레이 변수 파일 확인
cat vars/users.yml

# 실행
ansible-playbook create-user3.yml
...
TASK [Create User ansible3] *********
...

# 확인
for i in {1..3}; do echo ">> tnode$i <<"; ssh tnode$i tail -n 4 /etc/passwd; echo; done
```


#### 4. 추가 변수
ansible-playbook 을 수행할 때, 함께 파라미터로 넘겨주는 변수를 의미합니다.

**이 변수는 가장 우선순위가 높습니다.**
- 변수 우선 순위 : 추가변수(실행 시 파라미터) > 플레이 변수 > 호스트 변수 > 그룹 변수

create-user3.yml 플레이북을 사용하는데, -e 명령어를 통해 파라미터로 넘겨줄 수 있습니다.

```bash
# (터미널2) 모니터링
watch -d "ssh tnode3 tail -n 3 /etc/passwd"

# 실행
ansible-playbook -e user=ansible4 create-user3.yml
...
TASK [Create User ansible4] *********
...

# 확인
for i in {1..3}; do echo ">> tnode$i <<"; ssh tnode$i tail -n 5 /etc/passwd; echo; done
ansible2:x:1003:1003::/home/ansible2:/bin/sh
ansible3:x:1004:1004::/home/ansible3:/bin/sh
ansible4:x:1005:1005::/home/ansible4:/bin/sh
```

#### 5. 작업 변수
플레이북 수행 할 때 , task 수행 이후 결과값을 저장하는 변수입니다. 특정 작업을 수행한 이후 후속 작업에서 그 결과를 사용할 때 사용됩니다.
- 예를 들어 클라우드 시스템에 VM을 생성한다고 가정해보겠습니다. 이를 위해서는 네트워크나 운영체제 이미지와 같은 가상 자원이 필요합니다.
- 가상 자원을 조회하고, 조회된 결과를 가지고 VM을 생성할 때는 작업 변수를 사용하면 좋습니다.

사용할땐 플레이북을 수정합니다. 이때 ```register``` 를 선언하게 되는데, ```register``` 의 값에 선언된 변수에 테스크 실행 결과를 저장하겠다는 의미가 됩니다.
- debug 모듈을 사용하여 작업 결과를 출력합니다.

```yaml
cat create-user4.yml
---

- hosts: db
  tasks:
  - name: Create User {{ user }}
    ansible.builtin.user:
      name: "{{ user }}"
      state: present
    register: result # result 변수에 Create User Task 실행결과 저장

  - ansible.builtin.debug: # debug 모듈 사용
      var: result
```

작성한 플레이북을 실행해서 확인해 봅니다.
- TASK ```[ansible.builtin.debug]``` 에 첫번째 Task 실행 결과가 저장된것을 확인할 수 있습니다.
```bash
# (터미널2) 모니터링
watch -d "ssh tnode3 tail -n 3 /etc/passwd"

# 실행
ansible-playbook -e user=ansible5 create-user4.yml

PLAY [db] ************************************************************************************************************

TASK [Gathering Facts] ***********************************************************************************************
ok: [tnode3]

TASK [Create User ansible5] ******************************************************************************************
changed: [tnode3]

TASK [ansible.builtin.debug] *****************************************************************************************
ok: [tnode3] => {
    "result": {
        "changed": true,
        "comment": "",
        "create_home": true,
        "failed": false,
        "group": 1006,
        "home": "/home/ansible5",
        "name": "ansible5",
        "shell": "/bin/sh",
        "state": "present",
        "system": false,
        "uid": 1006
    }
}

PLAY RECAP ***********************************************************************************************************
tnode3                     : ok=3    changed=1    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
```

# Ansible Vault
앤서블을 사용할 때 패스워드나 API 키 등 주요 데이터에 대한 엑세스 권한이 필요할 수 있습니다. 이런 정보들은 변수로써 텍스트 파일로 저장되는데, 이는 보안상에 취약한 위험을 야기합니다.

따라서 앤서블은 사용되는 모든 데이터파일을 암호화 하고, 암호화된 파일의 내용을 해독할 수 있는 ```Ansible Vault``` 기능을 제공합니다.
- 데이터를 암호화하고, 암호화된 플레이북을 실행하는 기능

## Ansible Vault 사용법
## 1. 암호화된 파일 만들기
ansible-vault 명령어를 통해 암호화된 파일을 만듭니다.
```bash
# ansible-vault create 로 생성하려는 플레이북 파일 생성
ansible-vault -h
```

mysecret.yml 이라는 암호화파일을 생성합니다.
- 이때 해당 파일의 비밀번호를 같이 입력하게 됩니다.
- password Confirm 이 완료되면 vi 에디터창이 뜨는데, :wq! 로 저장하고 빠져나옵니다.
- 이 에디터창에서 플레이북이나 뭐 암호화할 데이터를 기입해주면 됩니다.
```bash
ansible-vault create mysecret.yml
New Vault password: P@ssw0rd!
Confirm New Vault password: P@ssw0rd!
```

그럼 생성된 파일을 확인하면, 아래와 같이 암호화된 파일이 생성된것을 확인할 수 있습니다.

```yaml
cat mysecret.yml
$ANSIBLE_VAULT;1.1;AES256
32653936643735313135653831626264333863373439396238646637336537363931613430383531
3633393562633834633565613432313437303839363538380a636233306435383239306134363036
33343165313234656363643835646264653131356235366165663361633431333064373637643332
3838616364623661360a623364616265643065353037336436643137643263626562613139353364
3239
```

mysecret.yml 파일을 확인해보면, ansible-vault 명령어를 수행한 유저만이 접근할 수 있는 권한이 부여된것을 알 수 있습니다.
```bash
ll
...
-rw-------  1 root root  355 Jan 13 21:07 mysecret.yml
...
```

아래 명령어로 암호화된 파일을 복호화 합니다.
```bash
# 원래 파일 내용 확인(복호화)
ansible-vault view mysecret.yml
Vault password: P@ssword!
```


## 2. 파일을 이용한 암호화 파일 만들기
별도의 파일에 암호를 저장하고 해당 암호로 암호화된 파일을 생성하는 기능입니다.

먼저 별도 암호가 저장된 파일을 생성합니다.
```bash
echo 'P@ssw0rd!' > vault-pass
```

그 후 , ansible-vault create 로 생성하려는 플레이북 파일을 생성합니다.
- 암호가 저장된 파일을 이용해서 생성했기에, 따로 비밀번호를 물어보지 않고 바로 vi 에디터로 넘어갑니다.
```bash
# ansible-vault create 로 생성하려는 플레이북 파일 생성
ansible-vault create --vault-pass-file ./vault-pass mysecret1.yml
```

생성된 파일을 확인합니다.
- 파일권한, 파일내용, 원래파일내용(복호화)
```bash
# 파일을 생성한 소유자만 읽고 쓸수 있음을 확인
ll mysecret1.yml
-rw------- 1 root root 484 Dec 27 14:56 mysecret1.yml

# 파일 내용 확인 : 암호화되어 있음
cat mysecret.yml1

# 원래 파일 내용 확인(복호화)
ansible-vault view --vault-pass-file ./vault-pass mysecret1.yml

user: ansible
password: P@ssword!
```


## 3. 기존 파일을 암호화 하기
기존에 미리 만들어둔 평문 파일을 암호화할 수 있습니다.
```bash
# 기존 평문 파일 확인
ll create-user.yml
-rw-r--r-- 1 root root 131 Dec 26 14:16 create-user.yml

# 기존 평문 파일 암호화 설정
ansible-vault encrypt create-user.yml
New Vault password: P@ssw0rd!
Confirm New Vault password: P@ssw0rd!

# 암호화 설정 후 확인 : 파일소유자만 읽고 쓸수 있음
ll create-user.yml
-rw------- 1 root root 873 Dec 28 04:18 create-user.yml

cat create-user.yml
```

암호화된 파일을 복호화 하면서 output 옵션으로 특정 파일로 생성할 수 도 있습니다.
```bash
# --output 옵션으로 복호화 파일을 떨굴 파일명을 기입
ansible-vault decrypt create-user.yml --output=create-user-decrypted.yml

# 확인 : 암호화,복호화 파일은 파일 소유자만 읽고 쓸 수 있음
ll create-user*
cat create-user-decrypted.yml

# ubuntu 유저로 파일 확인 시도
## 복호화하여도 다른 유저는 접근불가
su - ubuntu -c 'cat /root/my-ansible/create-user-decrypted.yml'
```

그냥 원복시킬수도 있습니다.
```bash
#
ansible-vault decrypt create-user.yml
cat create-user.yml
```

## 4. 암호화된 비밀번호 변경
ansible-vault로 생성된 암호화 파일에 비밀번호를 변경할 수 있습니다.

```ansible-vault rekey ~``` 명령어로 변경하거나, 패스워드 입력 파일을 이용해 변경합니다.

### 4.1 ```ansible-vault rekey ~``` 사용
암호화된 mysecret.yml 파일의 비밀번호를 변경합니다.
```bash
ansible-vault rekey mysecret.yml
Vault password: 
New Vault password: 
Confirm New Vault password: 
```

### 4.2 패스워드 입력 파일을 이용해 변경
변경할 비밀번호 파일을 만들어두고 그 파일로 변경합니다.
```bash
# 패스워드 입력 파일을 이용해 패스워드 변경
cat vault-pass
P@ssw0rd!

ansible-vault rekey --new-vault-password-file=./vault-pass mysecret.yml
Vault password: NewP@ssw0rd!
Rekey successful
```

## ansible-vault 로 암호화시킨 플레이북을 실행하기
먼저 암호화된 mysecret을 생성합니다.
- 이 파일은 아래 변수를 담고 있습니다.
  - ```users: secretAnsibleUser```
```bash
ansible-vault create mysecret.yml
New Vault password: 123
Confirm New Vault password: 123
```

암호화가 잘 되었는지 확인
```bash
ansible-vault view ./vars/mysecret.yml 
Vault password: 123
users: secretAnsibleUser
```

해당 암호화파일을 사용하는 Playbook을 생성하고 실행해 봅니다.
```bash
cat create-user5.yml
---

- hosts: db
  vars_files:
    - vars/mysecret.yml

  tasks: 
  - name: Create User {{ user }}
    ansible.builtin.user:
      name: "{{ user }}"
      state: present
```

플레이북을 그냥 수행하면 에러 발생합니다.
- 결과로 vault secrets이 없어서 실행을 못한다 말합니다.
```bash
ansible-playbook create-user5.yml
ERROR! Attempting to decrypt but no vault secrets found
```

암호화된 파일로 플레이북을 실행하기 위해선, ```vault-id @prompt``` 옵션을 사용해야 합니다.
- 이때도 비밀번호를 입력하는것으로 실행할 수 있고, 해당 패스워드를 가진파일을 파라미터로 넣어서도 실행할 수 있습니다.
```bash
# (터미널2) 모니터링
watch -d "ssh tnode3 tail -n 3 /etc/passwd"

# 실행
ansible-playbook --vault-id @prompt create-user5.yml
Vault password (default): 123
...

# 패드워드 입력 없이 실행
cat vault-pass
ansible-playbook --vault-password-file=./vault-pass create-user5.yml
```

# Facts
앤서블이 관리 호스트에서 자동으로 검색한 변수(자동 예약 변수)입니다.
- 간단히 말하면 ***관리 호스트에서 수집된 정보*** 입니다.

팩트에는 플레이, 조건문, 반복문 또는 관리 호스트에서 수집한 값에 의존하는 기타 명령문의 일반 변수처럼 사용 가능한 호스트별 정보가 포함되어 있습니다

팩트에는 관리 호스트들의 다음 정보들이 포함될 수 있습니다.
- 호스트 이름
- 커널 버전
- 네트워크 인터페이스 이름
- 운영체제 버전
- CPU 개수
- 사용 가능한 메모리
- 스토리지 장치의 크기 및 여유 공간
- 등등…

***플레이북을 수행하면 자동으로 관리 호스트들의 팩트가 수집됩니다.***

## Facts 사용하기
기본적으로 활성화 되어 있으며, 플레이북을 수행할 때 자동으로 팩트가 수집됩니다.
- ```ansible_facts``` 변수로 사용합니다.

### 1. 파일 생성
fatcts를 debug 모듈로 출력하는 플레이북하나를 생성합니다.
```yaml
cat facts.yml
---

- hosts: db
  tasks:
  - name: Print all facts
    ansible.builtin.debug:
      var: ansible_facts # ansible_facts 변수에 관리 호스트들의 팩트들이 쌓임
```

만든 플레이북을 실행합니다.
- 실행 결과 [db] 그룹 호스트인 tnode3 의 팩트들이 출력됩니다.

```bash
ansible-playbook facts.yml
PLAY [db] **************************************************************************************************************************************************************************************************************************

TASK [Gathering Facts] *************************************************************************************************************************************************************************************************************
ok: [tnode3]

TASK [Print all facts] *************************************************************************************************************************************************************************************************************
ok: [tnode3] => {
    "ansible_facts": {
        "all_ipv4_addresses": [
            "10.10.1.13"
        ],
        "all_ipv6_addresses": [
            "fe80::27:2fff:fe27:d698"
        ],
        "ansible_local": {},
        "apparmor": {
            "status": "enabled"
...
```

이러한 팩트중 필요한것들만 꺼내와서 사용할 수 있습니다.
- ```ansible_facts``` 변수에 쌓이기 때문에, ```.``` 으로 참조해서 사용합니다.
  - ```ansible_facts.hostname``` : 해당 관리 호스트의 hostname
  - ```ansible_facts.default_ipv4.address``` : : 해당 관리 호스트의 ipv4
```yaml
cat facts.yml
---

- hosts: db
  tasks:
  - name: Print all facts
    ansible.builtin.debug: 
      msg: >
        The default IPv4 address of {{ ansible_facts.hostname }}
        is {{ ansible_facts.default_ipv4.address }}
```

플레이북 수행 시 msg로 출력됩니다.
```bash
ansible-playbook facts.yml

PLAY [db] **************************************************************************************************************************************************************************************************************************

TASK [Gathering Facts] *************************************************************************************************************************************************************************************************************
ok: [tnode3]

TASK [Print all facts] *************************************************************************************************************************************************************************************************************
ok: [tnode3] => {
    "msg": "The default IPv4 address of tnode3 is 10.10.1.13"
}

PLAY RECAP *************************************************************************************************************************************************************************************************************************
tnode3                     : ok=2    changed=0    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0  
```

이러한 팩트를 통해 조건문 등의 비교값으로 사용할 수 있습니다.
- 레드헷 리눅스면 뭘하고,, 우분투면 뭘하고,, 로키면 뭘하고,, 이렇게 다르게 줄 수 있음


## Facts 수집 끄기
팩트 수집을 할 필요 없는 테스크들일 경우 수집을 안하게끔 설정할 수 있습니다.
- 수집 안하게끔하여 호스트 부하를 줄임

또한 팩트를 수집한다는것은 해당 호스트에 특정 패키지를 설치해야만 하는 경우도 있기에 수집이 불가한경우도 간혹 있습니다.

이럴때 Facts의 수집을 비활성화 하여 최적화합니다.

### 1. 팩트 수집 시 호스트 프로세스 확인
아래 명령어를 통해 테스트해봅니다.
```bash
# 플레이북 생성
cat <<EOF > facts.yml
---

- hosts: db
  tasks:
  - name: Print all facts
    ansible.builtin.debug: 
      msg: >
        The default IPv4 address of {{ ansible_facts.hostname }}
        is {{ ansible_facts.default_ipv4.address }}
EOF

# (터미널2) tnode3에 SSH 접속 후 아래 모니터링
ssh tnode3
watch -d -n 1 pstree

# [ansible-server]에서 아래 플레이북 실행
## 아무 플레이북이던 상관없음
ansible-playbook facts.yml
ansible-playbook facts.yml
ansible-playbook facts.yml
```

테스트결과 아주 중요한 것을 확인할 수 있는데, **플레이북이 실행될 때 마다 python3 프로세스가 도는것을 확인할 수 있습니다.**

***ansible은 결국 제어 노드가 관리 노드에게 sshd 프로세스를 수행하여 특정 파이썬 모듈을 실행하고 실행결과를 갖고온다는것***을 알 수 있습니다.

그래서 , Ping 모듈을 사용한다 해서 실제 ICMP 가 아니라, 파이썬 모듈이라는것을 알 수 있습니다.
- 모듈을 직접개발해서 사용할수도 있을듯 ?


### 2. 팩트 수집 안하기
팩트를 수집하지 않으려면, 플레이북에서 설정을 추가하면 됩니다.

간단히 ```gather_facts: no``` 설정을 hosts 아래에 추가해주면 됩니다.

```yaml
cat facts.yml
---

- hosts: db
  gather_facts: no # facts 수집 disable

  tasks:
  - name: Print all facts
    ansible.builtin.debug:
      msg: >
        The default IPv4 address of {{ ansible_facts.hostname }}
        is {{ ansible_facts.default_ipv4.address }}
```

해당 플레이북을 수행하면 에러 납니다. 그 이유는 facts 수집을 꺼놧는데 아래에 수집하고있으니 에러납니다.

따라서 facts 를 사용하지 않는 플레이북을 생성합니다.

```yaml
---

- hosts: db
  gather_facts: no # facts 수집 disable

  tasks:
  - name: Print Message
    ansible.builtin.debug:
      msg: >
        Hello ansible !!!
```

플레이북 수행 시 에러없이 잘 동작합니다.

```bash
ansible-playbook facts-1.yml

PLAY [db] **************************************************************************************************

TASK [Print Message] ***************************************************************************************
ok: [tnode3] => {
    "msg": "Hello ansible !!!"
}

PLAY RECAP *************************************************************************************************
tnode3                     : ok=1    changed=0    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
```


### 3. 메뉴얼하게 팩트 수집 
전체 호스트에서는 팩트수집을 disable 시키고, 특정 task에서만 facts를 수집할 수 도 있습니다.

```ansible.builtin.setup``` 모듈로 특정 task에서 수동으로 수집합니다.
```yaml
---

- hosts: db
  gather_facts: no # facts 수집 disable

  tasks:
  - name: Manually gather facts
    ansible.builtin.setup: # 해당 task에서만 수동 수집
  
  - name: Print all facts
    ansible.builtin.debug:
      msg: >
        The default IPv4 address of {{ ansible_facts.hostname }}
        is {{ ansible_facts.default_ipv4.address }}
```


### 3. 사용자 지정 팩트 만들기
사용자 지정 팩트는 Ansible 팩트가 제공하지 않는 특정 환경이나 애플리케이션에 특화된 정보를 수집할 때 사용합니다.

아래와같은 상황에서 유용할 수 있습니다.
1. 특정 애플리케이션 데이터 수집: 시스템에 설치된 특정 소프트웨어의 버전, 구성 정보 등을 수집하는 데 사용할 수 있습니다.
2. 환경 특화 정보 수집: 특정 네트워크 구성, 데이터베이스 설정, 클라우드 환경의 메타데이터 등과 같이 표준 팩트에서 제공하지 않는 정보를 수집하는 데 사용할 수 있습니다.
3. 커스텀 검증 및 조건부 실행: 사용자 정의 팩트를 통해 특정 조건이 충족되었는지를 확인하고, 이를 바탕으로 플레이북의 흐름을 제어할 수 있습니다.
4. 인벤토리 확장: 시스템의 유형, 역할, 환경 설정 등과 같이 사용자가 정의한 특정 데이터를 기반으로 인벤토리를 동적으로 조정하거나 확장할 수 있습니다.
- ByChatGPT

사용자 지정 팩트는 관리 호스트의 로컬에 있는 ```/etc/ansible/facts.d``` 디렉터리 내에 ```‘*.fact’```로 저장되어야만 앤서블이 플레이북을 실행할 때 자동으로 팩트를 수집할 수 있습니다.

따라서 이러한 사용자 정의 팩트를 관리 호스트들에게 생성해두고 플레이북에서 호출하면, 내가 원하는 값들을 갖고와서 출력하거나 조건문등에 활용할 수 있습니다.
- ex) 웹서버 호스트에서는 web-server 라벨 출력, db 호스트에서는 db 라벨 출력
  이를 이용하여 web-server일경우 재시작수행 , db 일경우 제거 등 내입맛에 맞는 상황에맞게끔 플레이북을 생성할 수 있음

#### 3.1 디렉터리 생성 후 custom-facts 파일 생성
먼저 사용자 지정 팩트 디렉터리를 생성하고, 사용자지정 팩트를 생성합니다.

```bash
mkdir /etc/ansible/facts.d

# my-custom.fact 파일 생성
## 라벨링 작업,
cat <<EOT > /etc/ansible/facts.d/my-custom.fact
[packages]
web_package = httpd
db_package = mariadb-server

[users]
user1 = ansible
user2 = gasida
EOT

cat /etc/ansible/facts.d/my-custom.fact
```

#### 3.2 플레이북 작업
플레이북 파일을 생성합니다. 
- host를 localhost로 설정

```yaml
cat facts5.yml
---

- hosts: localhost

  tasks:
  - name: Print all facts
    ansible.builtin.debug:
      var: ansible_local
```

플레이북을 수행해 봅니다.
- 사용자 지정 팩트가 출력되는것을 확인할 수 있습니다.

```bash
ansible-playbook facts5.yml 
PLAY [localhost] *******************************************************************************************************************************************************************************************************************

TASK [Gathering Facts] *************************************************************************************************************************************************************************************************************
ok: [localhost]

TASK [Print all facts] *************************************************************************************************************************************************************************************************************
ok: [localhost] => {
    "ansible_local": {
        "my-custom": {
            "packages": {
                "db_package": "mariadb-server",
                "web_package": "httpd"
            },
            "users": {
                "user1": "ansible",
                "user2": "gasida"
            }
        }
    }
}

PLAY RECAP *************************************************************************************************************************************************************************************************************************
localhost                  : ok=2    changed=0    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0  
```