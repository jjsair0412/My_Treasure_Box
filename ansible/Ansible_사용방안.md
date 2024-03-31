---
title: Ansible 기본 사용방안
subtitle: Ansible 기본 사용 방안
tags: devops, opensource, ansible
domain: jjsair0412.hashnode.dev
---


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