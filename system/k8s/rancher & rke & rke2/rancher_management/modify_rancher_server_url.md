#CA 키 / 인증서 생성

openssl genrsa -out ca.key 2048

openssl req -new -x509 -days 3650 -key ca.key -subj "/C=KR/ST=SE/L=SE/O=test/CN=KW Root CA" -out ca.crt

#서버 키 / 인증서 생성

openssl req -newkey rsa:2048 -nodes -keyout server.key -subj "/C=KR/ST=SE/L=SE/O=test/CN=*.test.net" -out server.csr

openssl x509 -req -extfile <(printf "subjectAltName=DNS:test.net,DNS:rancher.test.net") -days 3650 -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt


kubectl create secret -n cattle-system tls tls-rancher-ingress --key server.key --cert server.crt


helm upgrade --install rancher . \
  --namespace cattle-system \
  --values values.yaml


kubectl get secret --namespace cattle-system bootstrap-secret -o go-template='{{.data.bootstrapPassword|base64decode}}{{ "\n" }}'


#### 바뀐 키값
openssl genrsa -out ca.key 2048

openssl req -new -x509 -days 3650 -key ca.key -subj "/C=KR/ST=SE/L=SE/O=jinseongtest/CN=KW Root CA" -out ca.crt

#서버 키 / 인증서 생성
openssl req -newkey rsa:2048 -nodes -keyout server.key -subj "/C=KR/ST=SE/L=SE/O=jinseongtest/CN=*.jinseongtest.net" -out server.csr

openssl x509 -req -extfile <(printf "subjectAltName=DNS:jinseongtest.net,DNS:rancher.jinseongtest.net") -days 3650 -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt

kubectl create secret -n cattle-system tls new-tls-rancher-ingress --key server.key --cert server.crt

서버 URL 바꾸자마자 끊기진않음. agent의 cattle-agent가 restart 되어야 끊김
- Agent 로그
time="2025-05-19T04:54:37Z" level=info msg="namespaceHandler: addProjectIDLabelToNamespace: adding label field.cattle.io/projectId=p-rbxdx to namespace=cattle-fleet-system"
time="2025-05-19T05:08:45Z" level=error msg="Error during subscribe websocket: close sent"
- 특정시간이후 아래 로그뜨면서 끊김
Configuring bootstrap node(s) custom-0fdaa5766c14: waiting for cluster agent to connect

# 서버의 CA 인증서 가져오기
echo -n | openssl s_client -connect rancher.test.net:443 -servername rancher.test.net 2>/dev/null | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > /tmp/rancher-ca.crt

# 인증서 내용 확인
openssl x509 -in /tmp/rancher-ca.crt -text -noout

# Ubuntu 시스템에 인증서 추가
sudo cp /tmp/rancher-ca.crt /usr/local/share/ca-certificates/

# CA 저장소 업데이트
sudo update-ca-certificates

# 시스템 CA 번들에 직접 추가
sudo sh -c 'cat /tmp/rancher-ca.crt >> /etc/ssl/certs/ca-certificates.crt'

# 또는 이 방법도 시도
sudo mkdir -p /usr/share/ca-certificates/extra
sudo cp /tmp/rancher-ca.crt /usr/share/ca-certificates/extra/
sudo sh -c 'echo "extra/rancher-ca.crt" >> /etc/ca-certificates.conf'
sudo update-ca-certificates

인증서바뀌면 Rancher Server에  tls-rancher-internal-ca 이거바꿔야됨 
# 인증서(tls.crt) 추출 및 내용 확인
kubectl get secret tls-rancher-internal-ca -n cattle-system -o jsonpath='{.data.tls\.crt}' | base64 -d > /tmp/ca.crt
openssl x509 -in /tmp/ca.crt -text -noout

이후 rancher agent에서 tls key 신뢰할수있는 인증서로 등록해야 함.
rancher agent systemd에서는 이렇게나옴
[Unit]
Description=Rancher System Agent
Documentation=https://www.rancher.com
Wants=network-online.target
After=network-online.target
[Install]
WantedBy=multi-user.target
[Service]
EnvironmentFile=-/etc/default/rancher-system-agent
EnvironmentFile=-/etc/sysconfig/rancher-system-agent
EnvironmentFile=-/etc/systemd/system/rancher-system-agent.env
Type=simple
Restart=always
RestartSec=5s
Environment=CATTLE_LOGLEVEL=info
Environment=CATTLE_AGENT_CONFIG=/etc/rancher/agent/config.yaml
Environment=CATTLE_AGENT_STRICT_VERIFY=true
ExecStart=/usr/local/bin/rancher-system-agent sentinel

기본적으로 rancher-agent는 /etc/rancher/agent/ca.crt 이키를 가지고 서버에 인증하며, 
/var/lib/rancher/agent/rancher2_connection_info.json 이파일안에 server 정보로 인증을 시도함.

이둘을 바꿔줘야됨,. server url이랑 tls key


# 인증서를 ConfigMap으로 생성
kubectl create configmap custom-ca-certs -n cattle-system --from-file=/etc/rancher/agent/ca.crt

