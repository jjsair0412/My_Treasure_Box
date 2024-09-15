# Fargate 란 ?
## Overview
AWS Fargate는 컨테이너에 적합한 서버리스 컴퓨팅 엔진으로 Amazon Elastic Container Service(ECS) 및 Amazon Elastic Kubernetes Service(EKS)에서 모두 작동합니다. Fargate는 애플리케이션을 빌드하는 데 보다 쉽게 초점을 맞출 수 있도록 해줍니다. 

Fargate에서는 서버를 프로비저닝하고 관리할 필요가 없어 애플리케이션별로 리소스를 지정하고 관련 비용을 지불할 수 있으며, 계획적으로 애플리케이션을 격리함으로써 보안 성능을 향상시킬 수 있습니다.

## Fargate With EKS
EKS와 Fargate를 같이 사용하면, Farget에서 관리할 파드를 label로 지정해두고, 해당 label을 갖고있는 Kubernetes Resource가 배포되면, Fargate workernode가 프로비저닝 됩니다.