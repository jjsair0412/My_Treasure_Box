# haproxy - rke2 - ceph 구성
## basic Explanation
구성의 흐름은 다음과 같습니다.

1. route53을 통해 DNS 주소를 resolve 하여 haproxy 노드의 elasticIP를 반환
2. 이중화된 haproxy가 k8s master node로 LB 
- master node 3중화 필요. 다음 아키텍쳐 구성에서 해결 예정
3. k8s cluster로 접근
- k8s cluster는 rke2로 구성
4. k8s cluster는 backend의 ceph storage를 사용중

## 구성도
![aws_1][aws_1]

[aws_1]:./images/aws_1.PNG


