# multi master 환경에서 etcd restore 하는 방법 - master node 3개

## Backup 전 리소스 정보
```
jenkins 백업 전 ns 목록
[centos@ip-10-250-227-204 ~]$ kubectl get ns -A
NAME              STATUS   AGE
default           Active   15h
jenkins           Active   11h
kube-node-lease   Active   15h
kube-public       Active   15h
kube-system       Active   15h





jenkins 백업 전 default ns get all
[centos@ip-10-250-227-204 ~]$ kubectl get all -A
NAMESPACE     NAME                                           READY   STATUS    RESTARTS   AGE
jenkins       pod/default-jvlh6                              1/1     Running   0          3m6s
jenkins       pod/jenkins-0                                  2/2     Running   2          11h
kube-system   pod/calico-kube-controllers-8575b76f66-zz8xz   1/1     Running   1          15h
kube-system   pod/calico-node-72wss                          1/1     Running   1          15h
kube-system   pod/calico-node-fnrdz                          1/1     Running   1          15h
kube-system   pod/calico-node-nghxm                          1/1     Running   1          15h
kube-system   pod/coredns-8474476ff8-blnpv                   1/1     Running   1          15h
kube-system   pod/coredns-8474476ff8-vw7l7                   1/1     Running   1          15h
kube-system   pod/dns-autoscaler-7df78bfcfb-4gm94            1/1     Running   1          15h
kube-system   pod/kube-apiserver-node1                       1/1     Running   1          15h
kube-system   pod/kube-apiserver-node2                       1/1     Running   1          15h
kube-system   pod/kube-apiserver-node3                       1/1     Running   1          15h
kube-system   pod/kube-controller-manager-node1              1/1     Running   2          15h
kube-system   pod/kube-controller-manager-node2              1/1     Running   2          15h
kube-system   pod/kube-controller-manager-node3              1/1     Running   2          15h
kube-system   pod/kube-proxy-8jc96                           1/1     Running   1          15h
kube-system   pod/kube-proxy-klfw9                           1/1     Running   1          15h
kube-system   pod/kube-proxy-xpx49                           1/1     Running   1          15h
kube-system   pod/kube-scheduler-node1                       1/1     Running   2          15h
kube-system   pod/kube-scheduler-node2                       1/1     Running   2          15h
kube-system   pod/kube-scheduler-node3                       1/1     Running   2          15h
kube-system   pod/nodelocaldns-cn9v2                         1/1     Running   1          15h
kube-system   pod/nodelocaldns-n7s7x                         1/1     Running   1          15h
kube-system   pod/nodelocaldns-vtf2d                         1/1     Running   1          15h

NAMESPACE     NAME                    TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)                  AGE
default       service/kubernetes      ClusterIP   10.233.0.1      <none>        443/TCP                  15h
jenkins       service/jenkins         NodePort    10.233.46.84    <none>        8080:32593/TCP           11h
jenkins       service/jenkins-agent   ClusterIP   10.233.26.181   <none>        50000/TCP                11h
kube-system   service/coredns         ClusterIP   10.233.0.3      <none>        53/UDP,53/TCP,9153/TCP   15h

NAMESPACE     NAME                          DESIRED   CURRENT   READY   UP-TO-DATE   AVAILABLE   NODE SELECTOR            AGE
kube-system   daemonset.apps/calico-node    3         3         3       3            3           kubernetes.io/os=linux   15h
kube-system   daemonset.apps/kube-proxy     3         3         3       3            3           kubernetes.io/os=linux   15h
kube-system   daemonset.apps/nodelocaldns   3         3         3       3            3           kubernetes.io/os=linux   15h

NAMESPACE     NAME                                      READY   UP-TO-DATE   AVAILABLE   AGE
kube-system   deployment.apps/calico-kube-controllers   1/1     1            1           15h
kube-system   deployment.apps/coredns                   2/2     2            2           15h
kube-system   deployment.apps/dns-autoscaler            1/1     1            1           15h

NAMESPACE     NAME                                                 DESIRED   CURRENT   READY   AGE
kube-system   replicaset.apps/calico-kube-controllers-8575b76f66   1         1         1       15h
kube-system   replicaset.apps/coredns-8474476ff8                   2         2         2       15h
kube-system   replicaset.apps/dns-autoscaler-7df78bfcfb            1         1         1       15h

NAMESPACE   NAME                       READY   AGE
jenkins     statefulset.apps/jenkins   1/1     11h






jenkins 백업 전 jenkins ns get all
[centos@ip-10-250-227-204 ~]$ kubectl get all -A -n jenkins
NAMESPACE     NAME                                           READY   STATUS    RESTARTS   AGE
jenkins       pod/default-jvlh6                              1/1     Running   0          3m24s
jenkins       pod/jenkins-0                                  2/2     Running   2          11h
kube-system   pod/calico-kube-controllers-8575b76f66-zz8xz   1/1     Running   1          15h
kube-system   pod/calico-node-72wss                          1/1     Running   1          15h
kube-system   pod/calico-node-fnrdz                          1/1     Running   1          15h
kube-system   pod/calico-node-nghxm                          1/1     Running   1          15h
kube-system   pod/coredns-8474476ff8-blnpv                   1/1     Running   1          15h
kube-system   pod/coredns-8474476ff8-vw7l7                   1/1     Running   1          15h
kube-system   pod/dns-autoscaler-7df78bfcfb-4gm94            1/1     Running   1          15h
kube-system   pod/kube-apiserver-node1                       1/1     Running   1          15h
kube-system   pod/kube-apiserver-node2                       1/1     Running   1          15h
kube-system   pod/kube-apiserver-node3                       1/1     Running   1          15h
kube-system   pod/kube-controller-manager-node1              1/1     Running   2          15h
kube-system   pod/kube-controller-manager-node2              1/1     Running   2          15h
kube-system   pod/kube-controller-manager-node3              1/1     Running   2          15h
kube-system   pod/kube-proxy-8jc96                           1/1     Running   1          15h
kube-system   pod/kube-proxy-klfw9                           1/1     Running   1          15h
kube-system   pod/kube-proxy-xpx49                           1/1     Running   1          15h
kube-system   pod/kube-scheduler-node1                       1/1     Running   2          15h
kube-system   pod/kube-scheduler-node2                       1/1     Running   2          15h
kube-system   pod/kube-scheduler-node3                       1/1     Running   2          15h
kube-system   pod/nodelocaldns-cn9v2                         1/1     Running   1          15h
kube-system   pod/nodelocaldns-n7s7x                         1/1     Running   1          15h
kube-system   pod/nodelocaldns-vtf2d                         1/1     Running   1          15h

NAMESPACE     NAME                    TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)                  AGE
default       service/kubernetes      ClusterIP   10.233.0.1      <none>        443/TCP                  15h
jenkins       service/jenkins         NodePort    10.233.46.84    <none>        8080:32593/TCP           11h
jenkins       service/jenkins-agent   ClusterIP   10.233.26.181   <none>        50000/TCP                11h
kube-system   service/coredns         ClusterIP   10.233.0.3      <none>        53/UDP,53/TCP,9153/TCP   15h

NAMESPACE     NAME                          DESIRED   CURRENT   READY   UP-TO-DATE   AVAILABLE   NODE SELECTOR            AGE
kube-system   daemonset.apps/calico-node    3         3         3       3            3           kubernetes.io/os=linux   15h
kube-system   daemonset.apps/kube-proxy     3         3         3       3            3           kubernetes.io/os=linux   15h
kube-system   daemonset.apps/nodelocaldns   3         3         3       3            3           kubernetes.io/os=linux   15h

NAMESPACE     NAME                                      READY   UP-TO-DATE   AVAILABLE   AGE
kube-system   deployment.apps/calico-kube-controllers   1/1     1            1           15h
kube-system   deployment.apps/coredns                   2/2     2            2           15h
kube-system   deployment.apps/dns-autoscaler            1/1     1            1           15h

NAMESPACE     NAME                                                 DESIRED   CURRENT   READY   AGE
kube-system   replicaset.apps/calico-kube-controllers-8575b76f66   1         1         1       15h
kube-system   replicaset.apps/coredns-8474476ff8                   2         2         2       15h
kube-system   replicaset.apps/dns-autoscaler-7df78bfcfb            1         1         1       15h

NAMESPACE   NAME                       READY   AGE
jenkins     statefulset.apps/jenkins   1/1     11h





jenkins 백업 전 pv
[centos@ip-10-250-227-204 ~]$ kubectl get pv
NAME          CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS   CLAIM                 STORAGECLASS   REASON   AGE
jenkins-pvc   20Gi       RWO            Retain           Bound    jenkins/jenkins-pvc                           11h



jenkins 백업 전 pvc
[centos@ip-10-250-227-204 ~]$ kubectl get pvc -n jenkins
NAME          STATUS   VOLUME        CAPACITY   ACCESS MODES   STORAGECLASS   AGE
jenkins-pvc   Bound    jenkins-pvc   20Gi       RWO                           11h

[centos@ip-10-250-227-204 ~]$ kubectl get pvc
No resources found in default namespace.




jenkins 백업 전 설치한  플러그인 목록
LDAP	 
Token Macro	 
OkHttp	
GitHub API	 
GitHub	 
OWASP Markup Formatter	 
Ant	 
```
#  START BACKUP
## etcd snapshot 생성
```
ETCDCTL_API=3 etcdctl snapshot save <snapshot save할 .db파일 위치> \
--cert=<cert_file_path> \
--key=<key_file_path> \
--cacert=<cacert_file_path> \
--endpoints=https://127.0.0.1:2379
```

