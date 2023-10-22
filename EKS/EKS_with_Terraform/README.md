# Amazon EKS ( Elastic Kubernetes Service )

-   CloudNet@ 팀 gasida님의 DOIK(Database Operator In Kubernetes) 스터디를 진행하며 작성한 글 입니다.
- [EKS Terraform github](https://github.com/terraform-aws-modules/terraform-aws-eks#important-note)
- 해당 문서는 아래 링크의 문서를 보고 공부한내용을 정리하였습니다.
  - [링크](https://learnk8s.io/terraform-eks)
  - [terraform 코드](https://github.com/hashicorp/learn-terraform-provision-eks-cluster)

## TroubleShooting

EKS 생성후 만들어진 Kube-config가 작동하지 않고, 아래와같은 에러가 발생할 경우

```
You must be logged in to the server (Unauthorized)
```

아래 docs를 참고하여 해결

-   [How do I resolve the error "You must be logged in to the server (Unauthorized)" when I connect to the Amazon EKS API server?](https://repost.aws/knowledge-center/eks-api-server-unauthorized-error)

## Overview

EKS는 자체 Kubernetes control plane 노드를 설치 운영할 필요 없이, Kubernetes 실행에 사용할 수 있는 Amazon 관리형 Kubernetes 서비스 입니다.

AWS의 여러 가용영역에 걸쳐 Kubernetes control plane을 구성할 수 있으며, 비정상 인스턴스를 감지 및 교체하고 자동화된 버전업데이트 및 패치를 제공합니다.

AWS의 여러 서비스들과 통합하여 운영할 수 있습니다.

-   Amazon ECR
-   ELB
-   IAM
-   VPC  
    등

### EKS 지원 버전

4개의 마이너 버전을 지원 (2023.10.19 현재 버전으로 1.24 ~ 1.28) 하며, 평균 3개월마다 새로운 버전을 제공하고, 각 버전은 12개월간 지원합니다.

-   [관련 docs](https://docs.aws.amazon.com/eks/latest/userguide/kubernetes-versions.html)

### AWS EKS 배포시 알아야할점

-   Kubernetes API Server 및 etcd를 AWS에서 관리하게 됩니다.
-   3개의 AZ에서 Kubernetes control plane을 실행합니다.
-   클러스터에 노드를 추가하게 되면 , control plane이 스케일업됩니다.

EKS를 자동화 배포하기 위해 아래 방법들이 추천됩니다.

1.  AWS 웹 인터페이스
2.  eksctl
3.  Terraform

해당 문서에선 Terraform을 통해 EKS를 배포합니다.

## Deploy Amazon EKS with Terraform

Terraform을 통해 EKS를 배포합니다.

-   [Terraform 정리해둔 문서](https://github.com/jjsair0412/My_Treasure_Box/blob/main/Terraform/Terraform_basic_info.md)

먼저 Terraform이 정상적으로 설치되어있는지 확인합니다.

```
$ terraform version
Terraform v1.5.7
```

aws-cli 를 통해 aws 계정이 정상적으로 등록되어있는지를 확인합니다.

-   아래명령어를 호출하면, 현재 등록된 AWS 자격 증명과 관련된 ID값을 반환합니다.
-   반환된다면 정상처리된것.
    
    ```
    aws sts get-caller-identity
    ```
    

### 1 terraform 구성
- [terraform 전체 코드는 해당 URL에 있습니다.](https://github.com/hashicorp/learn-terraform-provision-eks-cluster)

main.tf파일을 정의합니다.
```
# Copyright (c) HashiCorp, Inc.
# SPDX-License-Identifier: MPL-2.0

provider "aws" {
  region = var.region
}

# Filter out local zones, which are not currently supported 
# with managed node groups
data "aws_availability_zones" "available" {
  filter {
    name   = "opt-in-status"
    values = ["opt-in-not-required"]
  }
}

locals {
  cluster_name = "education-eks-${random_string.suffix.result}"
}

resource "random_string" "suffix" {
  length  = 8
  special = false
}

module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "5.0.0"

  name = "education-vpc"

  cidr = "10.0.0.0/16"
  azs  = slice(data.aws_availability_zones.available.names, 0, 3)

  private_subnets = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
  public_subnets  = ["10.0.4.0/24", "10.0.5.0/24", "10.0.6.0/24"]

  enable_nat_gateway   = true
  single_nat_gateway   = true
  enable_dns_hostnames = true

  public_subnet_tags = {
    "kubernetes.io/cluster/${local.cluster_name}" = "shared"
    "kubernetes.io/role/elb"                      = 1
  }

  private_subnet_tags = {
    "kubernetes.io/cluster/${local.cluster_name}" = "shared"
    "kubernetes.io/role/internal-elb"             = 1
  }
}

module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "19.15.3"

  cluster_name    = local.cluster_name
  cluster_version = "1.27"

  vpc_id                         = module.vpc.vpc_id
  subnet_ids                     = module.vpc.private_subnets
  cluster_endpoint_public_access = true

  eks_managed_node_group_defaults = {
    ami_type = "AL2_x86_64"

  }

  eks_managed_node_groups = {
    one = {
      name = "node-group-1"

      instance_types = ["t3.small"]

      min_size     = 1
      max_size     = 3
      desired_size = 2
    }

    two = {
      name = "node-group-2"

      instance_types = ["t3.small"]

      min_size     = 1
      max_size     = 2
      desired_size = 1
    }
  }
}


# https://aws.amazon.com/blogs/containers/amazon-ebs-csi-driver-is-now-generally-available-in-amazon-eks-add-ons/ 
data "aws_iam_policy" "ebs_csi_policy" {
  arn = "arn:aws:iam::aws:policy/service-role/AmazonEBSCSIDriverPolicy"
}

module "irsa-ebs-csi" {
  source  = "terraform-aws-modules/iam/aws//modules/iam-assumable-role-with-oidc"
  version = "4.7.0"

  create_role                   = true
  role_name                     = "AmazonEKSTFEBSCSIRole-${module.eks.cluster_name}"
  provider_url                  = module.eks.oidc_provider
  role_policy_arns              = [data.aws_iam_policy.ebs_csi_policy.arn]
  oidc_fully_qualified_subjects = ["system:serviceaccount:kube-system:ebs-csi-controller-sa"]
}

resource "aws_eks_addon" "ebs-csi" {
  cluster_name             = module.eks.cluster_name
  addon_name               = "aws-ebs-csi-driver"
  addon_version            = "v1.20.0-eksbuild.1"
  service_account_role_arn = module.irsa-ebs-csi.iam_role_arn
  tags = {
    "eks_addon" = "ebs-csi"
    "terraform" = "true"
  }
}
```

#### main.tf 섹션별 주석

EKS cluster를 생성할 때 사용한 main.tf파일에 대한 설명입니다.

main.tf파일의 주요 부분은 모듈별로 확인할 수 있는데, 각각 아래와 같습니다.

1.  **module "vpc"**
2.  **module "eks"**

#### 1\. **module "vpc"**

다음을 aws에 생성하라고 명령합니다.

1.  VPC
2.  private subnet 3개 , public subnet 3개
3.  단일 NAT Gateway
4.  [EKS Cluster에서 사용하는 내부 LB 및 public NLB를 자동 프로비저닝하기 위한 subnet의 TAG](https://docs.aws.amazon.com/eks/latest/userguide/network-load-balancing.html)

#### 2\. **module "eks"**

다음을 aws에 생성하라고 명령합니다.

1.  control plane
2.  worker node
    -   worker node들은 EKS에서 private subnet에 생성되야하기 때문에 , `subnet_ids = module.vpc.private_subnets` 섹션을 통하여 VPC 모듈에서 생성한 private subnet을 할당합니다.
3.  security group 설정
4.  Auto Scalling group 설정

#### 3\. **모듈을 제외한 나머지**

다음을 설정합니다.

1.  cluster에 대한 올바른 IAM 권한설정
2.  cluster health check
3.  kubeconfig 파일 생성

### 2\. EKS 배포

Terraform init 명령어를 통해 .tfstate 설정파일을 생성해줍니다.

```
$ terraform init
```

terraform validate 명령어로 오류가없는지 검증합니다.

```bash
$ terraform validate
```

정상수행되면 아래와같은 Success 문구가 출력됩니다.

```bash
Success! The configuration is valid, but there were some validation warnings as shown above.
```

plan 명령을 실행하여, 어떤 인프라를 구축할지 예측 결과를 확인합니다.

```bash
terraform plan
```

plan 수행결과, 테스트가 완료되었다고 판단된다면 apply 명령어로 EKS Cluster를 배포합니다.

-   배포시간은 20분정도 소요됩니다.
-   `Apply complete! Resources: 49 added, 0 changed, 0 destroyed.` 메세지가 출력되면 성공
    
```
terraform apply
...
Apply complete! Resources: 49 added, 0 changed, 0 destroyed.
```
    

### 3. 배포결과 확인


생성한 EKS Cluster의 KubeConfig를 구성합니다.

```bash
aws eks --region $(terraform output -raw region) update-kubeconfig \
    --name $(terraform output -raw cluster_name)
```


노드 상태를 확인합니다.

```bash
kubectl get nodes
NAME                                            STATUS   ROLES    AGE   VERSION
ip-10-0-1-225.ap-northeast-2.compute.internal   Ready    <none>   39m   v1.27.5-eks-43840fb
ip-10-0-2-89.ap-northeast-2.compute.internal    Ready    <none>   39m   v1.27.5-eks-43840fb
ip-10-0-3-190.ap-northeast-2.compute.internal   Ready    <none>   39m   v1.27.5-eks-43840fb
```

## 테라폼을 통한 EKS 관리

Terraform은 Infrastructure as Code, 인프라를 코드로 관리할 수 있는 장점을 가졌기에, main.tf파일을 간단히 수정하고 plan과 apply 명령어를 통하여 EKS의 인프라 구성을 조작할 수 있습니다.

### 노드추가

main.tf파일의 node\_group 모듈에 `two`라는 워커노드를 추가해보겠습니다.

```
...
  eks_managed_node_groups = {
    one = {
      name = "node-group-1"

      instance_types = ["t3.small"]

      min_size     = 1
      max_size     = 3
      desired_size = 2
    }
    two = {
      name = "node-group-2"

      instance_types = ["t3.small"]

      min_size     = 1
      max_size     = 2
      desired_size = 1
    }
  }
...
```

요구사항에 맞게 해당 워커노드의 상세정보를 기입해 줍니다.

그리고 plan 명령어로 변경 사항을 미리 확인합니다.

```
terraform plan
```

확인이 완료되면 , apply로 실제 AWS에 반영합니다.

```
terraform apply
```

kubectl 명령어로 노드개수를 확인해봅니다. 워커노드 한대가 추가된것을 볼 수 있습니다.

```
kubectl get nodes --kubeconfig kubeconfig_basick8s 
NAME                                              STATUS   ROLES    AGE   VERSION
node-1                                            Ready    <none>   30m   v1.24.17-eks-43840fb
node-2                                            Ready    <none>   94s   v1.24.17-eks-43840fb
```

### Sample App과 AWS LoadBalancer 통합방안

먼저 deployment 한개를 배포하고 , port-forward 명령어로 로컬의 8888번 포트로 접근해보겠습니다.

-   **현재 [CNI](https://jjsair0412.tistory.com/2) 를 생성해주지 않았기 때문에, CIDR는 Node의 Private Subnet CIDR를 따라가게 됩니다.**
-   **따라서 Pod, svc 등 Kubernetes Resource는 , node의 private subnet CIDR 대역과 동일한 대역을 가지게됩니다.**

**_Deployment Sample_**

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: helloeks
spec:
  selector:
    matchLabels:
      name: hello-kubernetes
  template:
    metadata:
      labels:
        name: hello-kubernetes
    spec:
      containers:
        - name: app
          image: paulbouwer/hello-kubernetes:1.10.1
          ports:
            - containerPort: 8080
```

port-foward로 로컬의 8888로 오는 모든 트래픽을 , Kubernetes deployment의 8080 포트로 전달합니다.

```
kubectl port-forward <helloeks-podname> 8080:8080
```

- 결과  

![firstResult](./images/first_result.png)
    

그러나 파드는 지속적으로 고정된 IP를 가지지 않으며, 또한 제거되거나 다시 생성되면서 이름이 계속해서 변화하기때문에, Kubernetes의 Service로 Deployment를 관리해주어야 합니다.

Service 중 , LoadBalancer type의 Service를 배포합니다.

-   label이 name: helloeks 인 deployment를 대상으로 , 8080번 포트와 80 포트를 포트포워딩해줍니다.

**_Service Sample_**

```
apiVersion: v1
kind: Service
metadata:
  name: hello-kubernetes
spec:
  type: LoadBalancer
  ports:
    - port: 80
      targetPort: 8080
  selector:
    name: hello-kubernetes
```

위의 service를 배포하면 , AWS는 Classic LoadBalancer를 프로비저닝하게 됩니다.

-   생성된 LB의 Endpoint:80 에 접근하여 배포한 hello-kubernetes web에 접근할 수 있습니다.

get svc 결과 :

```
kubectl get svc
NAME               TYPE           CLUSTER-IP     EXTERNAL-IP               PORT(S)        AGE
hello-kubernetes   LoadBalancer   10.100.9.129   {Loadbalancer-Dns-Name}  80:32317/TCP   5m1s
...
```

describe 결과 :

```
kubectl describe svc 
Name:                     hello-kubernetes
Namespace:                default
Labels:                   <none>
Annotations:              <none>
Selector:                 name=helloeks
Type:                     LoadBalancer
IP Family Policy:         SingleStack
IP Families:              IPv4
IP:                       10.100.9.129
IPs:                      10.100.9.129
LoadBalancer Ingress:     {Loadbalancer-Dns-Name}
Port:                     <unset>  80/TCP
TargetPort:               8080/TCP
NodePort:                 <unset>  32317/TCP
Endpoints:                172.16.3.68:8080
Session Affinity:         None
External Traffic Policy:  Cluster
Events:
  Type    Reason                Age   From                Message
  ----    ------                ----  ----                -------
  Normal  EnsuringLoadBalancer  74s   service-controller  Ensuring load balancer
  Normal  EnsuredLoadBalancer   71s   service-controller  Ensured load balancer
```

그러나 이러한방법 **(ServiceType : LoadBalancer)** 엔 치명적인 단점이 존재하는데,

**_한번에 하나의 서비스에 하나의 CLB가 생성된다는점 입니다._**

-   Kubernetes Cluster에 외부로 노출해야하는 Service가 무수히 많아진다면, 그만큼 LB도 같이생성된다는의미..

따라서 Serivce의 type을 ClusterIP로 두어 파드의 단일진입점과 고정된 IP를 가지게끔 구성하고, 앞단에 Ingress를 사용하여 하나의 LoadBalacner로 Kubernetes Cluster에 배포된 Application에 접근합니다.

-   Ingress Controller의 종류는 많습니다. nginx, kong 등.. 각 Ingress의 특성에 따라 상황에 맞게 선택하여 사용합니다.

### Application LoadBalacner를 Ingress로 사용하기

기존 AWS에서 ALB를 생성했듯이, Listeners, TargetGroups 등을 설정하여 포워딩하는 대신에 , **_ALB Ingress Controller_** 를 EKS Cluster에 helm으로 설치하여 사용할 수 있습니다.

Kubernetes에서 Ingress manifest Yaml Kubectl로 apiserver에 요청을 보내어 정의하면, Ingress Controller는 ALB가 알 수 있는 형태( Listeners, TargetGroup 등) 으로 변환해 줍니다.

#### worker node IAM 권한변경

ALB를 EKS Cluster에서 사용하기위해, 먼저 각 worker Node의 권한을 변경해야 합니다.

그러한 이유는 , **Ingress Controller는 배포될 때 Pod로 배포되게 되는데 , 그 Pod가 EKS Cluster 어디에 배포될지 모르기 때문에, AWS ALB를 각 worker node가 관리하려면 worker node instance가 AWS ALB를 수정 및 조회할 수 있는 권한이 필요하기 때문입니다.**

-   main.tf파일에 아래와 같은 resource를 추가합니다.
-   추가로 정책이 들어가있는 iam-policy.json 파일을 main.tf와 동일경로에 생성합니다.
    -   [전체코드 및 iam-policy.json 파일위치](https://github.com/jjsair0412/My_Treasure_Box/tree/main/EKS/EKS_with_Terraform)
        
```tf
resource "aws_iam_policy" "worker_policy" {
  name        = "worker-policy"
  description = "Worker policy for the ALB Ingress"

  policy = file("iam-policy.json")
}

resource "aws_iam_role_policy_attachment" "additional" {
  for_each = module.eks.eks_managed_node_groups

  policy_arn = aws_iam_policy.worker_policy.arn
  role       = each.value.iam_role_name
}
```

IAM 권한을 추가하여 인프라에 변경사항이 있기 때문에, plan으로 문제가 없는지, 어떤 리소스가 추가되는지 확인합니다.
```bash
$ terraform plan
````

변경사항이 문제가 없다면, apply로 반영합니다.

```
$ terraform apply
...
Apply complete! Resources: 4 added, 0 changed, 1 destroyed.
```

### eks/aws-load-balancer-controller 설치

helm chart로 배포합니다.

-   helm에 대한 설명은 아래 링크에 기입해두었습니다.
-   [helm chart란?](https://github.com/jjsair0412/My_Treasure_Box/blob/main/DevOps_solutions/helm%20info.md)
- eks/aws-load-balancer-controller chart를 사용합니다.

```bash
$ helm repo add eks https://aws.github.io/eks-charts

$ helm repo update

# cluster 이름을 정확히 기입해야합니다.
# vpc 이름또한 정확히 기입해야합니다.
$ helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  --set region=ap-northeast-2 \
  --set vpcId=vpc-0ee50c3647e1ad27f \
  --set clusterName=education-eks # EKS clusterName
```

배포된 ALB ingress controller의 pod 상태 확인합니다.

```
$ kubectl get pods -l "app.kubernetes.io/name=aws-load-balancer-controller"
NAME                                            READY   STATUS    RESTARTS   AGE
aws-load-balancer-controller-54d848c6cd-jlw44   1/1     Running   0          12s
aws-load-balancer-controller-54d848c6cd-mbrf5   1/1     Running   0          12s
```

### Ingress 배포

생성한 Ingress를 사용하기위해, Ingress Manifest로 Ingress를 생성합니다.

```
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: hello-kubernetes
  annotations:
    alb.ingress.kubernetes.io/scheme: internet-facing # 내부 또는 public LB를 사용하도록 구성
    kubernetes.io/ingress.class: alb # alb Ingress Controller를 사용
spec:
  rules:
  - http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: hello-kubernetes
            port:
              number: 80
```

### 결과확인
ALB loadbalancer가 프로비저닝된것을 확인할 수 있습니다.
```bash
kubectl describe ing
Name:             hello-kubernetes
Labels:           <none>
Namespace:        default
Address:          {alb_dns_name}
Ingress Class:    <none>
Default backend:  <default>
Rules:
  Host        Path  Backends
  ----        ----  --------
  *           
              /   hello-kubernetes:80 (<error: endpoints "hello-kubernetes" not found>)
Annotations:  alb.ingress.kubernetes.io/scheme: internet-facing
              kubernetes.io/ingress.class: alb
Events:
  Type    Reason                  Age                From     Message
  ----    ------                  ----               ----     -------
  Normal  SuccessfullyReconciled  10s (x2 over 65s)  ingress  Successfully reconciled
```

## Terraform으로 EKS를 프로비저닝하는 동시에 , ALB를 같이 설치하는 방안
EKS를 프로비저닝하면서 , ALB도 같이 설치하는 방안이 있습니다.

### 사전 작업
이미 aws-load-balancer-controller가 설치되어 있다면, provider로 설치하려 할 때 아래와 같은 에러가발생하기 때문에, 지우고 진행
```bash
...
│ Error: rendered manifests contain a resource that already exists. Unable to continue with install: Secret "aws-load-balancer-tls" in namespace "default" exists and cannot be imported into the current release: invalid ownership metadata; annotation validation error: key "meta.helm.sh/release-name" must equal "ingress": current value is "aws-load-balancer-controller"
│ 
│   with helm_release.ingress,
│   on main.tf line 152, in resource "helm_release" "ingress":
│  152: resource "helm_release" "ingress" {
│ 
```
제거
```
helm uninstall aws-load-balancer-controller
```

**Terraform의 provider중 Helm provider를 사용하면, Terraform으로 EKS가 프로비저닝되는 동시에, aws-load-balancer-controller chart도 같이 배포시킬 수 있습니다.**
- 필요한 리소스들을 모두 terraform main.tf에 미리 정의해두고, 필요할떄마다 꺼내다쓸수잇습니다.

### main.tf 수정
main.tf에, Helm provider를 추가합니다.

```
provider "helm" {
  kubernetes {
    host                   = data.aws_eks_cluster.cluster.endpoint
    cluster_ca_certificate = base64decode(data.aws_eks_cluster.cluster.certificate_authority.0.data)
    token                  = data.aws_eks_cluster_auth.cluster.token
    }
}

resource "helm_release" "ingress" {
  name       = "ingress"
  chart      = "aws-load-balancer-controller"
  repository = "https://aws.github.io/eks-charts"
  version    = "1.6.1"

  set {
    name  = "region"
    value = var.region
  }
  set {
    name  = "vpcId"
    value = eks.vpc_id
  }
  set {
    name  = "clusterName"
    value = local.cluster_name
  }
}
```

provider가 추가되었기 때문에, Helm provider를 초기화한 뒤 plan으로 확인합니다.
```bash
terraform init
terraform plan
```

plan 시 문제가 없다면, apply로 진행합니다.
```bash
terraform apply
```

EKS가 프로비저닝됨과 동시에 helm chart로 aws-load-balancer-controller 가 같이 설치된것을 확인할 수 있습니다.

```bash
# helm list -A
NAME    NAMESPACE       REVISION        UPDATED                                 STATUS          CHART                                   APP VERSION
ingress default         1               2023-10-21 00:04:24.98008 +0900 KST     deployed        aws-load-balancer-controller-1.6.1      v2.6.1     

# kubectl get pods
NAME                                                   READY   STATUS    RESTARTS   AGE
helloeks-7d9768bb49-tmsgv                              1/1     Running   0          21m
ingress-aws-load-balancer-controller-9b6b5c567-2h2hm   1/1     Running   0          54s
ingress-aws-load-balancer-controller-9b6b5c567-cxflq   1/1     Running   0          54s
```

## terraform destroy
```bash
terraform destroy
```
destroy 명령어로 테라폼을 통해 프로비저닝한 aws 리소스를 제거합니다.