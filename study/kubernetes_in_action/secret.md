# secret ?
보안이 유지되어야 하는 정보를 저장하는 리소스인 secret

## 0. secret 
secret은 기본적으로 configmap과 동일한 구조를 갖고 있기 때문에 , 명령어도 cm -> secret으로만 변경 시켜 주면 생성 , 조회 , 삭제 , 변경이 가능합니다.

secret의 최대 크기는 1MB로 제한됩니다.

## 1. k8s에서 secret의 보안
- k8s에서 secret은 메모리에만 저장시킵니다.
    - 물리 저장소에 저장되면 , 디스크를 완전삭제하는 과정이 필요해서
- k8s에서 secret에 접근해야 하는 파드가 실행되는 노드에서만 secret을 배포시킵니다.
- k8s에서 secret은 key-value 구조를 갖고 있는데 ,이때 value는 Base64 인코딩 문자열로 표시됩니다.

## 2. default token secret
k8s의 모든 pod에는 secret volume이 자동 연결 되어 있습니다.

```bash
$ kubectl get secret
NAME                  Type ...
default-token-cfea1   Kubernetes.io/service-account-type ...
```

얘는 3가지 데이터를 갖고 있습니다.
1. ca.crt
2. namespace
3. token

이 친구들은 k8s api 서버와 통신할 때 필요한 모든것을 나타냅니다.

pod에는 다음 경로에 해당 파일들이 존재하며 , 얘네들로 k8s-api-server와 통신할 수 있습니다.
```bash
$ kubectl exec mypods ls /var/run/secrets/kubernetes.io/serviceaccount/
ca.crt
namespcae
token
```

## 3. private registry
docker private registry에서 image를 땡겨올 때 , secret을 사용할 수 있습니다.

```
$ kubectl create secret docker-registry mydockerhubsecert \
--docker-username=jjsair0412 \
--docker-password={password} \
--docker-email=jjsair0412@naver.com
```

- [관련 공식 문서](https://kubernetes.io/ko/docs/tasks/configure-pod-container/pull-image-private-registry/)

***private registry에 접근하는 방법은 , k8s provider마다 다릅니다***