# multi master 환경에서 etcd restore 하는 방법 - master node 3개
## START BACKUP
## etcd snapshot 생성

```
ETCDCTL_API=3 etcdctl snapshot save <snapshot save할 .db파일 위치> \
--cert=<cert_file_path> \
--key=<key_file_path> \
--cacert=<cacert_file_path> \
--endpoints=https://127.0.0.1:2379
```

## master etcd node가 될 node를 제외한 나머지 node에서 작업
### 1. etcd.yaml파일 이동
- 기존 static pod로 생성되어있는 etcd를 stop시키기 위함이다.
### 2. kubelet stop
```
$ systemctl stop kubelet
$ sysetmctl status kubelet
```
### 3. member 폴더 삭제
- 기존 member 폴더를 제거한다.
```
$ rm -rf /var/lib/memeber
```
## master node 1번에서 작업
### 1. member 폴더 제거
```
$ rm -rf /var/lib/member
```
### 2. restore
```
$ ETCDCTL_API=3 etcdctl snapshot restore <backup_file 위치> --endpoints=https://127.0.0.1:2379 \
--data-dir=<data-dir 위치> \
--cert=<cert_file_path> \
--key=<key_file_path> \
--cacert=<cacert_file_path> \
```
### 3. member list 확인
- etcd member list 명령어를 통해 자기 자신만 출력되는지 확인
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



### 4. 데이터 restore 상태 체크

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

-   add 후 출력되는 결과 복사해놓기  
 **ex )**

```
ETCD_NAME="etcd2"
ETCD_INITIAL_CLUSTER="etcd1=https://<node1_ip>:2380,etcd2=https://<node2_ip>:2380"
ETCD_INITIAL_ADVERTISE_PEER_URLS="https://<node2_ip>:2380"
ETCD_INITIAL_CLUSTER_STATE="existing"
```

## master node 2번에서 작업
### 1. 옮겨두었던 etcd.yaml파일 기존 위치로 복귀
- 복사해놓은 결과를 보고 , 결과와 동일하게 yaml파일 수정
### 2. kubelet 실행
```
$ systemctl start kubelet
$ systemctl status kubelet
```

## master node 1번에서 작업
### 1. add된 node 상태 확인
```
$ ETCDCTL_API=3 etcdctl --write-out=table member list --endpoints=https://127.0.0.1:2379 \
--cacert=<cacert_path> \
--cert=<cert_path> \
--key=<key_path>
```
### 2. 리소스 백업 상태 확인
```
$ kubectl get pods
```
### 3. member add

```
$ ETCDCTL_API=3 etcdctl member add <etcd_name> --peer-urls=https://<node3_ip>:2380 --endpoints=https://127.0.0.1:2379 \
--cert=<cert_file_path> \
--key=<key_file_path> \
--cacert=<cacert_file_path> \
--endpoints=https://127.0.0.1:2379
```

-   add 후 출력되는 결과 복사해놓기  
 **ex )**

```
ETCD_NAME="etcd2"
ETCD_INITIAL_CLUSTER="etcd1=https://<node1_ip>:2380,etcd2=https://<node2_ip>:2380",etcd3=https://<node3_ip>:2380"
ETCD_INITIAL_ADVERTISE_PEER_URLS="https://<node3_ip>:2380"
ETCD_INITIAL_CLUSTER_STATE="existing"
```
## master node 3번에서 작업
### 1. 옮겨두었던 etcd.yaml파일 기존 위치로 복귀
- 복사해놓은 결과를 보고 , 결과와 동일하게 yaml파일 수정
### 2. kubelet 실행
```
$ systemctl start kubelet
$ systemctl status kubelet
```
## master node 1번에서 작업
### 1. add된 node 상태 확인
```
$ ETCDCTL_API=3 etcdctl --write-out=table member list --endpoints=https://127.0.0.1:2379 \
--cacert=<cacert_path> \
--cert=<cert_path> \
--key=<key_path>
```
### 2. 리소스 백업 상태 확인
```
$ kubectl get pods
```

## 전체 node에서 작업

-   전체 node reboot

## [](https://github.com/jjsair0412/kubernetes_info/blob/main/Multi-master-etcd_backup/multi_master_etcd_backup_etcd.env%EC%9D%BC%20%EA%B2%BD%EC%9A%B0.md#%EC%A3%BC%EC%9D%98%EC%82%AC%ED%95%AD)주의사항

-   add 후 출력되는 결과값인 env 정보들을 추가한 이후 ,변경하면 안됀다. 변경했을경우 다른 node의 etcd를 찾지 못한다.