### master etcd를 제외한 node etcd stop

```
$ systemctl stop etcd

$ systemctl status etcd
```
###  master etcd를 제외한 /var/lib/etcd 폴더 제거
```
$ rm -rf /var/lib/etcd
```
## master node 1번에서 작업

### 1. member 폴더 제거
```
$ rm -rf /var/lib/etcd
```

### 2. restore

```
ETCDCTL_API=3 etcdctl snapshot restore <backup_file_path> --endpoints=https://127.0.0.1:2379 \
--data-dir=<member_file_path> \
--cert=<cert_key_file_path> \
--key=<key_file_path> \
--cacert=<ca_cert_file_path> \
--name=etcd1 \
--initial-cluster="etcd1=https://<node1_ip>:2380,etcd2=https://<node2_ip>:2380,etcd3=https://<node3_ip>:2380" \ 
--initial-advertise-peer-urls="https://<node1_ip>:2380"

```



  

### 2. 2. etcd.env 수정
  

```
- ETCD_INITIAL_CLUSTER_STATE=new # 기존 existing 을 new로 변경
- ETCD_INITIAL_CLUSTER=etcd1=https://<node1_ip>:2380 # 자기 자신만 바라보게끔 변경
```

### 3. etcd restart

```
$ systemctl restart etcd
$ systemctl status etcd
```

