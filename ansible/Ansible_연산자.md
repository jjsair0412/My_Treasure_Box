# 반복문
앤서블 플레이북에서 반복문을 사용함으로써 동일한 모듈을 사용하는 작업을 여러변 작성하지 않아도 됩니다.

예를들어 포트를 방화벽에 추가한다면, ```loop``` 반복문을 이용해서 작업 하나로 여러개 포트를 추가할 수 있습니다.

## 단순 반복문
특정 항목에 대한 작업을 반복합니다. ```loop``` 키워드를 작업에 추가하면 작업을 반복해야 하는 항목의 목록을 값으로 사용합니다.

### 1. 반복문 사용하지 않을경우
반복문을 사용하지 않을 경우 플레이북이 아래와 같습니다.

두 task(sshd, rsyslog) 가 반복되는것을 확인할 수 있습니다.
```yaml
---
- hosts: all
  tasks:
    - name: Check sshd state
      ansible.builtin.service:
        name: sshd
        state: started

    - name: Check rsyslog state
      ansible.builtin.service:
        name: rsyslog
        state: started
```

### 2. 반복문 사용할 경우
반복되는 작업을 ```loop``` 키워드 안에 넣고 반복합니다. ```loop``` 키워드 아래에는 체크할 서비스인 ```sshd```와 ```rsyslog``` 를 나열합니다.


이떄 ```loop``` 키워드 내의 항목들이 ```"{{ item }}"```  변수안에 들어가게 됩니다.
```yaml
---
- hosts: all
  tasks:
  - name: Check sshd and rsyslog state
    ansible.builtin.service:
      name: "{{ item }}"
      state: started
    loop:
      - sshd
      - rsyslog
```

플레이북을 수행해 봅니다.
- 수행 결과 item 변수에 나열해둔 sshd, rsyslog 값이 순서대로 들어가는것을 확인할 수 있습니다.
```bash
ansible-playbook check-services1.yml 

PLAY [all] *************************************************************************************************************************************************************************************************************************

TASK [Check sshd and rsyslog state] ************************************************************************************************************************************************************************************************
ok: [tnode3] => (item=sshd)
ok: [tnode2] => (item=sshd)
ok: [tnode1] => (item=sshd)
ok: [tnode3] => (item=rsyslog)
ok: [tnode2] => (item=rsyslog)
ok: [tnode1] => (item=rsyslog)

PLAY RECAP *************************************************************************************************************************************************************************************************************************
tnode1                     : ok=1    changed=0    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
tnode2                     : ok=1    changed=0    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
tnode3                     : ok=1    changed=0    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0 
```

변수를 조금 응용해서 ```loop``` 키워드 내부의 값들을 따로 변수할당을 해놓고 ```loop``` 에선 변수값들을 할당받을 수 도 있습니다.
- 별도파일로 빼서도 사용이 가능
```yaml
---
- hosts: all
  vars:
    services:
      - sshd
      - rsyslog
  
  tasks:
    - name: Check sshd and rsyslog state
      ansible.builtin.service:
        name: "{{ item }}"
        state: started
      loop: "{{ services }}"
```
## 사전 목록에 의한 반복문
플레이북 작성 시, 여러개의 아이템이 묶여서 동시에 수행되어야하는 경우가 있습니다.

예를들어 test1 파일을 생성하는데 파일 모드를 0644 으로 해라. 와 test2 파일을 생성하는데 파일 모드를 0600 으로 해라.  라는 여러개의 반복구문이 있을 수 있습니다.
이는 정확히 파일 생성-파일모드 변경 , 파일생성-파일모드변경 4가지 작업이 이루어져야 하지만 2개씩 묶여서 반복되어야 합니다.
- 한번의 반복 안에서 여러 연산이 일어나야함

이럴 때 사전 목록에 의한 반복문을 사용합니다.

