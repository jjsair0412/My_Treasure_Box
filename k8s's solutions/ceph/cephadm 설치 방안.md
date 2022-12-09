# ceph 설치 방안
## 1. precondition
해당 문서는 ceph를 베어메탈 환경에서 cephadm으로 설치하는 방안에 대해 기술합니다.
## 2. install ceph
cephadm을 공식 git에서 download합니다.
```
#cephadm download
curl --silent --remote-name --location https://github.com/ceph/ceph/raw/quincy/src/cephadm/cephadm
chmod +x cephadm
```

ubuntu일 경우
```
apt update 
apt install -y python3 
sudo update-ca-certificates --fresh
export SSL_CERT_DIR=/etc/ssl/certs

./cephadm add-repo --release quincy
./cephadm install
```

centos인 경우 ( centos7에서 테스트 완료 )
```
#centos7
./cephadm add-repo --release octopus
rpm --import 'https://download.ceph.com/keys/release.asc'
```

mon으로 사용할 vm ( 노드 ) 의 ip를 입력합니다.
```
cephadm bootstrap --mon-ip 10.xx.xx.xxx --allow-fqdn-hostname
sudo cephadm install ceph-common
```

install 완료시 제공되는 정보들인 접근 URL 정보와 user 이름 , password를 기억해 둡니다.
```
URL: https://hostname:8443/
	    User: admin
	Password: tfvi8fb8uh
```
ceph tools가 설치되어 있는 cluster container 접속 command 제공
```
You can access the Ceph CLI with:

	sudo /sbin/cephadm shell --fsid e13eef96-4f66-11ed-8fae-fa163ec466bd -c /etc/ceph/ceph.conf -k /etc/ceph/ceph.client.admin.keyring
```

disk ceph에 mount되지 않고 남아있는 여유 disk를 osd에 mount
```
ceph orch apply osd --all-available-devices
```


