# Docker private registry load & push shell script
- 인터넷이 연결되지 않는 폐쇄망 구성에서 image를 사용할 때 , priavte registry로 image들을 push하는 쉘 스크립트 파일입니다.
- 해당 스크립트를 실행할 때 , image파일 경로를 추가하여 실행해야 합니다.
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