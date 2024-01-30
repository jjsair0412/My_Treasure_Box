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

lookup plugin을 사용하여 구글에 변수들을 검색하는 playbook을 작성하였습니다.

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