```loop``` 키워드 내부에 ```log-path``` 와 ```log-mode``` 변수를 한 묶음씩 선언합니다. 그리고 ```"{{ item['log-path'] }}"``` , ```"{{ item['log-mode'] }}"``` 이렇게 task에서 변수를 참조합니다.
```yaml
---
- hosts: all
  tasks:
    - name: Create file
      ansible.builtin.file:
        path: "{{ item['log-path'] }}"
        mode: "{{ item['log-mode'] }}"
        state: touch
      loop: # loop 키워드 내부에 한묶음씩 선언
        - log-path: /var/log/test1.log
          log-mode: '0644'
        - log-path: /var/log/test2.log
          log-mode: '0600'
```

수행하면 정상적으로 파일이 생성되는것을 확인할 수 있습니다.
```bash
ansible-playbook make-file.yml

PLAY [all] *************************************************************************************************************************************************************************************************************************

TASK [Create file] *****************************************************************************************************************************************************************************************************************
changed: [tnode1] => (item={'log-path': '/var/log/test1.log', 'log-mode': '0644'})
changed: [tnode3] => (item={'log-path': '/var/log/test1.log', 'log-mode': '0644'})
changed: [tnode2] => (item={'log-path': '/var/log/test1.log', 'log-mode': '0644'})
changed: [tnode3] => (item={'log-path': '/var/log/test2.log', 'log-mode': '0600'})
changed: [tnode1] => (item={'log-path': '/var/log/test2.log', 'log-mode': '0600'})
changed: [tnode2] => (item={'log-path': '/var/log/test2.log', 'log-mode': '0600'})

PLAY RECAP *************************************************************************************************************************************************************************************************************************
tnode1                     : ok=1    changed=1    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
tnode2                     : ok=1    changed=1    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
tnode3                     : ok=1    changed=1    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
```

## 반복문과 Register 변수 사용
Register 변수를 사용해서 반복문 수행 결과를 캡쳐한 이후 작업 결과를 확인할 수 있습니다.

```yaml
---
- hosts: all
  tasks: 
    - name: loop echo test
      ansible.builtin.shell: "echo 'I can speak {{ item }}'"
      loop:
        - Korean
        - English
      register: result

    - name: show result
      ansible.builtin.debug:
        var: result
```

