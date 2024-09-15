# 도전과제
## 1. vault에 AWS SecretManager를 활용해보세요
암호화할 변수 파일을 ansible-vault를 통해 생성합니다.

```bash
# vault 암호는 편하게 입력
ansible-vault create vars/secret.yml
New Vault password: qwe123
Confirm New Vault password: qwe123

## 에디터 창으로 전환 : (아래 내용 복붙) user_info 변수에 userid와 userpw가 같이 있는 사전형 변수를 정의
---

user_info:
  - userid: "ansible"
    userpw: "ansiblePw1"
  - userid: "stack"
    userpw: "stackPw1"
```


해당 변수파일을 복호화 할 수 있는 비밀번호를 가진 파일을 생성한 뒤, AWS Secret Manager에 등록해둡니다.
- ansible-vault로 암호화된 파일을 복호화할 비밀번호를 가진 파일을 생성함
- 차후 playbook을 수행할 때, Secret Manager에 등록된 파일을 가지고오는 쉘 스크립트로 비밀번호를 가져옴

```bash
$ cat <<EOT> vars/pwd.yml
qwe123
EOT

$ chmod go-rx vars/pwd.yml

$ ls -l vars
total 8
-rw--w---- 1 ubuntu ubuntu   7 Jan 31 01:04 pwd.yml
-rw------- 1 ubuntu ubuntu 743 Jan 31 01:02 secret.yml
```

이후 AWS Secret Manager로 암호를 생성하고 확인합니다.


```bash
# AWS SecretManager로 암호 생성 및 확인
$ aws secretsmanager create-secret --name pwd --secret-string file://vars/pwd.yml --region ap-northeast-2


# 정상 생성 여부 확인
$ aws secretsmanager get-secret-value --secret-id pwd --region ap-northeast-2 | jq
```

생성한 암호 파일은 제거합니다.

```bash
$ shred -u vars/pwd.yml  

$ ls -l vars/
total 4
-rw------- 1 ubuntu ubuntu 743 Jan 31 01:02 secret.yml
```

Secret Manager에 등록된 비밀번호를 갖고올 쉘 스크립트를 작성합니다.


```bash
cat <<EOF> getPwd.sh
#!/bin/bash
aws secretsmanager get-secret-value \
  --secret-id pwd \
  --region ap-northeast-2 \
  | jq -r .SecretString
EOF

$ chmod +x getPwd.sh
```

playbook을 작성합니다.

```yaml
cat <<EOF> create_user.yml
---
- hosts: all

  vars_files: 
    - vars/secret.yml

  tasks:
  - name: Create User
    ansible.builtin.user:
      name: "{{ item.userid }}"
      password: "{{ item.userpw }}"
    loop: "{{ user_info }}"
EOF
```

playbook을 수행할 때, 생성한 쉘 스크립트를 통해 vault로 암호화된 파일을 복호화 합니다.

```bash
$ ansible-playbook create_user.yml --vault-password-file ./getPwd.sh
PLAY [all] *************************************************************************************************************************************************************************************************************************

TASK [Gathering Facts] *************************************************************************************************************************************************************************************************************
ok: [tnode2]
ok: [tnode3]
ok: [tnode1]

TASK [Create User] *****************************************************************************************************************************************************************************************************************
ok: [tnode2] => (item={'userid': 'ansible', 'userpw': 'ansiblePw1'})
ok: [tnode1] => (item={'userid': 'ansible', 'userpw': 'ansiblePw1'})
ok: [tnode3] => (item={'userid': 'ansible', 'userpw': 'ansiblePw1'})
ok: [tnode1] => (item={'userid': 'stack', 'userpw': 'stackPw1'})
[WARNING]: The input password appears not to have been hashed. The 'password' argument must be encrypted for this module to work properly.
ok: [tnode2] => (item={'userid': 'stack', 'userpw': 'stackPw1'})
ok: [tnode3] => (item={'userid': 'stack', 'userpw': 'stackPw1'})

PLAY RECAP *************************************************************************************************************************************************************************************************************************
tnode1                     : ok=2    changed=0    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
tnode2                     : ok=2    changed=0    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
tnode3                     : ok=2    changed=0    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
```


## 2. Lookups 플러그인을 활용한 playbook를 직접 작성해서 실습해보세요.
lookup plugin은 ansible playbook의 외부 소스(files, databases, key/value stores, APIs, and other services)의 데이터에 액세스할 수 있습니다.
ookup plugin을 사용하여 구글에 변수들을 검색하는 playbook을 작성하였습니다.

