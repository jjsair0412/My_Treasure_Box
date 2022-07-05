
# Docker private registry load & push shell script
## 1. docker private registry일 경우
- 인터넷이 연결되지 않는 폐쇄망 구성에서 image를 사용할 때 , private registry로 image들을 push하는 쉘 스크립트 파일입니다.
- 해당 스크립트를 실행할 때 , image파일 경로를 추가하여 실행해야 합니다.
- docker save명령어로 tar파일을 꺼내줄 때 , 아래와 같은 형식을 지켜주어야 합니다. 
```
# example
$ docker save -o filename.tar <repo>:<tag>

$ docker save -o ubutu-18.04.tar ubuntu:18.04
```
- 위의 형식을 지키지 않는다면, image이름과 tag값을 가져오지 않습니다.
  -  reg : private registry 주소
  -  dir : image 경로 위치
```
$ cat docker-ltp.sh
#! /bin/bash
reg="10.xxx.xxx.xxx:5000"
dir="$1"

for f in $dir/*.tar; do
  image_name=$(cat $f | docker load | awk '{print $3}')

  docker tag $image_name $reg/$image_name
  docker push $reg/$image_name
  docker rmi $image_name
done


# 스크립트 실행의 예 - /home/centos/harbor 내부 image들을 registry로 push
$ sh docker-ltp.sh /home/centos/harbor/
```
## 2. harbor일 경우
- private registry를 harbor로 사용할 경우 , 아래 스크립트를 사용합니다.
```
$ cat sc.sh
#! /bin/bash
reg="harbor.xxx.xxx.xyz" # harbor ingress domain 주소
dir="$1"
proj="$2" # harbor 내부 프로젝트 이름
namespace="harbor" # harbor namespace


for f in $dir/*.tar; do
  image_name=$(cat $f | docker load | awk '{print $3}')

  docker tag $image_name $reg/$proj/$image_name
  docker push $reg/$proj/$image_name
  docker rmi $image_name
done


# 사용 예시
$ sh sc.sh <image_위치> <project_이름>

# 사용 방안
$ sh sc.sh /home/centos/solutions/gitlab gitlab
```