### 4. 정상적으로 실행됐는지 member list 명령어를 통해 확인
- 결과에 자기 자신만 출력되어야 한다. 
- node1에 대한 정보만 출력되어야 한다.
```

$ ETCDCTL_API=3 etcdctl --write-out=table member list --endpoints=https://127.0.0.1:2379 \
--cacert=<cacert_path> \
--cert=<cert_path> \
--key=<key_path>


+-----------------+---------+-------+-----------------------------+-----------------------------+------------+
|       ID        | STATUS  | NAME  |         PEER ADDRS          |        CLIENT ADDRS         | IS LEARNER |
+-----------------+---------+-------+-----------------------------+-----------------------------+------------+
| e01d37e9dfb1f6a | started | etcd1 | https://<node1_ip>:2380     | https://<node1_ip>:2379     |      false |
+-----------------+---------+-------+-----------------------------+-----------------------------+------------+


```


  

이때 PEER ADDRS이 localhost가 찍혀 있으면 안됀다.

만약 localhost가 출력될 경우 , etcd member update 명령을 통해서 변경시켜주어야 한다.

  
  

### 5. 데이터 restore 상태 체크
```
$ kubectl get pods 
```
  

### 6. member add
```
$ ETCDCTL_API=3 etcdctl member add <etcd_name> --peer-urls=https://<node2_ip>:2380 --endpoints=https://127.0.0.1:2379 \
--cert=<cert_file_path> \
--key=<key_file_path> \
--cacert=<cacert_file_path> \
--endpoints=https://127.0.0.1:2379
```

