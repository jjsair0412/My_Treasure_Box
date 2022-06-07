# EFK 설치 - with helm chart
## 1. Prerequisites
- efk는 elasticsearch , Kibana , Fleuntbit 세가지 솔루션을 이용해 로깅을 모니터링 할 수 있는 솔루션입니다.
- 해당 문서는 efk의 helm 설치 방안에대해 설명합니다.
## 2. install efk
### 2.1 efk 관리용 namespace 생성
- efk 관리용 namespace를 생성합니다.
```
$ kubectl create namespace efk
```
### 2.2 helm repo 등록
- 차례대로 elastic search , kibana , fleuntbit 세가지 설치합니다.
- install할 때 timezone 설정으로 한국 시간을 설정해줍니다.
#### 2.2.1 elastic search 설치
- 첫번째로 elastic-search를 설치하기위해 values파일을 가지고 옵니다.
- helm repo에 elastic 정보를 add 합니다.
```
$ helm repo add elastic https://helm.elastic.co
$ helm repo update
```
- helm pull 명령어로 elasticsearch values파일 가지고 옵니다.
```
$ helm pull elastic/elasticsearch --untar
```
- helm upgrade 명령어를 통해 elasticsearch 설치합니다.
  values파일 수정하거나 , set 옵션, values파일 생성 등의 방법으로 특정 옵션을 지정해줍니다.
  해당 문서의 설치 환경은 ceph 스토리지를 사용하기 때문에 , storageClassName을 지정해 주었습니다.

```
$ helm upgrade --install elastic . \
-n efk \
--set volumeClaimTemplate.resources.requests.storage=200Gi \
--set volumeClaimTemplate.storageClassName=ceph-filesystem \
--set volumeClaimTemplate.accessModes={ReadWriteMany} \
--set extraEnvs[0].name=TZ \
--set extraEnvs[0].value=Asia/Seoul \
-f values.yaml
```
- pod 상태 확인
```
$ kubectl get all -n efk
NAME                         READY   STATUS    RESTARTS   AGE
pod/elasticsearch-master-0   1/1     Running   0          2m30s
pod/elasticsearch-master-1   1/1     Running   0          2m30s
pod/elasticsearch-master-2   1/1     Running   0          2m30s

NAME                                    TYPE        CLUSTER-IP    EXTERNAL-IP   PORT(S)             AGE
service/elasticsearch-master            ClusterIP   10.233.8.63   <none>        9200/TCP,9300/TCP   2m30s
service/elasticsearch-master-headless   ClusterIP   None          <none>        9200/TCP,9300/TCP   2m30s

NAME                                    READY   AGE
statefulset.apps/elasticsearch-master   3/3     2m30s
```
#### 2.2.2 Kibana 설치
- kibana 또한 elastic repo에 위치합니다.
  helm repo에 elastic 정보를 add 합니다.
```
$ helm repo add elastic https://helm.elastic.co
$ helm repo update
```
- helm pull 명령어로 kibana values파일 가지고 옵니다.
```
$ helm pull elastic/kibana --untar
```
- helm upgrade 명령어를 통해 kibana 설치합니다.
  values파일 수정하거나 , set 옵션, values파일 생성 등의 방법으로 특정 옵션을 지정해줍니다.
```
$ helm upgrade --install kibana . -n efk \
--set extraEnvs[1].name=TZ \
--set extraEnvs[1].value=Asia/Seoul \
-f values.yaml
```
- pod 상태 확인
```
$ kubectl get all -n efk
NAME                                 READY   STATUS    RESTARTS   AGE
pod/elasticsearch-master-0           1/1     Running   0          8m53s
pod/elasticsearch-master-1           1/1     Running   0          8m53s
pod/elasticsearch-master-2           1/1     Running   0          8m53s
pod/kibana-kibana-656c874b7b-dp54m   1/1     Running   0          3m12s

NAME                                    TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)             AGE
service/elasticsearch-master            ClusterIP   10.233.8.63     <none>        9200/TCP,9300/TCP   8m53s
service/elasticsearch-master-headless   ClusterIP   None            <none>        9200/TCP,9300/TCP   8m53s
service/kibana-kibana                   ClusterIP   10.233.46.179   <none>        5601/TCP            3m12s

NAME                            READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/kibana-kibana   1/1     1            1           3m12s

NAME                                       DESIRED   CURRENT   READY   AGE
replicaset.apps/kibana-kibana-656c874b7b   1         1         1       3m12s

NAME                                    READY   AGE
statefulset.apps/elasticsearch-master   3/3     8m53s
```
#### 2.2.3 Fleuntbit 설치
- Fleuntbit는 fluent repo에 위치해 있습니다.
  helm repo에 fluent 정보를 add합니다.
