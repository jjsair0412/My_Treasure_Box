# WildFly 쿠버네티스 환경에서의 세션 클러스터링 구현 보고서

## 0. 목차
- [1. 개요](#1-개요)
- [2. Wildfly 세션 클러스터링_이론](#2-wildfly-세션-클러스터링-방법이론)
- [3. wildfly 세션 클러스터링_실습_및_문제원인분석](#3-wildfly-세션-클러스터링-방법)
- [4. 시사점](#4-시사점)

## 1. 개요

본 보고서는 쿠버네티스(Kubernetes) 환경에서 WildFly 애플리케이션 서버의 세션 클러스터링 구현 과정과 발생한 문제, 그리고 해결 방안에 대해 설명합니다. 특히 KUBE_PING 프로토콜을 사용한 파드 디스커버리와 TCP 통신 설정 과정에서 발생한 이슈에 초점을 맞춥니다.

주요 이슈는 KUBE_PING을 통해 클러스터 멤버(파드)는 정상적으로 발견되었으나, 실제 TCP 통신을 통한 클러스터 형성에 실패하는 문제였습니다. 이 문제는 바인딩 주소 설정과 직접적인 관련이 있음을 확인하고 해결했습니다.

## 2. WildFly 세션 클러스터링 방법(이론)

### 2.1 세션 클러스터링이란?

세션 클러스터링은 여러 애플리케이션 서버 인스턴스 간에 사용자 세션을 공유하는 기술입니다. 이를 통해 고가용성과 로드 밸런싱을 구현할 수 있으며, 한 서버가 다운되더라도 사용자 세션이 유지됩니다.

### 2.2 WildFly의 클러스터링 아키텍처

WildFly는 다음과 같은 핵심 컴포넌트를 사용하여 클러스터링을 구현합니다:

1. **JGroups**: 클러스터 멤버 간 통신을 담당하는 툴킷
2. **Infinispan**: 분산 데이터 그리드로 세션 데이터를 저장하고 복제
3. **mod_cluster/mod_jk**: 로드 밸런싱과 세션 어피니티 제공

### 2.3 WildFly의 네트워크 설정 아키텍처

WildFly의 네트워크 설정은 다음과 같은 세 가지 주요 구성 요소로 이루어져 있습니다:

1. **인터페이스(Interfaces)**: 네트워크 인터페이스를 정의하며, IP 주소나 네트워크 인터페이스 이름으로 지정
2. **소켓 바인딩 그룹(Socket Binding Groups)**: 소켓 바인딩의 집합으로, 포트와 인터페이스를 연결
3. **소켓 바인딩(Socket Bindings)**: 특정 서비스가 사용할 포트와 인터페이스 조합을 정의

#### 인터페이스 설정

인터페이스 설정은 서버가 바인딩할 수 있는 네트워크 인터페이스를 정의합니다:

- standalone-ha.xml
```xml
<interfaces>
    <!-- 관리 인터페이스 - 기본적으로 로컬호스트만 접근 가능 -->
    <interface name="management">
        <inet-address value="${jboss.bind.address.management:127.0.0.1}"/>
    </interface>
    
    <!-- 공개 인터페이스 - 애플리케이션 트래픽용 -->
    <interface name="public">
        <inet-address value="${jboss.bind.address:127.0.0.1}"/>
    </interface>
    
    <!-- 개인 인터페이스 - 클러스터 내부 통신용 -->
    <interface name="private">
        <inet-address value="${jboss.bind.address.private:127.0.0.1}"/>
    </interface>
    
    <!-- 쿠버네티스용 인터페이스 - 모든 네트워크 인터페이스에 바인딩 -->
    <interface name="kubernetes">
        <inet-address value="${jboss.bind.address.kubernetes:0.0.0.0}"/>
    </interface>
</interfaces>
```

각 인터페이스는 다음과 같은 목적으로 사용됩니다:
- **management**: 관리 콘솔 및 관리 API 접근용
- **public**: 애플리케이션 HTTP/HTTPS 트래픽용
- **private**: 클러스터 내부 통신용 (주로 JGroups가 사용)
- **kubernetes**: 쿠버네티스 환경에서 모든 인터페이스 바인딩용

#### 소켓 바인딩 그룹

소켓 바인딩 그룹은 관련된 소켓 바인딩들을 그룹화하며, 기본 인터페이스와 포트 오프셋을 지정합니다:

- standalone-ha.xml
```xml
<socket-binding-group name="standard-sockets" default-interface="public" port-offset="${jboss.socket.binding.port-offset:0}">
    <!-- 소켓 바인딩 정의 -->
</socket-binding-group>
```

- **name**: 소켓 바인딩 그룹의 이름
- **default-interface**: 소켓 바인딩에서 인터페이스를 명시적으로 지정하지 않을 때 사용되는 기본 인터페이스
- **port-offset**: 그룹 내 모든 포트에 적용되는 오프셋 (동일 호스트에서 여러 서버 실행 시 유용)

#### 소켓 바인딩

소켓 바인딩은 특정 서비스가 사용할 포트와 인터페이스를 정의합니다:

- standalone-ha.xml
```xml
<socket-binding name="http" interface="public" port="${jboss.http.port:8080}"/>
```

- **name**: 소켓 바인딩의 이름 (다른 서브시스템에서 참조함)
- **interface**: 바인딩할 인터페이스 (지정하지 않으면 소켓 바인딩 그룹의 default-interface 사용)
- **port**: 사용할 포트 번호 (시스템 속성으로 재정의 가능)

#### 클러스터링을 위한 특수 소켓 바인딩

쿠버네티스 상 Pod끼리 JGroups 클러스터링을 위해 TCP 기반 클러스터링용도 소켓 바인딩 작업이 필요합니다.
- **K8s Network는 UDP Multicast를 지원하지 않기 때문**

- standalone-ha.xml
```xml
<!-- TCP 기반 클러스터링용 -->
<socket-binding name="jgroups-tcp" interface="kubernetes" port="7600"/>
<socket-binding name="jgroups-tcp-fd" interface="kubernetes" port="57600"/>

<!-- UDP 기반 클러스터링용 (멀티캐스트) -->
<socket-binding name="jgroups-udp" interface="private" port="55200" multicast-address="${jboss.default.multicast.address:230.0.0.4}" multicast-port="45688"/>
<socket-binding name="jgroups-udp-fd" interface="private" port="54200"/>
```

쿠버네티스 환경에서는 멀티캐스트가 일반적으로 지원되지 않기 때문에 TCP 기반 소켓 바인딩을 사용합니다.

### 2.4 쿠버네티스 환경에서의 클러스터링 흐름

쿠버네티스 환경에서 WildFly 세션 클러스터링의 데이터 통신 흐름은 다음과 같습니다:

1. **멤버 디스커버리 단계**:
   - KUBE_PING 프로토콜이 쿠버네티스 API를 통해 동일한 라벨을 가진 파드들의 목록을 조회
   - 조회된 파드 정보(이름, IP 등)를 수집

2. **클러스터 형성 단계**:
   - 각 파드는 자신의 물리적 주소(IP:PORT)를 다른 파드에 알림
   - TCP 연결을 통해 파드 간 통신 채널 형성
   - GMS(Group Membership Service) 프로토콜을 통해 클러스터 뷰 형성

3. **세션 복제 단계**:
   - 사용자 세션 생성 시 Infinispan을 통해 세션 데이터를 분산 저장
   - 세션 데이터 변경 시 클러스터 내 다른 멤버에 변경사항 전파
   - 로드 밸런싱으로 요청이 다른 파드로 라우팅되어도 세션 유지

## 3. WildFly 세션 클러스터링 방법

### 3.1 발생한 문제

클러스터링 설정 과정에서 다음과 같은 문제가 발생했습니다:

1. KUBE_PING을 통해 다른 파드를 성공적으로 발견했으나, 클러스터 뷰에는 단일 노드만 표시됨
2. 물리적 주소가 `0.0.0.0:7600`으로 설정되어, 다른 파드가 연결을 시도할 때 실패

로그 분석 결과:
```
17:22:05,722 INFO [org.jgroups.protocols.pbcast.GMS] (ServerService Thread Pool -- 84) jin-was-6f85b664d-qmb65: no members discovered after 4388 ms: creating cluster as coordinator
```

```
17:22:09,545 INFO [org.infinispan.CLUSTER] (ServerService Thread Pool -- 89) ISPN000079: Channel jin-was-cluster local address is jin-was-6f85b664d-qmb65, physical addresses are [0.0.0.0:7600]
```

### 3.2 문제 원인 분석

KUBE_PING은 쿠버네티스 API를 통해 파드를 발견하는 데는 성공했지만, TCP 연결 단계에서 문제가 발생했습니다.

원인은 다음과 같습니다:
0. Kube-Ping을 이용하여 동일한 Label을 가진 Pod는 확인되었음.
1. JGroups TCP 전송이 `0.0.0.0`에 바인딩되어 있어, 다른 파드가 연결을 시도할 때 유효한 목적지 주소가 아님.(0.0.0.0:7600 으로 TCP 연결됨.)
2. 파드의 실제 IP 주소를 사용하여 바인딩해야 하는데, 기본 설정은 그렇지 않음

### 3.3 해결 방법

#### 3.3.1 WildFly standalone-ha.xml 설정 수정

JGroups TCP 전송 설정에 바인딩 주소와 관련된 속성을 추가했습니다:

```xml
<subsystem xmlns="urn:jboss:domain:jgroups:8.0">
    <channels default="ee">
        <!-- 클러스터 이름 설정 -->
        <channel name="ee" stack="tcp" cluster="jin-was-cluster"/>
    </channels>
    <stacks>
        <stack name="tcp">
            <transport type="TCP" socket-binding="jgroups-tcp">
                <!-- 파드 IP를 바인딩 주소로 설정 -->
                <property name="bind_addr">${env.POD_IP}</property>
                <!-- 다른 노드에게 알려질 주소 설정 -->
                <property name="tcp.address">${env.POD_IP}</property>
                <!-- 외부 네트워크에서 접근 가능한 주소 설정 -->
                <property name="external_addr">${env.POD_IP}</property>
            </transport>
            <protocol type="kubernetes.KUBE_PING">
                <!-- 네임스페이스 설정 -->
                <property name="namespace">${env.KUBERNETES_NAMESPACE}</property>
                <!-- 파드 선택 라벨 -->
                <property name="labels">${env.KUBERNETES_LABELS}</property>
                <!-- 포트 범위 (0은 기본 포트만 사용) -->
                <property name="port_range">0</property>
                <!-- 쿠버네티스 API 서버 주소 -->
                <property name="masterHost">kubernetes.default.svc</property>
                <!-- 쿠버네티스 API 서버 포트 -->
                <property name="masterPort">443</property>
                <!-- 연결 타임아웃 -->
                <property name="connectTimeout">5000</property>
                <!-- 읽기 타임아웃 -->
                <property name="readTimeout">5000</property>
                <!-- 작업 시도 횟수 -->
                <property name="operationAttempts">5</property>
            </protocol>
            <!-- 분할된 클러스터 병합 프로토콜 -->
            <protocol type="MERGE3">
                <property name="min_interval">10000</property>
                <property name="max_interval">30000</property>
            </protocol>
            <!-- 장애 감지 프로토콜 -->
            <socket-protocol type="FD_SOCK" socket-binding="jgroups-tcp-fd"/>
            <protocol type="FD_ALL">
                <property name="timeout">10000</property>
                <property name="interval">3000</property>
            </protocol>
            <protocol type="VERIFY_SUSPECT"/>
            <!-- 메시지 전송 보장 프로토콜 -->
            <protocol type="pbcast.NAKACK2"/>
            <protocol type="UNICAST3"/>
            <protocol type="pbcast.STABLE"/>
            <!-- 그룹 멤버십 서비스 -->
            <protocol type="pbcast.GMS">
                <property name="join_timeout">10000</property>
                <property name="view_bundling">true</property>
            </protocol>
            <protocol type="MFC"/>
            <protocol type="FRAG3"/>
        </stack>
    </stacks>
</subsystem>
```

#### 3.3.2 소켓 바인딩 그룹 설정

WildFly에서 소켓 바인딩은 네트워크 리스닝 포트와 인터페이스를 연결하는 중요한 설정입니다. 이 설정은 JGroups가 사용할 네트워크 인터페이스와 포트를 정의합니다.

**소켓 바인딩 이론적 설명:**

1. **소켓 바인딩 그룹**: WildFly에서 모든 소켓 바인딩을 그룹화하는 컨테이너입니다. 각 서버 구성에는 하나의 소켓 바인딩 그룹이 있습니다.

2. **default-interface**: 소켓 바인딩에서 명시적으로 인터페이스를 지정하지 않을 경우 사용되는 기본 인터페이스입니다.

3. **port-offset**: 모든 포트에 적용되는 오프셋 값으로, 여러 WildFly 인스턴스를 동일한 머신에서 실행할 때 포트 충돌을 방지하는 데 유용합니다.

4. **socket-binding**: 특정 네트워크 서비스를 위한 포트와 인터페이스 설정입니다.
   - **name**: 소켓 바인딩의 고유 식별자
   - **port**: 사용할 포트 번호
   - **interface**: 바인딩할 네트워크 인터페이스 (지정하지 않으면 default-interface 사용)

5. **JGroups 소켓 바인딩**: 클러스터링을 위해 JGroups가 사용하는 특수 소켓 바인딩입니다.
   - **jgroups-tcp**: JGroups TCP 메시지 교환용 소켓
   - **jgroups-tcp-fd**: 장애 감지(Failure Detection)를 위한 소켓

**클러스터링에서 소켓 바인딩의 중요성:**

클러스터링에서 소켓 바인딩은 클러스터 멤버 간 통신 채널을 정의합니다. 잘못 구성될 경우 클러스터 형성에 실패할 수 있습니다. 쿠버네티스 환경에서는 특히 `jgroups-tcp`와 `jgroups-tcp-fd` 소켓 바인딩이 올바른 인터페이스를 참조하는지 확인하는 것이 중요합니다.
- ***UDP 지원하지 않기 때문***

```xml
<socket-binding-group name="standard-sockets" default-interface="public" port-offset="${jboss.socket.binding.port-offset:0}">
    <!-- 기본 소켓 바인딩 -->
    <socket-binding name="ajp" port="${jboss.ajp.port:8009}"/>
    <socket-binding name="http" port="${jboss.http.port:8080}"/>
    <socket-binding name="https" port="${jboss.https.port:8443}"/>
    
    <!-- JGroups TCP 소켓 - kubernetes 인터페이스(0.0.0.0) 사용 -->
    <socket-binding name="jgroups-tcp" interface="kubernetes" port="7600"/>
    <socket-binding name="jgroups-tcp-fd" interface="kubernetes" port="57600"/>
    
    <!-- 관리 인터페이스 소켓 -->
    <socket-binding name="management-http" interface="management" port="${jboss.management.http.port:9990}"/>
    <socket-binding name="management-https" interface="management" port="${jboss.management.https.port:9993}"/>
    
    <!-- 기타 소켓 바인딩 -->
    <socket-binding name="txn-recovery-environment" port="4712"/>
    <socket-binding name="txn-status-manager" port="4713"/>
</socket-binding-group>
```

위 설정에서 중요한 점은 `jgroups-tcp` 소켓 바인딩이 `kubernetes` 인터페이스를 사용하도록 설정되어 있다는 것입니다. 이는 JGroups가 모든 네트워크 인터페이스(`0.0.0.0`)에서 들어오는 연결을 수신하도록 합니다. 그러나 앞서 설명한 대로, 실제 통신에는 `tcp.address` 및 `bind_addr` 속성을 통해 파드의 실제 IP를 사용해야 합니다.

### 3.3.3 인터페이스 설정

WildFly의 인터페이스 설정은 서버가 네트워크 통신에 사용할 IP 주소나 네트워크 인터페이스를 정의합니다. 이 설정은 클러스터링을 위한 바인딩 주소를 결정하는 데 중요한 역할을 합니다.

**인터페이스 설정의 이론적 설명:**

1. **인터페이스 개념**: WildFly에서 인터페이스는 서버가 바인딩할 수 있는 네트워크 주소 또는 인터페이스의 논리적 이름입니다. 서버는 여러 인터페이스를 정의하고, 각각 다른 목적으로 사용할 수 있습니다.

2. **인터페이스 표현 방식**:
   - **inet-address**: 특정 IP 주소 지정
   - **nic**: 특정 네트워크 인터페이스 카드 지정
   - **nic-match**: 정규식으로 네트워크 인터페이스 지정
   - **subnet-match**: 특정 서브넷에 속하는 주소 지정
   - **any-address**: 모든 주소(0.0.0.0 or ::)에 바인딩

3. **시스템 속성 사용**: 인터페이스 정의에서 시스템 속성(예: `${jboss.bind.address}`)을 사용하여 서버 시작 시 동적으로 바인딩 주소를 지정할 수 있습니다.

4. **표준 인터페이스**:
   - **management**: 관리 인터페이스(관리 콘솔, CLI 등)에 사용
   - **public**: 애플리케이션 트래픽(HTTP, HTTPS 등)에 사용
   - **private**: 클러스터 내부 통신에 사용
   - **unsecure**: 비보안 통신에 사용 (선택적)

5. **쿠버네티스 환경을 위한 특수 인터페이스**: 쿠버네티스 환경에서는 모든 네트워크 인터페이스(0.0.0.0)에 바인딩하기 위한 특수 인터페이스를 정의하는 것이 일반적입니다.

**쿠버네티스 환경에서의 설정:**

```xml
<interfaces>
    <interface name="management">
        <inet-address value="${jboss.bind.address.management:127.0.0.1}"/>
    </interface>
    <interface name="public">
        <inet-address value="${jboss.bind.address:127.0.0.1}"/>
    </interface>
    <interface name="private">
        <inet-address value="${jboss.bind.address.private:127.0.0.1}"/>
    </interface>
    <!-- 쿠버네티스용 인터페이스 - 모든 네트워크 인터페이스 바인딩 -->
    <interface name="kubernetes">
        <inet-address value="${jboss.bind.address.kubernetes:0.0.0.0}"/>
    </interface>
</interfaces>
```

**인터페이스와 JGroups 설정의 관계:**

인터페이스 설정은 소켓 바인딩을 통해 JGroups에 영향을 미칩니다. `kubernetes` 인터페이스를 `0.0.0.0`으로 설정하고, 소켓 바인딩에서 이 인터페이스를 참조하면 JGroups가 모든 네트워크 인터페이스에서 연결을 수신할 수 있습니다.

하지만 앞서 설명한 대로, JGroups TCP 전송 설정의 `bind_addr` 속성이 이 인터페이스 설정을 재정의할 수 있습니다. 쿠버네티스 환경에서는 파드의 실제 IP를 `bind_addr`로 설정하여 다른 파드가 연결할 수 있는 주소를 사용하도록 하는 것이 중요합니다.

#### 3.3.4 쿠버네티스 Deployment 설정

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: jin-was
  namespace: test
spec:
  replicas: 2
  selector:
    matchLabels:
      app: jin-was
  template:
    metadata:
      labels:
        app: jin-was
    spec:
      serviceAccountName: jin-kubeping-service-account  # KUBE_PING용 서비스 어카운트
      containers:
      - name: wildfly
        image: wildfly:latest
        ports:
        - containerPort: 8080  # HTTP
        - containerPort: 7600  # JGroups
        - containerPort: 57600 # JGroups FD
        env:
        # 파드 IP를 환경 변수로 설정
        - name: POD_IP
          valueFrom:
            fieldRef:
              fieldPath: status.podIP
        # 쿠버네티스 네임스페이스
        - name: KUBERNETES_NAMESPACE
          valueFrom:
            fieldRef:
              fieldPath: metadata.namespace
        # KUBE_PING 라벨 셀렉터
        - name: KUBERNETES_LABELS
          value: "app=jin-was"
        # 클러스터 이름
        - name: CLUSTER_NAME
          value: "jin-was-cluster"
        # JGroups 클러스터 패스워드
        # 필수는 아니나 권장 설정임.
        - name: JGROUPS_CLUSTER_PASSWORD
          valueFrom:
            secretKeyRef:
              name: jgroups-secret
              key: password
        volumeMounts:
        - name: wildfly-config
          mountPath: /opt/jboss/wildfly/standalone/configuration/standalone-ha.xml
          subPath: standalone-ha.xml
      volumes:
      - name: wildfly-config
        configMap:
          name: wildfly-config
---
# KUBE_PING 권한을 위한 ServiceAccount
apiVersion: v1
kind: ServiceAccount
metadata:
  name: jin-kubeping-service-account
  namespace: test
---
# KUBE_PING 권한 설정
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: pod-reader
  namespace: test
rules:
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "list"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: pod-reader-binding
  namespace: test
subjects:
- kind: ServiceAccount
  name: jin-kubeping-service-account
  namespace: test
roleRef:
  kind: Role
  name: pod-reader
  apiGroup: rbac.authorization.k8s.io
---
```

### 3.5 캐시 복제 및 세션 이동 모니터링

세션 클러스터링이 올바르게 설정되었는지 확인하는 중요한 단계는 실제 세션 데이터가 노드 간에 복제되고 이동하는 것을 모니터링하는 것입니다. 이를 통해 클러스터링이 단순히 설정된 것이 아니라 실제로 작동하고 있음을 확인할 수 있습니다.

#### 3.5.1 Infinispan 캐시 활동 확인 방법

WildFly에서 Infinispan 캐시 활동을 모니터링하는 방법은 다음과 같습니다:

1. **로깅 레벨 상향 조정**:
   ```xml
   <logger category="org.infinispan">
       <level name="TRACE"/>
   </logger>
   <logger category="org.infinispan.transaction">
       <level name="TRACE"/>
   </logger>
   <logger category="org.infinispan.distribution">
       <level name="TRACE"/>
   </logger>
   ```

2. **세션 복제 관련 로그 확인**:
   세션 복제가 발생할 때 다음과 같은 로그가 표시됩니다:
   ```
   TRACE [org.infinispan.interceptors.distribution.NonTxDistributionInterceptor] (default task-15) Sending remote get for key SESSION_ID to owners [other-node]
   ```
   ```
   DEBUG [org.infinispan.statetransfer.StateConsumerImpl] (StateTransferTask-2,null,jin-was-xxx) Added key SESSION_ID to received keys for segment 42. Expecting 100 more keys for this segment
   ```

3. **캐시 리밸런싱 로그 확인**:
   노드가 추가되거나 제거될 때 다음과 같은 리밸런싱 로그가 표시됩니다:
   ```
   17:31:13,562 INFO  [org.infinispan.CLUSTER] (thread-8,jin-was-cluster,jin-was-57ff775987-btpf7) [Context=app.war] ISPN100002: Starting rebalance with members [jin-was-57ff775987-btpf7, jin-was-57ff775987-vqmrc], phase READ_OLD_WRITE_ALL, topology id 2
   ```

#### 3.5.2 세션 이동 테스트 애플리케이션 구현

실제 세션 복제를 테스트하기 위해 간단한 웹 애플리케이션을 구현할 수 있습니다:

```java
@WebServlet("/SessionTestServlet")
public class SessionTestServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(true);
        PrintWriter out = response.getWriter();
        
        // 현재 서버(파드) 이름 표시
        String serverName = System.getProperty("jboss.node.name", 
                            System.getenv("HOSTNAME"));
        
        // 세션 정보 표시
        out.println("<html><body>");
        out.println("<h1>Session Test</h1>");
        out.println("<p>Server: " + serverName + "</p>");
        out.println("<p>Session ID: " + session.getId() + "</p>");
        
        // 세션 카운터 증가
        Integer counter = (Integer) session.getAttribute("counter");
        if (counter == null) {
            counter = 1;
        } else {
            counter++;
        }
        session.setAttribute("counter", counter);
        
        // 세션 속성 표시
        out.println("<p>Counter: " + counter + "</p>");
        out.println("<p>Session Creation Time: " + new Date(session.getCreationTime()) + "</p>");
        
        // 세션에 저장된 모든 속성 표시
        out.println("<h2>All Session Attributes:</h2>");
        Enumeration<String> attributeNames = session.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String name = attributeNames.nextElement();
            out.println("<p>" + name + ": " + session.getAttribute(name) + "</p>");
        }
        
        out.println("</body></html>");
    }
}
```

#### 3.5.3 JMX를 통한 Infinispan 캐시 모니터링

WildFly는 JMX를 통해 Infinispan 캐시 통계를 모니터링할 수 있는 기능을 제공합니다:

1. **통계 활성화**:
   ```xml
   <subsystem xmlns="urn:jboss:domain:infinispan:13.0">
       <cache-container name="web" statistics-enabled="true">
           <!-- 설정 내용 -->
       </cache-container>
   </subsystem>
   ```

2. **JMX 콘솔 접근**:
   WildFly 관리 콘솔에서 "Runtime" → "JMX" → "org.wildfly.clustering.infinispan.web"에 접근하여 다음과 같은 정보를 확인할 수 있습니다:
   - 캐시 적중률(hits/misses)
   - 읽기/쓰기 작업 수
   - 리밸런싱 작업
   - 세션 수

3. **CLI를 통한 모니터링**:
   ```
   /subsystem=infinispan/cache-container=web/distributed-cache=dist:read-resource(include-runtime=true)
   ```

#### 3.5.4 세션 복제 확인 결과

환경에서 세션 복제 테스트를 수행한 결과, 다음과 같은 중요한 활동이 관찰되었습니다:

1. **세션 복제 확인**:
   로그에서 다음과 같은 메시지를 통해 세션 복제가 이루어지고 있음을 확인할 수 있었습니다:
   ```
   DEBUG [org.infinispan.interceptors.distribution.NonTxDistributionInterceptor] (default task-15) Sending remote get for key SESSION_7F2A3BC9E8E7D1C55D6E35A09F3B7F1E to owners [jin-was-57ff775987-vqmrc]
   ```

2. **세션 데이터 일관성**:
   서로 다른 파드에 접속해도 세션 카운터 값이 지속적으로 증가하여 세션 데이터가 일관되게 유지됨을 확인했습니다.

3. **리밸런싱 성공**:
   파드를 추가하거나 제거할 때 다음과 같은 리밸런싱 과정이 성공적으로 수행되었습니다:
   ```
   17:31:14,143 INFO [org.infinispan.CLUSTER] (thread-12,jin-was-cluster,jin-was-57ff775987-btpf7) [Context=http-remoting-connector] ISPN100010: Finished rebalance with members [jin-was-57ff775987-btpf7, jin-was-57ff775987-vqmrc], topology id 5
   ```

이러한 관찰 결과를 통해 WildFly 클러스터링이 올바르게 설정되어 세션 데이터가 파드 간에 성공적으로 복제 및 공유되고 있음을 확인할 수 있었습니다.

### 3.6 구현 결과

## 4. 시사점

### 4.1 JGroups 바인딩 주소와 소켓 바인딩의 관계

이번 구현 과정에서 가장 중요한 발견은 JGroups TCP 전송의 바인딩 주소 설정과 소켓 바인딩 설정 간의 관계입니다. 이 두 가지 설정이 상호작용하는 방식을 이해하는 것이 클러스터링 성공의 핵심이었습니다.

#### 소켓 바인딩과 바인딩 주소의 관계

1. **소켓 바인딩이 정의하는 것**:
   - `jgroups-tcp` 소켓 바인딩은 JGroups가 사용할 인터페이스와 포트를 정의합니다.
   - 예: `<socket-binding name="jgroups-tcp" interface="kubernetes" port="7600"/>` 설정은 JGroups가 `kubernetes` 인터페이스(0.0.0.0)의 7600 포트를 사용하도록 지정합니다.

2. **JGroups 바인딩 주소 속성이 정의하는 것**:
   - `bind_addr` 속성은 소켓 바인딩에서 지정한 인터페이스를 재정의할 수 있습니다.
   - `tcp.address`와 `external_addr` 속성은 다른 노드에 알려질 주소를 지정합니다.

3. **우선순위**:
   - JGroups 프로토콜 설정의 `bind_addr` 속성은 소켓 바인딩의 인터페이스 설정보다 우선합니다.
   - 즉, `<property name="bind_addr">${env.POD_IP}</property>`가 설정되면 소켓 바인딩의 인터페이스 설정은 무시됩니다.

### 4.2 쿠버네티스 환경에서의 최적 설정 전략

쿠버네티스 환경에서 WildFly 클러스터링을 위한 최적의 설정 전략은 다음과 같습니다:

1. **인터페이스 설정**:
   - `kubernetes` 인터페이스를 `0.0.0.0`으로 설정하여 모든 인터페이스에서 연결 수신
   - `<interface name="kubernetes"><inet-address value="${jboss.bind.address.kubernetes:0.0.0.0}"/></interface>`

2. **소켓 바인딩 설정**:
   - JGroups 소켓을 `kubernetes` 인터페이스에 바인딩
   - `<socket-binding name="jgroups-tcp" interface="kubernetes" port="7600"/>`

3. **JGroups 전송 설정**:
   - 파드의 실제 IP를 `bind_addr`, `tcp.address`, `external_addr` 속성에 설정
   - ```xml
     <transport type="TCP" socket-binding="jgroups-tcp">
         <property name="bind_addr">${env.POD_IP}</property>
         <property name="tcp.address">${env.POD_IP}</property>
         <property name="external_addr">${env.POD_IP}</property>
     </transport>
     ```

4. **환경 변수 설정**:
   - 파드 YAML에 `POD_IP` 환경 변수 설정
   - ```yaml
     env:
     - name: POD_IP
       valueFrom:
         fieldRef:
           fieldPath: status.podIP
     ```

이러한 조합을 통해:
- 서버는 모든 인터페이스(`0.0.0.0`)에서 들어오는 연결을 수신합니다.
- 클러스터 통신에는 파드의 실제 IP 주소가 사용됩니다.
- 다른 파드는 실제 IP 주소를 사용하여 성공적으로 연결할 수 있습니다.

### 4.3 클러스터링 디버깅 방법

클러스터링 문제 해결 시 다음과 같은 접근 방법이 효과적입니다:

1. JGroups 로그 분석: GMS, TCP, KUBE_PING 로그를 통해 클러스터 형성 과정 추적
2. 네트워크 연결 확인: 파드 간 TCP 포트 연결성 테스트
3. 단계별 접근: 디스커버리 → 연결 → 클러스터 형성 순으로 문제 분석

### 4.3 쿠버네티스 환경에서의 WildFly 배포 고려사항

1. **서비스 어카운트 권한**: KUBE_PING이 쿠버네티스 API에 접근하기 위한 적절한 RBAC 권한 필요
2. **세션 어피니티**: 로드 밸런싱 시 세션 어피니티 설정으로 사용자 경험 개선
3. **헤드리스 서비스**: 필요에 따라 파드 간 직접 통신을 위한 헤드리스 서비스 고려
- 헤드리스 서비스는 필수는 아니나, 통신격리를 위해 사용 고려

### 4.4 결론

쿠버네티스 환경에서 WildFly 세션 클러스터링을 구현하기 위해서는 JGroups TCP 전송 설정, 특히 바인딩 주소 관련 속성을 정확히 이해하고 설정하는 것이 중요합니다.