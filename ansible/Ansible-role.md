
# 앤서블을 통한 협업 방안
앤서블은 협업을 위한 방법이 있습니다. 작성한 플레이북을 공유하거나 다른사람의 플레이북을 가지고와서 활용할 수 있습니다.

이는 작업시간을 단축할 뿐만 아니라 잘 만들어진 플레이북의 구조를 학습하여 실력향상에도 도움이 됩니다.

이는 **앤서블 롤을 통해 구현하고 앤서블 갤럭시를 통해 공유할 수 있으며, 누군가 이미 만들어놓은 롤을 검색하여 가져올 수 있습니다.**

## 앤서블 롤
앤서블 롤은 ***플레이북 내용을 기능별로 나누어서 공통 부품으로 관리/재사용 하기 위한 구조*** 입니다.

롤의 장점은 아래와 같습니다.

- 플레이북에서 전달된 **변수**를 사용할 수 있습니다. 변수 미설정 시 기본값을 롤의 해당 변수에 설정하기도 합니다.
- **콘텐츠**를 **그룹화**하여 코드를 다른 사용자와 쉽게 공유할 수 있습니다.
- 웹 서버, 데이터베이스 서버 또는 깃(Git) 리포지터리와 같은 **시스템** 유형의 **필수 요소**를 **정의**할 수 있습니다.
- 대규모 프로젝트를 쉽게 관리할 수 있습니다.
- 다른 사용자와 **동시**에 **개발**할 수 있습니다.
- 잘 작성한 롤은 앤서블 **갤럭시**를 통해 **공유**하거나 다른 사람이 공유한 롤을 가져올 수도 있습니다.

**앤서블 롤 구조** : 롤은 하위 디렉터리 및 파일의 표준화된 구조에 의해 정의됩니다.

- 최상위 디렉터리는 롤 자체의 이름을 의미하고, 그 안은 tasks 및 handlers 등 롤에서 목적에 따라 정의된 하위 디렉터리로 구성됩니다.
- 아래 표는 롤의 최상의 디렉터리 아래에 있는 하위 디렉터리의 이름과 기능을 설명한 것 입니다.

| 하위 디렉터리 | 기능 |
| --- | --- |
| defaults | 이 디렉터리의 main.yml 파일에는 롤이 사용될 때 덮어쓸 수 있는 롤 변수의 기본값이 포함되어 있습니다. 이러한 변수는 우선순위가 낮으며 플레이에서 변경할 수 있습니다. |
| files | 이 디렉터리에는 롤 작업에서 참조한 정적 파일이 있습니다. |
| handlers | 이 디렉터리의 main.yml 파일에는 롤의 핸들러 정의가 포함되어 있습니다. |
| meta | 이 디렉터리의 main.yml 파일에는 작성자, 라이센스, 플랫폼 및 옵션, 롤 종속성을 포함한 롤에 대한 정보가 들어 있습니다. |
| tasks | 이 디렉터리의 main.yml 파일에는 롤의 작업 정의가 포함되어 있습니다. |
| templates | 이 디렉터리에는 롤 작업에서 참조할 Jinja2 템플릿이 있습니다. |
| tests | 이 디렉터리에는 롤을 테스트하는 데 사용할 수 있는 인벤토리와 test.yml 플레이북이 포함될 수 있습니다. |
| vars | 이 디렉터리의 main.yml 파일은 롤의 변수 값을 정의합니다. 종종 이러한 변수는 롤 내에서 내부 목적으로 사용됩니다. 또한 우선순위가 높으며, 플레이북에서 사용될 때 변경되지 않습니다. |

### 1. 롤 생성 방안
롤을 생성하기 위해선 ```ansible-galaxy``` 키워드를 사용합니다.

먼저 아래 명령어로 init을 수행합니다.

```bash
$ ansible-galaxy role init my-role
- Role my-role was created successfully
```

