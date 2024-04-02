# Linux Top Command에 대한 이해
#### 해당 문서는 책 'DevOps와 SE를 위한 리눅스 커널 이야기' 를 읽고 정리한 내용 입니다.
## OverView

저는 container 환경을 좋아합니다. DockerFile만 코드에 갖고있다면 어디서든 빠르게 배포하고 어떠한 구조로 Application이 동작하는지 간편하게 확인할 수 있기 때문입니다. 

심지어는 Private Image Repository 건 , Public Image Repository 건 상관 없이 Docker Image만 Repository에 push 되어 있다면, 간단한 docker 명령어로 배포할수 있다는점도 너무 좋습니다. (프로세서 종류는.. 논외입니다..)

허나 이렇게 간편하고 좋은 Docker container 또한 , 알고보면 linux 프로세스입니다.

linux cgroup과 namespace를 기반으로 독립적이게 구성되어진 것이기에, container 또한 호스트 linux에 실행중인 프로세스에 불과합니다.

따라서 container 조차 linux에 완전히 격리된 상태가 아니기에,, 서버를 관리하는 입장에선 프로세스의 상태를 확인하고 상황에 따라 조치를 취하는것이 정말 중요합니다.

리눅스 프로세스를 감시하는 방법은 다양하지만, 시스템 상태를 빠르고 간편하게 파악할 수 있는 linux ```top``` command를 정리해 보았습니다.

## top 구경하기
일단 top 명령어를 수행해서 확인해 봅니다.



