# jenkins with svn
## overview
SVN을 사용하는 환경에서 Jenkins Pipeline을 통해 Java App 배포

## ENV
- Source Repo : SVN
- Build Tool : Maven
- 배포 환경 : Container
- Jenkins 구축 : K8s 

## Requirements
- Image Tag 방식 : SVN Commit Hash
- Docker In Docker Image 생성 필요(Jenkins Agent Pod)
- 사전에 dind Image 생성 필요
- Tag 변경 이후 CI/CD 흐륾 구현 필요(ex. ArgoCD)
- Helm Chart 필요