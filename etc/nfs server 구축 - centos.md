
# NFS 서버 설정 방안
- 해당 문서는 centos7 os에서 nfs 서버를 설정하는 방법에 대해 설명합니다.
## 1. install package
- 아래 명령어를 통해서 NFS 서버를 설치합니다.
```
$ yum -y install nfs-utills
```
## 2. use nfs server
- systemctl 명령어로 nfs-server를 켜줍니다. start , stop , restart 명령어 모두 허용됩니다.
```
# nfs-server enable
$ systemctl enable nfs-server

$ systemctl start nfs-server
```
### 2.1 nfs 설정
- root 계정으로 진행합니다.
- 첫번째로 공유폴더 지정 리스트를 생성해야 합니다.
  /etc/exports 라는 파일을 생성해서 , 리스트를 작성합니다.
```
$ sudo su

$ cat /etc/exports
/share 10.xxx.*.*(rw,sync)
```
위처럼 등록한다면 , ~/share 폴더를 10.250.*.* 망에 있는 모든 PC를 read, write 권한 설정한다는 것 입니다.
또한 sync 옵션은 파일을 쓸 때 서버와 클라인트 싱크를 맞춘다는 의미 입니다.
[nfs 옵션 list](https://server-talk.tistory.com/320)

- / 아래 share 디렉터리를 생성후 권한을 부여합니다.
```
$ cd /
$ mkdir share

# 권한 부여
$ chmod 707 /share

# 권한부여 상태 확인
# ls -al | grep share
drwx---rwx.  2 root root     6 Jun 21 09:27 share
```
- exports에 수정 내용을 반영합니다.
```
$ exportfs -r
```
- nfs-server 서비스를 가동시킵니다.
```
$ systemctl start nfs-server
$ systemctl enable nfs-server
```
- 방화벽 설정이 되어있다면 , 방화벽을 stop합니다.
```
$ service firewalld stop
```
### 3. nfs 설정 확인
```
$ showmount -e
Export list for ip-10-250-226-172.ap-northeast-1.compute.internal:
/share 10.xxx.*.*

$ exportfs -v
/share          10.xxx.*.*(sync,wdelay,hide,no_subtree_check,sec=sys,rw,secure,root_squash,no_all_squash)
```
### 4. 클라이언트와 nfs 연동
- 테스트 환경은 k8s를 사용하고있기 때문에 , nfs를 사용하는 storageclass를 생성하여 연동 하였습니다.
- 만약 일반 클라이언트와 연동하고싶다면 , 아래 포스팅을 참조하면 됩니다.
[일반 클라이언트와 연동방안](https://ansan-survivor.tistory.com/687)

### 4.1 Default psp 설정
- nfs를 설치하기 위해선 전체 클러스터에 기본 설정되어있는 pod security policy에  nfs를 허용하도록. 수정해주어야 합니다.
```
# psp 를 get하여 PodSecurityPolicy를 확인할 수 있다.
$ kubectl get psp
NAME                      PRIV    CAPS   SELINUX    RUNASUSER          FSGROUP     SUPGROUP    READONLYROOTFS   VOLUMES
global-restricted-psp     false          RunAsAny   MustRunAsNonRoot   MustRunAs   MustRunAs   false            configMap,emptyDir,projected,secret,downwardAPI,persistentVolumeClaim
system-unrestricted-psp   true    *      RunAsAny   RunAsAny           RunAsAny    RunAsAny    false            *,
```
- global psp와 system psp 둘다 nfs수정해주어야 한다. volume에 nfs설정
```
$ kubectl edit psp ~
...
apiVersion: policy/v1beta1 
kind: PodSecurityPolicy 
  metadata: 
    name: example 
  spec: 
    volumes: 
     - nfs  # 추가
...
```
[관련 이슈](https://github.com/kubernetes-retired/external-storage/issues/1145)
[관련 podsecuritypolicy k8s 문서](https://kubernetes.io/ko/docs/concepts/policy/pod-security-policy/)


#### 4.2 nfs-client provisioner 설치
- helm chart로 nfs-client provisioner를 설치합니다. [chart](https://artifacthub.io/packages/helm/kvaps/nfs-server-provisioner)
```
$ helm upgrade --install --kubeconfig=$KUBE_CONFIG  nfs-subdir-external-provisioner . \
--set nfs.server=10.xxx.xxx.xxx \ # nfs server 주소
--set nfs.path=/share \ # mount 경로
-n nfs \ 
-f values.yaml

$ kubectl get all -n nfs
NAME                                                  READY   STATUS    RESTARTS   AGE
pod/nfs-subdir-external-provisioner-9bdcb4bb6-mgx68   1/1     Running   0          3m17s

NAME                                              READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/nfs-subdir-external-provisioner   1/1     1            1           15m

NAME                                                         DESIRED   CURRENT   READY   AGE
replicaset.apps/nfs-subdir-external-provisioner-9bdcb4bb6    1         1         1       3m18s
```
- 설치가 완료되면 , storageclass가 생성된 것을 확인할 수 있습니다.
```
$ kubectl get sc
NAME         PROVISIONER                                     RECLAIMPOLICY   VOLUMEBINDINGMODE   ALLOWVOLUMEEXPANSION   AGE
nfs-client   cluster.local/nfs-subdir-external-provisioner   Delete          Immediate           true                   9m
```
- 생성된 sc를 default sc로 변경합니다.
```
$ kubectl patch storageclass nfs-client -p '{"metadata": {"annotations":{"storageclass.kubernetes.io/is-default-class":"true"}}}'
```
### 5. troubleshooting
- 아래 에러가 발생했을 경우 , values.yaml파일의 pod security 속성을 변경해서 helm install
```
Error: container has runAsNonRoot and image will run as root (pod: "nfs-subdir-external-provisioner-5ddc8f ~
```
- runAsUser의 1000번대는 일반 user도 사용할 수 있다는 의미 입니다. 1000, 1001, 1002 ~
```
$ cat values.yaml
...
securityContext:
  runAsUser: 1000
...
```