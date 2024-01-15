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

