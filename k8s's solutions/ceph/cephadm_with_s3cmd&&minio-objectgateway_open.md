# cephadm with s3cmd
cephadm은 설치한 이후에 object gateway를 open 해야 합니다.

해당 예제는 cephadm에서 ceph shell 명령어로 진행 합니다.

## 1. ceph rgw service 생성
ceph dashboard에서 rgw service를 생성해야 합니다.

초기 rgw를 배포 합니다.
- hostname은 mon의 hostname을 사용합니다. 
    - cephadm 부트스트랩시 사용한 hostname
```bash
#초기 rgw 배포
#ceph orch apply <유형> <name>
$ ceph orch apply rgw foo
```

```bash
#rgw Label 추가
#ceph orch host label add <hostname> <name>
$ ceph orch host label add ubuntu rgw
```

mon을 부트스트랩 할 때 사용한 명령어는 다음과 같습니다.
```bash
$ cephadm bootstrap --mon-ip 10.0.0.1 --allow-fqdn-hostname --ssh-user ubuntu
```

```bash
#rgw 자동자격 증명이 자동으로 기본 dashboard로 생성되어 있는 것을 수동 커맨드 적용
$ ceph dashboard set-rgw-credentials
```

rgw service 상태가 running인지 확인합니다.
```bash
~ ceph orch ls
```

## 2. ceph user 생성
s3 command를 날릴 user를 생성합니다.

```bash
#사용 예
$ radosgw-admin user create --uid=USER_ID --display-name=DISPLAY_NAME --system

#실제 수행 명령어
$ radosgw-admin user create --uid=admin --display-name=admin --system
```

그리고 해당 user의 accesskey 및 secretkey 를 아래 명령어로 확인한 후 , 복사하여 저장해 둡니다.
```bash
#사용 예
$ radosgw-admin user info --uid=USER_ID

#실제 수행 명령어
$ radosgw-admin user info --uid=admin
```

저장한 key값 두개 (accesskey , secretkey) 를 dashboard에 제공합니다.
```bash
#사용 예
$ ceph dashboard set-rgw-api-access-key ACCESS_KEY_FILE
$ ceph dashboard set-rgw-api-secret-key SECRET_KEY_FILE

#실 수행 명령어
$ ceph dashboard set-rgw-api-access-key ACCESS_KEY_FILE
$ ceph dashboard set-rgw-api-secret-key SECRET_KEY_FILE
```

## s3cmd 설치
oboject gateway에 접근하고 테스트 할 s3cmd를 설치 합니다.

```bash
$ sudo apt-get update

$ sudo apt install s3cmd
```

s3cmd configuration 진행합니다.
- 아래 설명대로 진행합니다.
```bash
$ s3cmd --configure

Access Key와 Secret Key는 Object Gateway -> Users의 Key값을 사용
Default Region은 기본 값을 사용.
S3 Endpoint인 rgw service는 80번 포트로 open됩니다.
accessing a bucket은 위의 주소를 그대로 사용하거나 버켓 경로까지 기입.
Use HTTPS protocol은 no로 설정한다(yes일 경우 ca.cert파일 필요)
```

ceph orch ps | grep rgw 명령어로 rgw port 정보와  , rgw service가 올라간 mon이 어딘지 찾습니다.
```bash
$ ceph orch ps | grep rgw
rgw.foo.ip-10-0-0-1.imgxxl  ip-10-0-0-1  *:80         running (24m)     3m ago   3h    84.1M        -  17.2.5     cc65afd6173a  6db9099c5afb 
```

rgw.foo service가 올라간 mon의 external ip : 80번이 endpoint가 됩니다.

### s3cmd test
s3cmd ls 명령어로 ceph dashboard에서 생성한 ceph bucket이 정상적으로 출력되는지 확인 합니다.
```bash
$ s3cmd ls
2023-02-21 06:35  s3://ceph-test
2023-02-21 07:29  s3://downloadfile
2023-02-21 05:49  s3://test
2023-02-21 06:20  s3://test-3
2023-02-21 06:36  s3://ttt
2023-02-21 07:29  s3://uploadfile
```

## minio 일 경우
2022년 2월부터 , object gateway 모드를 지원 종료한다는 공지가 있습니다.
- 2022년 10월 24일부로 지원이 종료되었습니다.

따라서 지원 종료가 되지 않은 minio 버전을 사용해야 하며 , 해당 버전은 다음과 같습니다.
- helm chart는 minio:RELEASE.2021-02-14T04-01-33Z 버전


```bash
$ helm repo add minio https://helm.min.io/

$ helm install -n minio minio minio/minio --version 8.0.10 --create-namespace

$ helm install -n minio minio minio/minio -f custom.yaml --version 8.0.10 --create-names
pace
```

minio를 helm으로 설치하고 , 연동할 경우 values.yaml의 다음 값들을 변경 한 뒤 helm install 합니다.
```yaml
$ cat custom.yaml
persistence:
  enabled: true
  size: 5Gi
service:
  type: NodePort
  port: 9000
  nodePort: 32000
s3gateway: # minio를 s3 server가 아닌 s3를 보여주는 gateway mode로 사용
  enabled: true
  replicas: 4
  serviceEndpoint: "http://3.3.3.3:80" #s3 endpoint 주소
  accessKey: "accesskey" #ceph object 유저의 accesskey
  secretKey: "secretkey" #ceph object 유저의 secretkey
```