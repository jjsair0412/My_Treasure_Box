# 

## Kasten ( k10 ) 활용 방안

kasten 관리용 namespace 생성

```bash
kubectl create ns kasten-io
```

kasten ( k10 ) helm repo 추가 및 동기화

```bash
helm repo add kasten https://charts.kasten.io/
helm repo update
```

kasten helm install

```bash
helm pull kasten/k10 --untar
helm install k10 kasten/k10 --namespace=kasten-io
```

kasten 설치 결과 확인

```bash
kubectl get pods -n kasten-io
```

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/5810d1ed-d8ca-4491-aa22-2480558bd6ef/Untitled.png)

kasten ui access 설정 

- kasten은 gateway service를통해서 ui를 접근합니다.

```bash
kubectl --namespace kasten-io port-forward service/gateway 8080:8000
```

ui 확인

- 아래 url로 접근해야 합니다.
    - kasten ingress 생성할때도 참고 필요 ( /k10/#/ 으로 리다이렉션 해야 함 )

```bash
http://127.0.0.1:8080/k10/#/
```

- 이메일과 회사 이름을 작성합니다..

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/6bf1482d-f60c-42c3-8c27-2d35e316a0bc/Untitled.png)

- Applications
    - namespace와 같은 단위 입니다.
    - kasten은 application별로 backup & restore 작업을 수행할 수 있습니다.

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/9075398b-fc8e-4c7f-a0c9-91b8237cc5c5/Untitled.png)

---

## BackUP TEST

### test용 argocd install

- argocd를 통해서 backup test를 진행합니다.

argocd helm install

```bash
kubectl create namespace argo
```

argocd helm repo 추가 및 동기화

```bash
helm repo add argo https://argoproj.github.io/argo-helm
helm repo update
```

argocd 설치용  helm chart download

```bash
helm pull argo/argo-cd --untar
```

argocd helm install

- NodePort로 install

```bash
helm upgrade --install argocd . \
--namespace=argo \
--set controller.logLevel="info" \
--set server.logLevel="info" \
--set repoServer.logLevel="info" \
--set server.replicas=2 \
--set server.ingress.https=true \
--set repoServer.replicas=2 \
--set controller.enableStatefulSet=true \
--set installCRDs=false \
--set server.service.type=NodePort \
-f values.yaml
```

설치 결과 확인

```bash
kubectl get pods -n argo
```

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/9d99729e-d6de-4c83-a88e-2e2ee3855166/Untitled.png)

admin 계정 password 확인

```bash
kubectl -n argo get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
```

argocd ui 확인

![스크린샷 2022-10-11 오후 4.55.39.png](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/3b8640e5-97a8-4ae6-9c5a-a5c2f5b3c9be/%E1%84%89%E1%85%B3%E1%84%8F%E1%85%B3%E1%84%85%E1%85%B5%E1%86%AB%E1%84%89%E1%85%A3%E1%86%BA_2022-10-11_%E1%84%8B%E1%85%A9%E1%84%92%E1%85%AE_4.55.39.png)

## Backup test

- test 프로세스
    1. elastic repository 등록
    2. kibana application 생성 및 배포
    3. kasten을 통해 snapshot 생성
    4. argocd application 및 repository 제거
    5. 생성한 snapshot으로 kasten에서 backup

1. mysql repository 등록
    - setting → Repository에서 Connect repo using ssh 버튼으로 elastic repository 등록 → connect 버튼으로 등록 완료
        - Name : elastic
        - Project : default
        - Repository URL : [https://github.com/elastic/helm-charts.git](https://github.com/elastic/helm-charts.git)
    
    ![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/991b48ad-47c6-428e-9be5-1c514302001c/Untitled.png)
    

등록 완료 후 상태 확인

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/bd8d69ae-2141-4705-ad33-777862370c03/Untitled.png)

1. kibana application 생성 및 배포

먼저 kibana 관리용 namespace를 생성합니다.

```bash
kubectl create ns kibana
```

- application → NEW APP → kibana helm chart 선택 후 create → sync 버튼으로 배포
    - Application Name : kibana-test
    - Project Name : default
    - Repository URL : [https://github.com/elastic/helm-charts.git](https://github.com/elastic/helm-charts.git)
    - Revision : HEAD
    - Path : kibana
    - Cluster URL : [https://kubernetes.default.svc](https://kubernetes.default.svc/)
    - Namespace : kibana
    - VALUES FILES : values.yaml

kibana 배포 

- application 클릭 후 SYNCHRONIZE 버튼 클릭합니다.
    - ( 배포가 되지 않더라도 , 해당 상태로 되돌아오면 backup 이 완료된것이기에 넘어가도록 합니다. )

![스크린샷 2022-10-11 오후 5.07.31.png](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/6934d4cf-2c53-492a-ba18-3262e52a6943/%E1%84%89%E1%85%B3%E1%84%8F%E1%85%B3%E1%84%85%E1%85%B5%E1%86%AB%E1%84%89%E1%85%A3%E1%86%BA_2022-10-11_%E1%84%8B%E1%85%A9%E1%84%92%E1%85%AE_5.07.31.png)

![스크린샷 2022-10-11 오후 5.25.53.png](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/d018558c-b6bb-4d45-a333-ce3237f73b31/%E1%84%89%E1%85%B3%E1%84%8F%E1%85%B3%E1%84%85%E1%85%B5%E1%86%AB%E1%84%89%E1%85%A3%E1%86%BA_2022-10-11_%E1%84%8B%E1%85%A9%E1%84%92%E1%85%AE_5.25.53.png)

### snapshot 생성

- kasten dashboard 이동 → Application 클릭

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/e4554458-56ad-48af-bf81-e0eac281dbc4/Untitled.png)

백업 대상 application ( namespace ) 의 Policy를 생성합니다

- Policy를 생성하는것은 추가 사항입니다. ( 권장 )
- application의 Create a Policy 버튼을 클릭하여 생성합니다.
    - snapshot 대상 및 주기 , 고급 옵션을 선택합니다.

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/ac5b1833-a9e9-4348-b362-c144fa01fb96/Untitled.png)

