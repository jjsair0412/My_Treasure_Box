# Message_Queue
- [Yoga_전체환경_RabbitMQ_설치방안_docs](https://docs.openstack.org/install-guide/environment-messaging.html)
- [Yoga_ubuntu_RabbitMQ_설치방안_docs](https://docs.openstack.org/install-guide/environment-messaging-ubuntu.html)


## OverView
OpenStack은 Message Queue를 사용해서 , 서비스간 작업상태 및 상태정보를 조정하게 됩니다. **메세지 큐 서비스는 일반적으로 controller 노드에서 같이 실행**됩니다.

OpenStack은 [RabbitMQ](https://www.rabbitmq.com/) , [Qpid](https://qpid.apache.org/) , [ZeroMQ](http://zeromq.org/)를 포함해서 많은 메시지 큐를 지원합니다.
- 그러나 , OpenStack을 패키징할때 대부분은 특정 메세지 큐를 지원합니다.

해당 문서는 RabbitMQ를 대상으로 설치합니다.


## ENV
RabbitMQ는 Controller 노드에서 실행되게끔 구성하였습니다.

## RabbitMQ 설치
### 1. 패키지 설치
apt 명령어로 rabbitmq-server를 설치합니다.
```bash
$ sudo apt-get install rabbitmq-server
```

### 2. 사용자 추가
openstack 사용자를 RabbitMQ에 추가합니다.
- 이때 , RABBIT_PASS 비밀번호를 적절하게 조절합니다.
    - 테스트 환경이기 때문에 , ```1234``` 로 조정합니다.
```bash
$ rabbitmqctl add_user openstack RABBIT_PASS

# 실 사용 명령어
$ rabbitmqctl add_user openstack 1234
Adding user "openstack" ...
```

### 3. 사용자 권한 수정
추가한 openstack 사용자의 권한을 configuration, write, read 로 부여합니다.
```bash
$ rabbitmqctl set_permissions openstack ".*" ".*" ".*"
Setting permissions for user "openstack" in vhost "/" ...
```