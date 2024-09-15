# Ansible 도전과제 풀이
## 1. 반복문
### 1.1 반복문으로 10명의 user를 생성하고 삭제
```loop``` 키워드로 해결 

```yaml
---
- hosts: all
  vars:
    users:
      - ansible1
      - ansible2
      - ansible3
      - ansible4
      - ansible5
      - ansible6
      - ansible7
      - ansible8
      - ansible9
      - ansible10
  
  tasks:
  - name: create 10 user
    ansible.builtin.user:
      name: "{{ item }}"
      state: present
    loop:
      "{{ users }}"
    
  - name: Delete 10 user
    ansible.builtin.user:
      name: "{{ item }}"
      state: absent
      remove: yes
    loop:
      "{{ users }}"
```

## 2. 조건문
### 2.1 Ubuntu OS이면서 fqdn으로 tnode1 인 경우, debug 모듈을 사용하여 OS 정보와 fqdn 정보를 출력해보자

```yaml
--- 
- hosts: all
  
  tasks:

    - name: Print os Type and fqdn info
      ansible.builtin.debug:
        msg: >-
              OS Type : {{ ansible_facts['distribution'] }}
              fqdn : {{ ansible_facts['fqdn'] }}
      when: 
        - ansible_facts['distribution'] == "Ubuntu"
        - ansible_facts['hostname'] == "tnode1"
```

### 2.2 반복문+조건문을 함께 사용해보자

```yaml
---
- hosts: all
  vars:
    support_dists:
      - Ubuntu
      - CentOS
    support_version:
      - "22.04"

  tasks:
    - name: Print os Type and FQDN Info
      ansible.builtin.debug:
        msg: >-
              OS Type : {{ ansible_facts['distribution'] }}
              fqdn : {{ ansible_facts['fqdn'] }}
              uuid : {{ item.uuid }}
              mount : {{ item.mount }}
      loop: "{{ ansible_facts['mounts'] }}"
      when: 
        - ansible_facts['distribution'] in support_dists 
        - ansible_facts['distribution_version'] in support_version
      
```


## 3. 핸들러
### 3.1 apache2 패키지를 apt 모듈을 통해서 설치 시, 핸들러를 호출하여 service 모듈로 apache2를 재시작 해보자
```yaml
---
- hosts: tnode1
  tasks:
    - name: install apache2
      ansible.builtin.apt:
        name: apache2
        state: latest
      notify:
        - restart apache2
    
  handlers:
    - name: restart apache2
      ansible.builtin.command: systemctl restart apache2
      register: result
```


## 4. block
### 4.1 block rescure always 키워드를 사용한 플레이북을 작성하여 테스트 해보자
```yaml
---
- hosts: tnode1
  vars:
    target_deamons:
      - apache2

  tasks:
    - name: check packages
      block:
        - name: check apache install result
          ansible.builtin.systemd_service:
            name: "{{ item }}"
            state: started
          loop: "{{ target_deamons }}"
      
      rescue:
        - name: install packages
          ansible.builtin.apt:
            name: "{{ item }}"
            state: latest
          loop: "{{ target_deamons }}"
        
      always:
        - name: start packages
          ansible.builtin.systemd_service: 
            name: "{{ item }}"
            state: restarted
          loop: "{{ target_deamons }}"
          notify: print_result
      
  handlers:
    - name: print_result
      ansible.builtin.debug:
        msg: "Service restarted"
  
```