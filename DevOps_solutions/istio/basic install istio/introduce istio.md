# istio install
## 1. introduce about istio
쿠버네티스 환경에서 다수의 컨테이너가 동작할 때 , 각 컨테이너의 트래픽을 관찰하고 정상 동작 여부를 모니터링하는것은 DevOps팀에게 부담이 될 수 있습니다.

개발자들이 MSA 아키텍처를 활용하여 여러 파드들이 메시 ( mesh ) 환경으로 구성되어 있다면 , 트래픽 관찰 및 장애 복구 등이 더욱 더 어려워질 것입니다.

따라서 istio를 사용합니다.

istio는 서비스 연결, 보안 , 제어 , 관찰 기능을 구현할 수 있는데 , 이것으로 모든 컨테이너를 로깅하거나 정책 시스템을 통합할 수 있는 API 플랫폼 입니다.

istio는 MSA간의 모든 네트워크 통신을 관찰하는 사이드카 프록시를 배치해 각 포드에 istio-proxy 사이드카로 모니터링 할 수 있습니다.

istio로 배포한 application은 istio의 crd인 gateway.networking.istio.io를 사용하여 외부에 노출시켜야 하며 , VirtualService crd와 연동해야 합니다.

**따라서 istio를 사용하면 ingress-nginx와 같은 ingress 진입점이 한가지 더 생성된다고 생각하면 됩니다.**

