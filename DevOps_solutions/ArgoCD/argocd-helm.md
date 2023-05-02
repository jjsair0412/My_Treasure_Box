
# ArgoCD Install Helm
## [](#prerequisites)1. Prerequisites

-   Helm chart를 이용한 argocd 설치
-   참고 링크
    -   [argocd docs](https://argo-cd.readthedocs.io/en/stable/)
    -   [argocd Helm Chart](https://github.com/argoproj/argo-helm.git)
   
## 2. ArgoCD 설치
### 2.1 namespace 설정
- ArgoCD 배포용 namespace 설정
```
$ kubectl create namespace argo
```

-   Agrocd Helm Repo 추가 및 동기화

```
$ helm repo add argo https://argoproj.github.io/argo-helm
$ helm repo update
```

-   Agrocd 설치 용 Helm Download

```
$ helm pull argo/argo-cd --untar
```

### 2.2 Helm ArgoCD Chart 설치
installCRDs를 false로 두고 , 차트 외부에 CRD를 설치합니다.

```bash
$ helm upgrade --install argocd . \
--namespace=argo \
--set dex.logLevel="info" \
--set notifications.logLevel="info" \
--set controller.enableStatefulSet=true \
--set installCRDs=false \
-f values.yaml
```

차트 외부 CRD 설치
```bash
kubectl apply -k "https://github.com/argoproj/argo-cd/manifests/crds?ref=<appVersion>"

# Eg. version v2.4.9
kubectl apply -k "https://github.com/argoproj/argo-cd/manifests/crds?ref=v2.4.9"
```

아래와 같은 output을 확인할 수 있습니다.
```bash
...
1. kubectl port-forward service/argocd-server -n argo 8080:443

    and then open the browser on http://localhost:8080 and accept the certificate

2. enable ingress in the values file `server.ingress.enabled` and either
      - Add the annotation for ssl passthrough: https://argo-cd.readthedocs.io/en/stable/operator-manual/ingress/#option-1-ssl-passthrough
      - Set the `configs.params."server.insecure"` in the values file and terminate SSL at your ingress: https://argo-cd.readthedocs.io/en/stable/operator-manual/ingress/#option-2-multiple-ingress-objects-and-hosts

After reaching the UI the first time you can login with username: admin and the random password generated during the installation. You can find the password by running:

kubectl -n argo get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d # default password
```

### 2.3 ArgoCD ingress 설정
-   Https Ingress 용 TLS 인증서 생성
- TLS 인증서를 생성하지 않아도 무관하다. -> 실습할때만

```
$ kubectl create -n argocd secret tls argocd-tls --key {key.file.name} --cert {cert.file.name}
```

-   Argo CD runs both a gRPC server (used by the CLI), as well as a HTTP/HTTPS server 용 Ingress 2개를 생성
-   주의 사항은 Insecure Mode를 활성화 하지 않을 경우 nginx.ingress.kubernetes.io/backend-protocol: "HTTPS"를 명시해줘야 Redirection Loop Error가 발생 하지 않음
```
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: argocd-server-http-ingress
  namespace: argo
  annotations:
    kubernetes.io/ingress.class: "nginx"
    ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
    nginx.ingress.kubernetes.io/backend-protocol: "HTTPS"
spec:
  rules:
  - host: "jinseong.xxx.xxx.net"
    http:
      paths:
      - pathType: Prefix
        path: /
        backend:
          service:
            name: argocd-server
            port:
              number: 80
  tls: # TLS를 생성하지 않았다면 , 해당 key와 value들 제거
  - hosts:
    - jinseong.xxx.xxx.net
    secretName: # tls secret name

---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: argocd-server-grpc-ingress
  namespace: argo
  annotations:
    kubernetes.io/ingress.class: "nginx"
    nginx.ingress.kubernetes.io/backend-protocol: "GRPC"
spec:
  rules:
  - host: "dev-jinseong.xxx.xxx.net"
    http:
      paths:
      - pathType: Prefix
        path: /
        backend:
          service:
            name: argocd-server
            port:
              number: 80
  tls: # TLS를 생성하지 않았다면 , 해당 key와 value들 제거
  - hosts:
    - dev-jinseong.xxx.xxx.net
    secretName: # tls secret name
```
## 3. 초기 UI 확인
- 초기 Password Get
- 초기 id 또한 admin이다.
```
$ kubectl -n argo get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d && echo
```
-  ArgoCD UI 확인
![argocd-ui-1][argo-ui-1]

  

[argo-ui-1]:./images/argo-ui-1.PNG