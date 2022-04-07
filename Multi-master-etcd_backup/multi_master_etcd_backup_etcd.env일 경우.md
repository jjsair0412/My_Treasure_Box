# multi master 환경에서 etcd restore 하는 방법 - master node 3개


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