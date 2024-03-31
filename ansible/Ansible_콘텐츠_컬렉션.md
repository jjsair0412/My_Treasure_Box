---
title: Ansible 콘텐츠 컬렉션
subtitle: Ansible 콘텐츠 컬렉션
tags: devops, opensource, ansible
domain: jjsair0412.hashnode.dev
---


# 콘텐츠 컬렉션
Asible 자체 업데이트와 핵심 Ansible 코드 업데이트를 분리시켜서 많은 벤더사와 개발자가 독립적으로 컬렉션을 자신의 속도에 맞게 유지관리 배포할 수 있는 기능.

이말은 Ansible 자체 업데이트가 Ansible 코드에 영향을 미치지 않는다는 의미입니다.

Ansible Version 2.9 이상에만 콘텐츠 컬렉션을 지원합니다.


## Commands
다양한 명령어를 통해 Ansible 콘텐츠 컬렉션을 받아오거나 설치 및 삭제를 할 수 있습니다.

### 1. 명령어 확인
```bash
$ ansible-galaxy collection -h
usage: ansible-galaxy collection [-h] COLLECTION_ACTION ...

positional arguments:
  COLLECTION_ACTION
    download         Download collections and their dependencies as a tarball for an offline install.
    init             Initialize new collection with the base structure of a collection.
    build            Build an Ansible collection artifact that can be published to Ansible Galaxy.
    publish          Publish a collection artifact to Ansible Galaxy.
    install          Install collection(s) from file(s), URL(s) or Ansible Galaxy
    list             Show the name and version of each collection installed in the collections_path.
    verify           Compare checksums with the collection(s) found on the server and the installed copy. This does not verify dependencies.

options:
  -h, --help         show this help message and exit
```

### 2. 설치된 컬렉션 확인
앤서블이 설치된 프로젝트 환경에 설치된 컬렉션들을 확인합니다.

```bash
$ ansible-galaxy collection list

# /usr/lib/python3/dist-packages/ansible_collections
Collection                    Version
----------------------------- -------
amazon.aws                    6.5.0  
ansible.netcommon             5.3.0  
ansible.posix                 1.5.4  
ansible.utils                 2.12.0 
ansible.windows               1.14.0 
...
```

### 3. 컬렉션 설치 및 삭제
앤서블 컬렉션을 설치합니다.

```bash
# 설치대상 컬렉션 확인
$ ansible-galaxy collection list openstack.cloud

# /usr/lib/python3/dist-packages/ansible_collections
Collection      Version
--------------- -------
openstack.cloud 2.2.0  

# tree 명령어를 통해 collection이 실제 위치한 공간을 확인해봅니다.
tree /usr/lib/python3/dist-packages/ansible_collections/openstack -L 3
/usr/lib/python3/dist-packages/ansible_collections/openstack
└── cloud
    ├── CHANGELOG.rst
    ├── COPYING
    ├── FILES.json
    ├── MANIFEST.json
    ├── README.md
    ├── __pycache__
    │   └── setup.cpython-310.pyc
    ├── bindep.txt
    ├── meta
    │   └── runtime.yml
    ├── plugins
    │   ├── doc_fragments
    │   ├── inventory
    │   ├── module_utils
    │   └── modules
    ├── requirements.txt
    └── setup.py
```

Ansible Collection을 설치합니다.
```bash
# 특정 버전으로 설치
$ ansible-galaxy collection install openstack.cloud:2.1.0
```

오프라인 설치(타르볼) 을 지원하기 위해서 컬렉션을 tar 형태로 다운로드하는것 또한 가능합니다.
```bash
# -p 디렉터리 지정
$ ansible-galaxy collection download -p ./collection openstack.cloud

# 확인
$ tree collection/
collection/
├── openstack-cloud-2.2.0.tar.gz
└── requirements.yml
```

tar 파일로 컬렉션을 설치할 수 도 있습니다.
```bash
# tar 파일로 컬렉션 설치
$ ansible-galaxy collection install ./collection/openstack-cloud-2.2.0.tar.gz
```
