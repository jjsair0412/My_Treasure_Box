# Chaos Engineering With EKS
## OverView
EKS Cluster에 배포된 Pod들을 대상으로 Chaos Test를 진행하면서, chaos test의 전반적인 이론과 더불어 EKS , karpenter 에 대해 학습합니다.

작성 시 테스트에 참고한 공식문서는 다음과 같음
- [Amazon EKS 워크로드의 지속적인 복원력 확인을 위한 카오스 엔지니어링 (Chaos Engineering)](https://aws.amazon.com/ko/blogs/tech/continuous-resilience-testing-for-amazon-eks-workloads-with-chaos-engineering/)

## 이론
카오스 엔지니어링(Chaos Engineering)은 테스트 방법론 중 하나입니다.

실제 운영환경에서 마주칠 수 있는 다양한 장애 상황을 견딜 수 있는 견고한 시스템을 구축하기 위해 시스템의 신뢰성을 검증하는 방법입니다.

분산 시스템(ex. MSA) 환경에서, 각 서비스들이 정상 작동하고 있음에도 불구하고 서비스 간 상호작용(통신 과정 등)에 문제가 발생하여 예기치 못한 결과가 발생할 수 있습니다. 이러한 상황은 시스템 전체에 걸쳐 장애 상황을 야기할 수 있으며, 이를 방지해야 합니다.

카오스 엔지니어링은 이러한 상황을 고객 경험에 문제를 일으키기 전에 발견하여, 시스템의 약점을 도출해내는 방법입니다.

### 1. 카오스 엔지니어링 프로세스
카오스 엔지니어링은 다음과 같은 프로세스로 진행합니다.
- 위 출처를 둔 공식문서 발췌

![01-chaos-engineering-procedure][01-chaos-engineering-procedure]

[01-chaos-engineering-procedure]:../images/01-chaos-engineering-procedure.png

#### 1. 안정 상태(정상 상태) 정의
    
    대상 서비스에 대해 특정 기준을 정의해서 해당 기준의 임계치를 넘어가면 잘 운영되고 있다고 판단합니다. 이때 기준치들은 측정 가능해야 하며, ***측정 기준치를 정상 상태라 합니다.***

    예를들어 처리량 , 에러율 , 지연 시간 , 가용성 , 응답 시간 등 이 기준이 될 수 있습니다.

#### 2. 가설 수립

    가설 수립 단계에서는 기본적으로 시스템이 안정 상태를 유지 할 것이라는 가설을 세웁니다. 
    
    아키텍처가 고가용성을 지원하도록 설계했는데, 기대한대로 동작하는 지 검증해야 하기 때문입니다. 필요에 따라서는 시스템이 예상한 방식대로 예외를 발생시키는 지 확인하는 가설을 세울 수도 있습니다. 
    
    단위 테스트를 할 때, 예외가 발생하면 테스트가 실패하는 것 뿐아니라 기대한 예외가 발생하는 테스트를 하는 것과 비슷합니다.

#### 3. 실험

    설정한 가설을 검증하기 위해 , 실제로 실험을 수행합니다.

    예를들어 "네트워크가 단절되더라도 안정 상태를 유지할것이다" 라는 가설을 세웠다면, 실제로 네트워크를 차단하는 실험을 진행합니다.

#### 4. 검증

    수행한 실험 결과를 분석합니다.

    그에 따른 시스템의 전반적인 약점을 파악하고, 개선방안을 제시합니다.

#### 5. 개선

    분석한 결과 도출된 개선방안을 토대로 시스템을 개선합니다.

카오스 엔지니어링은 위와같은 5가지 프로세스로 진행되며 , 시스템이 안정화될 때 까지 진행합니다.

### 2. 카오스 엔지니어링 대상과 공통 실패 원인
카오스 엔지니어링의 대상은 인프라 계층 혹은 서비스의 핵심 응용 프로그램이 될 가능성이 높습니다.

그러한 이유는 이 두 대상에서 장애가 발생했을 경우 파급 효과가 연쇄적인부분일 확률이 높고, 카오스 엔지니어링을 통해 신뢰성을 확보하기에 좋기 때문입니다.

또한 카오스 엔지니어링의 실패 원인은 다양하겠지만 , 예시로는 다음과 같습니다.
- EC2 인스턴스가 갑자기 중지됨
- 예기치 않은 CPU 또는 메모리 스파이크
- 알려지지 않은 네트워크 지연
- ECS 컨테이너가 등록 취소됨 (ECS 일 경우)
- 쿠버네티스 노드가 삭제됨

## 카오스 엔지니어링 실습
EKS cluster에 대한 카오스 엔지니어링 테스트는 , [chaos_toolkit](https://chaostoolkit.org/reference/usage/install/) 을 사용합니다.

chaos toolkit은 python으로 작동하며, 카오스 엔지니어링을 코드로 수행할 수 있다는 장정믈 가집니다.

### 0. PreRequired
EKS 관련 생성권한이 있는 Iam User로 aws configure를 세팅합니다.

### 1. EKS Cluster 생성
eksctl 명령어로 EKS Cluster를 생성합니다.
- EKS 관련 설명은 생략

eksctl 명령어를 사용하며, 테스트 목적이기 때문에 [해당 가이드](https://docs.aws.amazon.com/eks/latest/userguide/getting-started-eksctl.html) 를 통해 프로비저닝된 eks로 테스트합니다.

```eks.yml``` 파일은 같은 레포지토리에 있는 ```eks.yml``` 파일을 사용합니다.
- [eks.yml](.//eks.yml)

```bash
# eksctl 사용 사례
eksctl create cluster --name my-cluster --region region-code --fargate

# 실제 사용 명령여
eksctl create cluster -f eks.yml
#### .... eks 프로비저닝 .... ####
```

생성 이후 확인합니다.

```bash
$ kubectl get nodes
NAME                                               STATUS   ROLES    AGE     VERSION
ip-172-31-56-188.ap-northeast-2.compute.internal   Ready    <none>   5m12s   v1.29.3-eks-ae9a62a
```

### 2. chaos toolkit repository 작업
