# Kubernetes HA Architecture
Kubernetes를 구축할 때 , 고 가용성 을 고려하여 구축한 아키텍처 입니다.

## 1. 구성
MASTER : 3
WORKER : 8

개발용 solution들은 ingress gateway를 이용하여 접근합니다.
- worker 1 ~ worker 4번에만 solution pod가 동작하도록 nodeselector 설정

실 application들은 istio gateway를 이용하여 접근합니다.
- worker 5 ~ worker 8번에만 application pod가 동작하도록 nodeselector 설정

### 1.1 LB 구성
LB는 각각의 gateway마다 설정합니다.

- solution용 LB VIP : 
    - 10.1.1.2:80 , 443 -> worker1IP~4IP:30001 , worker1IP~4IP:30002
- application용 LB VIP : 
    - 10.1.1.3:80 , 443 -> worker5IP~8IP:30003 , worker5IP~8IP:30004
- master 부하분산용 LB VIP : 
    - 10.1.1.4:5000 -> master1IP~3IP:6443


### 1.2 kubectl bastion server
쿠버네티스 관리자는 bastion 서버에서 10.1.1.4:5000으로 kubectl 명령어를 날립니다.
kubeconfig 파일에서 해당 정보를 가지고있게 되며, 요청받은 LB ( 10.1.1.4 VIP LB ) 는 부하분산하여
master 1번과 2 , 3번중 하나의 6443 포트 ( kube-apiserver port ) 로 전달하여 명령어를 수행합니다.

### 1.3 ETCD
master 노드에서 따로 꺼낸 ETCD 클러스터로 고 가용성을 확보합니다.

## 관련 문서
- (고 가용성 토폴로지 선택하기)[https://kubernetes.io/ko/docs/setup/production-environment/tools/kubeadm/ha-topology/]
- (kubeadm으로 고 가용성 클러스터 구축하기)[https://kubernetes.io/docs/setup/production-environment/tools/kubeadm/high-availability/]

![k8s_ha_구성][k8s_ha_구성]

[k8s_ha_구성]:./images/k8s_ha_구성.png