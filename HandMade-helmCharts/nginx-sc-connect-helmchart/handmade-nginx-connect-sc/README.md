## openEBS 및 타 storageClass와 연동이 가능한 nginx helm chart입니다.

### helm upgrade command
```
kubectl create ns nginx

helm upgrade --install nginx . -n nginx -f values.yaml
```

values.yaml의 default는 OpenEBS와 deployments가 연동하는 값이 들어가 있습니다.
입맛대로 조정하여 테스트에 활용합시다.

해당 차트와 관련된 문서 주소는 아래와 같습니다.
[pv pvc sc 연동 테스트](https://github.com/jjsair0412/kubernetes_info/blob/main/etc/storageClass%20-%3E%20pv%20-%3E%20pvc%20Retain%20test.md)