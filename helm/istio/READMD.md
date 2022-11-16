# istio information
istio-ingressgateway는 기본적으로 LB type으로 배포됩니다.

아래 문서는 Test를 LB가 없이 NodePort로 동작시켰기에 svc들을 모두 edit명령어를 통해 NodePort로 변경시켜서 사용했습니다.

따라서 LB가 없다면 , NodePort를 사용해야 하며 ,  앞단 haproxy나 matallb같은 물리 LB가 존재하는 경우에는 istio-ingressgateway 구성을 변경시켜야 합니다.

kiali나 prometheus를 같이 배포해야 편하기에 , helm install이 더 적당한 설치 방안인것 같기도 하지만 , istioctl로 설치합니다.

## istio gateway 설정
istio는 crd인 gateway가 생성되고 , 얘는 ingress와 동일한 역할을 합니다.

nginx ingress 흐름
- ingress -> service -> pod

istio gateway 흐름
- Gateway -> VirtualService -> pod

따라서 ingress와 동일하게 gateway를 custom하여 설정할 수 있으며 , 아래 공식 문서를 참조합니다.
[istio gateway 설정](https://istio.io/latest/docs/reference/config/networking/gateway/)