```yaml
---
- hosts: localhost
  vars:
    searchVars:
      - test
      - hello_world

  tasks:
  - name: url lookup splits lines by default
    ansible.builtin.debug:
      msg: "{{ lookup('url', 'https://www.google.com/search?q={{ item }}', wantlist=True) }}"
    loop: "{{ searchVars }}"
```


## 3. Ubuntu 와 CentOS에 apache http를 설치하는 playbook을 작성해서 실습해보세요 (롤/템플릿 사용은 편한대로)
role을 생성해서 apache http의 index.html을 jinja2 변수를 확장하여 생성합니다.
- apache http는 ```tnode3``` 번에 설치할 예정입니다.

먼저 ```ansible.cfg``` 파일과 , ```inventory``` 를 생성합니다.

- ansible.cfg 파일 생성
```bash
cat <<EOF> ansible.cfg
[defaults]
inventory = ./inventory
remote_user = ubuntu
ask_pass = false
inject_facts_as_vars = false
roles_path = ./roles

[privilege_escalation]
become = true
become_method = sudo
become_user = root
become_ask_pass = false
EOF
```

- inventory 생성
```bash
cat <<EOF> inventory
[tnode]
tnode1
tnode2
tnode3
EOF
```

role을 생성합니다.
```bash
ansible-galaxy role init apacheRole.jinseong
```

아래 변수 대상으로 변수를 생성합니다.
- OS List
- 설치 package 이름
```bash
cat <<EOF> apacheRole.jinseong/vars/main.yml
---
# vars file for apacheRole.jinseong

package_name: apache2
os_list:
  - RedHat
  - CentOS
EOF
```

JinJa2 Template으로 Index.html 파일을 생성합니다.
```bash
cat <<EOF> apacheRole.jinseong/templates/index.html.j2
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>환영합니다 - Apache 웹 서버</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 40px;
        }
        h1 {
            color: #006400;
        }
        p {
            color: #333;
        }
    </style>
</head>
<body>
    <h1>Ansible로 만들어진 웹 서버에 오신 것을 환영합니다!</h1>
    <p>이 페이지는 Ansible로 만들어진 웹 페이지입니다.</p>
</body>
</html>
EOF
```

핸들러를 작성합니다.
- apache 서비스를 재 시작하는 테스크가 포함됩니다.
```yaml
cat <<EOF> apacheRole.jinseong/handlers/main.yml
---
# handlers file for apacheRole.jinseong

- name: Restart apache2
  ansible.builtin.service:
    name: "{{ service_name }}"
    state: restarted
EOF
```

각 운영체제별 Task를 따로따로 만들어 줍니다.
- Ubuntu
```yaml
cat <<EOF> apacheRole.jinseong/tasks/Ubuntu.yml
---

- name: Install apache2 using apt
  ansible.builtin.apt:
    name: "{{ package_name }}"
    state: latest
EOF
```

- CentOS
```yaml
cat <<EOF> apacheRole.jinseong/tasks/CentOS.yml
---

- name: Install apache using dnf
  ansible.builtin.dnf:
    name: "{{ package_name }}"
    state: latest
EOF
```

- RedHat
```yaml
cat <<EOF> apacheRole.jinseong/tasks/RedHat.yml
---

- name: Install apache using dnf
  ansible.builtin.dnf:
    name: "{{ package_name }}"
    state: latest
EOF
```

메인 테스크를 작성합니다.
- apache2 설치, index.html 파일 변경, apache2 서비스 재 시작 순으로 테스크가 진행됩니다.
- facts들 중 os type에 따라 설치 명령어가 다르기 때문에, Type을 체크해서 다른 Task파일을 가져오도록 작성합니다.

```yaml
cat <<EOF> apacheRole.jinseong/tasks/main.yml
---
# tasks file for apacheRole.jinseong

- name: Import Current Playbook
  ansible.builtin.include_tasks:
    file: "{{ ansible_facts.distribution }}.yml"
  
- name: Copy Index.html when Ubuntu
  ansible.builtin.template:
    src: index.html.j2
    dest: /var/www/html/index.html
  notify: "Restart apache2"
  when: ansible_facts.distribution == "Ubuntu"

- name: Copy Index.html when Other OS
  ansible.builtin.template:
    src: index.html.j2
    dest: /var/www/html/index.html
  notify: "Restart apache2"
  when: ansible_facts.distribution in os_list
EOF
```

메인 플레이북인 ```install_apache.yml``` 을 생성해서 지금까지 만들어준 role을 수행합니다.

```yaml
cat <<EOF> install_apache.yml
---
- hosts: tnode3
  roles:
    - role: apacheRole.jinseong
EOF
```

