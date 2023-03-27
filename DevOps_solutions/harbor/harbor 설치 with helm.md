
# harbor 설치 - with helm chart
## 1. Prerequisites
- harbor는 private registry , image를 저장하고 관리할 수 있는 솔루션 입니다.
- 해당 문서는 helm chart를 통해서 harbor를 설치 및 배포하는 과정을 담고 있습니다.
## 2. install harbor
### 2.1 harbor 관리용 namespace 생성
- efk 관리용 namespace를 생성합니다.
```
$ kubectl create namespace harbor
```
### 2.2 helm repo 등록
- helm repo에 harbor repo를 추가합니다.
```
$ helm repo add harbor https://helm.goharbor.io

$ helm repo update
```

### 2.3 secret 생성
- harbor는 tls 인증서가 필요합니다.
- create 명령어를 통해 tls인증서를 생성합니다.
```
$ kubectl create -n harbor secret tls tls-harbor --key xxx.xxxx.xxx.key --cert xxx.xxxxx.xxx.crt
secret/tls-harbor created
```

### 2.4 harbor values파일 수정
- 해당 예제에서는 storage를 ceph로 사용하기에 , values파일을 수정합니다.
  또한 생성한 tls 인증서의 이름도 등록 시켜 줍니다.
```
$ cat setting-values.yaml
externalURL: https://harbor.xxx.xxxxx.xyz

expose:
  tls:
    secret:
      secretName: tls-harbor

```
- persistent 관련 설정은 values파일을 직접 수정해야 합니다 !
```
persistence:
  persistentVolumeClaim:
    registry:
      storageClass: "ceph-filesystem"
      accessMode: ReadWriteMany
    chartmuseum:
      storageClass: "ceph-filesystem"
      accessMode: ReadWriteMany
    jobservice:
      storageClass: "ceph-filesystem"
      accessMode: ReadWriteMany
    database:
      storageClass: "ceph-filesystem"
      accessMode: ReadWriteMany
    redis:
      storageClass: "ceph-filesystem"
      accessMode: ReadWriteMany
    trivy:
      storageClass: "ceph-filesystem"
      accessMode: ReadWriteMany
```

- values.yaml파일을 수정합니다.
  ingress 설정부분에 nginx정보를 추가합니다.