ssl 인증 및 header 설정들은 istio gateway에서 설정값으로 설정합니다.
[istio gateway 설정](https://istio.io/latest/docs/reference/config/networking/gateway/)

[istio 공식 문서 - istio란 ?](https://istio.io/latest/about/service-mesh/)

## 2. istio 동작 방식
istio를 배포하게 되면 , 파드 내부에 사이드카 컨테이너가 하나 더 생성됩니다.

**사이드카 컨테이너는 proxy 컨테이너로 , prometheus로 메트릭을 전송해주고 , prometheus는 istio dashboard인 kiali로 메트릭을 전송합니다.**

사용자는 kiali에 접근하여 시각화된 메시구조를 확인할 수 있습니다.

### 2.1 설치 시 주의사항
istio는 namespace별로 파드를 관리합니다.

따라서 istio에서 관리할 project ( MSA application ) 이 올라갈 namespace에 특정 설정을 해 주어야 해당 namespace의 파드에 사이드카 컨테이너가 같이 올라가게 됩니다.

istio를 사용할 namespace에는 istio-injection=enabled라는 label을 등록시켜야 합니다.
```bash
# default namespace에서 istio-injection 사용
$ kubectl label namespace default istio-injection=enabled
```
[해당 내용 공식문서](https://istio.io/latest/docs/setup/additional-setup/sidecar-injection/#controlling-the-injection-policy)
## 3. deploy istio
해당 문서에서는 istio를 istioctl로 설치합니다.

istio는 세가지 방법으로 배포할 수 있습니다.
1. istioctl
2. istio operator
3. istio manifest generator
4. helm chart

공식 문서에 따른 best pratice는 istioctl로 설치하는것 이기에 해당 방안을 따릅니다.

### 3.1 istioctl install
istioctl부터 설치합니다.
- 문서작성 시점 ( 20221116 ) 일자로 istio의 latest version은 1.16.0 입니다.
```bash
$ curl -L https://istio.io/downloadIstio | sh - # latest version download
$ cd istio-1.16.0 
$ export  PATH=$PWD/bin:$PATH
$ istioctl # 설치 확인
```
### 3.1 istio helm 설치
helm 설치 방안은 아래와 같습니다.

helm pull로 values.yaml파일을 꺼내와서 상세 설정을 변경시킬 수 있습니다.
```
$ helm repo add istio https://istio-release.storage.googleapis.com/charts
$ helm repo update


$ helm install install/kubernetes/helm/istio \
--name istio \
--namespace istio-system \
--set tracing.enabled=true \
--set global.mtls.enabled=true \
--set grafana.enabled=true \
--set kiali.enabled=true \
--set servicegraph.enabled=true
```

### 3.2 istio profile 설치
istio 설치 profile 목록을 확인하여 , istio 배포 형태를 선택합니다.
필요한 모듈만 떼어내서 설치합시다.

해당 문서에는 demo로 설치합니다.
demo profile은 적당한 리소스 요구 사항으로 Istio 기능을 보여주도록 설계된 구성입니다.
bookinfo와 같은 application의 작업을 수행하기에 적합합니다. ( 제일 보편적 )

![istio-1][istio-1]

[istio-1]:./images/istio-1.PNG

[profile 관련 모든 정보 문서 꼭 읽어보기](https://istio.io/latest/docs/setup/additional-setup/config-profiles/)

아래 명령어로 설치 가능한 profile을 체크합니다.
```bash
$ istioctl profile list
    ambient
    default
    demo
    empty
    external
    minimal
    openshift
    preview
    remote
```

istio demo profile을 설치합니다. ( 5분정도 소요 )
```bash
# profile 옵션에 원하는 profile을 넣어주면 됩니다.
$ istioctl install --set profile=demo --skip-confirmation
✔ Istio core installed                                                                                                                                        
✔ Istiod installed                                                                                                                                            
✔ Egress gateways installed                                                                                                                                   
✔ Ingress gateways installed                                                                                                                                  
✔ Installation complete                                                                                                                                       Making this installation the default for injection and validation.

Thank you for installing Istio 1.16.  Please take a few minutes to tell us about your install/upgrade experience!  https://forms.gle/99uiMML96AmsXY5d6
```
### 3.3 kiali 및 prometheus 배포
istio dashbaord인 kiali와 dashboard에 메트릭을 뿌려줄 prometheus를 배포합니다.

helm chart로 설치했다면 values.yaml로 배포를 한번에 할 수 있기에 따로 배포할 필요가 없습니다.
```bash
kubectl apply -f samples/addons/kiali.yaml -n istio-system
kubectl apply -f samples/addons/prometheus.yaml -n istio-system
```

테스트 시에는 LB가 없기 때문에 service type을 nodePort로 변경하여 확인합니다.

## 4. bookinfo application 배포
[bookinfo istio 공식 문서](https://istio.io/latest/docs/examples/bookinfo/)
bookinfo application을 배포하여 istio 관리 대상으로 지정해 보겠습니다.

bookinfo 관리용 namespace 생성 및 istio-injection label 추가
```bash
$ kubectl create ns bookinfo

$ kubectl label namespace bookinfo istio-injection=enabled
```

### 4.1 gateway api crd 배포 ( 모든 istio 환경에 필수 )
Kubernetes Gateway API CRD부터 배포합니다.
대부분의 k8s system은 gateway api crd가 배포되어있지 않기 때문에 get 명령으로 확인 후 생성합니다.
```bash
# k8s gateway api crd 확인
$ kubectl get crd gateways.gateway.networking.k8s.io

# 없다면 아래 명령으로 배포
$ kubectl kustomize "github.com/kubernetes-sigs/gateway-api/config/crd?ref=v0.5.1"  |  kubectl apply -f -;
```

### 4.2 bookinfo 배포
bookinfo application을 배포합니다.
```bash
$ kubectl -n bookinfo apply -f samples/bookinfo/platform/kube/bookinfo.yaml
```

배포상태를 확인합니다.
```bash
$ kubectl get pods -n bookinfo
NAME                              READY   STATUS    RESTARTS   AGE
details-v1-7d4d9d5fcb-sm4qc       2/2     Running   0          2m53s
productpage-v1-66756cddfd-j2wm7   2/2     Running   0          2m52s
ratings-v1-85cc46b6d4-vs56j       2/2     Running   0          2m53s
reviews-v1-777df99c6d-l96cx       2/2     Running   0          2m52s
reviews-v2-cdd8fb88b-snc4k        2/2     Running   0          2m52s
reviews-v3-58b6479b-7lwsb         2/2     Running   0          2m52s
```

### 4.3 istio gateway 생성
bookinfo용 istio gateway를 생성합니다.
애플리케이션을 생성했지만 게이트웨이를 추가로 생성해야 외부로 접근이 가능합니다.
istio gateway는 crd이며, 마치 쿠버네티스에서 제공되는 Ingress처럼 동작합니다.

```bash
$ kubectl apply -f samples/bookinfo/networking/bookinfo-gateway.yaml -n bookinfo
```

배포가 정상적으로 진행되었는지 확인합니다.
```bash
$ kubectl get gateway.networking.istio.io -n bookinfo
NAME               AGE
bookinfo-gateway   27m
```

## 5. RESULT :  bookinfo 접근 및 kiali dashboard 확인
istio는 기본적으로 LB 타입으로 생성됩니다.

readme에 작성했듯이 nodePort로 열어두었기 때문에 , 공인ip:포트 로 접근하여 정상적으로 frontend page가 출력되는지 확인합니다.


http://3.xxx.xxx.xxx:31331/productpage

![istio-2][istio-2]

[istio-2]:./images/istio-2.PNG


bookinfo 아키텍쳐는 다음과 같습니다.

![istio-4][istio-4]

[istio-4]:./images/istio-4.PNG

kiali dashboard에서 bookinfo 메트릭이 확인되는지 체크합니다.
좌측 메뉴바에 Graph 를 클릭하여 bookinfo namespace를 클릭합니다.

bookinfo 페이지를 계속 세로고침하여 트래픽을 생성한 뒤 , 데시보드를 확인합니다.

각 파드에서 파드로 메트릭이 이동하는것을 시각화하여 볼 수 있으며 , 어떤 파드에서 어떤 파드로 이동할 때 2xx , 3xx , 4xx , 5xx 에러가 발생했는지 , 트래픽이 얼마나 쌓엿는지 등을 확인하여 조치할 수 있습니다.
![istio-3][istio-3]

[istio-3]:./images/istio-3.PNG