플레이북을 수행합니다.

```bash
ansible-playbook install_apache.yml
```

결과를 확인합니다.
- apache2가 정상적으로 설치되었는지 확인합니다.

```bash
# apache2 설치확인
$ ssh tnode3 systemctl status apache2
● apache2.service - The Apache HTTP Server
     Loaded: loaded (/lib/systemd/system/apache2.service; enabled; vendor preset: enabled)
     Active: active (running) since Sat 2024-02-03 00:11:32 KST; 33s ago
       Docs: https://httpd.apache.org/docs/2.4/
    Process: 6642 ExecStart=/usr/sbin/apachectl start (code=exited, status=0/SUCCESS)
   Main PID: 6647 (apache2)
      Tasks: 55 (limit: 4598)
     Memory: 5.0M
        CPU: 41ms
     CGroup: /system.slice/apache2.service
             ├─6647 /usr/sbin/apache2 -k start
             ├─6648 /usr/sbin/apache2 -k start
             └─6649 /usr/sbin/apache2 -k start

Feb 03 00:11:32 tnode3 systemd[1]: Starting The Apache HTTP Server...
Feb 03 00:11:32 tnode3 apachectl[6646]: AH00558: apache2: Could not reliably determine the server's fully qualified domain name, using 10.10.1.13. Set the 'ServerName' directive globally to suppress this message
Feb 03 00:11:32 tnode3 systemd[1]: Started The Apache HTTP Server.
```

- index.html이 정상적으로 변경되었는지 확인합니다.
```bash
# index.html 변경 확인
$ curl tnode3
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>환영합니다 - Apache 웹 서버</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 40px;
        }
        h1 {
            color: #006400;
        }
        p {
            color: #333;
        }
    </style>
</head>
<body>
    <h1>Ansible로 만들어진 웹 서버에 오신 것을 환영합니다!</h1>
    <p>이 페이지는 Ansible로 만들어진 웹 페이지입니다.</p>
</body>
</html>
```

## 4. Jinja2 템플릿을 활용한 예시 playbook를 구글링하여 실습 환경에 맞게 구성 후 실습해보세요
앤서블에서는 변수의 확장을 위해서 파이썬에서 템플릿을 위해 정의된 엔진인 jinja2를 사용합니다.