- add 후 출력되는 결과 복사해놓기
**ex )**

```
ETCD_NAME="etcd2"
ETCD_INITIAL_CLUSTER="etcd1=https://<node1_ip>:2380,etcd2=https://<node2_ip>:2380"
ETCD_INITIAL_ADVERTISE_PEER_URLS="https://<node2_ip>:2380"
ETCD_INITIAL_CLUSTER_STATE="existing"
```

  

## master node 2번에서 작업

### 1. etcd.env파일 수정
- 복사해놓은 add 후 결과를 etcd.env파일에 붙여넣기 한다.
- 기존 정보는 주석 처리 하거나 제거한다.
```
ETCD_NAME="etcd2"
ETCD_INITIAL_CLUSTER="etcd1=https://<node1_ip>:2380,etcd2=https://<node2_ip>:2380"
ETCD_INITIAL_ADVERTISE_PEER_URLS="https://<node2_ip>:2380"
ETCD_INITIAL_CLUSTER_STATE="existing"
```


  

### 2. etcd start

```
$ systemctl start etcd

$ systemctl status etcd
```

  ## master node 1번에서 작업

### 4. add한 member 상태 start인지 확인 
- member list 명령어를 통해 start 상태 확인

```
$ $ ETCDCTL_API=3 etcdctl --write-out=table member list --endpoints=https://127.0.0.1:2379 \
--cacert=<cacert_path> \
--cert=<cert_path> \
--key=<key_path>
```

### 6. member add
- 마지막 node3 추가
```
$ ETCDCTL_API=3 etcdctl member add <etcd_name> --peer-urls=https://<node3_ip>:2380 --endpoints=https://127.0.0.1:2379 \
--cert=<cert_file_path> \
--key=<key_file_path> \
--cacert=<cacert_file_path> \
--endpoints=https://127.0.0.1:2379
```

- add 후 출력되는 결과 복사해놓기
**ex )**

```
ETCD_NAME="etcd3"
ETCD_INITIAL_CLUSTER="etcd1=https://<node1_ip>:2380,etcd3=https://<node3_ip>:2380,etcd2=https://<node2_ip>:2380"
ETCD_INITIAL_ADVERTISE_PEER_URLS="https://<node3_ip>:2380"
ETCD_INITIAL_CLUSTER_STATE="existing"
```
  
  ## master node 3번에서 작업 
  ### 1. etcd.env파일 수정
- 복사해놓은 add 후 결과를 etcd.env파일에 붙여넣기 한다.
- 기존 정보는 주석 처리 하거나 제거한다.
```
ETCD_NAME="etcd3"
ETCD_INITIAL_CLUSTER="etcd1=https://<node1_ip>:2380,etcd3=https://<node3_ip>:2380,etcd2=https://<node2_ip>:2380"
ETCD_INITIAL_ADVERTISE_PEER_URLS="https://<node3_ip>:2380"
ETCD_INITIAL_CLUSTER_STATE="existing"
```


  

### 2. etcd start

```
$ systemctl start etcd

$ systemctl status etcd
```


## 전체 node에서 작업 
- 전체 node reboot

## 주의사항
- add 후 출력되는 결과값인 env 정보들을 추가한 이후 ,변경하면 안됀다.
변경했을경우 다른 node의 etcd를 찾지 못한다. 



