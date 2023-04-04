# In_public_cloud_Tomcat_session_clustering - 작성중 .. 테스트 완료되면 작성 완료할 예정
## Precondition
해당 문서는 public cloud 환경에서 Tomcat Session Clustering을 하는 방안에 대해 기술한 문서입니다.

- **Tomcat 인스턴스들 끼리 세션을 공유할 redis같은 친구를 두는것이 권장됩니다.**
- 그러나 서버 구성에 있어서 부득이하게 redis를 사용하지 못한다면 , 해당 문서 방안을 따라야 합니다.

## 1. Tomcat Session Clustering의 방식
기본적으로 Tomcat은 Session을 공유하기 위해 multicast를 사용하여 같은 톰켓 맴버들끼리 세션을 공유하게 됩니다.

만약 public cloud환경이 아니라면 , server.xml과 web인 apache에서 아래처럼 한줄씩만 추가해주면 됩니다.
```xml
<!-- server.xml -->
<Cluster className="org.apache.catalina.ha.tcp.SimpleTcpCluster"/>

<!-- web.xml -->
<distributable/>
```

그러나 public cloud에선 네트워크 성능을 위해서 Broadcast 및 multicast를 지원하지 않기 때문에, , 문제가 생깁니다.

Tomcat 내부 인스턴스들끼리 member join이 되었다 하더라도 , multicast로 세션을 공유하지 못하기에 정적 IP로 멤버를 추가시켜주어야 합니다.

## 2. 서버 구성도

## 3. server.xml 구성


### 3.1 1번 vm
