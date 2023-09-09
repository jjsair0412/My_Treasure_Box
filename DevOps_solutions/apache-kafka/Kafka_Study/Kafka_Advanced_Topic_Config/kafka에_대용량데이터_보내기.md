# kafka에게 대용량 데이터 보내는 방법
kafka는 토픽 메세지당 1MB를 허용하는게 기본값.

>1MB보다 큰 메세지는 비 효율적이기에 늘리는것은 비추. 상황에 따라 다르긴함.

그래도 방법이 없는건 아니다.

## 1. 외부 스토리지를 사용하는 방법
object storage인 Amazon S3나 , 구글 클라우드 스토리지 , HDFS 등의 외부 스토리지에 데이터를 업로드 하고 , 해당 데이터의 레퍼런스(파일 저장한 위치정보 등 메타데이터) 를 kafka한태 보내는 방법

    그니까 , 외부 스토리지에 실제 파일을 업로드하고 , kafka topic에는 업로드한 파일이 어디에 있는지같은 정보만 topic에 쌓아놓으면

    컨슈머는 위치정보만 topic에서 끌고와서 , 고 정보를 가지고 실제 파일을 다운로드한다는것

## 2. kafka 매게변수 수정 방법
kafka broker , consumer , producer의 메개변수를 수정해서 기본값 1MB자체를 바꾸는 방법

### kafka broker에서 바꿔야할 것들
1. ```message.max.bytes```
    - 이 값을 1MB에서 다른 값으로 바꾸면 된다.
    - ex) 10MB

### kafka topic에서 바꿔야할 것들
1. ```max.message.bytes```
    - 이 값을 1MB에서 다른 값으로 바꾸면 된다.
    - ex) 10MB

또한 , server.properties의 값중 아래 값을 바꿔야 함.
1. ```replica.fetch.max.bytes=10485880 ```
    - 10485880 는 10MB를 의미

### consumer에서 바꿔야할 것들
1. ```max.partition.fetch.bytes=10485880```
    - 10485880 는 10MB를 의미

### producer에서 바꿔야할 것들
1. ```max.request.size=10485880```
    - 10485880 는 10MB를 의미