출력 포멧을 표준출력으로 변경해서 결과값을 한줄로 확인할 수 도 있습니다.
*(심화) Return Values : Common - [Link](https://docs.ansible.com/ansible/latest/reference_appendices/common_return_values.html#common)*

- **stderr** : 명령의 표준 에러
    - stderr_lines : 표준 에러 출력을 행 단위로 구분한 목록
- **stdout** : 명령의 표준 출력
    - stdout_lines : 표준 출력을 행 단위로 구분한 목록
- **rc** : ‘return code’ 반환 코드
- **msg** : 사용자가 전달한 일반 문자열 메시지
- 등등

```yaml
---
- hosts: all
  tasks: 
    - name: loop echo test
      ansible.builtin.shell: "echo 'I can speak {{ item }}'"
      loop:
        - Korean
        - English
      register: result

    - name: show result
      ansible.builtin.debug:
        msg: "Stdout: {{ item.stdout }}"
      loop: "{{ result.results }}"
```


## ETC. 예전 스타일의 반복문
```loop``` 키워드를 사용하지 않고 예전 스타일로 반복문을 수행하는 경우도 있습니다.
- 비권장 , 그러나 예전 플레이북을 분석해야할 일도 있기 때문에 알고잇는것이 좋음

| **반복문 키워드** | **설명** |
| --- | --- |
| with_items | 문자열 목록 또는 사전 목록과 같은 단순한 목록의 경우 loop 키워드와 동일하게 작동함.loop와 달리 목록으로 이루어진 목록이 with_items에 제공되는 경우 단일 수준의 목록으로 병합되며, 반복문 변수 item에는 각 반복 작업 중 사용되는 목록 항목이 있음. |
| with_file | 제어 노드의 파일 이름을 목록으로 사용할 경우 사용되며, 반복문 변수 item에는 각 반복 작업 중 파일 목록에 있는 해당 파일의 콘텐츠가 있음. |
| with_sequence | 숫자로 된 순서에 따라 값 목록을 생성하는 매개 변수가 필요한 경우 사용되며, 반복문 변수 item에는 각 반복 작업 중 생성된 순서대로 생성된 항목 중 하나의 값이 있음. |

# 조건문
앤서블은 플레이북에서 조건문을 사용하여 특정 조건이 충족되었을 경우 작업 또는 플레이를 실행할 수 있습니다. 예를들어 조건문을 사용하여 호스트의 운영체제 버전에 해당하는 서비스를 설치하는 식으로 사용할 수 있습니다.

조건문은 ```when``` 구문을 사용합니다.

기본적으로 ```when``` 구문에 있는게 충족되면 task를 실행되고, 충족되지않으면 task를 스킵시키는 형태 입니다.

## Boolean
```when``` 구문에 ```true | false``` 가 들어왔을 경우 , ```false``` 라면 task skip, ```true``` 라면 task를 수행하는 가장 단순한 테스트 플레이북입니다.

```yaml
---
- hosts: localhost
  vars:
    run_my_task: true # or false , false일 경우 echo message tasks 스킵됨
  
  tasks:
  - name: echo message
    ansible.builtin.shell: "echo test"
    when: run_my_task
    register: result
  
  - name: Show result
    ansible.builtin.debug:
      var: result
```

## 조건 연산자
bool 변수(true, false) 외에도 조건 연산자를 사용할 수 있습니다.

예를 들어 when 문에 `ansible_facts[’machine’] == “x86_64”` 라는 구문을 사용했다면 `ansible_facts[’machine’]` 값이 `x86_64`일 때만 해당 태스크를 수행합니다.

| 연산 예시 | 설명 |
| --- | --- |
| ansible_facts[’machine’] == “x86_64” | ansible_facts[’machine’] 값이 x86_64와 같으면 true |
| max_memory == 512 | max_memory 값이 512와 같다면 true |
| min_memory > 128 | min_memory 값이 128보다 작으면 true |
| min_memory > 256 | min_memory 값이 256보다 크면 true |
| min_memory <= 256 | min_memory 값이 256보다 작거나 같으면 true |
| min_memory >= 512 | min_memory 값이 512보다 크거나 같으면 true |
| min_memory != 512 | min_memory 값이 512와 같지 않으면 true |
| min_memory is defined | min_memory 라는 변수가 있으면 true |
| min_memory is not defined | min_memory 라는 변수가 없으면 true |
| memory_available | memory 값이 true이며 true, 이때 해당 값이 1이거나 True 또는 yes면 true |
| not memory_available | memory 값이 false이며 true, 이때 해당 값이 0이거나 False 또는 no면 true |
| ansible_facts[’distribution’] in supported_distros | ansible_facts[’distribution’]의 값이 supported_distros 라는 변수에 있으면 true |
- **!=** : 값이 같지 않을 때 **참** true
- **>, >=, <=, <** : ‘초과, ‘ 이상’, ‘이하’, ‘미만’ 일 때에 **참** true
- **not** : 조건의 부정
- **and, or** : ‘그리고’, ‘또는’의 의미로 여러 조건의 조합 가능
- **in** : 값이 포함된 경우에 **참** true. 예를 들어 2 in “1, 2, 3” 은 **참** true
- **is defined** : 변수가 정의된 경우 **참** true

조건연산자를 이용하여 ```ansible_facts['distribution']``` 에 수집된 값중 Ubuntu, CentOS 가 포함되어 있을 경우에 메세지를 출력하는 플레이북 입니다.

```yaml
---
- hosts: all
  vars:
    supported_distros:
      - Ubuntu
      - CentOS
  
  tasks:
    - name: Print Supported Os
      ansible.builtin.debug:
        msg: "This {{ ansible_facts['distribution'] }} need to use apt }}"
      when: ansible_facts['distribution'] in supported_distros
```

## AND , OR 연산 - 복수연산자
앤서블 when 구문은 여러개의 조건이 들어가는 복수 연산자를 사용할 수 있습니다. 예를들어 운영체제가 Ubuntu 이거 서버 타입이 x86_64 일 경우에만 작업이 실행하게끔 구성할 수 있습니다.

이는 AND , OR 연산자로 수행할 수 있습니다.
- AND 연산 : ```and```
- OR 연산 : ```or```

둘중 하나가 참일경우 참인 ```or``` 연산의 예시 플레이북 입니다.

```yaml
--- 
- hosts: all
  tasks:
    - name: Print os type
      ansible.builtin.debug:
        msg: >- # 여러줄 출력하기 위해 >- 구문 추가
             OS Type : {{ ansible_facts['distribution'] }} 
             OS Type : {{ ansible_facts['distribution_version'] }}
      # ansible_facts['distribution'] 이 Ubuntu 이거나 CentOS 일 경우
      when: ansible_facts['distribution'] == "CentOS" or ansible_facts['distribution']  == "Ubuntu"
```

둘다 참일경우 참인 ```and``` 연산의 예시 플레이북 입니다.

```yaml
---
- hosts: all

  tasks:
    - name: Print os type
      ansible.builtin.debug:
        msg: >-
             OS Type: {{ ansible_facts['distribution'] }}
             OS Version: {{ ansible_facts['distribution_version'] }}
      # ansible_facts['distribution'] 이 Ubuntu 이고 OS Version이 22.04 일 경우
      when: ansible_facts['distribution'] == "Ubuntu" and ansible_facts['distribution_version'] == "22.04"
```

```and``` 연산자는 키워드 대신 ```-``` 으로도 표현이 가능합니다.

```yaml
---
- hosts: all
  tasks:
    - name: Print os type
      ansible.builtin.debug:
        msg: >-
             OS Type : {{ ansible_facts['distribution'] }}
             OS Version : {{ ansible_facts['distribution_version'] }}
      # ansible_facts['distribution'] 이 Ubuntu 이고 OS Version이 22.04 일 경우
      when:
        - ansible_facts['distribution'] == "Ubuntu"
        - ansible_facts['distribution_version'] == "22.04"
```

```and``` 연산자와 ```or``` 연산자를 같이 사용할 수 있습니다.

아래의 예시 플레이북은 CentOS이면서 8 이거나, Ubuntu이면서 22.04 인 경우를 표현합니다.

```yaml
---
- hosts: all
  tasks:
    - name: Print os type
      ansible.builtin.debug:
        msg: >- 
              OS Type : {{ ansible_facts['distribution'] }}
              OS Version : {{ ansible_facts['distribution_version'] }}
      when: >
              ( ansible_facts['distribution'] == "CentOS" and
                ansible_facts['distribution_version'] == "8" )
              or
              ( ansible_facts['distribution'] == "Ubuntu" and
                ansible_facts['distribution_version'] == "22.04" )
```


## 반복문과 조건문을 함께 사용하기
- [ansible.builtin.command 모듈 공식문서](https://docs.ansible.com/ansible/latest/collections/ansible/builtin/command_module.html)

반복문과 조건문을 같이 사용하서 더 정교한 플레이북을 작성할 수 있습니다.

아래 예시는 db 관리 host를 대상으로 ```ansible_facts``` 에 수집된 여러개의 마운트 정보들을 반복하면서, 마운트 경로가 ```"/"``` 이고 ```item['size_available']``` 가 ```300000000``` 보다 클 경우 tasks를 수행하게끔 작성한 플레이북 입니다.

- ansible_facts['mounts'] 정보, 여러개가 있는것을 확인할 수 있습니다.
```bash
 "ansible_mounts": [
        {
            "block_available": 7108813,
            "block_size": 4096,
            "block_total": 7574288,
            "block_used": 465475,
            "device": "/dev/root",
            "fstype": "ext4",
            "inode_available": 3796182,
            "inode_total": 3870720,
            "inode_used": 74538,
            "mount": "/",
            "options": "rw,relatime,discard,errors=remount-ro",
            "size_available": 29117698048,
            "size_total": 31024283648,
            "uuid": "9e71e708-e903-4c26-8506-d85b84605ba0"
        },
        {
            "block_available": 0,
            "block_size": 131072,
            "block_total": 446,
            "block_used": 446,
            "device": "/dev/loop1",
            "fstype": "squashfs",
            "inode_available": 0,
            "inode_total": 10944,
            "inode_used": 10944,
            "mount": "/snap/core18/2812",
            "options": "ro,nodev,relatime,errors=continue,threads=single",
            "size_available": 0,
            "size_total": 58458112,
            "uuid": "N/A"
        },
        {
          .... 계속
```

- 플레이북 입니다.

```yaml
---
- hosts: db
  tasks:
    - name: Print Root Directory Size
      ansible.builtin.debug:
        msg: "Directory {{ item.mount }} size is {{ item.size_available }}"
      loop: "{{ ansible_facts['mounts'] }}"
      when: item['mount'] == "/" and item['size_available'] > 300000000
```

register로 작업 결과를 저장해 두고 후속 tasks에서 register 결과값을 조건으로 사용할 수 도 있습니다.
- 아래 플레이북은 systemctl 명령어로 rsyslog가 active인지를 체크하여 해당 결과를 result 변수에 저장하고, Print rsyslog status 태스크에서 result.stdout 값이 active 일 경우에만 해당 값을 출력합니다.

```yaml
---
- hosts: all
  tasks: 
    - name: Get rsyslog service status
      ansible.builtin.command: systemctl is-active rsyslog
      register: result
    
    - name: Print rsyslog status
      ansible.builtin.debug:
        msg: "Rsyslog status is {{ result.stdout }}"
      when: result.stdout == "active"
```

# 핸들러 및 작업 실패 처리
앤서블 모듈은 멱등(idempotent) 이 가능하도록 설계되었습니다.
- 플레이북을 여러번 실행하더라도 결과가 항상 동일

또한 플레이 및 해당 작업은 여러번 실행될 수 있지만, 해당 호스트는 원하는 상태로 만드는데 필요한 경우에만 변경됩니다.
- 이미 원하는 상태에 도달해 있으면 변경되지 않음

그러나 한 작업에서 시스템을 변경해야 하는 경우 추가작업이 필요할 수 있는데, 예를들어서 구성파일을 변경하고 서비스를 재 시작하는것과 같은 작업이 있습니다.

핸들러는 이러한 상황에서 다른 작업에서 트리거한 알림에 응답하여 후속 작업을 진행하는 작업이며, 해당 호스트에서 작업이 변경될 때만 핸들러에게 통지합니다.
- 원하는 상태에 도달하여 호스트가 변하지 않았다면, 핸들러도 호출되지 않습니다.

[핸들러 공식문서](https://docs.ansible.com/ansible/latest/playbook_guide/playbooks_handlers.html)

## 핸들러 사용 방안
사용방안은 간단합니다. ```notify``` 키워드를 사용하여 ```tasks``` 에서 ```handler``` 의 이름을 지정하고, ```tasks``` 와 같은 ```line``` 에 ```handler``` 들을 정의합니다.

아래 플레이북을 수행하면 ```restart rsyslog``` ```task``` 가 수행된 이후 ```print msg``` ```handler```가 호출되게 됩니다.

```yaml
---
- hosts: tnode2
  tasks: 
    - name: restart rsyslog
      ansible.builtin.service:
        name: "rsyslog"
        state: restarted
      notify: # 아래 호출할 handlers의 이름을 나열
        - print msg
  

  handlers: # handler 선언부
    - name: print msg
      ansible.builtin.debug:
        msg: "rsyslog is restarted"
```

## 작업 실패 무시방안
앤서블은 플레이북 수행 중 각 작업의 반환값을 평가하여 작업 성공유무를 판단하게 되는데, 일반적으로 작업이 실패하면 앤서블은 이후의 모든 작업을 건너뜁니다.

그러나 ```ignore_errors``` 키워드로 task가 실패하더라도 플레이북을 계속 수행할 수 있습니다.

아래처럼. 실패를 무시할 tasks에 위 ```ignore_errors``` 키워드를 yes 로 설정해 두면, 수행에 실패하더라도 다음 task가 수행됩니다.

```yaml
---
- hosts: tnode1
  tasks:
    - name: install apache3
      ansible.builtin.apt:
        name: apache3
        state: latest
      ignore_errors: yes # 해당 작업 실패하더라도 무시하고 다음 task 수행

    - name: print msg
      ansible.builtin.debug:
        msg: "Before task is ignore"
```


## 작업 실패 후 핸들러 수행 방안
해당 task가 실패하면 이전에 nofiy 받은 handler도 전부다 수행되지 못합니다. 그러나 ```force_handlers``` 키워드를 사용하면 task가 실패하더라도 호출될 handler도 수행됩니다.
```yaml
---
- hosts: tnode2
  force_handlers: yes # notify 걸려있는 handler는 후속 tasks들이 실패하더라도 다 호출되게끔 설정
  
  tasks:
    - name: restart rsyslog
      ansible.builtin.service:
        name: "rsyslog"
        state: restarted
      notify:
        - print msg
    
    - name: install apache3 # 해당 tasks는 실패함
      ansible.builtin.apt:
        name: "apache3"
        state: latest
    
  handlers:
    - name: print msg
      ansible.builtin.debug:
        msg: "rsyslog is restart"
```


## 작업 실패 조건 지정
### 앤서블에서 쉘 스크립트를 사용하는것을 권장하지 않는 이유들..
command 계열의 모듈을 사용하면, 앤서블에서 **셸 스크립트를 실행한 뒤 결과로 실패 또는 에러 메시지를 출력해도, 앤서블에서는 작업이 성공했다고 간주**합니다.

또한 **어떠한 명령이라도 쉘 스크립트가 실행되기 때문에 테스크 실행 상태를 항상 changed**가 됩니다.

만약 어쩔수 없이 쉘 스크립트를 앤서블 플레이북에서 쉘 스크립트를 수행해야 하는 경우엔 , ```failed_when``` 키워드를 사용하여 **작업이 실패했음을 나타내는 조건을 지정**할 수 있습니다.

```yaml
---
- hosts: tnode1

  tasks:
    - name: Run user add script
      ansible.builtin.shell: /home/ubuntu/adduser-script.sh
      register: command_result
    
    - name: Print msg
      ansible.builtin.debug:
        msg: "{{ command_result.stdout }}"
```

위의 adduser-script.sh 파일은 tnode1 호스트에서 수행되게 되는데, 해당 스크립트는 **에러가나는 스크립트 입니다.**

그러나, 플레이북을 수행하면 **에러발생이 아닌 changed로 잘 작업이 수행되었다고 출력됩니다.**

```bash
ansible-playbook failed-when-1.yml 

PLAY [tnode1] **********************************************************************************************************************************************************************************************************************

TASK [Run user add script] *********************************************************************************************************************************************************************************************************
changed: [tnode1]

TASK [Print msg] *******************************************************************************************************************************************************************************************************************
ok: [tnode1] => {
    "msg": "Please input user id and password.\nUsage: adduser-script.sh \"user01 user02\" \"pw01 pw02\""
}

PLAY RECAP *************************************************************************************************************************************************************************************************************************
tnode1                     : ok=2    changed=1    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
```

따라서 ```failed_when``` 키워드로 해당 스크립트의 결과값에 어떤 값이 들어가 있을 때 tasks가 실패하도록 작성해야만 합니다.
- command_result.stdout 에서 Please input user id and password 문자열이 포함되면 해당 tasks는 실패로 간주하는 조건입니다.

```yaml
---
- hosts: tnode1

  tasks:
    - name: Run user add script
      ansible.builtin.shell: /home/ubuntu/adduser-script.sh
      register: command_result
      # command_result.stdout 에서 Please input user id and password 문자열이 포함되면 해당 tasks는 실패로 간주함
      failed_when: "'Please input user id and password' in command_result.stdout"
    
    - name: Print msg
      ansible.builtin.debug:
        msg: "{{ command_result.stdout }}"
```

플레이북 수행 시 다음과 같이 tasks가 실패합니다.
```bash
ansible-playbook failed-when-1.yml 

PLAY [tnode1] **********************************************************************************************************************************************************************************************************************

TASK [Run user add script] *********************************************************************************************************************************************************************************************************
fatal: [tnode1]: FAILED! => {"changed": true, "cmd": "/home/ubuntu/adduser-script.sh", "delta": "0:00:00.005139", "end": "2024-01-18 23:16:36.117670", "failed_when_result": true, "msg": "", "rc": 0, "start": "2024-01-18 23:16:36.112531", "stderr": "", "stderr_lines": [], "stdout": "Please input user id and password.\nUsage: adduser-script.sh \"user01 user02\" \"pw01 pw02\"", "stdout_lines": ["Please input user id and password.", "Usage: adduser-script.sh \"user01 user02\" \"pw01 pw02\""]}

PLAY RECAP *************************************************************************************************************************************************************************************************************************
tnode1                     : ok=0    changed=0    unreachable=0    failed=1    skipped=0    rescued=0    ignored=0   
```

그러나 이렇게 실패조건들을 전부다 넣을순 없기에,, **ansible 쉘 스크립트를 사용하는것은 지향하는것이 좋습니다.**

# 앤서블 블록 및 오류처리
앤서블은 플레이북 내에서 block 이라는 오류를 제어하는 문법을 제공합니다.
- 논리 조건문 (switch문의 default와 비슷함)

```block``` 을 통해 ```rescue``` , ```always``` 키워드를 함께 사용함으로써 오류를 처리할 수 있습니다.

- ```block``` : faile_when 구문을 사용하여 실패 조건을 정의함
- ```rescue``` : ```block``` 구문이 실패했다면 호출됨
- ```always``` : 실패에 관계없이 항상 실행됨

아래 플레이북은 로그파일을 생성하는 플레이북인데, 다음과 같은 로직을 가지고 있습니다.
- ```block``` : 로그 폴더가 없다면 실패
- ```rescue``` : 로그폴더 생성
- ```always``` : 생성한 로그폴더에 로그파일 생성

만약 로그폴더가 있다면, ```rescue``` task 는 Skip 되고 ```block```, ```always``` 만 수행됩니다.

```yaml
---
- hosts: tnode2
  vars:
    logDir: /var/log/daily_log
    logfile: todays.log

  tasks:
    - name: Configure Log Env
      block:
        - name: Find Directory
          ansible.builtin.find:
            path: "{{ logDir }}"
          register: result
          # result.msg 에 Not all paths 문자열 있으면 해당 block task 실패
          failed_when: "'Not all paths' in result.msg"
        
      rescue:
        - name: Make Directory when Not found Directory
          ansible.builtin.file:
            path: "{{ logDir }}"
            state: directory
            mode: '0755'
      
      always:
        - name: Create File
          ansible.builtin.file:
            path: "{{ logDir }}/{{ logfile }}"
            state: touch
            mode: '0644'
```