생성 완료 후 run once 버튼으로 생성한 policy 등록합니다.

- policy는 여러개 생성해 두고 사용할 수 있습니다.

데시보드에서 policy 개수와 policy 등록 상태를 확인할 수 있습니다.

![스크린샷 2022-10-11 오후 5.31.09.png](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/a236e4e4-c837-4ca0-a926-f6adaa0bf05c/%E1%84%89%E1%85%B3%E1%84%8F%E1%85%B3%E1%84%85%E1%85%B5%E1%86%AB%E1%84%89%E1%85%A3%E1%86%BA_2022-10-11_%E1%84%8B%E1%85%A9%E1%84%92%E1%85%AE_5.31.09.png)

policy가 등록된 application은 Compliant With Policies로 status가 변경됩니다.

![스크린샷 2022-10-11 오후 5.36.15.png](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/bb300d30-27fe-4416-be70-ad9bda6400c7/%E1%84%89%E1%85%B3%E1%84%8F%E1%85%B3%E1%84%85%E1%85%B5%E1%86%AB%E1%84%89%E1%85%A3%E1%86%BA_2022-10-11_%E1%84%8B%E1%85%A9%E1%84%92%E1%85%AE_5.36.15.png)

 백업 대상 application의 snapshot을 클릭한 뒤 , profile 등 옵션 선택 후 snapshot 생성합니다.

![스크린샷 2022-10-11 오후 5.39.00.png](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/ae820215-a5e0-4fc2-96ae-10d19f251c99/%E1%84%89%E1%85%B3%E1%84%8F%E1%85%B3%E1%84%85%E1%85%B5%E1%86%AB%E1%84%89%E1%85%A3%E1%86%BA_2022-10-11_%E1%84%8B%E1%85%A9%E1%84%92%E1%85%AE_5.39.00.png)

dashboard에서 backup process 상태를 확인할 수 있습니다.

![스크린샷 2022-10-11 오후 5.39.43.png](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/0b90ff29-b3f5-411f-b19d-5d0895a65c53/%E1%84%89%E1%85%B3%E1%84%8F%E1%85%B3%E1%84%85%E1%85%B5%E1%86%AB%E1%84%89%E1%85%A3%E1%86%BA_2022-10-11_%E1%84%8B%E1%85%A9%E1%84%92%E1%85%AE_5.39.43.png)

argocd에서 kibana를 제거합니다.

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/888273d3-2117-45fe-b461-f75ea2274ebe/Untitled.png)

elastic repository또한 제거합니다.

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/697e6df4-0cd7-4f86-b22c-fe07f296495b/Untitled.png)

만들어둔 snapshot으로 restore 진행합니다.

- dashboard → applications → argo 탭에서 restore 클릭
    - 생성해둔 snapshot중 한가지 클릭 → restore 클릭

![스크린샷 2022-10-11 오후 5.43.16.png](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/9abc8ac2-cd16-4a94-9a09-d8193517fe2d/%E1%84%89%E1%85%B3%E1%84%8F%E1%85%B3%E1%84%85%E1%85%B5%E1%86%AB%E1%84%89%E1%85%A3%E1%86%BA_2022-10-11_%E1%84%8B%E1%85%A9%E1%84%92%E1%85%AE_5.43.16.png)

restore status dashboard에서 확인

![스크린샷 2022-10-11 오후 5.43.51.png](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/fffbb6f3-5c5e-4ba7-a948-a27ba6798318/%E1%84%89%E1%85%B3%E1%84%8F%E1%85%B3%E1%84%85%E1%85%B5%E1%86%AB%E1%84%89%E1%85%A3%E1%86%BA_2022-10-11_%E1%84%8B%E1%85%A9%E1%84%92%E1%85%AE_5.43.51.png)

backup 완료 상태 확인

1. repository 상태 확인

![스크린샷 2022-10-11 오후 5.45.16.png](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/3bfbe77b-3586-40f5-89f6-bc90ab206373/%E1%84%89%E1%85%B3%E1%84%8F%E1%85%B3%E1%84%85%E1%85%B5%E1%86%AB%E1%84%89%E1%85%A3%E1%86%BA_2022-10-11_%E1%84%8B%E1%85%A9%E1%84%92%E1%85%AE_5.45.16.png)

1. application 상태 확인