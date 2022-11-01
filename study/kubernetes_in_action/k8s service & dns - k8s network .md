# k8s service & DNS -> K8S network
해당 문서는 k8s의 모든 resource에 대한 내용을 담고있지 않습니다.
service에 대해서 공부 한 뒤 새로 알게된 사실을 기술합니다.
## 1. Pod service & DNS
### 1.1 Multi Port service
Pod는 service를 통해 외부에 노출시킬 수 있습니다. 
service의 selector를 통해 노출시킬 Pod를 지정하고 , 특정 port 번호를 지정하여 생성된 service의 가상 IP를 통해 노출시킵니다.
- 대상 파드의 여러 포트번호를 지정할 수 있습니다.  ( multi port service )
 port 이름으로 구분합니다. 
- 이름으로 지정하는 이유는 , 포트번호가 변경되더라도 쉽게 대응할 수 있게끔 하기 위함에 있습니다.
```yaml
# service
apiVersion: v1
kind: Service
metadata:
  name: kubia
spec:
  ports:
  - name: http # 대상 파드의 여러 포트번호를 지정할 수 있다.
    port: 80
    targetPort: 8080
  - name: https
    port: 443
    targetPort: 8443
  selector:
    app: kubia

# pod
apiVersion: v1
kind: Pod
metadata:
  name: kubia
spec:
  containers:
  - name: kubia
    image: luksa/kubia
    ports:
    - name: http # service의 http 포트와 포트포워딩 ( 80 )
      containerPort: 8080 
    - name: https # service의 https 포트와 포트포워딩 ( 443 )
      containerPort: 8443
```
### 1.2 Pod 환경변수
#### 1.2.1 Pod env
service를 통해 pod를 외부로 노출시킬 수 있습니다. 따라서 , pod가 service의 port와 ip를 알아야 합니다.
pod는 service의 port번호와 ip를 pod 환경변수 및 kube-dns가 생성하는 FQDN으로 알 수 있습니다.

```bash
kubectl exec {pod-name} env
```
위 명령어를 통해 Pod의 환경 변수를 확인할 수 있습니다.
Pod 환경변수에는 service의 port와 ip를 가지고 있습니다.
아래와 같이 출력되게 됩니다.

아래 결과가 출력되는 Pod에 연결된 service 이름은 kubia 입니다.

만약 service의 이름이 jinseong이라면 , JINSEONG_PORT_80_TCP .... 이런식으로 만들어집니다.
```bash
...
KUBERNETES_SERVICE_HOST=10.96.0.1
KUBIA_PORT_80_TCP_PORT=80 # port
KUBIA_PORT_443_TCP_ADDR=10.111.160.102 # service ip
KUBIA_PORT_443_TCP_PROTO=tcp # protocol
KUBIA_PORT_80_TCP_ADDR=10.111.160.102 # service ip
KUBIA_PORT_443_TCP_PORT=443 # port
KUBIA_PORT_443_TCP=tcp://10.111.160.102:443 # service ip & port
KUBERNETES_PORT=tcp://10.96.0.1:443 # kubernetes service ip & port
HOSTNAME=kubia-7px5x # pod name
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
```
#### 1.2.2 kube-dns
kubernetes는 kube-system namespace에 kube-dns pod를 생성시킵니다.
해당 Pod는 kubernetes 내부의 DNS서버 역할을 하며 , 모든 Pod는 기본적으로 kube-dns의 dns 서버를 사용하도록 구성됩니다.
- Pod 내부의 /etc/resolv.conf 파일을 확인해 보면 . 알 수 있다.
- 각 파드의 dnsPolicy 설정을 통해 kube-dns ( 내부 dns 서버 ) 를 사용할지 여부를 지정할 수 있다.

파드 내부에서 실행된 모든 DNS 쿼리는 kube-dns를 통과합니다.

***각 서비스는 내부 DNS 서버 ( kube-dns ) 에서 DNS 항목을 가져오고 , 서비스 이름을 알 고 있는 Pod는 환경변수 대신 FQDN( 정규화된 도메인 이름 ) 으로 액세스 할 수 있습니다.***

FQDN은 아래와 같은 형태를 띄게 됩니다.

```
jinseong.default.svc.cluster.local
```

- jinseong : service name
- default : namespace name
- svc : service라는것을 나타냄
- cluster.local : cluster name ( default : cluster.local )

kube-dns를 통해 FQDN를 사용한다 하더라도 , Pod는 환경변수를 통해 service의 ip와 port를 알고 있어야 합니다.
서비스가 표준 port 번호 ( http : 80 , mysql : 8081 .. ) 를 사용하는 경우엔 문제가 없지만 , 그렇지 않은 경우에는 환경변수를 통해 port 번호를 알아야 하기 때문입니다.

***파드 내부에서 FQDN로 curl 명령을 수행하면 , 정상 작동 합니다.
그러나 , 파드 내부에서 FQDN로 ping 명령을 수행하면 , 응답이 없습니다.
이러한 이유는 , service의 cluster ip가 가상 ip 이므로 , service port가 있어야만 의미있는 값이 되기 때문입니다.***
