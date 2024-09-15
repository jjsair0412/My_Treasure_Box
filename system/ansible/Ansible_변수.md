---
title: Ansible 변수
subtitle: Ansible 에서 변수 사용방안
tags: devops, opensource, ansible
domain: jjsair0412.hashnode.dev
---

# Ansible 사용 방안 - 변수

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
