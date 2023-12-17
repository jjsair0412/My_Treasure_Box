# cert-manager install and basic usecase
## ETC
cert-manager 트러블슈팅 과정
- cert-manager는 private subnet에 위치한 k8s일 경우, 에러가 발생할확률이 높습니다. **이유는 tls key를 생성하기 위해 challenge 리소스를 생성합니다. 이 challenge는 DNS로 접근하게 되는데, ClusterIssuer가 설정된 nginx로 http 요청을 보낼경우 해당 nginx가 DNS에 닿지 못한다면 에러가 발생합니다.

따라서 트러블슈팅이 발생한다면, 아래 공식 document를 확인하고 과정을 따라가며 메세지를 확인한뒤 에러를 해결하면 됩니다.
- [공식 docs-trobleshooting](https://cert-manager.io/docs/troubleshooting/acme/)

##  Precondition
해당 문서는 cert-manager를 설치하고 , 기본 사용 방안에 대해 기술합니다.

설치 환경은 Docker Desktop을 이용한 one node k8s 환경에서 설치합니다.
- [cert-manager 공식 문서](https://cert-manager.io/docs/configuration/selfsigned/)

## 0. cert-manager 개요
cert-manager는 kubernetes 내부에서 SSL 인증서를 생성하고 , 인증서가 유효하고 최신 상태인지 확인하고 만료되기 전에 구성된 시간에 인증서 갱신을 시도합니다.

Kubernetes 내에서 외부에 존재하는 Issuers를 활용하거나 selfsigned Issuer를 직접 생성해서 생성하여 Certificate를 생성하고, 이때 생성된 Certificate를 관리하며 인증서의 만료 시간이 가까워지면 인증서를 자동으로 갱신해줍니다.

### 0.1 Issuer의 종류
1. ClusterIssuer
    - 모든 namespace에 적용되는 인증서를 생성합니다.
2. Issuer
    - 특정 namespace에 적용되는 인증서를 생성합니다.

## 1. install cert-manager
helm chart로 cer-manager를 설치합니다.

```bash
$ helm repo add jetstack https://charts.jetstack.io

$ helm repo update

$ helm pull jetstack/cert-manager --version v1.11.0 --untar
```

### 1.1 namespace 생성
cert-manager를 관리하기 위한 namespace를 생성합니다.
```
$ kubectl create namespace cert-manager
```

## 2. Install CustomResourceDefinitions
cert-manager는 CRD resources 가 필수로 필요합니다.

따라서 kubectl 또는 helm chart를 수정하여 설치 합니다.
- kubectl 명령어로 설치
```bash
$ kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.11.0/cert-manager.crds.yaml
```

- helm chart values.yaml 파일 수정하여 설치
```yaml
...
installCRDs: false # true로 변경하여 설치

replicaCount: 1
...
```

## 3. helm install
helm chart 명령어를 통해 cert-manager를 설치합니다.
```bash
$ helm install \
    cert-manager jetstack/cert-manager \
    --create-namespace \
    --version v1.11.0 \
    # --set installCRDs=true
```

## 4. create Issuer
- [Issuer 관련 공식 문서](https://cert-manager.io/docs/configuration/selfsigned/)

k8s 내부에서 사용하기 위한 certificate를 생성하기 위해서 , elf-signed Issuer를 만들어 줍니다.

Issuer가 certificate를 생성해 줍니다.


email부분에만 작성하고 그대로 사용합니다.
- ClusterIssuer
```yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    # The ACME server URL
    server: https://acme-staging-v02.api.letsencrypt.org/directory
    # Email address used for ACME registration
    email: <your@email>
    # Name of a secret used to store the ACME account private key
    privateKeySecretRef:
      name: letsencrypt-staging
    # Enable the challenge provider
    solvers:
      - dns01:
          cloudflare:
            email: <your-email> 
            apiTokenSecretRef:
              name: cloudflare-api-key-secret #cloudflare api token
              key: api-token
```

- Issuer
```yaml
apiVersion: cert-manager.io/v1
kind: Issuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    # The ACME server URL
    server: https://acme-v02.api.letsencrypt.org/directory
    # Email address used for ACME registration
    email: <your@email>
    # Name of a secret used to store the ACME account private key
    privateKeySecretRef:
      name: letsencrypt-prod
    # Enable the challenge provider
    solvers:
      - dns01:
          cloudflare:
            email: <your-email>
            apiTokenSecretRef:
              name: cloudflare-api-key-secret #cloudflare api token
              key: api-token
```

kubectl apply 명령어로 생성합니다.
```
$ kubectl apply -f my-issuer.yaml
```

## 5. ingress생성
cert-manager의 annotation을 사용하여 ingress를 생성함으로써 key값을 관리하는 secret과 ingress를 같이 생성합니다.

### 5.1 일반 Issuer일 경우
namespace에 종속적인 Issuer일 경우 ```cert-manager.io/issuer``` 를 사용합니다.

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: test
  annotations:
    kubernetes.io/ingress.class: "nginx"
    cert-manager.io/issuer: "letsencrypt-prod" # issure 등록
spec:
  tls:
    - hosts:
      - test2.jjsair0412.xyz
      secretName: stage-test-prod-4 # 생성한 인증서가 저장되는 secret
  rules:
  - host: test2.jjsair0412.xyz
    http:
      paths:
      - pathType: Prefix
        path: "/"
        backend:
          service:
            name: nginx
            port:
              number: 80
```


### 5.2 일반 ClusterIssuer일 경우
ClusterIssuer일 경우 ```cert-manager.io/cluster-issuer``` 를 사용합니다.

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: test
  annotations:
    kubernetes.io/ingress.class: "nginx"
    cert-manager.io/cluster-issuer: "letsencrypt-prod" # cluster-issure 등록
spec:
  tls:
    - hosts:
      - test2.jjsair0412.xyz
      secretName: stage-test-prod-4 # 생성한 인증서가 저장되는 secret
  rules:
  - host: test2.jjsair0412.xyz
    http:
      paths:
      - pathType: Prefix
        path: "/"
        backend:
          service:
            name: nginx
            port:
              number: 80
```