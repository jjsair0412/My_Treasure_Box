# offline ubuntu package install
- 해당 문서는 ubuntu 및 centos ( linux 기반 os ) 인 서버 & 인터넷 연결이 되지 않는 폐쇄망 환경에서  패키지를 설치하는 방법을 설명합니다.
- ubuntu 20.04 버전의 os 환경에서 테스트한 결과입니다.
## 1. install package 
- 먼저 인터넷이 연결된 환경에서 작업합니다.
- 설치 테스트는 nfs-common 패키지를통해 테스트 합니다.
### 1.1 대상 패키지 설치
```
# 의존성이 모두 포함된 해당 패키지의 데비안을 설치합니다.
$ sudo apt-get install --download-only <설치대상> -d -o=dir::cache=<데비안파일들_설치할_경로>

# usecase
$ sudo apt-get install --download-only nfs-common -d -o=dir::cache=/home/koo/nfs-common
```
### 1.2 패키지 설치 확인
- archives 폴더안에 deb파일들이 위치하게 됩니다.
```
$ ls
keyutils_1.6-6ubuntu1.1_amd64.deb         libnfsidmap2_0.25-5.1ubuntu1_amd64.deb  libtirpc-common_1.2.5-1_all.deb  nfs-common_1%3a1.3.4-2.5ubuntu3.4_amd64.deb  rpcbind_1.2.5-8_amd64.deb
libevent-2.1-7_2.1.11-stable-1_amd64.deb  libtirpc3_1.2.5-1_amd64.deb             lock                             partial
```
## 2. 패키지 이동
- offline 환경인 서버로 usb나 scp , sftp를통해 deb파일들을 이동시킵니다.
```
$ scp * ubuntu@10.xxx.xxx.xxx:/home/ubuntu
```
- 이동한 파일들을 환경에서 설치합니다.
```
$ sudo dpkg --force-all -i *.deb
```

