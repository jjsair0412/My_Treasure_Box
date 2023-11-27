# My_Treasure_Box
![header](https://capsule-render.vercel.app/api?type=waving&color=auto&height=300&section=header&text=%F0%9F%91%8BJINSEONG%20Treasure%20Box%E2%9C%8D&fontSize=50&animation=fadeIn&fontAlignY=38)

## ***☆ If this document was helpful to you, please give it a star !! ★***

## TO - Do - List
1. [Terraform 모듈화 정리](./Terraform/Terraform_module.md)
2. [ElasticSearch Scroll API 계념정리 및 Java에서 사용하는 방안 정리](./DevOps_solutions/Elastic_모음/ElasticSearch/ElasticSearch_Scroll_API.md)
3. [DNS Server 구축하기](./study/network/DNS_Server_구축해보기.md)

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

### 4. bad interpreter error
kubectl exec 명령어 변경점
- -- 를 추가로 넣어줘야 먹힘
```bash
# before
kubectl exec -it my-pod /bin/bash

# after
kubectl exec -it my-pod -- /bin/bash
```

### 5. bad interpreter error
윈도우에서 수정한 파일을 리눅스에 옮겨서 작업할 경우 , 개행문자가 파일에 섞여들어가서 발생하는 에러

ide 툴에서 개행문자가 안들어가게끔 하는 설정을 해 두면 미리 방지 가능

해결 방안 :
관련 문서
- [블로그 : 보안](https://securus.tistory.com/entry/binbashM-bad-interpreter-%EA%B7%B8%EB%9F%B0-%ED%8C%8C%EC%9D%BC%EC%9D%B4%EB%82%98-%EB%94%94%EB%A0%89%ED%84%B0%EB%A6%AC%EA%B0%80-%EC%97%86%EC%8A%B5%EB%8B%88%EB%8B%A4)
- [블로그 : 공부를 계속하는 ...](https://haepyung88.tistory.com/213)

## ETC 
### 1. K8S TLS Secret 생성 및 교체
- tls 인증서 생성 command line
```bash
$ kubectl create secret tls ${tls-secret-name} --key ${tls-key} --cert ${tls-cert} -n ${namespace} --save-config
```

- tls 인증서 yaml template 생성 command line
```bash
$ kubectl create secret tls ${tls-secret-name} --key ${tls-key} --cert ${tls-cert} -n ${namespace} --dry-run=client -o yaml > secret.yaml
```

- tls 인증서 교체
```bash
$ kubectl create secret tls ${tls-secret-name} --key ${tls-key} --cert ${tls-cert} -n ${namespace} --dry-run=client -o yaml | kubectl apply -f -
```
