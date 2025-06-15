# Rancher 구축
## Overview
해당 문서는 Rancher를 Self-Signed 인증서로 구축하는 방법에 대해 기술함.

인증서 관리 타입 별 방법이 상이함.
1. 공인 인증서
- kubernetes/tls secret 생성 후 ingress 등록

2. 사설 인증서
- CA 인증서, 서버 인증서로 kubernetes/tls secret 생성 ingress 등록
    - ca 인증서 : cattle-system 네임스페이스 "tls-ca" 로 생성, Rancher 구동 시 볼륨 등록
    - server 인증서 : rancher ingress "tls-rancher-ingress" 에 등록

3. Cert-Manager 사용 시
- 인증서 등록 시 ingress annotation 삭제 후 신규 인증서 적용

## 작업 진행
### 1. 인증서
- 등록할 도메인에 맞게끔 인증서 발급
```bash
#CA 키 / 인증서 생성

openssl genrsa -out ca.key 2048

openssl req -new -x509 -days 3650 -key ca.key -subj "/C=KR/ST=SE/L=SE/O=test/CN=KW Root CA" -out ca.crt

#서버 키 / 인증서 생성

openssl req -newkey rsa:2048 -nodes -keyout server.key -subj "/C=KR/ST=SE/L=SE/O=test/CN=*.test.net" -out server.csr

openssl x509 -req -extfile <(printf "subjectAltName=DNS:test.net,DNS:rancher.test.net") -days 3650 -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt


kubectl create secret -n cattle-system tls tls-rancher-ingress --key server.key --cert server.crt
```

### 2. Helm을 통한 Rancher 구축
- PrivateCA와 ingress.tls.source, hostname 수정 필요
```bash
ingress.tls.source=secret
privateCA=true

helm upgrade -i rancher rancher-stable/rancher \
  --namespace cattle-system \
  --set hostname=rancher.test.net \
  --set ingress.tls.source=secret \
  --set privateCA=true
```

## ETC 사설 인증서 및 ingress 수정 방안
```bash
#사설인증서를 통해 Agent 등록을 위한 CA CHECKSUM 확인       

curl -k -s -fL rancher.kw01/v3/settings/cacerts | jq -r .value > cacert.tmp

sha256sum cacert.tmp | awk '{print $1}'

#ingress.yml
---
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  annotations:
# 아래 2줄 삭제
    cert-manager.io/issuer: rancher # 제거
    cert-manager.io/issuer-kind: Issuer # 제거
    nginx.ingress.kubernetes.io/proxy-connect-timeout: "30"    
    nginx.ingress.kubernetes.io/proxy-read-timeout: "1800"    
    nginx.ingress.kubernetes.io/proxy-send-timeout: "1800"
  labels:
    app: rancher
  name: rancher
  namespace: cattle-system
spec:
  rules:
  - host: rancher.test.net
    http:
      paths:
      - backend:
          serviceName: rancher
          servicePort: 80
  tls:
  - hosts:
    - rancher.test.net
    secretName: tls-rancher-ingress
```