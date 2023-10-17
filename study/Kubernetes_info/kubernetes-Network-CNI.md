# Kubernetes CNI
## Overview
Kubernetes의 CNI에 대해 간략히 소개하고, 서드파티 CNI인 Flannel과 calico에 대해 기술 후 NetworkPolicy를 이해하기 위한 문서입니다.

## What is CNI ?
CNI는 Container Network Interface의 약자입니다.

CNI는 ,,


		컨테이너 오케스트레이션 시스템( Kubernetes, Docker 등.. ) 에서 , 
        
        컨테이너 . 간네트워크 연결을 설정하고 관리하기 위한 표준 인터페이스 입니다.
        
        또한 CNI는 컨테이너에 IP를 할당하고, 라우팅을 도와주며, 방화벽 Role을 설정하는 등의 네트워크 관련 작업을 수행하며
        
        컨테이너간 통신과 로드벨런싱, 서비스 디스커버리와 네트워크 보안등의 기능을 지원합니다.
        


Kubernetes는 자체적으로 컨테이너간의 네트워크를 담당하지 않기 때문에, 다양한 CNI 플러그인( calico, flannel, Weave 등 ..) 이 사용되게 됩니다.

## Kubenet plugin
Kubernets는 놀랍게도 Kubeadm으로 구성했을 경우 **Kubenet** 이라는 default CNI를 가지고 있는데 이를 사용하지 않습니다.

이유는 , kubenet은 경량화된 CNI 플러그인으로써, CNI로 동작하기에는 기능이 너무나 부족하기 때문에, 서드파티 CNI들이 제공하는 다양한 기능들을 사용하기 위해서도 있겠지만, kubenet은 그 자체로는 컨테이너간의 노드간 네트워킹조자 지원하지 않기 때문에 서드파티 CNI를 사용합니다.

### ETC) Kubenet CIDR 변경 방안
Kubeadm을 통해 Kubernetes cluster를 구축할 때 Kubenet의 CIDR를 변경할 수 있습니다.

```Kubeadm init``` 또는 ```Kubeadm join``` 명령어를 수행하는 순간에 ```--pod-network-cidr``` 플래그를 사용하면 CNI Plugin의 CIDR를 지정할 수 있습니다.

## 서드파티 CNI Plugin
- 서드파티 CNI Plugin에 대해 기술합니다.
	- Flannel , calico 두가지에 대해 기술합니다.

