---
title: Ansible Galaxy
subtitle: Ansible Galaxy 이론 및 사용방안
tags: devops, opensource, ansible
domain: jjsair0412.hashnode.dev
---

# Ansible Galaxy
Ansible-Galaxy는 사용자가 생성한 롤들을 공유하고 공유된 롤들을 가지고 와 롤을 개발합니다.

Ansible-Galaxy 공식 홈페이지
- https://galaxy.ansible.com/ui/

공식 홈페이지에서 Role 페이지로 이동한 이후 필요한 Role을 검색하여 찾을 수 있습니다.
- 헬름차트와 코드가 합쳐진느낌

***Ansible Galaxy에 올라가있는 Role 들은 과거 표기법으로 작성된부분들이 많음. 따라서 구 표기법이 가능하게끔 설정할 필요가 있을 수 도 있음***

## Commands
다양한 명령어를 통해 Galaxy에 올라가있는 롤을 검색하고 받아올 수 있습니다.

### 1. 롤 검색
```bash
# --platforms 옵션으로 role을 설치할 대상 서버의 운영체제를 검색
$ ansible-galaxy role search {role-name} --platforms {OS-Name}

# usecase
$ ansible-galaxy role search postgresql --platforms Ubuntu

Found 271 roles matching your search:

 Name                                                 Description
 ----                                                 -----------
 aaronpederson.postgresql                             PostgreSQL is a powerful, open source object-relational database system. It has more than 15 years of active development and a proven architecture that has earned it a stron>
 alainvanhoof.alpine_postgresql                       PostgreSQL for Alpine Linux
 alikins.postgresql                                   PostgreSQL server for Linux.
 AlphaHydrae.postgresql-dev                           Installs PostgreSQL for development.
 ...
```

### 2. 롤 상세 정보 확인
```bash
$ ansible-galaxy role info {role_name}

# usecase
$ ansible-galaxy role info geerlingguy.postgresql
```

### 3. 롤 가져오기
```bash
# -p 옵션으로 롤이 설치될 디렉터리 경로 지정
$ ansible-galaxy role install -p roles {role_name}

# usecase
$ ansible-galaxy role install -p roles geerlingguy.postgresql

# 가져온 role 확인
$ ansible-galaxy role list -p roles
# /home/ubuntu/my-ansible/roles
- geerlingguy.postgresql, 3.5.0
# /etc/ansible/roles
[WARNING]: - the configured path /home/ubuntu/.ansible/roles does not exist.
[WARNING]: - the configured path /usr/share/ansible/roles does not exist.
```

### 4. 롤을 이용하여 설치하기
```bash
cat <<EOF> role-galaxy.yml
---
- hosts: tnode1
  roles: # role 이름 설정
    - geerlingguy.postgresql
```

설치 수행
```bash
$ ansible-playbook role-galaxy.yml 
```

가져온 롤 삭제
```bash
$ ansible-galaxy role remove geerlingguy.postgresql

$ ansible-galaxy role list

$ rm -r roles
```
