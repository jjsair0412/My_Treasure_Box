# EKS CloudFormation
## Overview
Study 목적으로 EKS Cluster 생성 시 자동화를 위해 만들어둔 CloudFormation Code

### Stack 생성 순서
#### Network 구성
0. VPC
1. Subnet 
2. IGW, RT
3. Control Plane, Worker Node SG

#### EKS Cluster 구성
0. Control Plane
1. Worker Node

### lambda 함수 코드
- chatOps를 위해 Slack과 AmazonQ와 연동 이후 Lambda로 CloudFormation 코드 실행
- [eks-oneclick-deploy.py](./eks-oneclick-deploy.py)

```bash
# EKS 생성 command -> Amazon Q로 Slack Request 송신
lambda invoke --function-name eks-oneclick-deploy --payload '{"NumWorkerNodes":"2"}, {"WorkerNodesInstanceType":"t2.small"},{"KubernetesVersion":"1.29"}' --region ap-northeast-2
```