# OS 취약점 분석

## Overview
보고된 취약점들을 분석 후 대응 계획에 대해 스터디합니다. 

본 문서는 Rocky Linux 8.10 시스템과 관련 패키지의 취약점을 중심으로 작성되었습니다.

## 환경 정보

- **OS**: Rocky Linux 8.10
- **Kernel**: 4.18.0-513.5.1.el8_9.x86_64
- **분석 대상 서버**: Production Web Servers, Database Servers
- **패키지 관리**: DNF/YUM

## 분석 대상 패키지

1. kernel - 커널 핵심 기능
2. kernel-core - 커널 코어 기능
3. kernel-modules - 커널 모듈
4. sudo - 권한 상승 유틸리티
5. bash - 기본 쉘
6. glibc - GNU C 라이브러리
7. openssl - 보안 소켓 레이어 구현
8. openssh - 보안 원격 접속 도구
9. bind - DNS 서버 구현
10. systemd - 시스템 및 서비스 관리자
11. runc - 컨테이너 런타임
12. coreutils - 기본 시스템 유틸리티
13. free - 메모리 사용량 확인 도구

## 취약점 분석 절차

1. **취약점 식별 및 기초 조사**
  - RHSA/CVE 공지 확인
  - 영향받는 패키지 및 버전 식별
  - 심각도 및 공격 벡터 평가

2. **영향도 평가**
  - 영향받는 시스템 식별
  - 비즈니스 영향 분석
  - 위험도 산정

3. **검증 및 테스트**
  - 취약점 존재 여부 확인
  - 테스트 환경에서 패치 검증
  - 패치 적용 후 시스템 영향 평가

4. **대응 전략**
  - 패치 적용 계획
  - 대체 완화 조치 검토
  - 적용 우선순위 결정

5. **실행 및 모니터링**
  - 패치 배포 자동화
  - 패치 적용 검증

## 주요 패키지별 최근 취약점

### 커널 관련 (kernel, kernel-core, kernel-modules)

| CVE/RHSA ID | 심각도 | 설명 | 권장 조치 | 스터디 링크 |
|-------------|--------|------|-----------|--|
CVE-2024-53104	|높음	|kernel: media: uvcvideo: Skip parsing frames of type UVC_VS_UNDEFINED in uvc_parse_format (CVE-2024-53104)	|우선 패치 적용 ||
CVE-2024-50275	|높음	|kernel: arm64/sve: Discard stale CPU state when handling SVE traps (CVE-2024-50275)	|우선 패치 적용 ||
CVE-2024-26935	|높음	|kernel: scsi: core: Fix unremoved procfs host directory regression (CVE-2024-26935)	|우선 패치 적용 ||
CVE-2023-6931	|높음	|kernel: Out of boundary write in perf_read_group() as result of overflow a perf_event's read_size (CVE-2023-6931)	|우선 패치 적용 ||
CVE-2024-0565	|높음	|kernel: CIFS Filesystem Decryption Improper Input Validation Remote Code Execution Vulnerability in function receive_encrypted_standard of client (CVE-2024-0565)	|우선 패치 적용 ||
CVE-2023-51042	|높음	|kernel: use-after-free in amdgpu_cs_wait_all_fences in drivers/gpu/drm/amd/amdgpu/amdgpu_cs.c (CVE-2023-51042)	|우선 패치 적용 ||
CVE-2024-36886	|높음	|kernel: TIPC message reassembly use-after-free remote code execution vulnerability (CVE-2024-36886)	|우선 패치 적용 ||
CVE-2023-52881	|높음	|kernel: TCP-spoofed ghost ACKs and leak leak initial sequence number (CVE-2023-52881,RHV-2024-1001)	|우선 패치 적용 ||
CVE-2023-5178	|높음	|kernel: drivers/nvme/target/tcp.c use-after-free in nvmet_tcp_free_crypto	|우선 패치 적용 ||

### 시스템 유틸리티 (sudo, bash, coreutils, free)

| CVE/RHSA ID | 심각도 | 설명 | 권장 조치 | 스터디 링크 |
|-------------|--------|------|-----------|--|
CVE-2023-22809	|높음	|sudoedit (aka -e) feature mishandles extra arguments passed in user-provided environment variables, allowing local attacker to append arbitrary entries to the list of files to process, leading to privilege escalation.	|우선 패치 적용 ||
CVE-2023-26604	|높음	|systemd before 247 does not adequately block local privilege escalation for some Sudo configurations where systemctl status may be executed.	|우선 패치 적용 ||
CVE-2024-32487	|높음	|less: OS command injection	|우선 패치 적용 ||
CVE-2022-48624	|높음	|less: missing quoting of shell metacharacters in LESSCLOSE handling | 우선 패치 적용 ||

### 라이브러리 (glibc, openssl)

