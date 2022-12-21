# kubernetes_info
![header](https://capsule-render.vercel.app/api?type=waving&color=auto&height=300&section=header&text=👋%20Welcome%20to%20K8S%20INFOMATION!!&fontSize=50&animation=fadeIn&fontAlignY=38)

## INDEX
- [AWS - AWS 관련](https://github.com/jjsair0412/kubernetes_info/tree/main/AWS)
    - [AWS basic information - AWS 개념 정리](https://github.com/jjsair0412/kubernetes_info/blob/main/AWS/AWS%20basic%20information/AWS%20info.md)
    - [My AWS architecture - 만들어본 AWS 아키텍처 및 테라폼 코드 ( 코드 작성 예정 )](https://github.com/jjsair0412/kubernetes_info/tree/main/AWS/My%20AWS%20architecture)
        - [haproxy - rke2 - ceph composition](https://github.com/jjsair0412/kubernetes_info/tree/main/AWS/My%20AWS%20architecture/haproxy%20-%20rke2%20-%20ceph%20composition)
- [backup - backup 방안](https://github.com/jjsair0412/kubernetes_info/tree/main/backup)
    - [argo - CronJob으로 argcd backup . 타 솔루션에서 해당 방안 응용 가능 ](https://github.com/jjsair0412/kubernetes_info/tree/main/backup/argo)
- [cicd - cicd pipeline](https://github.com/jjsair0412/kubernetes_info/tree/main/cicd)
    - [springBoot-gradle - gradle project cicd pipeline 및 코드 (고도화 중)](https://github.com/jjsair0412/kubernetes_info/tree/main/cicd/springBoot-gradle)
        - [gradle-helm-chart - helm chart](https://github.com/jjsair0412/kubernetes_info/tree/main/cicd/springBoot-gradle/gradle-helm-chart)
        - [springBoot ( gradle ) jenkins pipeline - jenkins pipeline 및 실제 코드](https://github.com/jjsair0412/kubernetes_info/tree/main/cicd/springBoot-gradle/springBoot%20(%20gradle%20)%20jenkins%20pipeline)
    - [springBoot-maven - maven project cicd pipeline 및 코드 (코드 및 차트 제작예정)]()
- [Docker](https://github.com/jjsair0412/kubernetes_info/tree/main/Docker)
    - [docker compose](https://github.com/jjsair0412/kubernetes_info/tree/main/Docker/docker%20compose)
        - [docker compose info - docker compose 개념 정의 및 설치방안 가이드](https://github.com/jjsair0412/kubernetes_info/blob/main/Docker/docker%20compose/docker%20compose%20info.md)
        - [docker compose helm chart 변환 - docker compose -> k8s migration 가이드](https://github.com/jjsair0412/kubernetes_info/blob/main/Docker/docker%20compose/docker%20compose%20helm%20chart%20%EB%B3%80%ED%99%98.md)
    - [docker info - docker 개념정리 및 사용방안 정의](https://github.com/jjsair0412/kubernetes_info/blob/main/Docker/docker%20info.md)
    - [docker offline install - private registry 설치 및 연동 법 ( rke2 , kubeadm )](https://github.com/jjsair0412/kubernetes_info/blob/main/Docker/docker%20offline%20install%20-%20private%20registry%20%EC%84%A4%EC%B9%98%20%EB%B0%8F%20%EC%97%B0%EB%8F%99%20%EB%B2%95%20(%20rke2%20%2C%20kubeadm%20).md)
- [EFK - EFK stack info](https://github.com/jjsair0412/kubernetes_info/tree/main/efk)
    - [efk index lifecycle info - efk stack 사용 방안 및 lifecycle 정의](https://github.com/jjsair0412/kubernetes_info/blob/main/efk/efk%20index%20lifecycle%20info.md)
- [EKS - AWS EKS info](https://github.com/jjsair0412/kubernetes_info/tree/main/EKS)
    - [EKS_info - EKS 설치 & 사용 방안 및 개념 정의](https://github.com/jjsair0412/kubernetes_info/blob/main/EKS/EKS_info.md)
- [ETC - 기타 툴 설치방안 및 가이드 모음](https://github.com/jjsair0412/kubernetes_info/tree/main/etc)
    - [helm install - offline 환경](https://github.com/jjsair0412/kubernetes_info/blob/main/etc/helm%20install%20-%20offline%20%ED%99%98%EA%B2%BD.md)
    - [nfs server 구축 방안](https://github.com/jjsair0412/kubernetes_info/blob/main/etc/nfs%20server%20%EA%B5%AC%EC%B6%95.md)
    - [offline ubuntu package install - 폐쇄망 환경에서의 ubuntu package 설치 방안](https://github.com/jjsair0412/kubernetes_info/blob/main/etc/offline%20ubuntu%20package%20install.md)
    - [private registry push shell script - private registry에 push하는 shell script](https://github.com/jjsair0412/kubernetes_info/blob/main/etc/private%20registry%20push%20shell%20script.md)
    - [kubesphere-ci - kubesphere를 통한 ci 가이드](https://github.com/jjsair0412/kubernetes_info/tree/main/etc/kubesphere-ci)
        - [KE CI demo - kubesphere ci 가이드](https://github.com/jjsair0412/kubernetes_info/blob/main/etc/kubesphere-ci/KE%20CI%20demo.md)
- [grafana - grafana dashboard 관련 정보](https://github.com/jjsair0412/kubernetes_info/tree/main/grafana)
    - [grafana dashboards - dashboard json 파일](https://github.com/jjsair0412/kubernetes_info/tree/main/grafana/grafana%20dashboards)
        - [k8s grafana dashboard - 기본 dashboard json 파일](https://github.com/jjsair0412/kubernetes_info/blob/main/grafana/grafana%20dashboards/k8s%20grafana%20dashboard.json)
        - [k8s grafana multi cluster dashboard - multi cluster dashboard json 파일](https://github.com/jjsair0412/kubernetes_info/blob/main/grafana/grafana%20dashboards/k8s%20grafana%20multi%20cluster%20dashboard.json)
    - [grafana with redis - redis와 grafana 연동 방안](https://github.com/jjsair0412/kubernetes_info/tree/main/grafana/grafana%20with%20redis)
        - [grafana redis 연동 - 가이드 문서](https://github.com/jjsair0412/kubernetes_info/blob/main/grafana/grafana%20with%20redis/grafana%20redis%20%EC%97%B0%EB%8F%99.md)
- [HandMade-helmCharts - 제작한 helm chart](https://github.com/jjsair0412/kubernetes_info/tree/main/HandMade-helmCharts)
    - [loop-chart - values.yaml에서 설정해둔 갯수만큼 deployment 복제하여 여러개 생성하는 helm chart](https://github.com/jjsair0412/kubernetes_info/tree/main/HandMade-helmCharts/loop-chart)
    - [nginx-sc-connect-helmchart - openEBS storageclass를 사용하는 deployment를 생성하는 helm chart . 다른 sc로 응용 가능](https://github.com/jjsair0412/kubernetes_info/tree/main/HandMade-helmCharts/nginx-sc-connect-helmchart)
- [haproxy - haproxy 설치 방안 및 관련정보](https://github.com/jjsair0412/kubernetes_info/tree/main/haproxy)
    - [haproxy 설정 방안](https://github.com/jjsair0412/kubernetes_info/blob/main/haproxy/haproxy%20%EC%84%A4%EC%A0%95%20%EB%B0%A9%EC%95%88.md)
    - [haproxy 이중화 방안](https://github.com/jjsair0412/kubernetes_info/blob/main/haproxy/haproxy%20%EC%9D%B4%EC%A4%91%ED%99%94%20%EB%B0%A9%EC%95%88.md)
- [k8s's solutions - cncf k8s 생태계 솔루션들의 설치 방안 및 가이드 모음](https://github.com/jjsair0412/kubernetes_info/tree/main/k8s's%20solutions)
    - [ArgoCD](https://github.com/jjsair0412/kubernetes_info/tree/main/k8s's%20solutions/ArgoCD)
        - [argcod_rollout_BlueGreen - argocd rollout 사용 가이드](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/ArgoCD/argcod_rollout_BlueGreen.md)
        - [argocd-helm - argocd helm 설치 가이드](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/ArgoCD/argocd-helm.md)
    - [ceph - ceph 개념 정의](https://github.com/jjsair0412/kubernetes_info/tree/main/k8s's%20solutions/ceph)
        - [K8S storageclass에 외부 ( 기존 ) ceph cluster 사용 가이드](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/ceph/K8S%20storageclass%EC%97%90%20%EC%99%B8%EB%B6%80%20(%20%EA%B8%B0%EC%A1%B4%20)%20ceph%20cluster%20%EC%82%AC%EC%9A%A9%20%EA%B0%80%EC%9D%B4%EB%93%9C.md)
        - [rook-ceph object gateway bucket 사용법 - s3 command로 ceph 관리 방안 ( RESTAPI로 ceph 관리 )](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/ceph/ceph%20object%20gateway%20bucket%20%EC%82%AC%EC%9A%A9%EB%B2%95.md)
        - [ceph with minio - minio를 통한 ceph 저장 파일 CRUD 시각화](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/ceph/ceph%20with%20minio.md)
        - [cephadm 설치 방안 - cephadm을 통한 삼중화 클러스터링 구성](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/ceph/cephadm%20%EC%84%A4%EC%B9%98%20%EB%B0%A9%EC%95%88%20-%20cephadm%EC%9D%84%20%ED%86%B5%ED%95%9C%20%EC%82%BC%EC%A4%91%ED%99%94%20%ED%81%B4%EB%9F%AC%EC%8A%A4%ED%84%B0%EB%A7%81%20%EA%B5%AC%EC%84%B1.md)
        - [rook-ceph 설치 방안 in k8s - k8s에서 ceph을 관리하기위한 rook을 통한 helm 설치 방안](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/ceph/rook-ceph%20%EC%84%A4%EC%B9%98%20%EB%B0%A9%EC%95%88%20in%20k8s.md)
    - [devsecops를 위한 ThreatMapper](https://github.com/jjsair0412/kubernetes_info/tree/main/k8s's%20solutions/devsecops%EB%A5%BC%20%EC%9C%84%ED%95%9C%20ThreatMapper)
        - [ThreatMapper 배포 방안](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/devsecops%EB%A5%BC%20%EC%9C%84%ED%95%9C%20ThreatMapper/ThreatMapper%20%EB%B0%B0%ED%8F%AC%20%EB%B0%A9%EC%95%88.md)
    - [efk](https://github.com/jjsair0412/kubernetes_info/tree/main/k8s's%20solutions/efk)
        - [efk_설치_with helm chart](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/efk/efk_%EC%84%A4%EC%B9%98_with%20helm%20chart.md)
    - [elk](https://github.com/jjsair0412/kubernetes_info/tree/main/k8s's%20solutions/elk)
        - [elk helm install - air gap 환경 포함](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/elk/elk%20helm%20install%20-%20air%20gap%20%ED%99%98%EA%B2%BD%20%ED%8F%AC%ED%95%A8.md)
    - [gitlab](https://github.com/jjsair0412/kubernetes_info/tree/main/k8s's%20solutions/gitlab)
        - [install gitlab with helm chart](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/gitlab/install%20gitlab%20with%20helm%20chart.md)
    - [gpu-operator](https://github.com/jjsair0412/kubernetes_info/tree/main/k8s's%20solutions/gpu-operator)
        - [GPU Operator 설치 - 폐쇄망 포함](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/gpu-operator/GPU%20Operator%20%EC%84%A4%EC%B9%98%20-%20%ED%8F%90%EC%87%84%EB%A7%9D%20%ED%8F%AC%ED%95%A8.md)
    - [grafana , prometheus](https://github.com/jjsair0412/kubernetes_info/tree/main/k8s's%20solutions/grafana%20%2C%20prometheus)
        - [grafana 및 prometheus 설치방안](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/grafana%20%2C%20prometheus/grafana%20%EB%B0%8F%20prometheus%20%EC%84%A4%EC%B9%98%EB%B0%A9%EC%95%88.md)
        - [prometheus grafana 연동 방안 - 응용 가능](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/grafana%20%2C%20prometheus/prometheus%20grafana%20%EC%97%B0%EB%8F%99%20%EB%B0%A9%EC%95%88.md)
    - [harbor](https://github.com/jjsair0412/kubernetes_info/tree/main/k8s's%20solutions/harbor)
        - [harbor 설치 with helm](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/harbor/harbor%20%EC%84%A4%EC%B9%98%20with%20helm.md)
    - [ingress-nginx](https://github.com/jjsair0412/kubernetes_info/tree/main/k8s's%20solutions/ingress-nginx)
        - [ingress nginx helm install - rke2, kubeadm](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/ingress-nginx/ingress%20nginx%20helm%20install%20-%20rke2%2C%20kubeadm%20.md)
    - [istio - istio 개념 기본 설명](https://github.com/jjsair0412/kubernetes_info/tree/main/k8s's%20solutions/istio)
        - [basic install istio - 설치 가이드 및 사용방안](https://github.com/jjsair0412/kubernetes_info/tree/main/k8s's%20solutions/istio/basic%20install%20istio)
    - [jenkins](https://github.com/jjsair0412/kubernetes_info/tree/main/k8s's%20solutions/jenkins)
        - [jenkins air gap install-helm](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/jenkins/jenkins%20air%20gap%20install-helm%20.md)
        - [jenkins-helm](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/jenkins/jenkins-helm.md)
    - [kasten](https://github.com/jjsair0412/kubernetes_info/tree/main/k8s's%20solutions/kasten)
        - [kasten ( k10 ) 설치 및 활용 방안](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/kasten/kasten%20(%20k10%20)%20%EC%84%A4%EC%B9%98%20%EB%B0%8F%20%ED%99%9C%EC%9A%A9%20%EB%B0%A9%EC%95%88.md)
    - [mariaDB & wordpress](https://github.com/jjsair0412/kubernetes_info/tree/main/k8s's%20solutions/mariaDB%20%26%20wordpress)
        - [mariadb&wordpress 설치 방안](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/mariaDB%20%26%20wordpress/mariadb%26wordpress.md)
    - [rabbitmq](https://github.com/jjsair0412/kubernetes_info/tree/main/k8s's%20solutions/rabbitmq)
        - [rabbitmq helm install](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/rabbitmq/rabbitmq%20helm%20install.md)
        - [rabbitmq user 정보 변경 방안](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/rabbitmq/rabbitmq%20user%20modfiy.md)
    - [redis](https://github.com/jjsair0412/kubernetes_info/tree/main/k8s's%20solutions/redis)
        - [install redis helm](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/redis/install%20redis%20helm.md)
    - [helm info - helm 사용 방안](https://github.com/jjsair0412/kubernetes_info/blob/main/k8s's%20solutions/helm%20info.md)
- [kubernetes 구축 - k8s ha구성 아키텍처 및 가이드](https://github.com/jjsair0412/kubernetes_info/tree/main/kubernetes%20%EA%B5%AC%EC%B6%95)
    - [ha 구축 - 고 가용성을 확보한 아키텍처](https://github.com/jjsair0412/kubernetes_info/tree/main/kubernetes%20%EA%B5%AC%EC%B6%95/ha%20%EA%B5%AC%EC%B6%95)
        - [kubeadm external etcd cluster - master 1EA , etcd 2EA 구성 . 해당문서 따라하면 worker , master + lb , external etcd 구성까지 완료할 수 있음.](https://github.com/jjsair0412/kubernetes_info/blob/main/kubernetes%20%EA%B5%AC%EC%B6%95/ha%20%EA%B5%AC%EC%B6%95/kubeadm%20external%20etcd%20cluster.md)
    - [kubeadm](https://github.com/jjsair0412/kubernetes_info/tree/main/kubernetes%20%EA%B5%AC%EC%B6%95/kubeadm)
    - [kubesphere , kubekey - kown issue](https://github.com/jjsair0412/kubernetes_info/tree/main/kubernetes%20%EA%B5%AC%EC%B6%95/kubesphere%20%2C%20kubekey)
        - [KubeKey를 통한 kubesphere 배포 방안](https://github.com/jjsair0412/kubernetes_info/tree/main/kubernetes%20%EA%B5%AC%EC%B6%95/kubesphere%20%2C%20kubekey/KubeKey%EB%A5%BC%20%ED%86%B5%ED%95%9C%20kubesphere%20%EB%B0%B0%ED%8F%AC%20%EB%B0%A9%EC%95%88)
        - [kubesphere with rke2 - rke2 cluster 위에 kubesphere 배포](https://github.com/jjsair0412/kubernetes_info/tree/main/kubernetes%20%EA%B5%AC%EC%B6%95/kubesphere%20%2C%20kubekey/kubesphere%20with%20rke2)
    - [kubespray](https://github.com/jjsair0412/kubernetes_info/tree/main/kubernetes%20%EA%B5%AC%EC%B6%95/kubespray)
        - [kubespray-version-upgarde](https://github.com/jjsair0412/kubernetes_info/blob/main/kubernetes%20%EA%B5%AC%EC%B6%95/kubespray/kubespray-version-upgarde.md)
        - [Online-kubespray - kubespray cluster 구성 방안](https://github.com/jjsair0412/kubernetes_info/blob/main/kubernetes%20%EA%B5%AC%EC%B6%95/kubespray/Online-kubespray.md)
    - [Minikube](https://github.com/jjsair0412/kubernetes_info/tree/main/kubernetes%20%EA%B5%AC%EC%B6%95/Minikube)
- [kubernetes resource - k8s resource에 관련한 가이드들](https://github.com/jjsair0412/kubernetes_info/tree/main/kubernetes%20resource)
    - [CRD 개발 관련 - cr , crd , k8s object , operator , controller , custom controller의 개념](https://github.com/jjsair0412/kubernetes_info/tree/main/kubernetes%20resource/CRD%20%EA%B0%9C%EB%B0%9C%20%EA%B4%80%EB%A0%A8%20-%20cr%20%2C%20crd%20%2C%20k8s%20object%20%2C%20operator)
        - [CRD 개발 - golang을 통한 CRD 개발기](https://github.com/jjsair0412/kubernetes_info/tree/main/kubernetes%20resource/CRD%20%EA%B0%9C%EB%B0%9C%20%EA%B4%80%EB%A0%A8%20-%20cr%20%2C%20crd%20%2C%20k8s%20object%20%2C%20operator/crd%20%EA%B0%9C%EB%B0%9C%20-%20golang)
            - [cr,crd yaml manifest - cr , crd yaml 파일](https://github.com/jjsair0412/kubernetes_info/tree/main/kubernetes%20resource/CRD%20%EA%B0%9C%EB%B0%9C%20%EA%B4%80%EB%A0%A8%20-%20cr%20%2C%20crd%20%2C%20k8s%20object%20%2C%20operator/crd%20%EA%B0%9C%EB%B0%9C%20-%20golang/cr%2Ccrd%20yaml%20manifest)
            - [custom controller golang code](https://github.com/jjsair0412/kubernetes_info/tree/main/kubernetes%20resource/CRD%20%EA%B0%9C%EB%B0%9C%20%EA%B4%80%EB%A0%A8%20-%20cr%20%2C%20crd%20%2C%20k8s%20object%20%2C%20operator/crd%20%EA%B0%9C%EB%B0%9C%20-%20golang/custom-operator-code)
    - [kubernetes Audit ( 감사 로그 ) - kube-api-server에서 로그 떨어트리기](https://github.com/jjsair0412/kubernetes_info/blob/main/kubernetes%20resource/kubernetes%20Audit%20(%20%EA%B0%90%EC%82%AC%20%EB%A1%9C%EA%B7%B8%20).md)
    - [RBAC - k8s command RBAC 설정 관련](https://github.com/jjsair0412/kubernetes_info/tree/main/kubernetes%20resource/RBAC)
        - [clusterrole & clusterrolebinding - all ns 접근 가능](https://github.com/jjsair0412/kubernetes_info/blob/main/kubernetes%20resource/RBAC/clusterrole%20%26%20clusterrolebinding%20-%20all%20ns%20%EC%A0%91%EA%B7%BC%20%EA%B0%80%EB%8A%A5.md)
        - [role & rolebinding - ns별 분리](https://github.com/jjsair0412/kubernetes_info/blob/main/kubernetes%20resource/RBAC/role%20%26%20rolebinding%20-%20ns%EB%B3%84%20%EB%B6%84%EB%A6%AC.md)
- [Multi-master-etcd_backup - multi master node 환경에서의 etcd backup restore 방안](https://github.com/jjsair0412/kubernetes_info/tree/main/Multi-master-etcd_backup)
    - [multi_master_etcd_backup_etcd.env일 경우](https://github.com/jjsair0412/kubernetes_info/blob/main/Multi-master-etcd_backup/multi_master_etcd_backup_etcd.env%EC%9D%BC%20%EA%B2%BD%EC%9A%B0.md)
    - [multi_master_etcd_backup_etcd.yaml일 경우](https://github.com/jjsair0412/kubernetes_info/blob/main/Multi-master-etcd_backup/multi_master_etcd_backup_etcd.yaml%EC%9D%BC%20%EA%B2%BD%EC%9A%B0.md)
- [rancher & rke & rke2 - suse rancher 관련](https://github.com/jjsair0412/kubernetes_info/tree/main/rancher%20%26%20rke%20%26%20rke2)
    - [install rancher - 각종 설치 방안](https://github.com/jjsair0412/kubernetes_info/tree/main/rancher%20%26%20rke%20%26%20rke2/install%20rancher)
        - [Rancher info - rancher 이론](https://github.com/jjsair0412/kubernetes_info/blob/main/rancher%20%26%20rke%20%26%20rke2/install%20rancher/Rancher%20info.md)
        - [offline 상태에서 Rancher RKE2 설치 - Tarball](https://github.com/jjsair0412/kubernetes_info/blob/main/rancher%20%26%20rke%20%26%20rke2/install%20rancher/offline%20%EC%83%81%ED%83%9C%EC%97%90%EC%84%9C%20Rancher%20RKE2%20%EC%84%A4%EC%B9%98%20-%20Tarball.md)
    - [rancher App backup & restore - 마켓플레이스에서 설치한 앱 backup & restore](https://github.com/jjsair0412/kubernetes_info/tree/main/rancher%20%26%20rke%20%26%20rke2/rancher%20App%20backup%20%26%20restore%20-%20%EB%A7%88%EC%BC%93%ED%94%8C%EB%A0%88%EC%9D%B4%EC%8A%A4%EC%97%90%EC%84%9C%20%EC%84%A4%EC%B9%98%ED%95%9C%20%EC%95%B1%20backup%20%26%20restore)
- [redis - redis 관련 정보](https://github.com/jjsair0412/kubernetes_info/tree/main/redis)
    - [redis command - redis command 정보](https://github.com/jjsair0412/kubernetes_info/blob/main/redis/redis%20command.md)
    - [redis info - redis 기본 개념 정리](https://github.com/jjsair0412/kubernetes_info/blob/main/redis/redis%20info.md)
- [study - 공부결과 정리](https://github.com/jjsair0412/kubernetes_info/tree/main/study)
    - [kubernetes_in_action - k8s study 결과 정리 ( 알게된 점들만 정리해둠 )](https://github.com/jjsair0412/kubernetes_info/tree/main/study/kubernetes_in_action)
        - [volume(pv,pvc,storageclass)](https://github.com/jjsair0412/kubernetes_info/tree/main/study/kubernetes_in_action/volume(pv%2Cpvc%2Cstorageclass))
            - [storageclass](https://github.com/jjsair0412/kubernetes_info/blob/main/study/kubernetes_in_action/volume(pv%2Cpvc%2Cstorageclass)/storageclass.md)
            - [volume_pv_pvc](https://github.com/jjsair0412/kubernetes_info/blob/main/study/kubernetes_in_action/volume(pv%2Cpvc%2Cstorageclass)/volume_pv_pvc.md)
        - [docker](https://github.com/jjsair0412/kubernetes_info/blob/main/study/kubernetes_in_action/docker.md)
        - [k8s resources - Deamonset + nodeSelector , Job ](https://github.com/jjsair0412/kubernetes_info/blob/main/study/kubernetes_in_action/k8s%20resoures.md)
        - [k8s service & dns - k8s network](https://github.com/jjsair0412/kubernetes_info/blob/main/study/kubernetes_in_action/k8s%20service%20%26%20dns%20-%20k8s%20network%20.md)
        - [MSA ?](https://github.com/jjsair0412/kubernetes_info/blob/main/study/kubernetes_in_action/MSA.md)
        - [pod 외부 노출 _nodeport](https://github.com/jjsair0412/kubernetes_info/blob/main/study/kubernetes_in_action/pod%20%EC%99%B8%EB%B6%80%20%EB%85%B8%EC%B6%9C%20_nodeport.md)
        - [Pod에 관해서 .. ](https://github.com/jjsair0412/kubernetes_info/blob/main/study/kubernetes_in_action/Pod%EC%97%90%20%EA%B4%80%ED%95%B4%EC%84%9C%20..%20.md)
        - [probe](https://github.com/jjsair0412/kubernetes_info/blob/main/study/kubernetes_in_action/probe.md)
- [Terraform](https://github.com/jjsair0412/kubernetes_info/tree/main/Terraform)
    - [Terraform info - terraform 기본적인 사용 방안](https://github.com/jjsair0412/kubernetes_info/blob/main/Terraform/Terraform%20info.md)

## storageClass local provisioner information
Local volumes do not currently support dynamic provisioning, however a StorageClass should still be created to delay volume binding until Pod scheduling.

동적 프로비저닝이 필요할 때에는 , storageclass를 local로 생성하는것 보다 nfs 등을 사용해서 storageclass를 사용하는 편이 편합니다.
local은 pv 동적 프로비저닝이 되지 않습니다.

[관련 문서](https://kubernetes.io/docs/concepts/storage/storage-classes/#local)

## known issues
### 1. kubeconfig file's location is not set in right direction.
The connection to the server localhost:8080 was refused - did you specify the right host or port?

- cp kube.config file into $HOME/.kube/config
- kubeconfig file is for each different which k8s provider systems.
    - exampe : location of rancher's kubeconfig file is /etc/rancher/rke2/rke2.yaml

```
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
```
### 2. helm install
```
curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
chmod 700 get_helm.sh
./get_helm.sh
```
### 3. install kubectl in linux
first , kubectl latest releases version download
```
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
```
install kubectl
```
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
```
If you don't have root permission, you can install kubectl in ~/.local/bin directory
```
chmod +x kubectl
mkdir -p ~/.local/bin
mv ./kubectl ~/.local/bin/kubectl
# 그리고 ~/.local/bin 을 $PATH의 앞부분 또는 뒷부분에 추가
```