```
...
  ingress:
    hosts:
      core: core.harbor.domain
      notary: notary.harbor.domain
    # set to the type of ingress controller if it has specific requirements.
    # leave as `default` for most ingress controllers.
    # set to `gce` if using the GCE ingress controller
    # set to `ncp` if using the NCP (NSX-T Container Plugin) ingress controller
    controller: default
    ## Allow .Capabilities.KubeVersion.Version to be overridden while creating ingress
    kubeVersionOverride: ""
    className: ""
    annotations:
      # note different ingress controllers may require a different ssl-redirect annotation
      # for Envoy, use ingress.kubernetes.io/force-ssl-redirect: "true" and remove the nginx lines below
      ingress.kubernetes.io/ssl-redirect: "true"
      ingress.kubernetes.io/proxy-body-size: "0"
      nginx.ingress.kubernetes.io/ssl-redirect: "true"
      nginx.ingress.kubernetes.io/proxy-body-size: "0"
      kubernetes.io/ingress.class: "nginx" # 추가
...
```
### 2.5 helm install 
- helm으로 harbor를 설치합니다.
- **externalURL는 외부에서 harbor로 접근하는 url을 말하는 것이며 , 두 가지 ingress는 install을 진행했을 경우 생성되는 ingress의 domain을 변경 시켜주는 옵션입니다.**
- values.yaml에 직접 변경해서 upgrade하여도 문제가 없습니다.
```
$ helm upgrade --install harbor . -n harbor \
--set expose.ingress.hosts.core=harbor.xxx.xxxxx.xyz \
--set expose.ingress.hosts.notary=notary.xxx.xxxxx.xyz \
--set externalURL=https://harbor.xxx.xxxxx.xyz \
-f values.yaml,setting-values.yaml
```
### 2.6 pod 상태 확인
- 정상 설치 여부를 확인합니다.
```
$ kubectl get all -n harbor
NAME                                        READY   STATUS    RESTARTS   AGE
pod/harbor-chartmuseum-f4b7c97c7-ddwct      1/1     Running   0          4m34s
pod/harbor-core-588fdb5777-vmjnz            1/1     Running   0          4m34s
pod/harbor-database-0                       1/1     Running   0          4m34s
pod/harbor-jobservice-5b7cb977c8-mfrh4      1/1     Running   0          4m34s
pod/harbor-notary-server-69c7cb7cc-cmqp2    1/1     Running   1          4m34s
pod/harbor-notary-signer-7b8dc95c99-n82pf   1/1     Running   1          4m34s
pod/harbor-portal-b9fb76db7-d6r2k           1/1     Running   0          4m34s
pod/harbor-redis-0                          1/1     Running   0          4m34s
pod/harbor-registry-5f4cff4f68-ltljr        2/2     Running   0          4m34s
pod/harbor-trivy-0                          1/1     Running   0          4m34s

NAME                           TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)             AGE
service/harbor-chartmuseum     ClusterIP   10.233.36.146   <none>        80/TCP              4m34s
service/harbor-core            ClusterIP   10.233.11.249   <none>        80/TCP              4m34s
service/harbor-database        ClusterIP   10.233.26.183   <none>        5432/TCP            4m34s
service/harbor-jobservice      ClusterIP   10.233.4.142    <none>        80/TCP              4m34s
service/harbor-notary-server   ClusterIP   10.233.31.63    <none>        4443/TCP            4m34s
service/harbor-notary-signer   ClusterIP   10.233.37.156   <none>        7899/TCP            4m34s
service/harbor-portal          ClusterIP   10.233.23.118   <none>        80/TCP              4m34s
service/harbor-redis           ClusterIP   10.233.30.203   <none>        6379/TCP            4m34s
service/harbor-registry        ClusterIP   10.233.56.58    <none>        5000/TCP,8080/TCP   4m34s
service/harbor-trivy           ClusterIP   10.233.36.251   <none>        8080/TCP            4m34s

NAME                                   READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/harbor-chartmuseum     1/1     1            1           4m34s
deployment.apps/harbor-core            1/1     1            1           4m34s
deployment.apps/harbor-jobservice      1/1     1            1           4m34s
deployment.apps/harbor-notary-server   1/1     1            1           4m34s
deployment.apps/harbor-notary-signer   1/1     1            1           4m34s
deployment.apps/harbor-portal          1/1     1            1           4m34s
deployment.apps/harbor-registry        1/1     1            1           4m34s

NAME                                              DESIRED   CURRENT   READY   AGE
replicaset.apps/harbor-chartmuseum-f4b7c97c7      1         1         1       4m34s
replicaset.apps/harbor-core-588fdb5777            1         1         1       4m34s
replicaset.apps/harbor-jobservice-5b7cb977c8      1         1         1       4m34s
replicaset.apps/harbor-notary-server-69c7cb7cc    1         1         1       4m34s
replicaset.apps/harbor-notary-signer-7b8dc95c99   1         1         1       4m34s
replicaset.apps/harbor-portal-b9fb76db7           1         1         1       4m34s
replicaset.apps/harbor-registry-5f4cff4f68        1         1         1       4m34s

NAME                               READY   AGE
statefulset.apps/harbor-database   1/1     4m34s
statefulset.apps/harbor-redis      1/1     4m34s
statefulset.apps/harbor-trivy      1/1     4m34s
```
## 3. ingress 생성
- harbor는 ingress를 values파일에서 생성해 줍니다. 
```
$ kubectl get ingress -n harbor
NAME                    CLASS    HOSTS                  ADDRESS         PORTS     AGE
harbor-ingress          <none>   harbor.xxx.xxxxx.xyz     10.233.23.197   80, 443   65s
harbor-ingress-notary   <none>   notary.xxx.xxxxx.xyz   10.233.23.197   80, 443   65s
```
## 4. 참고 
### 4.1 harbor 폐쇄망 설치 시 image private registry 설정 값
- harbor image를 private registry에서 받아오는 경우 설정해야하는 yaml 값 
- repositroy에 private registry 주소와 이미지 이름이 들어가게 되고 , tag를 지정해준다.
[harbor values.yaml setting](https://github.com/goharbor/harbor-helm)
```
$ cat private-registry-values.yaml
core:
  image:
    repository: 10.xxx.xxx.xxx:5000/goharbor/harbor-core
    tag: v2.5.1

portal:
  image:
    repository: 10.xxx.xxx.xxx:5000/goharbor/harbor-portal
    tag: v2.5.1

jobservice:
  image:
    repository: 10.xxx.xxx.xxx:5000/goharbor/harbor-jobservice
    tag: v2.5.1
	
registry:
  controller:
    image:
      repository: 10.xxx.xxx.xxx:5000/goharbor/harbor-registryctl 
	  tag: v2.5.1
  registry:
    image:
      repository: 10.xxx.xxx.xxx:5000/goharbor/registry-photon
	  tag: v2.5.1


chartmuseum:
  image:
    repository: 10.xxx.xxx.xxx:5000/goharbor/chartmuseum-photon
    tag: v2.5.1

trivy:
  image:
    repository: 10.xxx.xxx.xxx:5000/goharbor/trivy-adapter-photon
	tag: v2.5.1

notary:
  server:
    image:
      repository: 10.xxx.xxx.xxx:5000/goharbor/notary-server-photon
	  tag: v2.5.1
  signer:
    image:
      repository: 10.xxx.xxx.xxx:5000/goharbor/notary-signer-photon
	  tag: v2.5.1

database:
  internal:
    image:
      repository: 10.xxx.xxx.xxx:5000/goharbor/harbor-db
	  tag: v2.5.1

redis:
  internal:
    image:
      repository: 10.xxx.xxx.xxx:5000/goharbor/redis-photon
	  tag: v2.5.1

exporter:
  image:
    repository: 10.xxx.xxx.xxx:5000/goharbor/harbor-exporter
    tag: v2.5.1
```
### 4.2 harbor 폐쇄망 설치 후 docker login 설정 값
- ssl 인증서가 있다면 , 아래 설정을 따르지 않고 docker daemon.json값만 변경 시켜주면 됩니다.
- ssl 인증서가 없을 경우 , 아래 설정을 따릅니다.
```
$ cat values.yaml
...
 ingress:
    hosts:
      core: core.harbor.domain
      notary: notary.harbor.domain
    # set to the type of ingress controller if it has specific requirements.
    # leave as `default` for most ingress controllers.
    # set to `gce` if using the GCE ingress controller
    # set to `ncp` if using the NCP (NSX-T Container Plugin) ingress controller
    controller: default
    ## Allow .Capabilities.KubeVersion.Version to be overridden while creating ingress
    kubeVersionOverride: ""
    className: ""
    annotations:
      # note different ingress controllers may require a different ssl-redirect annotation
      # for Envoy, use ingress.kubernetes.io/force-ssl-redirect: "true" and remove the nginx lines below
      ingress.kubernetes.io/ssl-redirect: "false" # true에서 false로 변경 . https로 리다이렉트 설정
      ingress.kubernetes.io/proxy-body-size: "0"
      nginx.ingress.kubernetes.io/ssl-redirect: "false" # true에서 false로 변경 . https로 리다이렉트 설정
      nginx.ingress.kubernetes.io/proxy-body-size: "0"
      kubernetes.io/ingress.class: nginx
...
```
- docker json 설정 변경
  -  docker login 시 docker 명령어를 사용하기 때문에 , ssl인증서 에러가 발생할 수 있습니다. 따라서 /etc/docker 내부의 daemon.json을 생성 및 변경 시켜줍니다.
```
$ cat daemon.json
{
  "insecure-registries": ["harbor.xxx.xxx.xyz"]
}

# docker restart
$ systemctl restart docker 
```
- 설정을 완료한 후 , docker login test 합니다.
```
$ docker login harbor.xxx.xxx.xyz
Username: admin
Password:
WARNING! Your password will be stored unencrypted in /home/centos/.docker/config.json.
Configure a credential helper to remove this warning. See
https://docs.docker.com/engine/reference/commandline/login/#credentials-store

Login Succeeded
```