| CVE/RHSA ID | 심각도 | 설명 | 권장 조치 | 스터디 링크 |
|-------------|--------|------|-----------|--|
CVE-2023-4911	|높음	|glibc: Buffer overflow in ld.so while processing GLIBC_TUNABLES environment variable allows local attacker to execute code with elevated privileges.	|우선 패치 적용 ||
CVE-2023-4527	|높음	|glibc: getaddrinfo function with AF_UNSPEC and no-aaaa mode via /etc/resolv.conf may disclose stack contents and cause a crash.	|우선 패치 적용 ||
CVE-2023-4806	|높음	|glibc: getaddrinfo function may access freed memory in rare situation leading to application crash.	|우선 패치 적용 ||
CVE-2023-4813	|높음	|glibc: gaih_inet function may use freed memory in uncommon situation leading to application crash.	|우선 패치 적용 ||
CVE-2024-5535	|높음	|openssl: SSL_select_next_proto buffer overread	|우선 패치 적용 ||
CVE-2024-12797	|높음	|openssl: RFC7250 handshakes with unauthenticated servers don't abort as expected.	|우선 패치 적용 ||
CVE-2023-0286	|높음	|openssl: GENERAL_NAME_cmp vulnerability may allow attacker to pass arbitrary pointers to memcmp call, enabling memory read or DoS.	우선 패치 적용 ||
CVE-2023-38408	|높음	|openssh: ssh-agent PKCS#11 feature has insufficiently trustworthy search path leading to RCE if agent is forwarded to attacker-controlled system.	|우선 패치 적용 ||

### 네트워크 서비스 (openssh, bind)

| CVE/RHSA ID | 심각도 | 설명 | 권장 조치 | 스터디 링크 |
|-------------|--------|------|-----------|--|
CVE-2024-6387 |	높음 |	openssh: Race condition in handling signals during authentication leading to potential RCE as root on Linux systems.|우선 패치 적용 ||
CVE-2023-38408	|높음	|openssh: ssh-agent PKCS#11 feature has insufficiently trustworthy search path leading to RCE if agent is forwarded to attacker-controlled system.	|우선 패치 적용 ||
CVE-2025-26465	|높음	|openssh: Machine-in-the-middle attack if VerifyHostKeyDNS is enabled.	|우선 패치 적용 ||
CVE-2024-11187	|높음	|bind: bind9: Many records in the additional section cause CPU exhaustion	|우선 패치 적용 ||
CVE-2023-4408	|높음	|bind9: Parsing large DNS messages may cause excessive CPU load	|우선 패치 적용 ||
CVE-2023-50387	|높음	|bind9: KeyTrap - Extreme CPU consumption in DNSSEC validator	|우선 패치 적용 ||
CVE-2023-50868	|높음	|bind9: Preparing an NSEC3 closest encloser proof can exhaust CPU resources	|우선 패치 적용 ||

### 시스템 및 컨테이너 관리 (systemd, runc)

| CVE/RHSA ID | 심각도 | 설명 | 권장 조치 | 스터디 링크 |
|-------------|--------|------|-----------|--|
| CVE-2023-26604 | 높음 | systemd before 247 does not adequately block local privilege escalation for some Sudo configurations where systemctl status may be executed. | 우선 패치 적용  ||
| CVE-2024-21626 | 높음 | runc: File Descriptor Leak and Path Traversal vulnerability allows container escape.	 | 우선 패치 적용  ||

## 패치 우선순위 기준

1. **긴급 (즉시 패치)**
  - 원격 코드 실행 가능 취약점
  - 활발히 악용 중인 취약점
  - 인증 우회 취약점
  - 예: kernel, sudo, openssl의 심각도 높음 취약점

2. **높음 (72시간 이내)**
  - 권한 상승 취약점
  - 내부 네트워크에서 악용 가능한 취약점
  - 서비스 거부 공격 취약점
  - 예: openssh, bind, runc의 심각도 중간 취약점

3. **중간 (2주 이내)**
  - 정보 노출 취약점
  - 제한된 조건에서 악용 가능한 취약점
  - 예: bash, systemd의 심각도 중간 취약점

4. **낮음 (정기 패치)**
  - 로컬 접근 필요 취약점
  - 이론적 취약점
  - 예: coreutils, free의 심각도 낮음 취약점

## 패키지별 분석 접근법

### 커널 관련 패키지
- 운영 영향도가 높으므로 테스트 환경에서 철저한 검증 후 적용
- 패치 적용 시 반드시 재부팅 필요
- 클러스터 환경에서는 롤링 업데이트 전략 적용

### 시스템 유틸리티
- sudo 취약점은 권한 상승 관련이므로 최우선 패치
- bash 취약점은 로그인 쉘에 영향을 주므로 사용자 작업에 주의

### 네트워크 서비스
- 외부에 노출된 서비스는 우선 패치
- 패치 전 방화벽 규칙으로 임시 완화 조치 적용 가능

### 컨테이너 관련
- runc 취약점은 컨테이너 환경에서 중요도가 높음
- 적용 전 컨테이너 워크로드 영향 분석 필요

## 패치 관리 자동화

- **Ansible 플레이북 구성**
```yaml
 - name: Patch Critical Packages
   hosts: production_servers
   become: yes
   tasks:
     - name: Check current kernel version
       command: uname -r
       register: kernel_version
       changed_when: false

     - name: Update kernel packages
       dnf:
         name:
           - kernel
           - kernel-core
           - kernel-modules
         state: latest
       when: "'4.18.0-513' in kernel_version.stdout"
       register: kernel_updated

     - name: Update security critical packages
       dnf:
         name:
           - sudo
           - openssl
           - openssh
           - bind
           - runc
         state: latest
       register: security_updated

     - name: Update other packages
       dnf:
         name:
           - bash
           - glibc
           - systemd
           - coreutils
         state: latest
       register: other_updated

     - name: Reboot if kernel was updated
       reboot:
       when: kernel_updated.changed
```