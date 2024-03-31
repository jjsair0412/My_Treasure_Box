---
title: Ansible Facts
subtitle: Ansible Facts 이론 및 사용방안
tags: devops, opensource, ansible
domain: jjsair0412.hashnode.dev
---


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