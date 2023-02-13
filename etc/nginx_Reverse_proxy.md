# nginx_Reverse_proxy
해당 문서는 nginx reverse proxy 기능을 통해 docker container와 연결하는 방안에 대해 기술합니다.

## 0. nginx 설치
- ubuntu
```bash
# apt repository 에 설치하고자 하는 nginx 버전 추가 
# ubuntu 버전 (18.04: bionic, 16.04: xenial)
$ sudo touch /etc/apt/sources.list.d/nginx.list
$ echo "deb http://nginx.org/packages/ubuntu/ bionic nginx" | sudo tee -a /etc/apt/sources.list.d/nginx.list
$ echo "deb-src http://nginx.org/packages/ubuntu/ bionic nginx"| sudo tee -a /etc/apt/sources.list.d/nginx.list

# 인증 키 등록
$ wget http://nginx.org/keys/nginx_signing.key
$ sudo apt-key add nginx_signing.key

# 저장소 업데이트
$ sudo apt-get update

# nginx 설치
$ sudo apt-get install nginx
```

- centos
```bash
# nginx 공식 저장소 추가
sudo vim /etc/yum.repos.d/nginx.repo
# 파일에 아래 내용 추가
  [nginx]
  name=nginx repo
  baseurl=http://nginx.org/packages/centos/$releasever/$basearch/
  gpgcheck=0
  enabled=1

# nginx 설치
sudo yum install nginx
```

## 1. nginx.conf 파일 작성
nginx.conf 파일은 아래 경로에 위치합니다.

```bash
$ cd /etc/nginx/conf.d
$ ls
default.conf
```

nginx는 default.conf파일이 기본적으로 적용됩니다.

default.conf 파일에 reverse proxy 구성을 설정합니다.

### 1.1  server 구문
먼저 server 구문을 정의합니다.

server_name 필드에는 다음과 같이 정의합니다.
- www.jjsair0412.nonssl.com:80/ location으로 listen 중 ....
- 해당 경로로 요청 들어오면 , proxy_pass에 정의된 곳으로 reverse proxy 검.

```conf
server {
    listen 80;
    server_name www.jjsair0412.nonssl.com;
    
    location / {
        proxy_set_header HOST $host;
        proxy_pass         http://non-ssl-homepage$request_uri;
        proxy_redirect off;
    }
}

```

### 1.2 upstream 구문
upstream 구문에서 . server구문 proxy_pass 필드에 변수로 작성해 두었던 부분을 정의 합니다.
- non-ssl-homepage -> 127.0.0.1:1111 로 변경
```conf
upstream non-ssl-homepage {
    server 127.0.0.1:1111;
}
```

## 2. 전체 default.conf파일 정의

전체 conf 파일 코드는 다음과 같으며 , 서버 구성에 따라 유연하게 변화 합니다.
- 443 -> ssl 인증 등 ..

아래 구성처럼 구성하면 , 
- www.jjsair0412.nonssl.com:80 으로 요청 시 ( nginx 설치 서버 ) -> 127.0.0.1:1111 으로 전달
- www.jjsair0412.ssl.com:443 으로 요청 시 ( nginx 설치 서버 ) -> 127.0.0.1:2222 으로 전달

```conf
$ cat default.conf
upstream ssl-homepage {
    server 127.0.0.1:2222;
}

upstream non-ssl-homepage {
    server 127.0.0.1:1111;
}


server {
    listen 80;
    server_name www.jjsair0412.nonssl.com;
    
    location / {
        proxy_set_header HOST $host;
        proxy_pass         http://non-ssl-homepage$request_uri;
        proxy_redirect off;
    }
}


server {
    server_name www.jjsair0412.ssl.com;
    listen 443 ssl; # 443 -> ssl 인증해야됨
    
    location / {
        proxy_set_header HOST $host;
        proxy_pass         http://ssl-homepage$request_uri;
        proxy_redirect off;
    }

    # ssl 인증서 먹이는 부분
    ssl_certificate {.pem-key-path}
    ssl_certificate_key  {.pem-key-path}
    include {ssl-nginx.conf-path}
    ssl_dhparam {.pem-key-path}

}
```

## 2. nginx restart
nginx service를 restart 합니다.
```bash
$ sudo systemctl restart nginx
```