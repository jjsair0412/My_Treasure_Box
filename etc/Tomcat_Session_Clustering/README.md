# apache Tomcat session clustering in docker
## Precondition
해당 문서는 tomcat multi cluster 환경에서 session을 공유하는 방법에 대해 기술합니다.

***기본적으로 tomcat session clustering은 멀티캐스팅을 사용하여 수행합니다.***

### 참고 문서
- [tomcat_docs](https://tomcat.apache.org/tomcat-8.5-doc/cluster-howto.html)
- [삽질저장소 블로그_Load Balancing, Session Clustering](https://bingbingpa.github.io/load-balancing-session-clustering/)

### 구성 환경
![image_1][image_1]

[image_1]:./images/image_1

frontend에서 nginx가 Reverse proxy를 통해서 80포트와 backend tomcat 3대의 8080포트로 포트 포워딩 해 주는 구성입니다.
- tomcat이 docker container로 올라가는 구성이기 때문에 , 8080포트가 겹치더라도 문제가 없지만, docker 또는 k8s 환경이 아니라면 포트가 달라져야만 할 것입니다.

## 1. nginx 구성
nginx 구성은 다음과 같습니다.

80번 포트를 listen하고 있다가 , backend tomcat 3대로 load balancing 해 줍니다.

```conf
upstream backend  {
  server tomcat1:8080;
  server tomcat2:8080;
  server tomcat3:8080;
}

server {
    listen       80;
    server_name  localhost;

    #charset koi8-r;
    #access_log  /var/log/nginx/log/host.access.log  main;

    location / {
	proxy_pass  http://backend;
    }

    #error_page  404              /404.html;
}
```

## 2. tomcat 구성
각 구성에 대한 설정은 다음과같습니다.
- ***tomcat session clustering은 node가 4대 이상일 경우 , 속도가 매우 떨어지기 때문에 redis와 같은 별도의 session 관리 방안이 필요합니다.***

### 2.1 Manager
- 세션 복제 방안을 책임지는 객체 입니다.
    -   **DeltaManager** : 모든 노드에 동일한 세션을 복제합니다. 정보가 변경될 때 마다 복제하기 때문에 노드 개수가 많을 수록 네트워크 트래픽이 높아지고 메모리 소모가 심해집니다.
    - **BackupManager** : Primary Node 와 Backup Node 로 분리 되어 모든 노드에 복제하지 않고 Backup Node 에만 복제합니다.
    - **PersistentManager** : DB 나 파일 시스템을 이용하여 세션을 저장하는데, IO 문제가 생기기 때문에 실시간성이 떨어집니다.

### 2.2 Channel
- Tomcat 내부에서 사용되는 그룹 통신 프레임워크인 Tribes 입니다. 이 요소는 커뮤니케이션 및 멤버십 논리와 관련된 모든 것을 캡슐화합니다.
    - **Membership** : Cluster 안의 노드들을 동적으로 분별하는데 Multicast IP/PORT 를 통해 frequency 에 설정된 간격으로 각 노드들이 UDP packet 을 날려 상태를 확인합니다.
    - **Receiver** : Cluster 로부터 메시지를 수신하는 역활을 하며 blocking 방식 org.apache.catalina.tribes.transport.bio.BioReceiver와 non-blocking방식인 org.apache.catalina.tribes.transport.nio.NioReceiver을 지원합니다.
- **channelSendOptions** : 기본값은 8 이며, 8은 비동기 6은 동기 방식 입니다..

- 각 서비스별로 Membership 의 address 와 Receiver 의 port는 달라야 합니다.
```xml
<?xml version='1.0' encoding='utf-8'?>

<Server port="8005" shutdown="SHUTDOWN">
  <Listener className="org.apache.catalina.startup.VersionLoggerListener" />

  <Listener className="org.apache.catalina.core.AprLifecycleListener" SSLEngine="on" />

  <Listener className="org.apache.catalina.core.JreMemoryLeakPreventionListener" />
  <Listener className="org.apache.catalina.mbeans.GlobalResourcesLifecycleListener" />
  <Listener className="org.apache.catalina.core.ThreadLocalLeakPreventionListener" />


  <GlobalNamingResources>

    <Resource name="UserDatabase" auth="Container"
              type="org.apache.catalina.UserDatabase"
              description="User database that can be updated and saved"
              factory="org.apache.catalina.users.MemoryUserDatabaseFactory"
              pathname="conf/tomcat-users.xml" />
  </GlobalNamingResources>


  <Service name="Catalina">


    <Connector port="8080" protocol="HTTP/1.1"
               connectionTimeout="20000"
               redirectPort="8443" />

    <Connector port="8009" protocol="AJP/1.3" redirectPort="8443" />



    <Engine name="Catalina" defaultHost="localhost">


     
      <Cluster className="org.apache.catalina.ha.tcp.SimpleTcpCluster"
                 channelSendOptions="8">

          <Manager className="org.apache.catalina.ha.session.DeltaManager"
                   expireSessionsOnShutdown="false"
                   notifyListenersOnReplication="true"/>

          <Channel className="org.apache.catalina.tribes.group.GroupChannel">
            <Membership className="org.apache.catalina.tribes.membership.McastService"
                        address="228.0.0.4"
                        port="45564"
                        frequency="500"
                        dropTime="3000"/>
            <Receiver className="org.apache.catalina.tribes.transport.nio.NioReceiver"
                      address="auto"
                      port="4000"
                      autoBind="100"
                      selectorTimeout="5000"
                      maxThreads="6"/>

            <Sender className="org.apache.catalina.tribes.transport.ReplicationTransmitter">
              <Transport className="org.apache.catalina.tribes.transport.nio.PooledParallelSender"/>
            </Sender>
            <Interceptor className="org.apache.catalina.tribes.group.interceptors.TcpFailureDetector"/>
            <Interceptor className="org.apache.catalina.tribes.group.interceptors.MessageDispatch15Interceptor"/>
          </Channel>

          <Valve className="org.apache.catalina.ha.tcp.ReplicationValve"
                 filter=""/>
          <Valve className="org.apache.catalina.ha.session.JvmRouteBinderValve"/>


          <ClusterListener className="org.apache.catalina.ha.session.ClusterSessionListener"/>
        </Cluster>



      <Realm className="org.apache.catalina.realm.LockOutRealm">

        <Realm className="org.apache.catalina.realm.UserDatabaseRealm"
               resourceName="UserDatabase"/>
      </Realm>

      <Host name="localhost"  appBase="webapps"
            unpackWARs="true" autoDeploy="true">


        <Valve className="org.apache.catalina.valves.AccessLogValve" directory="logs"
               prefix="localhost_access_log" suffix=".txt"
               pattern="%h %l %u %t &quot;%r&quot; %s %b" />

      </Host>
    </Engine>
  </Service>
</Server>
```

## 3. docker-compose 구성 방안
```yml
version: '3.7'
services:
  portal:
    container_name: portal
    image: nginx  
    ports:
      - 8080:80
    volumes: 
      - ./cluster/default.conf:/etc/nginx/conf.d/default.conf
  tomcat1:
    image: tomcat:7.0.94-jre7-alpine
    container_name: tomcat1
    volumes:
      - ./cluster/server.xml:/usr/local/tomcat/conf/server.xml
      - ./cluster/ROOT:/usr/local/tomcat/webapps/ROOT
      - ./cluster/tomcat-users.xml:/usr/local/tomcat/conf/tomcat-users.xml
  tomcat2:
    image: tomcat:7.0.94-jre7-alpine
    container_name: tomcat2
    volumes:
      - ./cluster/server.xml:/usr/local/tomcat/conf/server.xml
      - ./cluster/ROOT:/usr/local/tomcat/webapps/ROOT
      - ./cluster/tomcat-users.xml:/usr/local/tomcat/conf/tomcat-users.xml
  tomcat3:
    image: tomcat:7.0.94-jre7-alpine
    container_name: tomcat3
    volumes:
      - ./cluster/server.xml:/usr/local/tomcat/conf/server.xml
      - ./cluster/ROOT:/usr/local/tomcat/webapps/ROOT
      - ./cluster/tomcat-users.xml:/usr/local/tomcat/conf/tomcat-users.xml 
```

## 4. UseCase
docker-compose 명령어를 사용합니다.
```bash
docker-compose up
```