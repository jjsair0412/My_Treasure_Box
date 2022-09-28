# ELK helm install - air gap 환경 포함
##   1. Prerequisites
-   아래 문서는 helm chart를 이용해  elk 를 설치하는 방안에 대해 설명합니다.
- elasticsearch의 모든 helm chart는 아래 url에 위치합니다.
[elasticsearch](https://github.com/elastic/helm-charts)
-  elk는 네가지 솔루션 조합으로 설치되게 됩니다. 따라서 설치할 솔루션은 총 네가지 입니다.
	-	1. elasticsearch
	-	2. kibana
	-	3. logstash
	-	4. filebeat
-	elk의 동작 프로세스는 다음과 같습니다.
	-	1. filebeat pod가 daemonset type으로 생성되어 파드가 모든 노드에 생성됨
	-	2. 생성된 filebeat pod가 로그를 수집하여 logstash로 로그 전송
	-	3. elasticsearch가 logstash의 로그 수집
	-	4. kibana에서 logstash에 수집된 로그를 ui에 그래프 및 다양한 방식으로 출력
- 또한 elk stack 설치 버전은 7.10.2 버전 입니다.
	- 7.10.2버전 이상은 유료 버전이기에 , 7.10.2버전을 사용합니다.
## 2.  ELK 설치
### 2.1 namespace 설정
- elk를 관리할 namespace를 생성합니다.
```
$ kubectl create namespace elk
```
### 2.2 install filebeat
#### 2.2.0 filebeat helm pull
- filebeat helm chart를 pull 합니다.
  [filebeat helm chart version 7.10.2 github](https://github.com/elastic/helm-charts/tree/v7.10.2/filebeat)
```
# helm repo 추가
helm repo add elastic https://helm.elastic.co

# helm pull
helm pull filebeat --version 7.10.2 elastic/filebeat --untar
```

#### 2.2.1 values.yaml 파일 수정
- 각 노드에 생성될 filebeat config 파일을 작성합니다.
	values.yaml에 있는 daemonset option에 추가합니다.
```
$ cat filebeat-setting-values.yaml
image: "docker.elastic.co/beats/filebeat"
imageTag: "7.10.2"

daemonset:
  filebeatConfig:  
    filebeat.yml: |  
      filebeat.inputs:  
      - type: container  
        paths:  
          - /var/log/containers/*.log  
        processors:  
        - add_kubernetes_metadata:  
            host: ${NODE_NAME}  
            matchers:  
            - logs_path:  
                logs_path: "/var/log/containers/"  
      output.logstash:  
        hosts: ["logstash-logstash-headless:5044"] # logstash service 접근 정보 . AAA type으로 작성, serviceName:port
```
#### 2.2.2 helm install
- helm chart로 filebeat를 설치합니다.
```
$ helm upgrade --install filebeat -n elk . -f values.yaml,setting-values.yaml
```
### 2.3 install logstash
#### 2.3.0 logstash helm pull
- logstash helm chart를 pull 합니다.
  [logstash helm chart version 7.10.2 github](https://github.com/elastic/helm-charts/tree/v7.10.2/logstash)
```
# helm repo 추가
helm repo add elastic https://helm.elastic.co

# helm pull
helm pull logstash --version 7.10.2 elastic/logstash --untar
```
#### 2.3.1 values.yaml 파일 수정
- logstash pipeline을 정의합니다.
	- input
		- log를 filebeat에서 수집하고 5044번 포트로 filebeat에서 수집한 로그를 받게 됩니다.
	- filter
		- 받은 log를 지정한 filter 형태로 필터링 합니다.
	- output
		- elasticsearch로 받은 로그를 전달합니다. 
```
$ cat logstash-setting-values.yaml
image: "docker.elastic.co/logstash/logstash"
imageTag: "7.10.2"


logstashPipeline:
  logstash.conf: |
    input {  
      beats { 
        port => 5044
      }
    }
    filter {
      date {
        match => ["logdate","MMM dd yy HH:mm:ss"]
        target => "@timestamp"
        timezone => "Asia/Seoul"
      }
    }
    output {
      elasticsearch {
        hosts => ["http://elasticsearch-master:9200"] # elasticsearch clusterIP service info
      }
    }
```
#### 2.3.2 helm install
- helm chart로 logstash를 설치합니다.
```
$ helm upgrade --install logstash -n elk . -f values.yaml,logstash-setting-values.yaml
```
### 2.4 install elasticsearch
#### 2.4.0 elasticsearch helm pull
- elasticsearch helm chart를 pull 합니다.
  [elasticsearch helm chart version 7.10.2 github](https://github.com/elastic/helm-charts/tree/v7.10.2/elasticsearch)
```
# helm repo 추가
helm repo add elastic https://helm.elastic.co

# helm pull
helm pull elasticsearch --version 7.10.2 elastic/elasticsearch --untar
```

#### 2.4.1 values.yaml 파일 수정
- elasticsearch values.yaml파일을 수정합니다.
```
$ cat elasticsearch-setting-values.yaml
image: "docker.elastic.co/elasticsearch/elasticsearch"
imageTag: "7.10.2"

clusterHealthCheckParams: "wait_for_status=yellow&timeout=1s"  # status값을 green에서 yellow로 변경합니다.

readinessProbe:
  initialDelaySeconds:400
  timeoutSeconds:500 # readinessProbe의 시간을 늘리면서 elasticsearch pod들이 동기화할 시간을 확보합니다.
```
#### 2.4.2 helm install
- helm chart로 elasticsearch를 설치합니다.
```
$ helm upgrade --install elasticsearch -n elk . -f values.yaml,elasticsearch-setting-values.yaml
```
### 2.5 install kibana
#### 2.5.0 kibana helm pull
- kibana helm chart를 pull 합니다.
  [kibana helm chart version 7.10.2 github](https://github.com/elastic/helm-charts/tree/v7.10.2/kibana)
```
# helm repo 추가
helm repo add elastic https://helm.elastic.co

# helm pull
helm pull kibana --version 7.10.2  elastic/kibana --untar
```

#### 2.5.1 values.yaml 파일 수정
- kibana values.yaml파일을 수정합니다.
```
$ cat kibana-setting-values.yaml
image: "docker.elastic.co/kibana/kibana"
imageTag: "7.10.2"

kibanaConfig: 
  kibana.yml: |
    elasticsearch.hosts: [http://elasticsearch-master:9200]
```
#### 2.5.2 helm install
- helm chart로 elasticsearch를 설치합니다.
	- set option을 통해 시간을 서울로 지정합니다.
```
$ helm upgrade --install kibana -n elk . --set extarEnvs[1].name=TZ --set extarEnvs[1].value=Asia/Seoul -f values.yaml,kibana-setting-values.yaml
```
## 3. ELK 설치 결과 확인
- kubectl 명령어를 통해 전체 pod가 동작중인지 확인합니다.
```
$ kubectl get all -n elk
```
### 3.1 ingress 생성
- elasticsearch와 kibana에 대한 ingress를 생성합니다.
```
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

---
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
```
