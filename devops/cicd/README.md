# cicd
## introduce repositry
직접 제작한 jenkins pipeline을 모아둔 directory 입니다.

## 1. springBoot-gradle ( gradle ) pipeline
k8s 위에 설치된 jenkins에서 , **jenkins agent가 어떤 방식으로 동작하는지**와

**gradle 기반의 springboot 프로젝트**를 build하는 **jenkins pipeline**이 있고

**docker in docker** 문제를 어떻게 해결해야하는지 작성되어 있습니다.

또한 jenkins ci 스크립트에서 빌드된 docker image가 배포될 argocd helm chart도 포함되어 있습니다.

## 2. Dockerfile 최적화 관련
docker를 통해 cicd를 구축할 때, Dockerfile을 경량화하는 방안이나 잘 작성하는방안들을 정리해둔 문서입니다.

## 3. Jenkins와 ArgoCD를 이용한 GitOps
Java Application을 docker로 빌드하고, 빌드된 이미지를 helm chart로 배포하는 일련의 과정을 자동화하는 방안에 대해 기술한 문서입니다.
- [gitops](./pipeline/jenkins_argocd_cicdworkload/)