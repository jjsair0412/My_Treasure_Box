# Tomcat log 순환 설정 방안
해당 문서는 Tomcat의 catalina.out 로그를 날짜별로 순환하는 방안에 대해 기술합니다.

## prerequisite
logrotate를 사용하기 때문에 , linux 서버여야만 합니다.

## 설정 환경
- tomcat version 9
- tomcat catalina.out path : /data/logs/instance_jjs/catalina.out 
- tomcat 설정 user 명 : tomcat

## 1. logrotate 파일 생성
/etc/logrotate.d 디렉터리 안에 파일을 생성합니다.

```bash
cd /etc/logrotate.d
```

tomcat의 logrotate 설정 파일을 생성합니다.
```bash
vi tomcat_logrotate

/data/logs/instance_jjs/catalina.out {
    su tomcat tomcat
    copytruncate
    daily
    missingok
    notifempty
    compress
    size 50K
    rotate 50
}
```

각 옵션에 대한 설명은 다음과 같습니다.
- su tomcat tomcat : catalina.out 파일의 권한이 tomcat:tomcat 이기에 권한을 맞춰줌
- copytruncate : 기존 파일을 백업하여 다른 파일로 이동 후 기존 파일은 제거
    - tomcat의 로그를 날짜별로 순환시켜야 하기 때문에 해당 옵션이 꼭 필요합니다.
- daily : tomcat의 catalina.out 로그 파일을 날짜별로 순환 
- missingok : 로그파일이 없더라도 에러를 발생시키지 않음
- notifempty : 파일 내용이 없다면 새로운 로그 파일을 생성하지 않음
- compress : 지나간 로그 파일을 gzip으로 압축
- size : 로그파일의 크기가 50k를 넘으면 순환. 단위 K, M 사용이 가능
- rotate : 로그 파일은 50개만큼 저장된 다음 제거되거나 메일로 보내질 수 있음


## 2. logrotate 수행
아래 명령어로 logrotate를 반영합니다.

```bash
# 강제 반영
logrotate -f /etc/logrotate.d/tomcat_logrotate

# 디버그 모드
logrotate -d /etc/logrotate.d/tomcat_logrotate

# 실행과정 화면 출력
logrotate -v /etc/logrotate.d/tomcat_logrotate
```