## restore 이후 리소스 정보
```
jenkins restore 후 ns 목록
[centos@ip-10-250-227-204 ~]$ kubectl get ns -A
NAME              STATUS   AGE
default           Active   19h
jenkins           Active   14h
kube-node-lease   Active   19h
kube-public       Active   19h
kube-system       Active   19h




jenkins restore 후 default ns get all
[centos@ip-10-250-227-204 ~]$ kubectl get all -A
NAMESPACE     NAME                                           READY   STATUS    RESTARTS   AGE
jenkins       pod/jenkins-0                                  2/2     Running   2          14h
kube-system   pod/calico-kube-controllers-8575b76f66-zz8xz   1/1     Running   8          19h
kube-system   pod/calico-node-72wss                          1/1     Running   1          19h
kube-system   pod/calico-node-fnrdz                          1/1     Running   2          19h
kube-system   pod/calico-node-nghxm                          1/1     Running   1          19h
kube-system   pod/coredns-8474476ff8-blnpv                   1/1     Running   1          19h
kube-system   pod/coredns-8474476ff8-vw7l7                   1/1     Running   1          19h
kube-system   pod/dns-autoscaler-7df78bfcfb-4gm94            1/1     Running   1          19h
kube-system   pod/kube-apiserver-node1                       1/1     Running   22         19h
kube-system   pod/kube-apiserver-node2                       1/1     Running   20         19h
kube-system   pod/kube-apiserver-node3                       1/1     Running   18         19h
kube-system   pod/kube-controller-manager-node1              1/1     Running   5          19h
kube-system   pod/kube-controller-manager-node2              1/1     Running   3          19h
kube-system   pod/kube-controller-manager-node3              1/1     Running   4          19h
kube-system   pod/kube-proxy-8jc96                           1/1     Running   1          19h
kube-system   pod/kube-proxy-klfw9                           1/1     Running   1          19h
kube-system   pod/kube-proxy-xpx49                           1/1     Running   1          19h
kube-system   pod/kube-scheduler-node1                       1/1     Running   6          19h
kube-system   pod/kube-scheduler-node2                       1/1     Running   4          19h
kube-system   pod/kube-scheduler-node3                       1/1     Running   3          19h
kube-system   pod/nodelocaldns-cn9v2                         1/1     Running   1          19h
kube-system   pod/nodelocaldns-n7s7x                         1/1     Running   1          19h
kube-system   pod/nodelocaldns-vtf2d                         1/1     Running   1          19h

NAMESPACE     NAME                    TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)                  AGE
default       service/kubernetes      ClusterIP   10.233.0.1      <none>        443/TCP                  19h
jenkins       service/jenkins         NodePort    10.233.46.84    <none>        8080:32593/TCP           14h
jenkins       service/jenkins-agent   ClusterIP   10.233.26.181   <none>        50000/TCP                14h
kube-system   service/coredns         ClusterIP   10.233.0.3      <none>        53/UDP,53/TCP,9153/TCP   19h

NAMESPACE     NAME                          DESIRED   CURRENT   READY   UP-TO-DATE   AVAILABLE   NODE SELECTOR            AGE
kube-system   daemonset.apps/calico-node    3         3         3       3            3           kubernetes.io/os=linux   19h
kube-system   daemonset.apps/kube-proxy     3         3         3       3            3           kubernetes.io/os=linux   19h
kube-system   daemonset.apps/nodelocaldns   3         3         3       3            3           kubernetes.io/os=linux   19h

NAMESPACE     NAME                                      READY   UP-TO-DATE   AVAILABLE   AGE
kube-system   deployment.apps/calico-kube-controllers   1/1     1            1           19h
kube-system   deployment.apps/coredns                   2/2     2            2           19h
kube-system   deployment.apps/dns-autoscaler            1/1     1            1           19h

NAMESPACE     NAME                                                 DESIRED   CURRENT   READY   AGE
kube-system   replicaset.apps/calico-kube-controllers-8575b76f66   1         1         1       19h
kube-system   replicaset.apps/coredns-8474476ff8                   2         2         2       19h
kube-system   replicaset.apps/dns-autoscaler-7df78bfcfb            1         1         1       19h

NAMESPACE   NAME                       READY   AGE
jenkins     statefulset.apps/jenkins   1/1     14h




jenkins restore 후 jenkins ns get all
[centos@ip-10-250-227-204 ~]$ kubectl get all -A -n jenkins
NAMESPACE     NAME                                           READY   STATUS    RESTARTS   AGE
jenkins       pod/jenkins-0                                  2/2     Running   2          14h
kube-system   pod/calico-kube-controllers-8575b76f66-zz8xz   1/1     Running   8          19h
kube-system   pod/calico-node-72wss                          1/1     Running   1          19h
kube-system   pod/calico-node-fnrdz                          1/1     Running   2          19h
kube-system   pod/calico-node-nghxm                          1/1     Running   1          19h
kube-system   pod/coredns-8474476ff8-blnpv                   1/1     Running   1          19h
kube-system   pod/coredns-8474476ff8-vw7l7                   1/1     Running   1          19h
kube-system   pod/dns-autoscaler-7df78bfcfb-4gm94            1/1     Running   1          19h
kube-system   pod/kube-apiserver-node1                       1/1     Running   22         19h
kube-system   pod/kube-apiserver-node2                       1/1     Running   20         19h
kube-system   pod/kube-apiserver-node3                       1/1     Running   18         19h
kube-system   pod/kube-controller-manager-node1              1/1     Running   5          19h
kube-system   pod/kube-controller-manager-node2              1/1     Running   3          19h
kube-system   pod/kube-controller-manager-node3              1/1     Running   4          19h
kube-system   pod/kube-proxy-8jc96                           1/1     Running   1          19h
kube-system   pod/kube-proxy-klfw9                           1/1     Running   1          19h
kube-system   pod/kube-proxy-xpx49                           1/1     Running   1          19h
kube-system   pod/kube-scheduler-node1                       1/1     Running   6          19h
kube-system   pod/kube-scheduler-node2                       1/1     Running   4          19h
kube-system   pod/kube-scheduler-node3                       1/1     Running   3          19h
kube-system   pod/nodelocaldns-cn9v2                         1/1     Running   1          19h
kube-system   pod/nodelocaldns-n7s7x                         1/1     Running   1          19h
kube-system   pod/nodelocaldns-vtf2d                         1/1     Running   1          19h

NAMESPACE     NAME                    TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)                  AGE
default       service/kubernetes      ClusterIP   10.233.0.1      <none>        443/TCP                  19h
jenkins       service/jenkins         NodePort    10.233.46.84    <none>        8080:32593/TCP           14h
jenkins       service/jenkins-agent   ClusterIP   10.233.26.181   <none>        50000/TCP                14h
kube-system   service/coredns         ClusterIP   10.233.0.3      <none>        53/UDP,53/TCP,9153/TCP   19h

NAMESPACE     NAME                          DESIRED   CURRENT   READY   UP-TO-DATE   AVAILABLE   NODE SELECTOR            AGE
kube-system   daemonset.apps/calico-node    3         3         3       3            3           kubernetes.io/os=linux   19h
kube-system   daemonset.apps/kube-proxy     3         3         3       3            3           kubernetes.io/os=linux   19h
kube-system   daemonset.apps/nodelocaldns   3         3         3       3            3           kubernetes.io/os=linux   19h

NAMESPACE     NAME                                      READY   UP-TO-DATE   AVAILABLE   AGE
kube-system   deployment.apps/calico-kube-controllers   1/1     1            1           19h
kube-system   deployment.apps/coredns                   2/2     2            2           19h
kube-system   deployment.apps/dns-autoscaler            1/1     1            1           19h

NAMESPACE     NAME                                                 DESIRED   CURRENT   READY   AGE
kube-system   replicaset.apps/calico-kube-controllers-8575b76f66   1         1         1       19h
kube-system   replicaset.apps/coredns-8474476ff8                   2         2         2       19h
kube-system   replicaset.apps/dns-autoscaler-7df78bfcfb            1         1         1       19h

NAMESPACE   NAME                       READY   AGE
jenkins     statefulset.apps/jenkins   1/1     14h





jenkins restore 후 pv
[centos@ip-10-250-227-204 ~]$  kubectl get pv
NAME          CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS   CLAIM                 STORAGECLASS   REASON   AGE
jenkins-pvc   20Gi       RWO            Retain           Bound    jenkins/jenkins-pvc                           14h



jenkins restore 후 pvc
[centos@ip-10-250-227-204 ~]$ kubectl get pvc -n jenkins
NAME          STATUS   VOLUME        CAPACITY   ACCESS MODES   STORAGECLASS   AGE
jenkins-pvc   Bound    jenkins-pvc   20Gi       RWO                           15h



[centos@ip-10-250-227-204 ~]$ kubectl get pvc
No resources found in default namespace.



jenkins restore 후 설치한  플러그인 목록
LDAP
Token Macro	
OkHttp	
GitHub API
GitHub
OWASP Markup Formatter
Ant
```