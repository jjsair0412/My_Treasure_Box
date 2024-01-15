# Ansible Password With AWS Secret Manager
Ansible Playbook에서 비밀번호를 생성할 때 , ansible-vault로 비밀번호를 암호화 하였습니다.

그러나 암호화된 파일을 관리해야하는 문제점이 있고, 파일을 갖고있기에 유실가능성도 있어서 해당 암호 파일을 AWS Secret Manager에 저장해두는 방법에 대해 기술한 문서입니다.

## 1. Ansible Server IAM Role 추가