### 1. Flannel Plugin
가장 대표적인 CNI Plugin인 [Flannel](https://github.com/flannel-io/flannel) Plugin.

Flannel 은 , 각 노드에서 바이너리 에이전트를 실행시키고, 클러스터의 모든 노드간 통신을 가능하게 합니다.

그러나 클러스터 안에 있는 모든 리소스들이 전부다 통신이 가능한것은 , 아래와같은 보안취약점이 있을 수 있습니다.

1. Pod간 통신
	- 모든 파드들이 제한 없이 통신할 수 있기에, 권한이 없는 파드가 접근해서는 안되는 파드에 접근하여 데이터 유출 및 위변조가 일어날 수 있습니다.
2. 외부 엑세스 관리 부제
	- 파드 내부에서 외부로 트래픽이 나갸아한다면, ( 외부 API와 연결되어야한다면 ) 해당 통신부분을통해 데이터를 빼낼 수 있습니다.
    - 이것은 인프라관점에서 Private Subnet으로 Kubernetes Cluster를 구성한 뒤 , 외부 연결은 NAT Gateway를 통해서만 진행한다면 1차적으로 방어할 순 있습니다.
3. DDos 공격
	- 허용되지 않은 트래픽이 대량발생할 수 있습니다.
    
따라서 이러한 보안 위협을 방지하기 위해 , Kubernetes는 **[Network Policies API](https://kubernetes.io/docs/concepts/services-networking/network-policies/)** 를 제공하여 , Pod로 진입하는 트래픽 (Ingress) 와 Pod에서 나가는 트래픽 (Egress) 를 제어할 수 있습니다.

또한 특정 파드, 네임스페이스, 또는 CIDR의 트래픽을 막거나 허용할 수 있습니다.

#### 1.1 Flannel 특징
**Flannel은 위에 말했던 NetworPolicy를 지원하지 않습니다.**

그러한 이유는 , Flannel은 클러스터의 각 노드에 대해 네트워크 계층의 IPv4 네트워크를 제공하는것에 중점을 두고있습니다.

컨테이너가 호스트에서 실행될 때, 컨테이너와 호스트간의 연결이 필요하고, 해당 연결을 통해 컨테이너는 호스트의 리소스와 네트워크 인프라를 활용합니다.

이때 ***Flannel은 , 컨테이너간의 통신을 관리하지 않고, 컨테이너가 다른 호스트에서 실행중인 컨테이너와 어떻게 통신하느냐에 대해서만 관리하게 됩니다.***
- 이말은 Flannel은, 컨테이너에서 발생한 트래픽이 다른 호스트에 실행중인 컨테이너에게 어떻게 전달하는지를 관리한다는 의미

**따라서 Flannel은 3계층 네트워크 계층, 즉 IPv4 통신에만 관여하며, label, namespace, CIDR 별로 트래픽을 차단하고 허용하는 NetworkPolicy는 3계층이 아닌 Application과 서비스 수준에서 트래픽을 제어하는것이기 때문에 Flannel은 지원하지 않습니다.** 

**NetworkPolicy를 사용하기 위해선 calico CNI를 사용해야 합니다.**

### 2. calico
#### 2.1 calico 특징
calico CNI는 Flannel CNI처럼 3게층 네트워크를 제공하게 되며, 이것은 파드간 통신을 처리하는데 사용됩니다.

또한 calico는 networkPolicy를 지원하여 애플리케이션 또는 서비스 수준에서 트래픽을 제어할 수 있으며, 다양한 네트워크 옵션을 통해 기능을 확장할 수 있습니다.

### 3. NetworkPolicy
  - 만약 현재사용중인 CNI가 NetworkPolicy를 지원하지 않는다면 
      이를 Kubernetes API Server에 반영한다 하더라도 효과가 없습니다.
      따라서 사용중인 CNI가 이를 지원하는지부터 확인해야하며 , Flannel Plugin은 이를 지원하지 않습니다. 
      - calico가 지원함

아래 NetworkPolicy yaml template을 통해 NetworkPolicy를 이해할 수 있습니다.
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: test-network-policy
  namespace: default
spec:
  podSelector:
    matchLabels:
      role: db
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - ipBlock:
        cidr: 172.17.0.0/16
        except:
        - 172.17.1.0/24
    - namespaceSelector:
        matchLabels:
          project: myproject
    - podSelector:
        matchLabels:
          role: frontend
    ports:
    - protocol: TCP
      port: 6379
  egress:
  - to:
    - ipBlock:
        cidr: 10.0.0.0/24
    ports:
    - protocol: TCP
      port: 5978
```

NetworkPolicy는 양방향 (Pod로 들어오는 트래픽 : Ingress && Pod에서 나가는 트래픽 : Egress) 으로 이루어집니다.


**podSelector**
먼저 , podSelector 부분을 확인해보면 , 해당 NetworkPolicy는 ```role: db``` label이 붙어있는 파드들에게 적용되어진다는것을 알 수 있습니다.
- 만약 podSelector가 비어있다면, 해당 네임스페이스의 모든 파드들에게 적용됩니다.

```yaml
spec:
  podSelector:
    matchLabels:
      role: db
```


**policyTypes**
policyTypes에는 Ingress , Egress 둘중 하나 또는 모두가 리스트형태로 들어갈 수 있습니다.

만약 NetworkPolicy에 policyTypes 가 지정되어 있지 않으면 기본적으로 Ingress 가 항상 설정되고, 네트워크폴리시에 Egress 가 있으면 이그레스가 적용되게 됩니다.
```yaml
spec:
  policyTypes:
  - Ingress
  - Egress
```

**ingress**
NetworkPolicy에선 whiteList(허용) ingress role이 모여있을 수 있으며, 각 규칙은 ***from과 ports에 모두 만족하는 트래픽을 허용하게 됩니다.***
```yaml
spec:
  ingress:
  - from:
    - ipBlock:
        cidr: 172.17.0.0/16
        except:
        - 172.17.1.0/24
    - namespaceSelector:
        matchLabels:
          project: myproject
    - podSelector:
        matchLabels:
          role: frontend
    ports:
    - protocol: TCP
      port: 6379
```

**egress**
NetworkPolicy에선 whiteList(허용) ingress role이 포함될 수 있으며, 각 규칙은 ***to와 ports에 모두 만족하는 트래픽을 허용하게 됩니다.**
- 예시에선 CIDR가 10.0.0.0/24 이며 , 포트가 5978 인 대상들과 일치시키게 됩니다.

```yaml
spec:
  egress:
  - to:
    - ipBlock:
        cidr: 10.0.0.0/24
    ports:
    - protocol: TCP
      port: 5978
```

따라서 위 NetworkPolicy를 설정할 경우 , 다음과 같이 동작하게 됩니다.

1. 인그레스 및 이그레스 트래픽에 대해 "default" 네임스페이스에서 "role=db"인 파드를 격리 합니다.(아직 격리되지 않은 경우).

2. (인그레스 규칙)은 "role=db" 레이블을 사용하는 "default" 네임스페이스의 모든 파드에 대해서 TCP 포트 6379로의 연결을 허용합니다. 인그레스을 허용 할 대상은 다음과 같습니다..
	- 172.17.0.0–172.17.0.255 와 172.17.2.0–172.17.255.255 의 범위를 가지는 IP 주소(예: 172.17.0.0/16 전체에서 172.17.1.0/24 를 제외)
    - "project=myproject" 를 레이블로 가지는 네임스페이스의 모든 파드
	- "role=frontend" 레이블이 있는 "default" 네임스페이스의 모든 파드


3. (이그레스 규칙)은 "role=db" 레이블이 있는 "default" 네임스페이스의 모든 파드에서 TCP 포트 5978의 CIDR 10.0.0.0/24 로의 연결을 허용합니다.
	- 이 말은, NetworkPolicy가 적용된 파드에서 10.0.0.0/24 CIDR 중 Port 5978 을 가지는 파드들에게 나가는 트래픽을 허용한다는 의미