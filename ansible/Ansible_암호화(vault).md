
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

## 복호화 비밀번호를 외부에 저장하기 - AWS Secret Manager에 저장하기
### 이론
암호화시킬 파일은 암호화되었기 때문에 외부에서 확인할 수 없지만, 막상 암호화된 파일을 복호화 할 수 있는 비밀번호를 가진 파일을 관리하는것 또한 필요하게 됩니다.

이를 위해 복호화 비밀번호를 담은 파일은 외부 (AWS Secret Manager) 에 저장해두고, 사용할때마다 Secret Manager 서비스에 접근하여 복호화하는 쉘 스크립트로 암호화파일을 사용하는 방안도 있습니다.

### 실습
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

