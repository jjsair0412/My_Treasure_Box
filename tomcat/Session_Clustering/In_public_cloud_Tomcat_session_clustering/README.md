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

## 2. 서버 구성
tomcat 인스턴스가 총 3대며 , 각 인스턴스들은 다른 EC2 VM에 위치합니다.

당연하겠지만 , 여기에 작성되는 ip 주소로 각 ec2들은 열린 포트로 통신이 가능해야 합니다.
## 3. server.xml 구성
각 요소들에대한 상세 설명은 주석으로 대신합니다.
- 전체 server.xml은 아래 링크를 타고가서 확인 가능
- [tomcat server.xml 설정](./cluster/)
```xml
<!-- 1번 서버 설정 -->
<Engine name="Catalina" defaultHost="localhost" jvmRoute="tomcat1">

<Cluster 
        channelSendOptions="8" 
        channelStartOptions="3" 
        className="org.apache.catalina.ha.tcp.SimpleTcpCluster">
    <Manager 
        className="org.apache.catalina.ha.session.DeltaManager" 
        expireSessionsOnShutdown="false" 
        notifyListenersOnReplication="true"
    />
    <Channel className="org.apache.catalina.tribes.group.GroupChannel">
        <!--  
            <Sender> 에는 세션 클러스터링 시 사용되는 멤버간 데이터 전송에 대한 구성이 들어감

            해당 예제에는 ReplicationTransmitter 클래스가 데이터 전송을 처리하는데, 그안에서 PooledParallelSender 클래스를 사용합니다.
        
            org.apache.catalina.tribes.transport.nio.PooledParallelSender는 NIO(Non-Blocking I/O) 기반의 데이터 전송을 수행하는 클래스입니다.
            PooledParallelSender는 여러 개의 동시 전송 작업을 처리하기 위해 스레드 풀을 사용합니다. 
        -->
        <Sender className="org.apache.catalina.tribes.transport.ReplicationTransmitter">
            <Transport className="org.apache.catalina.tribes.transport.nio.PooledParallelSender" />
        </Sender>
        <!--
            <Receiver> 에는 자기자신의 정보가 들어감. 
            어떤 포트로 세션 클러스터링에 사용되는 패킷을 수신할 지 설정합니다.

            해당 예제에선, 3100번 포트로 수신합니다.
         -->
        <Receiver 
            address="1번 Tomcat Server static IP" 
            autoBind="0" 
            className="org.apache.catalina.tribes.transport.nio.NioReceiver" 
            maxThreads="6" 
            port="3100" 
            selectorTimeout="5000"
        /> <!-- server1 information -->
        <!-- <Interceptor className="com.dm.tomcat.interceptor.DisableMulticastInterceptor" /> -->
        <Interceptor className="org.apache.catalina.tribes.group.interceptors.TcpPingInterceptor" staticOnly="true"/>
        <Interceptor className="org.apache.catalina.tribes.group.interceptors.TcpFailureDetector" />
        <!-- 
            StaticMembershipInterceptor 옵션은 정적 멤버십을 지원하는 클래스입니다.

            세션 클러스터에 참여하는 톰켓 인스턴스가 , 런타임에 동적으로 추가되지 않고 정적으로 추가할 수 있습니다.
        -->
        <Interceptor className="org.apache.catalina.tribes.group.interceptors.StaticMembershipInterceptor">
            <!-- 
                <Member> 에는 자기 자신을 제외한 세션 클러스터링에 참여할 나머지 모든 톰켓 ec2 vm들의 정보가 들어갑니다.

                여기엔 각 인스턴스들이 어떤 포트로 <Receiver> 가 열려 있는지 작성해야 하는데, 해당 예제에선 아래 정보로 열려있습니다.
                1번 Tomcat : 3100
                2번 Tomcat : 3200
                3번 Tomcat : 3300

                또한 uniqueId 옵션에는 각 인스턴스들을 식별할 고유값이 들어가야 하며 , 해당 예제에선 아래 정보로 구성되어 있습니다.
                1번 Tomcat : {0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,2}
                2번 Tomcat : {0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,3}
                3번 Tomcat : {0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,4}
             -->
            <Member 
                className="org.apache.catalina.tribes.membership.StaticMember" 
                port="3200" 
                host="2번 Tomcat Server static IP" 
                uniqueId="{0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,3}" 
            />
            <Member 
                className="org.apache.catalina.tribes.membership.StaticMember" 
                port="3300" 
                host="3번 Tomcat Server static IP" 
                uniqueId="{0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,4}" 
            />
        </Interceptor>
        <Interceptor className="org.apache.catalina.tribes.group.interceptors.MessageDispatchInterceptor" />
    </Channel>
    <!-- 
        org.apache.catalina.ha.tcp.ReplicationValve 으로 세션클러스터링 활성화.
        filter 옵션으로 Valve에 의해 처리될 요청 URL 패턴을 정의 , 여기에 작성된 정규 표현식에 일치하는 요청만이 세션 
        복제에 참여함.

        이렇게 하는 이유는 , 모든 요청을 전부다 세션 복제에 참여하게끔 구성하면 , 성능 저하가 발생할 수 있기에 일부 정적 데이터는 제외하고
        설정함.
     -->
    <Valve 
        className="org.apache.catalina.ha.tcp.ReplicationValve" 
        filter=".*\.gif;.*\.js;.*\.jpg;.*\.png;.*\.htm;.*\.html;.*\.css;.*\.txt;" 
    />
    <ClusterListener className="org.apache.catalina.ha.session.ClusterSessionListener" />
</Cluster>
```


