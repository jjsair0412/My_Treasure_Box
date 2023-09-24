# Headless Service
- [Headless service 공식문서](https://kubernetes.io/ko/docs/concepts/services-networking/service/#%ED%97%A4%EB%93%9C%EB%A6%AC%EC%8A%A4-headless-%EC%84%9C%EB%B9%84%EC%8A%A4)
## Headless Service 란 ?
Headless Service는 , 단순하게 ***.spec.clusterIP*** 가 ***None*** 인 서비스 입니다.

## 일반 Service VS Headless Service
일반 Kubernetes 서비스는 , Kube-proxy가 관리하고 ClusterIP가 할당되기 떄문에 , 서비스를 이용한 로드벨런싱 기능을 기본적으로 지원합니다.

그러나 , **Headless Service는  서비스를 거치지 않고 직접 파드에 접근되게 됩니다.**

따라서 서비스를 이용한 부하분산 기능을 지원하지 않습니다.

## 왜 쓸까 ?
Headless Service는 statefulset 에서 pod자체를 한 그룹으로 보고, 따로 묶어 관리해야할 때 사용합니다. 

예를들어 redis와 같이 클러스터링 되야 하며 , 부하분산이 아닌 StateFulSet 으로 각 파드의 상태를 유지시키고 싶을 때 , Headless Service를 StateFulSet의 앞단에 생성하여 Pod를 관리하게 됩니다.

## DNS 자동 구성 기준 - Pod 접근용 DNS 이름 구성 기준
Headless 서비스에서 , Pod에 직접 접근하기 위한 FQDN 이 생성되는 기준은 , Headless Service에 셀렉터가 정의되어 있는지 , 아닌지에 대한 여부에 달려 있습니다.

### 셀렉터가 있는 경우
Headless Service는 모든 Pod에 대한 DNS 레코드를 자동 생성합니다.

Kubernetes Control Plane은 Kubernetes API 내에서 Endpoint Object를 자동 생성하고, Headless Service가 셀렉터로 관리중인 하위 Pod들에 대한 FQDN (A 또는 AAAA 레코드) 를 생성하게 됩니다.

### 셀렉터가 없는 경우
셀렉터가 없는경우에 , Kubernetes 관리자는 서비스 yaml 템플릿에서 수동으로 해당 서비스의 endpoint를 구성해주어야 합니다.

- type: ExternalName 
    - 이렇게 설정할 경우에 , DNS는 DNS CNAME 레코드를 생성하여 해당 도메인에 붙게 됩니다.
- ExternalName이 아닐 경우
    - 서비스의 활성(ready) 엔드포인트의 모든 IP 주소에 대한 DNS A 레코드(IPv4) 또는 AAAA 레코드(IPv6)를 생성합니다. A 레코드와 AAAA 레코드는 도메인 이름을 각각 IPv4와 IPv6 주소에 매핑합니다.

#### type: ExternalName _ 예시
ExternalName 일 경우의 yaml template은 다음과 같습니다.
```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-external-service
spec:
  type: ExternalName
  externalName: example.com
```

위와 같이 설정할 경우, ```example.com```이라는 외부 도메인에 매핑됩니다. 

이렇게 생성된 서비스를 사용하면, 클러스터 내의 다른 리소스들은 ```my-external-service```라는 도메인 이름을 사용하여 ```example.com```에 접근할 수 있습니다.

Kube-DNS는 ```my-external-service``` 에 대한 CNAME 레코드를 생성하여 , 이를 ```example.com``` 으로 매핑하게 됩니다.

따라서 ```my-external-service``` 요청이 ```example.com``` 으로 보내지게 됩니다.
>주의할 점은 ,externalName 서비스는 Kubernetes Cluster 외부 서비스에 접근하기 위한 방법입니다. (외부의 example.com 도메인으로 접근)

#### ExternalName이 아닐 경우 _ 예시
ExternalName이 아닐 경우의 yaml template은 다음과 같습니다.
```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-headless-service
spec:
  ports:
  - protocol: TCP
    port: 80
    targetPort: 9376
---
apiVersion: v1
kind: Endpoints
metadata:
  name: my-headless-service
subsets:
  - addresses:
      - ip: 1.2.3.4
    ports:
      - port: 9376
```

직접 ```my-headless-service``` 의 Headless Service에 대한 endpoint를 수동으로 구성하게 됩니다.

따라서 수동으로 생성해준 endpoint 에 대한 FQDN 을 호출하면 , my-headless-service 서비스로 연결되게 됩니다.
```bash
FQDN : my-headless-service.default.svc.cluster.local
```
>주의 : endpoint를 구성할 때 , endpoint의 metadata.name 과,  연결대상 service의 metadata.name 은 동일해아 합니다.


## Headless Service 생김세
Headless Service는 다음과 같이 생성됩니다.

```yaml
apiVersion: v1
kind: Service
metadata:
  name: kubia
spec:
  clusterIP: None
  selector:
    app: kubia
  ports:
  - name: http
    port: 80
```