그럼 위의 [롤 디렉터리 구조](#앤서블-롤) 에 맞게끔 롤 기본 디렉터리들이 생성됩니다.
```bash
$ tree ./my-role/
./my-role/
├── README.md
├── defaults
│   └── main.yml
├── files
├── handlers
│   └── main.yml
├── meta
│   └── main.yml
├── tasks
│   └── main.yml
├── templates
├── tests
│   ├── inventory
│   └── test.yml
└── vars
    └── main.yml

8 directories, 8 files
```

### 2. 롤을 통한 앤서블 개발
이제 생성된 롤을 통해 플레이북을 개발해 봅니다.
- httpd를 설치하고 정적 페이지를 호스팅하는 앤서블 플레이북을 롤로 개발합니다.

롤은 다음 구조별로 개발하며, 폴더에 작성합니다.
- 롤 이름 : **my-role**
- **tasks (메인 태스크)**
    - install service : httpd 관련 패키지 설치
    - copy html file : index.html 파일 복사
- **files (정적 파일)**
    - index.html
- **handlers (핸들러)**
    - restart service : httpd 서비스 재시작
- **defaults (가변 변수)** : 메인 태스크에서 사용된 변수 선언
    - service_title
- **vars (불변 변수)** : 메인 태스크와 핸들러에서 사용된 변수 선언
    - service_name : 서비스명
    - src_file_path : 복사할 파일 경로
    - dest_file_path : 파일이 복사될 디렉터리 경로
    - httpd_packages : httpd 관련 패키지 목록
    - supported_distros : 지원 OS 목록

#### 2.1 task 구성
먼저 tasks를 구성합니다.
- 첫번째 tasks에서는 httpd 관련 패키지를 설치합니다. 관련 패키지가 여러개기 때문에 loop 문을 사용
- 두번째 tasks에서는 httpd 설치가 완료되기에 롤 files 폴더 내부에 있는 index.html 정적 파일을 복사해서 httpd index.html로 사용합니다.
```yaml
---
# tasks file for my-role

- name: install service {{ service_title }}
  ansible.builtin.apt:
    name: "{{ item }}"
    state: latest
  loop: "{{ httpd_packages }}"
  when: ansible_facts.distribution in supported_distros

- name: copy conf file
  ansible.builtin.copy:
    src: "{{ src_file_path }}"
    dest: "{{ dest_file_path }}"
  notify: 
    - restart service
```

#### 2.2 정적 파일 생성
이후 롤 files 폴더에 index.html 정적 파일을 생성합니다.
```bash
echo "Hello! Ansible" > files/index.html
```

#### 2.3 핸들러 생성
두번째 tasks가 끝나고 호출될 ```restart service``` 핸들러를 롤 handlers 폴더에 생성합니다.

핸들러에선 service 모듈을 사용하여 httpd 서비스를 재 시작 합니다.
```yaml
---
# handlers file for my-role

- name: restart service
  ansible.builtin.service:
    name: "{{ service_name }}"
    state: restarted
```

#### 2.4 가변 변수 작성
외부로부터 주입(재 정의) 이 가능한 가변 변수를 선언합니다. 롤 defaults 폴더 안 main.yml에 구성합니다.

```bash
echo 'service_title: "Apache Web Server"' >> defaults/main.yml
```

#### 2.5 불변 변수 작성
한번 정의되면 외부 주입등의 활동으로 변경될 수 없는 불변 변수를 선언합니다. 롤 내의 플레이북에서만 사용되는 변수로 정의하는편이 좋습니다.

롤 vars 폴더 안 main.yml에 구성합니다.
- tasks에서 httpd_packages 변수개수만큼 루프돌면서 설치되니까, apache2, apache2-doc 이 설치됩니다.
```yaml
---
# vars file for my-role

service_name: apache2
src_file_path: ../files/index.html
dest_file_path: /var/www/html
httpd_packages:
  - apache2
  - apache2-doc

supported_distros:
  - Ubuntu
```

#### 2.6 롤 실행하기
롤을 바로실행할순 없고, 플레이북을 생성해서 해당 플레이북에 롤을 import 시켜서 롤을 실행시켜야 합니다.

플레이북에 롤을 import 하는것은 앤서블 모듈로 수행하고, 두가지 모듈이 있습니다.
- 둘중하나 사용하면 됨
1. ansible.builtin.import_role
2. ansible.builtin.include_role
```yaml
---
- hosts: tnode1
  
  tasks:
    - name: Print Start Play
      ansible.builtin.debug:
        msg: "Let's start role play"

    - name: Install Service by role
      ansible.builtin.import_role:
        # 수행할 role 이름 작성
        name: my-role
```

해당 플레이북을 수행하면 생성한 my-role에 있는 tasks가 순차적으로 실행되는것을 볼 수 있습니다.
```bash
ansible-playbook role-example.yml 

PLAY [tnode1] **********************************************************************************************************************************************************************************************************************

TASK [Print Start Play] ************************************************************************************************************************************************************************************************************
ok: [tnode1] => {
    "msg": "Let's start role play"
}

TASK [my-role : install service {{ service_title }}] *******************************************************************************************************************************************************************************
changed: [tnode1] => (item=apache2)
changed: [tnode1] => (item=apache2-doc)

TASK [my-role : copy conf file] ****************************************************************************************************************************************************************************************************
changed: [tnode1]

RUNNING HANDLER [my-role : restart service] ****************************************************************************************************************************************************************************************
changed: [tnode1]

PLAY RECAP *************************************************************************************************************************************************************************************************************************
tnode1                     : ok=4    changed=3    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
```

curl로 웹에 접근하면, 정적페이지가 호스팅된것을 볼 수 있습니다.
```bash
$ curl tnode1
Hello! Ansible
```

index.html 파일을 변경하여 적용 후 변경적용이 되는지 확인합니다.
```bash
$ echo "Hello! CloudNet@" > my-role/files/index.html

# playbook 재 수행
$ ansible-playbook role-example.yml

# 확인
$ curl tnode1
Hello! CloudNet@
```

### 3. 플레이북의 roles 섹션 사용
플레이북안에 role을 여러개 지정해서 플레이북을 수행할 수 도 있습니다.

```yaml
---

- hosts: tnode1

  roles:
    - my-role
    - my-role2

  tasks:
    - name: Print finish role play
      ansible.builtin.debug:
        msg: "Finish role play"
```

### 4. role 가변 변수 전달 방안
롤 내부에 설정한 가변 변수는 플레이북에서 설정하여 플레이북을 수행할 수 있습니다.
```yaml
---

- hosts: tnode1

  roles:
    - role: my-role
      service_title: "Httpd Web"
    - role: my-role2

  tasks:
    - name: Print finish role play
      ansible.builtin.debug:
        msg: "Finish role play"
```

## 특수 작업 섹션
roles 섹션과 함께 자주 사용되는 플레이북 내부의 특수 작업 섹션이 있습니다.

- ```pre_tasks``` : tasks와 유사하지만 roles 섹션의 롤들보다 먼저 실행됩니다. 또한 ```pre_tasks``` 섹션의 작업을 핸들러에게 알리면, 해당 핸들러 작업이 롤 또는 일반 테스크 전에 실행되게 됩니다. 그 말은 ```pre_tasks``` 에 생성된 핸들러도 먼저 수행된다는 의미
- ```post_tasks``` : tasks 및 taks에서 알림을 받은 핸들러 다음에 수행됩니다.

```yaml
---

- hosts: tnode1

  # 먼저 수행
  pre_tasks:
    - name: Print Start role
      ansible.builtin.debug:
        msg: "Let's start role play"
 
  roles:
    - role: my-role
    - role: my-role2
 
  tasks:
    - name: Curl test
      ansible.builtin.uri:
        url: http://tnode1
        return_content: true
      register: curl_result
      notify: Print result
      changed_when: true

  # 제일 나중에 수행
  post_tasks:
    - name: Print Finish role
      ansible.builtin.debug:
        msg: "Finish role play"

  handlers:
    - name: Print result
      ansible.builtin.debug:
        msg: "{{ curl_result.content }}"
```

위의 예제 플레이북을 수행하면, 다음의 순서로 플레이북이 수행됩니다.
```bash
$ ansible-playbook test.yml 

PLAY [tnode1] **********************************************************************************************************************************************************************************************************************

TASK [Print Start role] ************************************************************************************************************************************************************************************************************
ok: [tnode1] => {
    "msg": "Let's start role play"
}

TASK [my-role : install service {{ service_title }}] *******************************************************************************************************************************************************************************
ok: [tnode1] => (item=apache2)
ok: [tnode1] => (item=apache2-doc)

TASK [my-role : copy conf file] ****************************************************************************************************************************************************************************************************
ok: [tnode1]

TASK [my-role2 : Config firewalld] *************************************************************************************************************************************************************************************************
ok: [tnode1] => (item=http)
ok: [tnode1] => (item=https)

TASK [my-role2 : Reload firewalld] *************************************************************************************************************************************************************************************************
changed: [tnode1]

TASK [Curl test] *******************************************************************************************************************************************************************************************************************
changed: [tnode1]

RUNNING HANDLER [Print result] *****************************************************************************************************************************************************************************************************
ok: [tnode1] => {
    "msg": "Hello! CloudNet@\n"
}

TASK [Print Finish role] ***********************************************************************************************************************************************************************************************************
ok: [tnode1] => {
    "msg": "Finish role play"
}

PLAY RECAP *************************************************************************************************************************************************************************************************************************
tnode1                     : ok=8    changed=2    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
```