이를 이용해서 앤서블 내부에서 코드처럼 로직을 작성하거나, ```.cfg``` 파일들과 같은 패키지별 설정 파일들을 동적으로 바꿀 때 많이 사용됩니다.
- 예를들어 [apache2 설치](#3-ubuntu-와-centos에-apache-http를-설치하는-playbook을-작성해서-실습해보세요-롤템플릿-사용은-편한대로) 예제에선 jinja2 template을 사용하여 index.html파일을 앤서블에서 미리 정의해두고 apache2를 설치하였습니다.

### 4.1 활용 방법
기본적으로 jinja2는 반드시 ```{{  }}``` 로 묶이며, 변수를 사용할때는 ```" "``` 으로 묶여야 합니다.
- 예를들어 변수값을 앤서블에서 가져올때 ```"{{  }}"``` 이렇게 작성합니다.

```{%...%}```
  - 제어가 들어가는 라인 -> with_Item, when, if
```{{ ... }}``` 
  - 표현식(문)이 들어가는 구문 -> {{ 변수 }}
```{#...#}``` 
  - 주석 표시

### 4.2 예제 수행
jinja2 template을 가지고 수집된 facts들을 통해 nginx 서비스의 설치 방법을 출력하는 playbook을 작성해 보겠습니다.

먼저 inventory, ansible.cfg 파일을 작성합니다.
```bash
$ cat <<EOF> ansible.cfg
[defaults]
inventory = ./inventory
remote_user = ubuntu
ask_pass = false
inject_facts_as_vars = false
roles_path = ./roles

[privilege_escalation]
become = true
become_method = sudo
become_user = root
become_ask_pass = false
EOF

$ cat <<EOF> inventory
[tnode]
tnode1
tnode2
tnode3
EOF
```

그리고 jinja2 template파일을 생성합니다.
- ```{%...%}``` 구문으로 else - if문을 작성합니다. 구문 내부에는 메시지가 들어갑니다.
- facts의 distribution으로 if문을 수행합니다.

```
cat <<EOF> msg.j2
{% if ansible_facts.distribution == 'Ubuntu' %}
   [ OS : Ubuntu ]
    >> dpkg -l | grep nginx
    OR
    >> service nginx status
{% elif ansible_facts.distribution == 'CentOS' and ansible_facts.distribution_version == '7' %}
   [ OS : CentOS ver7 ]
    >> yum list installed | grep nginx
    OR
    >> systemctl status nginx
{% elif ansible_facts.distribution == 'CentOS' and ansible_facts.distribution_version < '7' %}
   [ OS : CentOS ver6 ]
    >> yum list installed | grep nginx
    OR
    >> service nginx status
{% else %}
    >> service nginx status (* Gernally)
{% endif %}
EOF
```

playbook을 작성합니다.
- lookup을 통해 동적으로 변경된 jinja2 template msg.j2 파일을 단순히 출력합니다.
```yaml
cat <<EOF> jinja2-template.yml
---
- hosts: tnode

  tasks:
    - name: How to check the status of nginx for each of OS.
      debug: msg="{{lookup('template','msg.j2').split('\n')}}"
```

playbook을 수행해보면, facts 값에 따라서 동적으로 jinja2 코드가 수행되어 메시지가 출력되는것을 확인할 수 있습니다.
```bash
$ ansible-playbook jinja2_template.yml 

PLAY [tnode] ***********************************************************************************************************************************************************************************************************************

TASK [Gathering Facts] *************************************************************************************************************************************************************************************************************
ok: [tnode2]
ok: [tnode1]
ok: [tnode3]

TASK [How to check the status of nginx for each of OS.] ****************************************************************************************************************************************************************************
ok: [tnode1] => {
    "msg": [
        "   [ OS : Ubuntu ]",
        "    >> dpkg -l | grep nginx",
        "    OR",
        "    >> service nginx status",
        ""
    ]
}
ok: [tnode2] => {
    "msg": [
        "   [ OS : Ubuntu ]",
        "    >> dpkg -l | grep nginx",
        "    OR",
        "    >> service nginx status",
        ""
    ]
}
ok: [tnode3] => {
    "msg": [
        "   [ OS : Ubuntu ]",
        "    >> dpkg -l | grep nginx",
        "    OR",
        "    >> service nginx status",
        ""
    ]
}

PLAY RECAP *************************************************************************************************************************************************************************************************************************
tnode1                     : ok=2    changed=0    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
tnode2                     : ok=2    changed=0    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
tnode3                     : ok=2    changed=0    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
```

## 5. file과 lineinfile 모듈을 활용하여 /tmp/test.txt 파일을 생성하고 hello 문자열을 추가하는 playbook 작성해보세요
먼저 inventory, ansible.cfg 파일을 작성합니다.
```bash
$ cat <<EOF> ansible.cfg
[defaults]
inventory = ./inventory
remote_user = ubuntu
ask_pass = false
inject_facts_as_vars = false
roles_path = ./roles

[privilege_escalation]
become = true
become_method = sudo
become_user = root
become_ask_pass = false
EOF

$ cat <<EOF> inventory
[tnode]
tnode1
tnode2
tnode3
EOF
```

test.txt 파일을 생성하고, file명을 바꾸는 두가지 task를 가진 playbook을 생성합니다.
```bash
$ cat <<EOF> inline_test.yml
---
- hosts: tnode1
  tasks:
  - name: Create test.txt file
    ansible.builtin.file:
      path: "/tmp/test.txt"
      state: touch
      mode: "0755"

  - name: add hello text
    ansible.builtin.lineinfile:
      path: "/tmp/test.txt"
      line: "hello"
      regexp: "^hello"
      state: present
      create: true
EOF
```

playbook을 수행합니다.
```bash
ansible-playbook inline_test.yml 

PLAY [tnode1] **********************************************************************************************************************************************************************************************************************

TASK [Gathering Facts] *************************************************************************************************************************************************************************************************************
ok: [tnode1]

TASK [Create test.txt file] ********************************************************************************************************************************************************************************************************
changed: [tnode1]

TASK [add hello text] **************************************************************************************************************************************************************************************************************
changed: [tnode1]

PLAY RECAP *************************************************************************************************************************************************************************************************************************
tnode1                     : ok=3    changed=2    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
```

tnode1번에 ssh 연결하여 결과를 확인합니다.

```bash
$ ssh tnode1 cat /tmp/test.txt
hello
```

## 6. 앤서블 갤럭시에서 PostgreSQL 설치하는 롤을 검색하여, 해당 롤을 통해 tnode3에 설치해보세요
Ansible-Galaxy의 다운로드 수가 가장 높은 [geerlingguy.postgresql Role](https://galaxy.ansible.com/ui/standalone/roles/geerlingguy/postgresql/) 로 설치할 예정입니다.

먼저 inventory, ansible.cfg 파일을 작성합니다.
```bash
$ cat <<EOF> ansible.cfg
[defaults]
inventory = ./inventory
remote_user = ubuntu
ask_pass = false
inject_facts_as_vars = false
roles_path = ./roles

[privilege_escalation]
become = true
become_method = sudo
become_user = root
become_ask_pass = false
EOF

$ cat <<EOF> inventory
[tnode]
tnode1
tnode2
tnode3
EOF
```

role을 설치합니다.
- ```-p``` 옵션으로 role 설치위치를 지정합니다.

```bash
ansible-galaxy role install -p ./roles geerlingguy.postgresql
```

role이 정상적으로 설치되었는지 확인합니다.

```bash
ansible-galaxy role list
tree roles -L 3
```

main.yml 파일을 확인해봅니다.
- 오래된 Facts 표기법을 사용하기에 ansible.cfg 를 조절해야 합니다. 
- ```inject_facts_as_vars = True``` 설정을 추가하거나, 제거합니다. 기본적으로 True
- ```inject_facts_as_vars = True``` 설정을 false로 두면, 과거의 변수주입식 표기법을 사용하지 않고 ```ansible_facts['eth0']``` 과 같은 현대적표기법을 사용할 수 있습니다.

```bash
$ cat roles/geerlingguy.postgresql/tasks/main.yml 
---
# Variable configuration.
- include_tasks: variables.yml

# Setup/install tasks.
- include_tasks: setup-Archlinux.yml
  when: ansible_os_family == 'Archlinux'

- include_tasks: setup-Debian.yml
  when: ansible_os_family == 'Debian'

- include_tasks: setup-RedHat.yml
  when: ansible_os_family == 'RedHat'

- include_tasks: initialize.yml
- include_tasks: configure.yml

- name: Ensure PostgreSQL is started and enabled on boot.
  service:
    name: "{{ postgresql_daemon }}"
    state: "{{ postgresql_service_state }}"
    enabled: "{{ postgresql_service_enabled }}"

# Configure PostgreSQL.
- import_tasks: users.yml
- import_tasks: databases.yml
- import_tasks: users_props.yml
```

role 외부 최상단폴더에 playbook을 작성합니다.
- 사용할 role의 이름은, 설치한 role의 이름을 작성해줍니다.

```bash
cat <<EOF> pg_book.yml
---
- hosts: tnode3
  roles:
    - role: geerlingguy.postgresql
EOF
```

폴더구조는 최종적으로 다음과 같습니다.

```bash
$ tree -L 3
.
├── ansible.cfg
├── inventory
├── pg_book.yml
└── roles
    └── geerlingguy.postgresql
        ├── LICENSE
        ├── README.md
        ├── defaults
        ├── handlers
        ├── meta
        ├── molecule
        ├── tasks
        ├── templates
        └── vars

```

playbook을 수행하여 PostgreSQL을 tnode3에 설치합니다.

```bash
ansible-playbook pg_book.yml
```

설치결과를 확인해 봅니다.
- 데몬 실행상태 확인

```bash
$ ssh tnode3 systemctl status postgresql
● postgresql.service - PostgreSQL RDBMS
     Loaded: loaded (/lib/systemd/system/postgresql.service; enabled; vendor preset: enabled)
     Active: active (exited) since Sat 2024-02-03 00:55:48 KST; 9s ago
    Process: 9907 ExecStart=/bin/true (code=exited, status=0/SUCCESS)
   Main PID: 9907 (code=exited, status=0/SUCCESS)
        CPU: 1ms

Feb 03 00:55:48 tnode3 systemd[1]: Starting PostgreSQL RDBMS...
Feb 03 00:55:48 tnode3 systemd[1]: Finished PostgreSQL RDBMS.
```

- PG 데이터베이스 목록 확인

```bash
$ sudo -u postgres psql -c "\l"
could not change directory to "/home/ubuntu": Permission denied
                              List of databases
   Name    |  Owner   | Encoding | Collate |  Ctype  |   Access privileges   
-----------+----------+----------+---------+---------+-----------------------
 postgres  | postgres | UTF8     | C.UTF-8 | C.UTF-8 | 
 template0 | postgres | UTF8     | C.UTF-8 | C.UTF-8 | =c/postgres          +
           |          |          |         |         | postgres=CTc/postgres
 template1 | postgres | UTF8     | C.UTF-8 | C.UTF-8 | =c/postgres          +
           |          |          |         |         | postgres=CTc/postgres
(3 rows)
```