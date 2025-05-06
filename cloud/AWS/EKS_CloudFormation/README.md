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
2. 