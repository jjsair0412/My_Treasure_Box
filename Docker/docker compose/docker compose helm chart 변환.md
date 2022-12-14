# Docker compose -> k8s helm chart 
- 해당 문서는 docker compose.yml파일을 k8s의 yaml파일로 변환 후
변환된 k8s yaml파일을 helm chart로 정의하는 과정에 대해 기술합니다.
- 테스트 환경은 ubuntu 20.04 os를 사용했습니다.
- helmify는 helm version v3.6.0 이상만 지원합니다.
- 두가지 과정으로 나뉘게 됩니다.
1. docker compose.yml -> k8s yaml
2. k8s yaml -> helm chart convert
- 각 과정에 필요한 솔루션은 총 두가지입니다.
1. [Kompose](https://kompose.io/)
2. [Helmify](https://github.com/arttor/helmify)
## 1. docker compose.yml -> k8s yaml
### 1.1 kompose 설치
- [kompose 공식 github](https://github.com/kubernetes/kompose)
```
# wget으로 kompose 바이너리파일 가지고 옵니다.
wget https://github.com/kubernetes/kompose/releases/download/v1.26.1/kompose_1.26.1_amd64.deb # Replace 1.26.1 with latest tag

# apt install 진행
sudo apt install ./kompose_1.26.1_amd64.deb

# 설치 결과 확인
$ kompose version
1.26.1 (a9d05d509)
```
### 1.2 yaml 변환
- 테스트 시 사용한 docker compose.yml파일은 아래와 같습니다.	
- mysql을 사용하는 기본적인 wordpress 컨테이너 입니다. 
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
- kompose명령어를 통해 k8s yaml파일들로 변환시켜 줍니다.
```
$ kompose convert -f <docker-compose.yaml파일 이름>

# usecase
$ kompose convert -f docker-compose.yaml

$ ls -al
total 32
-rw-r--r-- 1 ubuntu ubuntu  243 Jul 19 07:27 app-claim0-persistentvolumeclaim.yaml
-rw-r--r-- 1 ubuntu ubuntu 1263 Jul 19 07:27 app-deployment.yaml
-rw-r--r-- 1 ubuntu ubuntu  365 Jul 19 07:27 app-service.yaml
-rw-r--r-- 1 ubuntu ubuntu  241 Jul 19 07:27 db-claim0-persistentvolumeclaim.yaml
-rw-r--r-- 1 ubuntu ubuntu 1186 Jul 19 07:27 db-deployment.yaml
```
- 만약 helm chart tree구조로 패키징하여 helm 명령어로 관리하고 싶다면 , -c 옵션을 추가합니다.
```
$ kompose convert -f docker-compose.yaml -c

$ tree
.
├── docker-compose
│   ├── Chart.yaml
│   ├── README.md
│   └── templates
│       ├── app-claim0-persistentvolumeclaim.yaml
│       ├── app-deployment.yaml
│       ├── app-service.yaml
│       ├── db-claim0-persistentvolumeclaim.yaml
│       └── db-deployment.yaml
└── docker-compose.yaml
```
- helm install 이후 결과와 비교하기 위해 , yaml파일들을 apply 시켜 결과값을 확인합니다.
```
$ kubectl get all -n default
NAME                       READY   STATUS    RESTARTS   AGE
pod/app-76899b67d4-khbqr   1/1     Running   0          29s
pod/db-869bc4d48d-hbzh2    1/1     Running   0          13s

NAME                 TYPE        CLUSTER-IP    EXTERNAL-IP   PORT(S)    AGE
service/app          ClusterIP   10.43.64.36   <none>        8080/TCP   24s
service/kubernetes   ClusterIP   10.43.0.1     <none>        443/TCP    90s

NAME                  READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/app   1/1     1            1           29s
deployment.apps/db    1/1     1            1           13s

NAME                             DESIRED   CURRENT   READY   AGE
replicaset.apps/app-76899b67d4   1         1         1       29s
replicaset.apps/db-869bc4d48d    1         1         1       13s

```
## 2. k8s yaml -> helm chart
- Helmify를 통해 k8s yaml파일을 helm chart로 구성합니다.
- kompose의 -c옵션은 , values.yaml파일이 구성되지 않아 yaml의 속성들을 중앙에서 관리하기가 불가능합니다.
- 그러나 helmify는 values.yaml속성이 생성되기에 중앙 관리가 가능해집니다.
### 2.1 Helmify 설치
```
# 설치대상 버전의 helmify의 tar파일을 wget으로 가져옵니다.
$ wget https://github.com/arttor/helmify/releases/download/v0.3.8/helmify_0.3.8_Linux_64-bit.tar.gz
$ tar xvfz helmify_0.3.8_Linux_64-bit.tar.gz
$mv helmify /usr/local/bin/.


# 설치 확인
$ helmify -h
Helmify parses kubernetes resources from std.in and converts it to a Helm chart.

Example 1: 'kustomize build <kustomize_dir> | helmify mychart'
  - will create 'mychart' directory with Helm chart from kustomize output.

Example 2: 'cat my-app.yaml | helmify mychart'
  - will create 'mychart' directory with Helm chart from yaml file.

Example 3: 'awk 'FNR==1 && NR!=1  {print "---"}{print}' /my_directory/*.yaml | helmify mychart'
  - will create 'mychart' directory with Helm chart from all yaml files in my_directory directory.

... 중략
```
### 2.2 k8s yaml -> helm chart convert
- Helmify 명령어를 통해 변환합니다.
- 변환할 yaml파일이 존재하는 디렉터리 위치에서 변환 명령어를 수행합니다.
```
$ awk 'FNR==1 && NR!=1 {print "---"}{print}' *.yaml | helmify mychart

# 변환결과 확인
$ tree
.
└── mychart
    ├── Chart.yaml
    ├── templates
    │   ├── _helpers.tpl
    │   ├── app-claim0.yaml
    │   ├── app.yaml
    │   ├── claim0.yaml
    │   └── deployment.yaml
    └── values.yaml
```
- values.yaml파일이 생성되고 , 컨테이너 설정값들이 중앙에서 관리 되는 것을 확인할 수 있습니다.
```
$ cat values.yaml
app:
  app:
    image:
      repository: wordpress
      tag: latest
  ports:
  - name: "8080"
    port: 8080
    targetPort: 80
  replicas: 1
  type: ClusterIP
db:
  db:
    image:
      repository: mysql
      tag: "5.7"
  replicas: 1
pvc:
  appClaim0:
    storageRequest: 100Mi
  claim0:
    storageRequest: 100Mi
```
- helm install 명령어로 반영되는지 확인합니다.
```
$ helm install myapp .

# kubectl apply -f 한 결과와 동일한 결과가 출력되는것을 확인할 수 있습니다.
$ kubectl get all
NAME                                     READY   STATUS    RESTARTS   AGE
pod/myapp-mychart-app-654758f7c6-g2bcs   1/1     Running   0          29s
pod/myapp-mychart-db-8655fb55c5-wd96g    1/1     Running   0          29s

NAME                        TYPE        CLUSTER-IP     EXTERNAL-IP   PORT(S)    AGE
service/kubernetes          ClusterIP   10.43.0.1      <none>        443/TCP    13d
service/myapp-mychart-app   ClusterIP   10.43.123.82   <none>        8080/TCP   29s

NAME                                READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/myapp-mychart-app   1/1     1            1           29s
deployment.apps/myapp-mychart-db    1/1     1            1           29s

NAME                                           DESIRED   CURRENT   READY   AGE
replicaset.apps/myapp-mychart-app-654758f7c6   1         1         1       29s
replicaset.apps/myapp-mychart-db-8655fb55c5    1         1         1       29s





$ helm list -A | grep myapp
myapp                           default         1               2022-07-19 07:52:26.431356069 +0000 UTC deployed        mychart-0.1.0                                   0.1.0
```