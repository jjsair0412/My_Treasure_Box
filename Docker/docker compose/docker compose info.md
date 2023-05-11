# Docker compose 
[docker compose git hub](https://github.com/docker/compose/releases/tag/v2.6.1)
## Docker compose란 ?
- 기존에 docker 컨테이너를 실행했던 run 명령어를 하나의 yaml파일로 정의해놓고 , 해당 이미지 컨테이너가 필요할 때마다 docker-compose 명령어로 컨테이너를 실행하게끔 하는 것 입니다.
- 아래의 docker 명령어와 , docker-compose.yaml은 동일한 결과값을 가집니다.

***docker command***
```
docker network create wordpress_net  # docker 네트워크 형성

docker \
run \
    --name "db" \
    -v "$(pwd)/db_data:/var/lib/mysql" \
    -e "MYSQL_ROOT_PASSWORD=123456" \
    -e "MYSQL_DATABASE=wordpress" \
    -e "MYSQL_USER=wordpress_user" \
    -e "MYSQL_PASSWORD=123456" \
    --network wordpress_net \
mysql:5.7

docker \
    run \
    --name app \
    -v "$(pwd)/app_data:/var/www/html" \
    -e "WORDPRESS_DB_HOST=db" \
    -e "WORDPRESS_DB_USER=wordpress_user" \
    -e "WORDPRESS_DB_NAME=wordpress" \
    -e "WORDPRESS_DB_PASSWORD=123456" \
    -e "WORDPRESS_DEBUG=1" \
    -p 8080:80 \
    --network wordpress_net \
wordpress:latest
```
***docker-compose.yaml***
```
version: "3.7"

services:
  db:
    image: mysql:5.7
    volumes:
      - ./db_data:/var/lib/mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: wordpress
      MYSQL_USER: wordpress_user
      MYSQL_PASSWORD: 123456
  
  app:
    depends_on: 
      - db
    image: wordpress:latest
    volumes:
      - ./app_data:/var/www/html
    ports:
      - "8080:80"
    restart: always
    environment:
      WORDPRESS_DB_HOST: db:3306
      WORDPRESS_DB_NAME: wordpress
      WORDPRESS_DB_USER: wordpress_user
      WORDPRESS_DB_PASSWORD: 123456
```
## Docker compose 사용법
### Docker compose 설치
- curl로 docker compose 설치파일 가져옵니다.
```
# 예시
$ sudo curl -L "https://github.com/docker/compose/releases/download/설치대상 version/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

# usecase
$ sudo curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
```

- docker compose 권한부여
```
$ sudo chmod +x /usr/local/bin/docker-compose
```
- 설치결과 확인 
```
$ docker-compose --version
docker-compose version 1.29.2, build 5becea4c
```
### docker compose 명령어
- 기본적으로 docker 명령어와 동일하다.
- 컨테이너 최초 실행
```
$ docker-compose up -d
```
- 컨테이너 중지 및 종료 ( stop & kill )
```
$ docker-compose down
```
- 작동중인 프로세스의 상태를 확인
```
$ docker-compose ps
```
[추가 명령어 list 블로그](https://yoonhoji.tistory.com/101)

### docker compose파일 뜯어보기
- 일단 compose.yml 파일을 작성할 때 , 먼저 compose파일의 version을 작성해주어야 합니다.
```
...
version: "3.7"
...
```
***mysql 컨테이너 compose파일 뜯어보기***
- service라는 지시어가 들어가게 되고 , 그 안에는 컨테이너 정보가 들어갑니다.
```
...
services:
  db: # 컨테이너 이름 
    image: mysql:5.7 # 사용할 image
...
```
- 컨테이너 내부 데이터를 저장할 volume을 설정합니다.
```
...
    volumes:
      - ./db_data:/var/lib/mysql # db_data와 컨테이너 내부 /var/lib/mysql을 연결
...
```
- 컨테이너내부 env정보는 environment 안에 들어가게 됩니다.
```
...
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: wordpress
      MYSQL_USER: wordpress_user
      MYSQL_PASSWORD: 123456
...
```
***wordpress compose파일 뜯어보기***
- wordpress는 mysql 컨테이너가 먼저 생성된 이후 생성되어야 합니다.
- 이럴 때 , 해당 컨테이너보다 먼저 생성될 컨테이너를 지정할 수 있는데 , depends_on을 사용합니다.
```
...
  app: # wordpress 컨테이너 이름
    depends_on: 
      - db # db컨테이너가 먼저 만들어진 후 wordpress 컨테이너가 생성된다.
...
```
- volume을통해 외부저장소에 데이터를 저장합니다.
```
...
    volumes:
      - ./app_data:/var/www/html # app_data와 컨테이너내부 /var/www/html을 연결합니다.
...
```
- 컨테이너 포트포워딩을 하기 위해 ports 옵션을 사용합니다.
```
...
    ports:
      - "8080:80"
...
```
- 환경변수 environment로 설정합니다.
```
...
    environment:
      WORDPRESS_DB_HOST: db:3306
      WORDPRESS_DB_NAME: wordpress
      WORDPRESS_DB_USER: wordpress_user
      WORDPRESS_DB_PASSWORD: 123456
...
```
- docker compose는 docker처럼 docker network를 설정할 필요 가 없습니다.
- 그냥 compose up 명령어를 사용하면 , network설정이 자동으로 설정됩니다.

### docker compose 외부 .env 사용하기
- docker compose 파일에서 , 가변값들을 외부에 지정하면서 올릴 수 있습니다.
- .env파일을 docker-compose와 동일 path에 생성한 뒤 , 아래처럼 지정합니다.

```.env
$ cat .env
PASSWD=pw1234
exportPort=1111
```

```yaml
version: "3.7"

services:
  postgres:
    image: postgres:13
    container_name: my-pg
    ports:
      - '${exportPort}:5432'
    environment:
      - POSTGRES_PASSWORD=${PASSWD}
```

```bash
$ docker-compose  --project-name pg -f .\docker-compose.yml up
```

- exportPort가 .env에 지정한 값인 1111로 변경
- PASSWD가 .env에 지정한 값인 pw1234로 변경

- 추가로 , --env-file 옵션을 이용해서 up 할 때 .특정 env를 지정해 줄 수 있습니다.
```bash
$ docker-compose  --project-name pg -f .\docker-compose.yml --env-file ./.env up
```