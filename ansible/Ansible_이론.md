---
title: Ansible 기본 이론
subtitle: Ansible 사용 전 기본이론 정리
tags: devops, opensource, ansible
domain: jjsair0412.hashnode.dev
---


# Ansible 이론

## 참고 링크
- [Ansible 공식 GitHub](https://github.com/ansible)
- [Ansible 공식 문서](https://docs.ansible.com/)
- [Ansible 공식 블로그](https://www.ansible.com/blog)

## Ansible이란 ?
Ansible은 오픈소스 IT 자동화 도구.

코드를 기반으로 여러 환경에 특정작업을 동일하게 적용될 수 있도록 도와주는 역할을 합니다.

### Ansible 특징
#### 1. Agentless
기존 자동화 도구들처럼 관리대상 Linux 노드에 Agent를 설치할 필요가 없습니다. Ansible은 Agnet 없이 SSH 기반으로 관리 노드에 접근하여 서버들을 관리합니다.

데몬 형식의 Agent를 통해 관리 노드를 관리햇을 때, 복잡한 추가 구성이나 패키지 모듈등을 설치하는 과정이 필요 없기에 편리합니다.

#### 2. 멱등성
동일한 연산을 여러번수행해도 결과가 달라지지 않는다는 성질을 갖고 있습니다.

#### 3. 쉬운 사용법과 다양한 모듈 제공
다른 자동화 도구에 비해 간단하고 복잡하지 않아 자동화 절차 및 과정을 이해하기가 쉽습니다.

yaml 기반의 문법을 갖고있기에 쉽게 작성하고 읽을 수 있습니다.

또한 파일 복사와 같은 일반시스템 관리부터 다양한 환경의 퍼블릭 클라우드 관련 모듈컬렉션까지 제공하기에, 쉽게 플레이북 예제를 찾아보고 자동화를 수행할 수 있습니다.

## Ansible 아키텍처
- Control Node (제어 노드)
    - 앤서블이 설치되는 노드로 운영체제가 리눅스라면 제어 노드가 될 수 있음.
    - 앤서블은 파이썬 모듈을 사용하기에 파이썬 설치가 필요

- Manage Node (관리 노드)
    - 앤서블이 제어하는 원격 시스템 또는 호스트
    - 리눅스가 설치된 노드일 수도 있고, 윈도우가 설치되어있을 수도 있음. 또는 퍼블릿 클라우드일경우, 프라이빗 클라우드일경우일수도 있음
    - 앤서블은 SSH 기반으로 작동하기에, 제어 노드와 SSH 연결이 되어야 하며 파이썬이 설치되어있어야 함.

- Inventory (인벤토리)
    - 제어 노드가 관리하는 관리 노드를 나열해둔 파일
    - 앤서블은 인벤토리에 사전 정의된 노드에만 접근할 수 있음
    - 인벤토리 목록은 관리 노드 성격별로 그룹화도 가능
```bash
# Inventory 예시
$ vi inventory
192.168.10.101

[WebServer]
web1.example.com
web2.example.com

[DBServer]
db1.example.com
db2.example.com
```

- Modules (모듈)
    - 앤서블은 관리 노드의 작업을 수행할 때 , SSH 연결 후 'Ansible Modules' 라는 스크립트를 푸시하여 작동
    - 대부분의 모듈은 시스템 상태를 설명하는 매게 변수를 허용하고, 모듈 실행이 완료되면 제거됨

- Plugin (플러그인)
    - 앤서블 핵심 기능 (데이터변환, 로그 출력, 인벤토리 연결 등) 을 강화하는 역할
    - **모듈이 대상 시스템에서 별도 프로세스로 실행되는 동안 플러그인은 제어 노드에서 실행됨**

- Playbook (플레이북)
    - 관리 노드에서 수행할 작업들을 Yaml 문법을 통해 순서대로 작성해둔 파일
    - 플레이북을 활용하여 관리 노드에 SSH 로 접근해 작업을 수행함
    - **플레이북은 사용자가 직접 작성하고 자동화를 완성하는 가장 중요한 파일**
```bash
# Playbook 예시
---
- hosts: webservers
  serial: 5  # 한 번에 5대의 머신을 업데이트하라는 의미
  roles:
  - common
  - webapp

- hosts: content_servers
  roles:
  - common
  - content
```