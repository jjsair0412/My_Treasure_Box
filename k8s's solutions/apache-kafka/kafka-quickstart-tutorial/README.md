# kafka-quickstart-tutorial
## **precondition**
해당 문서는 apache kafka 기본 계념을 잡기 위해 docker나 k8s 없이 그냥 설치하여 , producer, consumer , topic 등을 실습하는 튜토리얼 문서 입니다.

apache kafka의 공식 download 사이트는 다음과 같습니다.
- https://kafka.apache.org/downloads

해당 문서에서 사용된 kafka version은 **2.13.0** 입니다.

튜토리얼은 kafka 공식 문서와 블로그를 참조하였습니다.
- https://kafka.apache.org/quickstart
- https://soyoung-new-challenge.tistory.com/61

***kafka를 실행하기 위해선 , local java version이 꼭 8 이상이여야만 합니다 !***

## 1. kafka 설치
### 1.1 kafka 압축 파일 및 해제
wget으로 apache kafka를 받아온 뒤 tar파일을 압축 해제 합니다..

```bash
$wget https://downloads.apache.org/kafka/3.4.0/kafka_2.13-3.4.0.tgz

$tar -xzf kafka_2.13-3.4.0.tgz 

$ls
kafka_2.13-3.4.0  kafka_2.13-3.4.0.tgz
```

## 2. kafka envirement 설정
apache kafka는 실행하기 위해서 **Zookeeper**나 **KRaft**를 꼭 함께 사용해야만 합니다.

해당 튜토리얼에선 두가지 모두 정리하지만 , 실제 실습할땐 둘중 하나만 사용해야만 합니다. !

Zookeeper나 KRaft 모두 
### 2.1 Zookeeper 설정
