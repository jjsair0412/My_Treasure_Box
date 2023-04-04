# apache Tomcat session clustering in docker , web and was 분리
## Precondition
해당 문서는 tomcat multi cluster 환경에서 session을 공유하는 방법에 대해 기술합니다.

tomcat session clustering의 자세한 설명은 , 아래의 링크를 참고 합니다.
해당 문서는 , tomcat session clustering이 되어 있는 상태에서 web과 was , apache와 tomcat을 분리시킨 환경을 구현한 예제 입니다.
- [tomcat session clustering 이론](https://github.com/jjsair0412/kubernetes_info/tree/main/etc/Tomcat_Session_Clustering)

기본적인 설정은 위의 링크 예제를 기반으로 하였으며 , README에서는 추가된 정보만 기술합니다.

httpd의 VirtualHost를 이용하여 reverse proxy를 구성한 뒤 , 백엔드 haproxy로 연결하는 형태로 구현하였습니다. 

## 1. architecture
![image_1][image_1]

[image_1]:./images/image_1.png

## 2. httpd.conf 구성
httpd conf파일에서 Reverse Proxy mode를 enable 해야 합니다.

mod_proxy 모듈이 필요한데 , httpd를 설치하면 default option으로 설치되어있기 때문에 , conf파일에서 주석만 해제하면 됩니다.

```conf
# 주석 해제
#LoadModule proxy_module modules/mod_proxy.so
#LoadModule proxy_http_module modules/mod_proxy_http.so
```

그리고 httpd.conf파일을 수정합니다.

맨 아래에 VirtualHost 정보를 기입합니다.

www.test.com:80번 포트로 httpd에 요청이 들어오면 , backend의 was-portal 이름을 가진 nginx container의 8112번 포트로 요청을 전달해주는 옵션입니다.
```conf
<VirtualHost *:80>
  ServerName www.test.com
  ProxyPreserveHost On
  ProxyPass / http://was-portal:8112/ 
  ProxyPassReverse / http://was-portal:8112/ 
</VirtualHost>
```

## 3. docker-compose.yml 생성
해당 예제는 docker-compose로 구현했기 때문에 , 로컬의 httpd.conf파일과 default.conf 파일을 mount 시켜서 구현 합니다.

```yml
version: '3.7'
services:
  web_server:
    user: "root"
    image: httpd:alpine
    restart: always
    ports:
      - 80:80
    volumes:
      - ./cluster/apache/httpd.conf:/usr/local/apache2/conf/httpd.conf
  was-portal:
    container_name: was-portal
    image: nginx  
    ports:
      - 8112:80
    volumes: 
      - ./cluster/default.conf:/etc/nginx/conf.d/default.conf
  tomcat1:
    image: tomcat:7.0.94-jre7-alpine
    container_name: tomcat1
    volumes:
      - ./cluster/server.xml:/usr/local/tomcat/conf/server.xml
      - ./cluster/ROOT:/usr/local/tomcat/webapps/ROOT
      - ./cluster/tomcat-users.xml:/usr/local/tomcat/conf/tomcat-users.xml
  tomcat2:
    image: tomcat:7.0.94-jre7-alpine
    container_name: tomcat2
    volumes:
      - ./cluster/server.xml:/usr/local/tomcat/conf/server.xml
      - ./cluster/ROOT:/usr/local/tomcat/webapps/ROOT
      - ./cluster/tomcat-users.xml:/usr/local/tomcat/conf/tomcat-users.xml
  tomcat3:
    image: tomcat:7.0.94-jre7-alpine
    container_name: tomcat3
    volumes:
      - ./cluster/server.xml:/usr/local/tomcat/conf/server.xml
      - ./cluster/ROOT:/usr/local/tomcat/webapps/ROOT
      - ./cluster/tomcat-users.xml:/usr/local/tomcat/conf/tomcat-users.xml 
```

## 5. etc/host 파일 수정
www.test.com으로 요청을 전달해야 하기 때문에 . etc/host파일을 수정합니다.

```h
# etc/host파일 수정
127.0.0.1 www.test.com
```

## 4. docker-compose up
docker-compose 명령어를 입력하여 결과를 확인합니다.
```bash
$ docker-compose up
```

book name에 다른 값이 들어가서 instance가 변경 되더라도 , 세션이 유지되는것을 확인할 수 있습니다.

![image_2][image_2]

[image_2]:./images/image_2.png