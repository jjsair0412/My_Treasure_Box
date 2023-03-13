# vagrant
vagrant와 virtual box를 이용하여 k8s cluster를 만들었습니다.

vagrantfile 원본은 해당 디렉터리에 존재합니다.

## 0. running environment
- vagrant 
- virtual box 
- host os : window
    - host os가 linux일 경우 , Vagrantfile의 synced_folder 경로 수정 필요
```Vagrantfile
...
master.vm.synced_folder "./join", "/home/vagrant/join"
...
```

|name | version| 
|--|--|
|k8s cluster |1.26 |
|container runtime |containerd |

### 0.1 minimum requirements
|name |requirements | 
|--|--|
|cpu |16Core |
|memory |16GB |

최소 요구사항이 안된다면 , Vagrantfile의 cpu , memory값 수정하여 반영

## 1. usecase
worker join 명령어가 떨어지는 join directory 생성
```bash
mkdir ./join
```

cluster up
```bash
vagrant up
```

cluster down
- vagrant vm 종료
```bash
vagrant halt
```

cluster exit
- vagrant vm 제거
```bash
vagrant destroy
```

Vagrantfile 변경 시 반영 명령어
- network 정책 및 특정 명령어는 반영 안되는경우 있음 .
```bash
vagrant reload
```
