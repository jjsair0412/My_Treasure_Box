# Kubesphere 배포 방안 - air gap 
## 1. Prerequisites
- 해당 문서는 kubekey를 기반으로 kubesphere를 배포하는 방안에 대해 기술합니다.
air gap 방식으로 설치하며 , 해당 예제에서는 custom한 ks-console 이미지를 사용합니다.  

- 또한 쿠버네티스가 설치되어있지 않은 환경에서 테스트 하였습니다.

- air gap 설치 방안은 두가지가 있습니다.
	- 1. manifest.yaml 파일로 image.tar파일을 만들어 설치하는 방법
	- 2. image-list로 모든 image를 private-registry로 push해서 설치하는 방법
		- 해당 문서는 1 번의 방법을 따릅니다.
- 전체 설치 방안은 아래의 공식 문서를 참조하였습니다. 
 [설치 방안 공식 문서](https://kubesphere.io/docs/v3.3/installing-on-linux/introduction/air-gapped-installation/)
## 2. System Requirements
- 시스템 환경 요구사항 정보는 아래의 링크에서 확인하실 수 있습니다.
  [시스템 환경 요구사항 정보](https://github.com/jjsair0412/kubernetes_info/blob/main/kubernetes%20%EA%B5%AC%EC%B6%95/KubeKey%EB%A5%BC%20%ED%86%B5%ED%95%9C%20kubesphere%20%EB%B0%B0%ED%8F%AC%20%EB%B0%A9%EC%95%88/Kubesphere%20%EB%B0%B0%ED%8F%AC%20%EB%B0%A9%EC%95%88.md#2--system-requirements)
## 3. Install KubeKey
### 3.1 kk 파일 생성
- 먼저 kubekey를 설치하기 위해 kk파일을 생성합니다.
  인터넷 통신이 되는 환경에서 작업합니다.
- 아래 명령어를 작성하여 kk 바이너리 파일을 생성합니다.
  설치 대상 kubekey version은 2.2.2 이며 , version 정보는 아래 링크에서 확인할 수 있습니다.
  [kubekey version 정보](https://github.com/kubesphere/kubekey/tags)
```
curl -sfL https://get-kk.kubesphere.io | VERSION=v2.2.2 sh -
```
### 3.2 manifest 파일 생성 및 구성
- source cluster 에서 kubekey를 사용하여 manifest 파일을 꺼내옵니다.
```
./kk create manifest
```
- manifest 파일을 구성합니다.
  해당 파일에서는 kubekey 및 kubespherer 설치에 필요한 모든 이미지 정보와 components ( etcd , helm , cni ... ) 들의 버전 정보를 기입합니다.
- 예시에서는 이미지 정보를 변경하지 않고 , private registry인 harbor에 올라가있는 custom ks-console image만 사용합니다.
- custom image를 사용하기 위해선 , tag와 image 이름이 default값과 동일해야 합니다. 만약 상이하게 구성하고 싶다면 kubekey의 소스코드를 수정해야 합니다. ( golang error 발생함 )
- 또한 private registry harbor에 login 해야 하기 때문에 , auths 정보를 추가해줍니다. 
  harbor에 tls 구성되어있기 때문에 insecure 설정은 해주지 않습니다.
- manifest  file 구성은 아래 링크를 보고 작성합니다.
  [manifest.yaml example](https://github.com/kubesphere/kubekey/blob/master/docs/manifest-example.md#the-manifest-definition)
- 또한 해당 예제에서는 docker pull limit 정책을 회피하기 위해 , harbor의 proxy project를 통해 구성하였습니다.
  custom image는 개인 docker hub에 push 시킨 뒤 작업하였습니다.
```
vi manifest-sample.yaml
---
apiVersion: kubekey.kubesphere.io/v1alpha2
kind: Manifest
metadata:
  name: sample
spec:
  arches:
  - amd64
  operatingSystems:
  - arch: amd64
    type: linux
    id: centos
    version: "7"
    repository:
      iso:
        localPath:
        url: https://github.com/kubesphere/kubekey/releases/download/v2.2.2/centos7-rpms-amd64.iso
  - arch: amd64
    type: linux
    id: ubuntu
    version: "22.04"
    repository:
      iso:
        localPath:
        url: https://github.com/kubesphere/kubekey/releases/download/v2.2.2/ubuntu-22.04-debs-amd64.iso
  kubernetesDistributions:
  - type: kubernetes
    version: v1.22.10
  components:
    helm:
      version: v3.6.3
    cni:
      version: v0.9.1
    etcd:
      version: v3.4.13
   ## For now, if your cluster container runtime is containerd, KubeKey will add a docker 20.10.8 container runtime in the below list.
   ## The reason is KubeKey creates a cluster with containerd by installing a docker first and making kubelet connect the socket file of containerd which docker contained.
    containerRuntimes:
    - type: docker
      version: 20.10.8
    crictl:
      version: v1.22.0
    docker-registry:
      version: "2"
    harbor:
      version: v2.4.1
    docker-compose:
      version: v2.2.2
  images:
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/kube-apiserver:v1.22.10
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/kube-controller-manager:v1.22.10
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/kube-proxy:v1.22.10
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/kube-scheduler:v1.22.10
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/pause:3.5
  - harbor.xxx.co.kr/kubesphere_image/coredns/coredns:1.8.0
  - harbor.xxx.co.kr/kubesphere_image/calico/cni:v3.23.2
  - harbor.xxx.co.kr/kubesphere_image/calico/kube-controllers:v3.23.2
  - harbor.xxx.co.kr/kubesphere_image/calico/node:v3.23.2
  - harbor.xxx.co.kr/kubesphere_image/calico/pod2daemon-flexvol:v3.23.2
  - harbor.xxx.co.kr/kubesphere_image/calico/typha:v3.23.2
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/flannel:v0.12.0
  - harbor.xxx.co.kr/kubesphere_image/openebs/provisioner-localpv:3.3.0
  - harbor.xxx.co.kr/kubesphere_image/openebs/linux-utils:3.3.0
  - harbor.xxx.co.kr/kubesphere_image/library/haproxy:2.3
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/nfs-subdir-external-provisioner:v4.0.2
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/k8s-dns-node-cache:1.15.12
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/ks-installer:v3.3.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/ks-apiserver:v3.3.0
  - harbor.xxx.co.kr/kubesphere_image/jjsair0412/ks-console:latest
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/ks-controller-manager:v3.3.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/kubectl:v1.20.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/kubectl:v1.21.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/kubectl:v1.22.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/kubefed:v0.8.1
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/tower:v0.2.0
  - harbor.xxx.co.kr/kubesphere_image/minio/minio:RELEASE.2019-08-07T01-59-21Z
  - harbor.xxx.co.kr/kubesphere_image/minio/mc:RELEASE.2019-08-07T23-14-43Z
  - harbor.xxx.co.kr/kubesphere_image/csiplugin/snapshot-controller:v4.0.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/nginx-ingress-controller:v1.1.0
  - harbor.xxx.co.kr/kubesphere_image/mirrorgooglecontainers/defaultbackend-amd64:1.4
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/metrics-server:v0.4.2
  - harbor.xxx.co.kr/kubesphere_image/library/redis:5.0.14-alpine
  - harbor.xxx.co.kr/kubesphere_image/library/haproxy:2.0.25-alpine
  - harbor.xxx.co.kr/kubesphere_image/library/alpine:3.14
  - harbor.xxx.co.kr/kubesphere_image/osixia/openldap:1.3.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/netshoot:v1.0
  - harbor.xxx.co.kr/kubesphere_image/kubeedge/cloudcore:v1.9.2
  - harbor.xxx.co.kr/kubesphere_image/kubeedge/iptables-manager:v1.9.2
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/edgeservice:v0.2.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/openpitrix-jobs:v3.2.1
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/devops-apiserver:v3.3.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/devops-controller:v3.3.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/devops-tools:v3.3.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/ks-jenkins:v3.3.0-2.319.1
  - harbor.xxx.co.kr/kubesphere_image/jenkins/inbound-agent:4.10-2
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/builder-base:v3.2.2
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/builder-nodejs:v3.2.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/builder-maven:v3.2.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/builder-maven:v3.2.1-jdk11
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/builder-python:v3.2.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/builder-go:v3.2.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/builder-go:v3.2.2-1.16
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/builder-go:v3.2.2-1.17
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/builder-go:v3.2.2-1.18
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/builder-base:v3.2.2-podman
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/builder-nodejs:v3.2.0-podman
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/builder-maven:v3.2.0-podman
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/builder-maven:v3.2.1-jdk11-podman
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/builder-python:v3.2.0-podman
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/builder-go:v3.2.0-podman
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/builder-go:v3.2.2-1.16-podman
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/builder-go:v3.2.2-1.17-podman
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/builder-go:v3.2.2-1.18-podman
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/s2ioperator:v3.2.1
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/s2irun:v3.2.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/s2i-binary:v3.2.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/tomcat85-java11-centos7:v3.2.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/tomcat85-java11-runtime:v3.2.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/tomcat85-java8-centos7:v3.2.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/tomcat85-java8-runtime:v3.2.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/java-11-centos7:v3.2.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/java-8-centos7:v3.2.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/java-8-runtime:v3.2.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/java-11-runtime:v3.2.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/nodejs-8-centos7:v3.2.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/nodejs-6-centos7:v3.2.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/nodejs-4-centos7:v3.2.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/python-36-centos7:v3.2.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/python-35-centos7:v3.2.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/python-34-centos7:v3.2.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/python-27-centos7:v3.2.0
  - quay.io/argoproj/argocd:v2.3.3
  - quay.io/argoproj/argocd-applicationset:v0.4.1
  - ghcr.io/dexidp/dex:v2.30.2
  - harbor.xxx.co.kr/kubesphere_image/library/redis:6.2.6-alpine
  - harbor.xxx.co.kr/kubesphere_image/jimmidyson/configmap-reload:v0.5.0
  - harbor.xxx.co.kr/kubesphere_image/prom/prometheus:v2.34.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/prometheus-config-reloader:v0.55.1
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/prometheus-operator:v0.55.1
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/kube-rbac-proxy:v0.11.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/kube-state-metrics:v2.3.0
  - harbor.xxx.co.kr/kubesphere_image/prom/node-exporter:v1.3.1
  - harbor.xxx.co.kr/kubesphere_image/prom/alertmanager:v0.23.0
  - harbor.xxx.co.kr/kubesphere_image/thanosio/thanos:v0.25.2
  - harbor.xxx.co.kr/kubesphere_image/grafana/grafana:8.3.3
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/kube-rbac-proxy:v0.8.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/notification-manager-operator:v1.4.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/notification-manager:v1.4.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/notification-tenant-sidecar:v3.2.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/elasticsearch-curator:v5.7.6
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/elasticsearch-oss:6.8.22
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/fluentbit-operator:v0.13.0
  - harbor.xxx.co.kr/kubesphere_image/library/docker:19.03
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/fluent-bit:v1.8.11
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/log-sidecar-injector:1.1
  - harbor.xxx.co.kr/kubesphere_image/elastic/filebeat:6.7.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/kube-events-operator:v0.4.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/kube-events-exporter:v0.4.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/kube-events-ruler:v0.4.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/kube-auditing-operator:v0.2.0
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/kube-auditing-webhook:v0.2.0
  - harbor.xxx.co.kr/kubesphere_image/istio/pilot:1.11.1
  - harbor.xxx.co.kr/kubesphere_image/istio/proxyv2:1.11.1
  - harbor.xxx.co.kr/kubesphere_image/jaegertracing/jaeger-operator:1.27
  - harbor.xxx.co.kr/kubesphere_image/jaegertracing/jaeger-agent:1.27
  - harbor.xxx.co.kr/kubesphere_image/jaegertracing/jaeger-collector:1.27
  - harbor.xxx.co.kr/kubesphere_image/jaegertracing/jaeger-query:1.27
  - harbor.xxx.co.kr/kubesphere_image/jaegertracing/jaeger-es-index-cleaner:1.27
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/kiali-operator:v1.38.1
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/kiali:v1.38
  - harbor.xxx.co.kr/kubesphere_image/library/busybox:1.31.1
  - harbor.xxx.co.kr/kubesphere_image/library/nginx:1.14-alpine
  - harbor.xxx.co.kr/kubesphere_image/joosthofman/wget:1.0
  - harbor.xxx.co.kr/kubesphere_image/nginxdemos/hello:plain-text
  - harbor.xxx.co.kr/kubesphere_image/library/wordpress:4.8-apache
  - harbor.xxx.co.kr/kubesphere_image/mirrorgooglecontainers/hpa-example:latest
  - harbor.xxx.co.kr/kubesphere_image/library/openjdk:8-jre-alpine
  - harbor.xxx.co.kr/kubesphere_image/fluent/fluentd:v1.4.2-2.0
  - harbor.xxx.co.kr/kubesphere_image/library/perl:latest
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/examples-bookinfo-productpage-v1:1.16.2
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/examples-bookinfo-reviews-v1:1.16.2
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/examples-bookinfo-reviews-v2:1.16.2
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/examples-bookinfo-details-v1:1.16.2
  - harbor.xxx.co.kr/kubesphere_image/kubesphere/examples-bookinfo-ratings-v1:1.16.3
  - harbor.xxx.co.kr/kubesphere_image/weaveworks/scope:1.13.0
  registry:
    auths:
      "harbor.xxx.co.kr":
        username: "admin"
        password: "P@88w0rd"

```
### 3.3 tar 파일 생성
- 아래 명령어를 통해 만들어둔 manifest.yaml 파일로  image.tar파일을 생성합니다.
```
./kk artifact export -m manifest-sample.yaml -o kubesphere.tar.gz
```
