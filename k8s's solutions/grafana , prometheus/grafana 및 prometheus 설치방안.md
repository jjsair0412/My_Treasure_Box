# prometheus and grafana dashboard 설치 
해당 문서는 prometheus와 grafana를 설치하는 방안에 대해 기술합니다.

## 1. 설치 환경
pv를 동적 프로비저닝 하기 위해 nfs를 설치하였습니다.

버전 정보는 다음과 같습니다.
|Solution Name| app version |
|--|--|
| K8S | v1.21.6 |
| prometheus | v2.40.5 |
| grafana | 9.3.1 |
| nfs | 3.0.0 |

## 2. 설치
### 2.1 prometheus 설치
prometheus는 helm chart로 설치합니다.
#### helm repo 추가 및 pull
```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

helm pull prometheus-community/prometheus --untar
```
기본적으로 prometheus는 export를 통해 grafana로 메트릭을 전송합니다.

따라서 사용할 dashboard에서 필요한 메트릭값이 무엇인가에 따라 필요한 export를 골라 설치해야 하며 , 위의 helm repo를 추가한다면 여러 exporter가 helm chart로 제공되어지는것을 확인할 수 있습니다.

helm search repo 명령어로 exporter 종류를 확인할 수 있습니다.
```bash
ubuntu@jjsair0412:~/helm/prometheus$ helm search repo pro
NAME                                                    CHART VERSION   APP VERSION     DESCRIPTION                                       
nfs-ganesha-server-and-external-provisioner/nfs...      1.4.0           3.0.0           nfs-server-provisioner is an out-of-tree dynami...
prometheus-community/kube-prometheus-stack              43.0.0          0.61.1          kube-prometheus-stack collects Kubernetes manif...
prometheus-community/prom-label-proxy                   0.1.0           v0.5.0          A proxy that enforces a given label in a given ...
prometheus-community/prometheus                         19.0.1          v2.40.5         Prometheus is a monitoring system and time seri...
prometheus-community/prometheus-adapter                 3.4.2           v0.10.0         A Helm chart for k8s prometheus adapter           
prometheus-community/prometheus-blackbox-exporter       7.1.3           0.22.0          Prometheus Blackbox Exporter                      
prometheus-community/prometheus-cloudwatch-expo...      0.22.0          0.15.0          A Helm chart for prometheus cloudwatch-exporter   
prometheus-community/prometheus-conntrack-stats...      0.5.3           v0.4.7          A Helm chart for conntrack-stats-exporter         
prometheus-community/prometheus-consul-exporter         0.5.1           0.4.0           A Helm chart for the Prometheus Consul Exporter   
prometheus-community/prometheus-couchdb-exporter        0.2.1           1.0             A Helm chart to export the metrics from couchdb...
prometheus-community/prometheus-druid-exporter          0.11.0          v0.8.0          Druid exporter to monitor druid metrics with Pr...
prometheus-community/prometheus-elasticsearch-e...      5.0.0           1.5.0           Elasticsearch stats exporter for Prometheus       
prometheus-community/prometheus-fastly-exporter         0.1.1           7.2.4           A Helm chart for the Prometheus Fastly Exporter   
prometheus-community/prometheus-json-exporter           0.5.0           v0.5.0          Install prometheus-json-exporter                  
prometheus-community/prometheus-kafka-exporter          1.7.0           v1.6.0          A Helm chart to export the metrics from Kafka i...
prometheus-community/prometheus-mongodb-exporter        3.1.2           0.31.0          A Prometheus exporter for MongoDB metrics         
prometheus-community/prometheus-mysql-exporter          1.11.1          v0.14.0         A Helm chart for prometheus mysql exporter with...
prometheus-community/prometheus-nats-exporter           2.10.1          0.10.1          A Helm chart for prometheus-nats-exporter         
prometheus-community/prometheus-nginx-exporter          0.1.0           0.11.0          A Helm chart for the Prometheus NGINX Exporter    
prometheus-community/prometheus-node-exporter           4.8.0           1.5.0           A Helm chart for prometheus node-exporter         
prometheus-community/prometheus-operator                9.3.2           0.38.1          DEPRECATED - This chart will be renamed. See ht...
prometheus-community/prometheus-pingdom-exporter        2.4.1           20190610-1      A Helm chart for Prometheus Pingdom Exporter      
prometheus-community/prometheus-postgres-exporter       4.0.0           0.11.1          A Helm chart for prometheus postgres-exporter     
prometheus-community/prometheus-pushgateway             2.0.2           v1.5.1          A Helm chart for prometheus pushgateway           
prometheus-community/prometheus-rabbitmq-exporter       1.3.0           v0.29.0         Rabbitmq metrics exporter for prometheus          
prometheus-community/prometheus-redis-exporter          5.3.0           v1.44.0         Prometheus exporter for Redis metrics             
prometheus-community/prometheus-smartctl-exporter       0.3.1           v0.8.0          A Helm chart for Kubernetes                       
prometheus-community/prometheus-snmp-exporter           1.2.1           0.19.0          Prometheus SNMP Exporter                          
prometheus-community/prometheus-stackdriver-exp...      4.1.0           0.12.0          Stackdriver exporter for Prometheus               
prometheus-community/prometheus-statsd-exporter         0.7.0           v0.22.8         A Helm chart for prometheus stats-exporter        
prometheus-community/prometheus-to-sd                   0.4.2           0.5.2           Scrape metrics stored in prometheus format and ...
prometheus-community/alertmanager                       0.22.2          v0.24.0         The Alertmanager handles alerts sent by client ...
prometheus-community/jiralert                           1.0.1           1.2             A Helm chart for Kubernetes to install jiralert   
prometheus-community/kube-state-metrics                 4.24.0          2.7.0           Install kube-state-metrics to generate and expo...
ingress-nginx/ingress-nginx                             4.4.0           1.5.1           Ingress controller for Kubernetes using NGINX a...
```

