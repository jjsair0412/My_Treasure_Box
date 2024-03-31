---
title: Ansible jinja2
subtitle: Ansible jinja2를 통한 변수 확장방법
tags: devops, opensource, ansible
domain: jjsair0412.hashnode.dev
---

# jinja2 변수확장법
앤서블에서는 변수의 확장을 위해서 파이썬에서 템플릿을 위해 정의된 엔진인 jinja2를 사용합니다.

이를 이용해서 앤서블 내부에서 코드처럼 로직을 작성하거나, ```.cfg``` 파일들과 같은 패키지별 설정 파일들을 동적으로 바꿀 때 많이 사용됩니다.
- 예를들어 [apache2 설치](#3-ubuntu-와-centos에-apache-http를-설치하는-playbook을-작성해서-실습해보세요-롤템플릿-사용은-편한대로) 예제에선 jinja2 template을 사용하여 index.html파일을 앤서블에서 미리 정의해두고 apache2를 설치하였습니다.


앤서블에서 **변수 확장**에는 파이썬으로 작성된 **Jinja2 템플릿 엔진**을 사용합니다.
원래 **플라스크** Flask라는 파이썬의 웹 애플리케이션 프로임워크로 HTML에 동적인 값을 설정할 때 사용되는 템플릿 엔진입니다.
진자의 ‘**템플릿의 변수 정보를 확장하고 출력한다**’ 일반적인 동작을 앤서블에서는 템플릿 모듈을 사용해 파일을 확장하는 것은 물론이고, 플레이북에 있는 변수 정보를 확장할 때도 진자2를 사용합니다.


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