# cattle-cluster-agent 배포 수정
kubectl patch deployment cattle-cluster-agent -n cattle-system --type=json -p='[
  {
    "op": "add", 
    "path": "/spec/template/spec/volumes/-", 
    "value": {
      "name": "custom-ca-certs",
      "configMap": {
        "name": "custom-ca-certs"
      }
    }
  },
  {
    "op": "add", 
    "path": "/spec/template/spec/containers/0/volumeMounts/-", 
    "value": {
      "name": "custom-ca-certs",
      "mountPath": "/etc/ssl/certs/custom-ca",
      "readOnly": true
    }
  },
  {
    "op": "add", 
    "path": "/spec/template/spec/containers/0/env/-", 
    "value": {
      "name": "SSL_CERT_DIR",
      "value": "/etc/ssl/certs:/etc/ssl/certs/custom-ca"
    }
  }
]'


----
도메인 변경 시 Server에서 변경할 인증서
1. tls-rancher-ingress
- rancher ingress 용도
2. tls-ca
3. tls-rancher-internal-ca
- rancher agent와 인증과정에서 사용할 ca key
-------------
작업 순서
0. Agent Cluster 설정 변경(필요 시)
    - cluster coredns 설정
    - 이때 Agent랑 server랑 연결 중단(Agent 설정 자동 수정)

1. Key 생성
#CA 키 / 인증서 생성
openssl genrsa -out ca.key 2048
openssl req -new -x509 -days 3650 -key ca.key -subj "/C=KR/ST=SE/L=SE/O=jinseong/CN=KW Root CA" -out ca.crt

#서버 키 / 인증서 생성
openssl req -newkey rsa:2048 -nodes -keyout server.key -subj "/C=KR/ST=SE/L=SE/O=jin/CN=*.jinseong.net" -out server.csr

## SAN 포함해서 서버 인증서 서명
openssl x509 -req -extfile <(printf "subjectAltName=DNS:jinseong.net,DNS:rancher.jinseong.net") -days 3650 -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt

openssl x509 -inform PEM -in ca.crt > cacerts.pem

cp ca.crt tls.crt
cp ca.key tls.key

kubectl -n cattle-system create secret generic tls-ca --from-file=cacerts.pem=./cacerts.pem

kubectl -n cattle-system create secret generic tls-ca \
  --from-file=cacerts.pem=./cacerts.pem

kubectl -n cattle-system create secret tls new-tls-rancher-ingress \
  --cert=tls.crt \
  --key=tls.key

kubectl get secret -n cattle-system new-tls-rancher-ingress -o jsonpath="{.data.tls\.crt}" | base64 --decode >> tls-rancher-ingress.crt
kubectl get secret -n cattle-system new-tls-rancher-ingress -o jsonpath="{.data.tls\.key}" | base64 --decode >> tls-rancher-ingress.key


kubectl -n cattle-system delete secret tls-rancher-internal-ca

kubectl -n cattle-system create secret tls tls-rancher-internal-ca \
  --cert=tls-rancher-ingress.crt \
  --key=tls-rancher-ingress.key

kubectl rollout restart deployment/rancher -n cattle-system

kubectl annotate clusters.management.cattle.io c-m-68gl9296 io.cattle.agent.force.deploy=true
## 1. Rancher ingress 전용 Key 생성
kubectl create secret -n cattle-system tls new-new-new-tls-rancher-ingress --key server.key --cert server.crt

2. server chart hostname, tls 이름 수정 후 재 설치
helm upgrade --install rancher . \
  --namespace cattle-system \
  --values values.yaml

3. rancher agent와 인증과정에서 사용할 ca key server쪽 ca Key 재 구성
- tls-ca
- tls-rancher-internal-ca
    - https://github.com/rancher/rancher/issues/36632

4. Rancher UI ServerURL 변경
    -연결된 Cluster Agent 해당 URL로 변경되어 Restart

- . Agent 강제 재 구성
- 아래 명령어 날리면, 해당클러스터 머신들에 배포된 cattle-agent가 신규 설정된 값으로 재배포됨.
    (통신이 된다는 가정 하에)
- kubectl annotate clusters.management.cattle.io <REPLACE_WITH_CLUSTERID> io.cattle.agent.force.deploy=true

kubectl annotate clusters.management.cattle.io c-m-dnrrf57k io.cattle.agent.force.deploy=true
# 특정 어노테이션 제거
kubectl annotate clusters.management.cattle.io c-m-dnrrf57k io.cattle.agent.force.deploy-
- kubectl annotate clusters.management.cattle.io <REPLACE_WITH_CLUSTERID> io.cattle.agent.force.deploy=true --overwrite
- kubectl annotate clusters.management.cattle.io c-m-dnrrf57k io.cattle.agent.force.deploy=true --overwrite
OR
- kubectl patch clusters.management.cattle.io <REPLACE_WITH_CLUSTERID> -p '{"status":{"agentImage":"dummy"}}' --type merge
- Note, that the patch command works on 2.6.x clusters and earlier. On 2.7.x and later, use the annotate command.