먼저 기본적인 prometheus부터 설치 합니다.

#### 관리용 namespace 설정
```
kubectl create ns pro
```

#### storageclass 설정
nfs sc를 사용하기에 setting-values.yaml파일을 생성하여 sc를 nfs로 변경합니다.
```
$ cat setting-values.yaml
server:
  persistentVolume:
    storageClass: nfs

alertmanager:
  persistence:
    storageClass: nfs
```

#### helm install
```
helm upgrade --install prometheus . -n pro  -f values.yaml,setting-values.yaml
```

#### 설치 결과 제공정보 확인
grafana에서 설정할 prometheus 접근 정보가 출력됩니다.

만약 grafana와 proemtheus가 동일 k8s 클러스터에 설치되어 있다면 , k8s dns가 AAA 접근법을 리졸빙 해주기에 아래와 같은 endpoint로 연결이 가능합니다.
```
# 위의 설치 과정대로 설치한다면 제공되어지는 prometheus 접근 endpoint
# {서비스이름}.{namespace명}.svc.cluster명
prometheus-server.pro.svc.cluster.local
```

그러나 외부 grafana에 prometheus를 연동해야 한다면. prometheus ingress를 생성하여 연결하거나 노드포트로 prometheus를 열어서 연동합니다.

설치결과 확인합니다.
```
ubuntu@jjsair0412:~/helm/prometheus$ kubectl get all -n pro
NAME                                                    READY   STATUS    RESTARTS   AGE
pod/prometheus-alertmanager-0                           1/1     Running   0          41s
pod/prometheus-kube-state-metrics-7cdcf7cc98-z2thr      1/1     Running   0          41s
pod/prometheus-prometheus-node-exporter-tltj8           1/1     Running   0          41s
pod/prometheus-prometheus-pushgateway-959d84d7f-s5f5r   1/1     Running   0          41s
pod/prometheus-server-88ccd7f9-pkgrm                    2/2     Running   0          41s

NAME                                          TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)    AGE
service/prometheus-alertmanager               ClusterIP   10.233.36.247   <none>        9093/TCP   41s
service/prometheus-alertmanager-headless      ClusterIP   None            <none>        9093/TCP   41s
service/prometheus-kube-state-metrics         ClusterIP   10.233.25.17    <none>        8080/TCP   41s
service/prometheus-prometheus-node-exporter   ClusterIP   10.233.42.201   <none>        9100/TCP   41s
service/prometheus-prometheus-pushgateway     ClusterIP   10.233.34.93    <none>        9091/TCP   41s
service/prometheus-server                     ClusterIP   10.233.17.6     <none>        80/TCP     41s

NAME                                                 DESIRED   CURRENT   READY   UP-TO-DATE   AVAILABLE   NODE SELECTOR   AGE
daemonset.apps/prometheus-prometheus-node-exporter   1         1         1       1            1           <none>          41s

NAME                                                READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/prometheus-kube-state-metrics       1/1     1            1           41s
deployment.apps/prometheus-prometheus-pushgateway   1/1     1            1           41s
deployment.apps/prometheus-server                   1/1     1            1           41s

NAME                                                          DESIRED   CURRENT   READY   AGE
replicaset.apps/prometheus-kube-state-metrics-7cdcf7cc98      1         1         1       41s
replicaset.apps/prometheus-prometheus-pushgateway-959d84d7f   1         1         1       41s
replicaset.apps/prometheus-server-88ccd7f9                    1         1         1       41s

NAME                                       READY   AGE
statefulset.apps/prometheus-alertmanager   1/1     41s
```

#### ingress 생성
prometheus ingress를 생성합니다.
```
$ cat ingress.yaml 
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: prometheus-ingress
  namespace: pro
  annotations:
    kubernetes.io/ingress.class: "nginx"
spec:
  rules:
  - host: "pro.jjs.com"
    http:
      paths:
      - pathType: Prefix
        path: /
        backend:
          service:
            name: prometheus-server
            port:
              number: 80
```
생성한 ingress로 접근하여 접근 확인 합니다.
만약 ingress를 생성했다면 , 차후 grafana와 연동할 때 해당 도메인을 입력해주면 되고 , 없다면 노드포트로 열어서 연동하면 됩니다.

### 2.2 grafana 설치
grafana또한 helm chart로 설치합니다.
```
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

helm pull grafana/grafana --untar
```

#### grafana 관리용 namespace 생성
```
kubectl create ns gra
```

#### helm install
```
helm upgrade --install grafana . -n gra -f values.yaml
```

#### 접근 정보 확인
default 계정은 다음과 같습니다.
```
# ID
admin

# pwd
kubectl get secret --namespace gra grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo
```

#### ingress 생성
```
$ cat ingress.yaml 
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: prometheus-ingress
  namespace: gra
  annotations:
    kubernetes.io/ingress.class: "nginx"
spec:
  rules:
  - host: "gra.jjs.com"
    http:
      paths:
      - pathType: Prefix
        path: /
        backend:
          service:
            name: grafana
            port:
              number: 80
```
생성된 ingress로 접근하여 접근 확인 합니다.