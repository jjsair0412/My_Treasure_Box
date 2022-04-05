# multi master 환경에서 etcd restore 하는 방법 - master node 3

### 모든 etcd.service stop
```
$ systemctl stop etcd
$systemctl status etcd
```
## master node 1번에서 작업 

### 1. master etcd의 etcd.env 파일 수정
```
ETCD_INITIAL_CLUSTER_STATE=existing 옵션을 new로 변경

ETCD_INITIAL_CLUSTER=etcd1=https://<host-ip>:2380, etcd2=https://<host-ip2>:2380, etcd3=https://<host-ip3>:2380  
```
위 옵션을 현재 etcd host만 남게끔 변경.
만약 현재 master node의 ip가 1.1.1.1 이라면 ,
```
ETCD_INITIAL_CLUSTER=etcd1=https://1.1.1.1:2380
```
위처럼 변경

### 2. etcd member 폴더 제거
- 해당 폴더에 파드 관련정보나 전체적인것들이 저장됨
```
$ rm -rf /var/lib/etcd
```
### 3. etcd 실행
```
$ systemctl start etcd
$ systemctl status etcd
```
### 4. 정상적으로 실행됐는지 member list 명령어를 통해 확인
```
$ ETCDCTL_API=3 etcdctl --write-out=table member list --endpoints=https://127.0.0.1:2379 \
--cacert=<cacert_path> \
--cert=<cert_path> \
--key=<key_path>
```

이때 PEER ADDRS이 localhost가 찍혀 있으면 안됀다.
그럴경우 kube-apiserver에 저장된 정보와 , 새로 생성해준 etcd member 정보가 맞지 않아 localhost를 바라보게 되는 것.

docker에 올라간 kube-apiserver를 kill해 주어야 한다.


### 5. etcd restore
만들어둔 backup 파일을 통해서 restore
```
$ ETCDCTP_API=3 etcdctl snapshot restore <backup_file_path> --data-dir=/var/lib/etcd

# usecase
$ ETCDCTP_API=3 etcdctl snapshot restore ~/backup/jenkins-backup.db --data-dir=/var/lib/etcd_new
```

### 6. 2번째 etcd add
add 명령어를 이용해서 add
```
$ ETCDCTP_API=3 etcdctl member add etcd2 --peer-urls=https://<Second_master_node_ip>:2380 --endpoints=https://127.0.0.1:2379 \
--cacert=<cacert_path> \
--cert=<cert_path> \
--key=<key_path>
```
member list 명령어를통해 add가 됐는지 확인
```
$ ETCDCTL_API=3 etcdctl --write-out=table member list --endpoints=https://127.0.0.1:2379 \
--cacert=<cacert_path> \
--cert=<cert_path> \
--key=<key_path>
```

## master node 2번에서 작업
### 1. etcd.env파일 수정
```
ETCD_INITIAL_CLUSTER_STATE=existing 옵션 그대로 반영
ETCD_INITIAL_CLUSTER=etcd1=https://<host-ip>:2380, etcd2=https://<host-ip2>:2380, etcd3=https://<host-ip3>:2380 
```
위 옵션을 자기 자신과 1번 master node만 포함되게끔 수정.

만약 node 2번의 host ip가 2.2.2.2 , node 1번의 host ip가 1.1.1.1 이라면 ,
```
ETCD_INITIAL_CLUSTER=etcd1=https://1.1.1.1:2380,etcd2=https://2.2.2.2.30.152:2380
```
위처럼 수정

### 2. etcd member 폴더 제거
- 해당 폴더에 파드관련정보나 전체적인것들이 저장됨
```
$ rm -rf /var/lib/etcd
```
### 3. etcd 실행
```
$ systemctl start etcd
$ systemctl status etcd
```

### 4. 정상적으로 실행됐는지 member list 명령어를 통해 확인
```
$ ETCDCTL_API=3 etcdctl --write-out=table member list --endpoints=https://127.0.0.1:2379 \
--cacert=<cacert_path> \
--cert=<cert_path> \
--key=<key_path>
```
list 결과에서 add시켜준 2번 master node의 etcd까지 start상태인지 확인


## master node 1번에서 작업 
### 1. 3번째 etcd add
add 명령어를 이용해서 add
```
$ ETCDCTP_API=3 etcdctl member add etcd2 --peer-urls=https://<third_master_node_ip>:2380 --endpoints=https://127.0.0.1:2379 \
--cacert=<cacert_path> \
--cert=<cert_path> \
--key=<key_path>
```

### 2. member list 확인
```
$ ETCDCTL_API=3 etcdctl --write-out=table member list --endpoints=https://127.0.0.1:2379 \
--cacert=<cacert_path> \
--cert=<cert_path> \
--key=<key_path>
```

## master node 3번에서 작업 
### 1. etcd.env파일 수정
```
ETCD_INITIAL_CLUSTER_STATE=existing 옵션 그대로 반영
ETCD_INITIAL_CLUSTER=etcd1=https://<host-ip>:2380, etcd2=https://<host-ip2>:2380, etcd3=https://<host-ip3>:2380 
```
위 옵션을 자기 자신과 1번 master node2번 mater node까지 다 추가되게끔 변경.

만약 node 3번의 host ip가 3.3.3.3 node 2번의 host ip가 2.2.2.2 , node 1번의 host ip가 1.1.1.1 이라면 
```
ETCD_INITIAL_CLUSTER=etcd1=https://1.1.1.1:2380,etcd2=https://2.2.2.2:2380,etcd3=https://3.3.3.3:2380
```
위처럼 수정

### 2. etcd member 폴더 제거
- 해당 폴더에 파드관련정보나 전체적인것들이 저장됨
```
$ rm -rf /var/lib/etcd
```

### 3. etcd 실행
```
$ systemctl start etcd
$ systemctl status etcd
```

### 4. 정상적으로 실행됐는지 member list 명령어를 통해 확인
```
$ ETCDCTL_API=3 etcdctl --write-out=table member list --endpoints=https://127.0.0.1:2379 \
--cacert=<cacert_path> \
--cert=<cert_path> \
--key=<key_path>
```

### 5. master etcd가잇는 master node에서 , member list 결과 확인
```
$ ETCDCTL_API=3 etcdctl --write-out=table member list --endpoints=https://127.0.0.1:2379 \
--cacert=<cacert_path> \
--cert=<cert_path> \
--key=<key_path>
```

add시켜준 3번 master node의 etcd까지 start상태인지 확인