```
$ helm repo add fluent https://fluent.github.io/helm-charts
$ helm repo update
```
- helm pull 명령어로 fluent values파일 가지고 옵니다.
```
$ helm pull fluent/fluent-bit --untar
```
- Input / Output 설정을 변경합니다 . 
아래 설정은 namespace 형태로 Elastic Index가 생성 되도록 설정 되어 있습니다.
만약 Namespace가 추가가 될 경우 [INPUT/OUTPUT]을 추가하여 재설치 합니다.
- {NAMESPACE_NAME} 에 namespace 이름을 넣어 줍니다.
- 해당 옵션은 log-values.yaml 이라는 파일을 생성해서 따로 관리합니다.
- [input / filters / output 관련 포스팅](https://haereeroo.tistory.com/20)


```
config:
  inputs: |
    [INPUT]
        Name tail
        Path /var/log/containers/*{NAMESPACE_NAME}*.log
        multiline.parser docker, cri
        Tag kube.{NAMESPACE_NAME}.*
        Mem_Buf_Limit 5MB
        Skip_Long_Lines On

  filters: |
    [FILTER]
        Name kubernetes
        Match kube.{NAMESPACE_NAME}.*
        Merge_Log On
        Kube_Tag_Prefix kube.{NAMESPACE_NAME}.var.log.containers.
        Keep_Log Off
        K8S-Logging.Parser On
        K8S-Logging.Exclude Off

  outputs: |
    [OUTPUT]
        Name es
        Match kube.{NAMESPACE_NAME}.*
        Host elasticsearch-master
        Logstash_Format On
        Logstash_Prefix {NAMESPACE_NAME}
        Replace_Dots On
        Retry_Limit False
```
- helm upgrade 명령어를 통해 fluentbit 설치합니다.
  values파일 수정하거나 , set 옵션, values파일 생성 등의 방법으로 특정 옵션을 지정해줍니다.
```
$ helm upgrade --install fluent-bit . -n efk \
--set env[0].name=TZ \
--set env[0].value=Asia/Seoul
-f values.yaml,log-values.yaml
```
- pod 상태 확인
```
$ kubectl get all -n efk
NAME                                 READY   STATUS    RESTARTS   AGE
pod/elasticsearch-master-0           1/1     Running   0          25m
pod/elasticsearch-master-1           1/1     Running   0          25m
pod/elasticsearch-master-2           1/1     Running   0          25m
pod/fluent-bit-7ltsn                 1/1     Running   0          14s
pod/fluent-bit-jnz7v                 1/1     Running   0          14s
pod/fluent-bit-rtl7g                 1/1     Running   0          14s
pod/kibana-kibana-656c874b7b-dp54m   1/1     Running   0          19m

NAME                                    TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)             AGE
service/elasticsearch-master            ClusterIP   10.233.8.63     <none>        9200/TCP,9300/TCP   25m
service/elasticsearch-master-headless   ClusterIP   None            <none>        9200/TCP,9300/TCP   25m
service/fluent-bit                      ClusterIP   10.233.53.203   <none>        2020/TCP            14s
service/kibana-kibana                   ClusterIP   10.233.46.179   <none>        5601/TCP            19m

NAME                        DESIRED   CURRENT   READY   UP-TO-DATE   AVAILABLE   NODE SELECTOR   AGE
daemonset.apps/fluent-bit   3         3         3       3            3           <none>          14s

NAME                            READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/kibana-kibana   1/1     1            1           19m

NAME                                       DESIRED   CURRENT   READY   AGE
replicaset.apps/kibana-kibana-656c874b7b   1         1         1       19m

NAME                                    READY   AGE
statefulset.apps/elasticsearch-master   3/3     25m
```
### 2.3 ingress 설정
- Elastic Search, Kibana에 대한 Ingress를 설정 합니다.

```
$ cat ingress.yaml
# Kibana Ingress
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: kibana-ingress
  namespace: efk
  annotations:
    kubernetes.io/ingress.class: "nginx"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
    nginx.ingress.kubernetes.io/backend-protocol: "HTTPS"
spec:
  rules:
  - host: "kibana.ju.jinseong.net"
    http:
      paths:
      - pathType: Prefix
        path: /
        backend:
          service:
            name: kibana-kibana
            port:
              number: 5601

...
# Elastic Search Ingress
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: elastic-ingress
  namespace: efk
  annotations:
    kubernetes.io/ingress.class: "nginx"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
    nginx.ingress.kubernetes.io/backend-protocol: "HTTPS"
spec:
  rules:
  - host: "elasticsearch.ju.jinseong.net"
    http:
      paths:
      - pathType: Prefix
        path: /
        backend:
          service:
            name: elasticsearch-master
            port:
              number: 9200


$ kubectl apply -f ingress.yaml
```
