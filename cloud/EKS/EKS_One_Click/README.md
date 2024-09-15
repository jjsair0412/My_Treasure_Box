# 학습용 EKS oneclick 배포 yaml
## overview
aws cloudformation yaml template을 통해 EKS를 배포시키는 yaml 입니다.
- [yaml template](./eks-oneclick.yaml)

## modify target
환경 별 대상 부분은 다음과 같습니다.

1. <<<<< **Deploy EC2** >>>>>
    1. **KeyName** : 작업용 bastion ec2에 SSH 접속을 위한 **SSH 키페어** 선택 *← 미리 SSH 키 생성 해두자!*
    2. **MyIamUserAccessKeyID** : **관리자** 수준의 권한을 가진 IAM User의 액세스 키ID 입력
    3. **MyIamUserSecretAccessKey** : **관리자** 수준의 권한을 가진 IAM User의 **시크릿 키ID** 입력 **← 노출되지 않게 보안 주의**
    4. **SgIngressSshCidr** : 작업용 bastion ec2에 **SSH 접속 가능한 IP** 입력 (**집 공인IP**/32 입력), 보안그룹 인바운드 규칙에 반영됨
    5. MyInstanceType: 작업용 bastion EC2 인스턴스의 타입 (기본 **t3.medium**) ⇒ 변경 가능
    6. LatestAmiId : 작업용 bastion EC2에 사용할 AMI는 아마존리눅스2 최신 버전 사용
2. <<<<< **EKS Config** >>>>>
    1. **ClusterBaseName** : EKS **클러스터 이름**
    2. **KubernetesVersion** : EKS 호환, 쿠버네티스 버전 (기본 **1.27** 버전 사용) ⇒ 변경 가능
    3. **WorkerNodeInstanceType**: 워커 노드 EC2 인스턴스의 타입 (기본 **t3.large**) ⇒ 변경 가능
    4. **WorkerNodeCount** : 워커노드의 갯수를 입력 (기본 **3대**) ⇒ 변경 가능
    5. **WorkerNodeVolumesize** : 워커노드의 EBS 볼륨 크기 (기본 **30GiB**) ⇒ 변경 가능
3. <<<<< **Region AZ** >>>>> : 리전과 가용영역을 지정, 기본값 : **ap-northeast-2**

## command
- 인스턴스 타입 변경
```bash
aws cloudformation deploy --template-file eks-oneclick.yaml --stack-name myeks --parameter-overrides KeyName=jinseongTest SgIngressSshCidr=$(curl -s ipinfo.io/ip)/32  MyIamUserAccessKeyID=AKIA5... MyIamUserSecretAccessKey='CVNa2...' ClusterBaseName=myeks --region ap-northeast-2
```

- 워커노드 인스턴스 타입 변경 : WorkerNodeInstanceType=t3.xlarge
```bash
aws cloudformation deploy --template-file eks-oneclick.yaml --stack-name myeks --parameter-overrides KeyName=jinseongTest SgIngressSshCidr=$(curl -s ipinfo.io/ip)/32  MyIamUserAccessKeyID=AKIA5... MyIamUserSecretAccessKey='CVNa2...' ClusterBaseName=myeks --region ap-northeast-2 WorkerNodeInstanceType=t3.xlarge 
```

## 결과 확인
- CloudFormation stack 배포 완료 후 작업용 bastion EC2 IP 출력
```bash
aws cloudformation describe-stacks --stack-name myeks --query 'Stacks[*].Outputs[0].OutputValue' --output text
```

- 작업용 bastion EC2 접근
```bash
ssh -i ~/.ssh/jinseongTest.pem ec2-user@$(aws cloudformation describe-stacks --stack-name myeks --query 'Stacks[*].Outputs[0].OutputValue' --output text)
```

## EKS 제거
- 작업용 bastion EC2에서 아래 명령어 기입
    - EKS가 제거될 때 까지 ssh 연결 유지 필요
```bash
eksctl delete cluster --name $CLUSTER_NAME && aws cloudformation delete-stack --stack-name $CLUSTER_NAME
```

## ETC
- 생성 실패시 cloudformation describe 하여 에러원인 파악 후 해결
```bash
# 전체 스택 이벤트 확인
aws cloudformation describe-stack-events --stack-name myeks

# 실패한 이벤트만 확인
aws cloudformation describe-stack-events --stack-name myeks --query 'StackEvents[?contains(ResourceStatus, `FAILED`)]'
```

- AWS Load Balancer Controller 설치
```bash
# 설치
helm repo add eks https://aws.github.io/eks-charts
helm repo update
helm install aws-load-balancer-controller eks/aws-load-balancer-controller -n kube-system --set clusterName=$CLUSTER_NAME \
  --set serviceAccount.create=false --set serviceAccount.name=aws-load-balancer-controller

# 설치 확인
kubectl get all -n kube-system aws-load-balancer-controller
```

- ExternalDNS 설치
>ExternalDNS란 Public한 도메인서버를 사용하여 쿠버네티스의 리소스를 쿼리할 수 있게 해주는 오픈소스 솔루션입니다. 
>
>도메인서버에 종속되지 않고 쿠버네티스 리소스를 통해 DNS레코드를 동적으로 관리할 수 있는 장점이 있습니다.

```bash
$ MyDomain=<자신의 도메인>
$ echo "export MyDomain=<자신의 도메인>" >> /etc/profile

# usecase
$ MyDomain=jinseong.link
$ echo "export MyDomain=jinseong.link" >> /etc/profile

$ MyDnsHostedZoneId=$(aws route53 list-hosted-zones-by-name --dns-name "${MyDomain}." --query "HostedZones[0].Id" --output text)

$ echo $MyDomain, $MyDnsHostedZoneId

$ curl -s -O https://raw.githubusercontent.com/cloudneta/cnaeblab/master/_data/externaldns.yaml

# envsubst 사용하여 ExternalDNS 컨트롤러 설치 
$ MyDomain=$MyDomain MyDnsHostedZoneId=$MyDnsHostedZoneId envsubst < externaldns.yaml | kubectl apply -f -
```
