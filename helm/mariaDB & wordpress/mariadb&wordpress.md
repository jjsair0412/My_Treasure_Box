# MariaDB를 이용한 client 연결 실습
## Precondition
- 모든 예제에서 중괄호 {} 는 제거하고 값을 입력합니다.
- [mariaDB helm](https://github.com/bitnami/charts/tree/master/bitnami/mariadb-galera)
## 1. MariaDB 설치
### 1.1 namespace 설정
- mariaDB 관리용 namespace를 생성합니다.
```
$ kubectl create ns maria
```
### 1.2 Helm install & mariaDB 설정
- helm repo에 bitnami chart를 추가합니다.
```
$ helm repo add bitnami https://charts.bitnami.com/bitnami
```
- pull 명령어를 통해서 mariaDB를  values파일을 pull 합니다.
```
$ helm pull bitnami/mariadb-galera --untar
```
### 1.3 mariaDB user 등록
- 생성할 database와 user , password를 등록합니다.
```
rootUser:
  user: root
  password: {root_password}
  forcePassword: true

db:
  user: {username}
  password: {db_password}
  name: my_database
  forcePassword: true

galera:
  mariabackup:
    user: mariabackup
    password: {mariabackup_password}
    forcePassword: true
```
- mariaDB를 설치합니다.
```
$ helm upgrade --install mariadb . -f values.yaml -n maria
```
- root user 비밀번호 확인 명령어
-- 해당 명령어를 통해서 나온 결과값으로 , values.yaml의 root user 비밀번호를 등록해야 할 경우가 있을 수 도 있습니다.
```
$ echo "$(kubectl get secret --namespace maria mariadb-mariadb-galera -o jsonpath="{.data.mariadb-root-password}" | base64 --decode)"
```
## 2. WordPress 설치
- pv , pvc는 환경에 맞게끔 유동적으로 생성합니다.
- 아래의 예는 storage를 longhorn으로 사용중이기에 , pvc만 생성해줍니다.
- ingress를 사용하거나 , nodeport를 사용하거나 환경에 맞게끔 유동적으로 service를 구성합니다.
### 2.1 namespace 설정
- wordpress 관리용 namespace를 생성합니다.
```
$ kubectl create ns wordpressns
```
### 2.1 Create Persistent Volume Claim 
- wordpress가 사용할 pvc를 생성합니다.
```
kind: PersistentVolumeClaim
apiVersion: v1
metadata:
  namespace: wordpressns
  name: wordpress-volumeclaim
spec:
  accessModes:
    - ReadWriteMany
  resources:
    requests:
      storage: 10Gi
```
### 2.1 Create Deployments 
- 연결을 테스트할 wordpress를 배포합니다.
```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: wordpress
  namespace: wordpressns
  labels:
    app: wordpress
spec:
  replicas: 2
  selector:
    matchLabels:
      app: wordpress
  template:
    metadata:
      labels:
        app: wordpress
    spec:
      containers:
        - image: wordpress
          name: wordpress
          env:
          - name: WORDPRESS_DB_HOST
            value: {service.dns.name}:3306 
          - name: WORDPRESS_DB_NAME
            value: {db_name} # values.yaml에 들어간 db 이름
          - name: WORDPRESS_DB_USER
            value: {username} # values.yaml에 들어간 username
          - name: WORDPRESS_DB_PASSWORD
            value: {password} # values.yaml에 들어간 user의 password
          ports:
            - containerPort: 80
              name: wordpress
          volumeMounts:
            - name: wordpress-persistent-storage
              mountPath: /var/www/html
      volumes:
        - name: wordpress-persistent-storage
          persistentVolumeClaim:
            claimName: wordpress-volumeclaim

```
### 2.2 Create service 
- wordpress를 묶어줄 clusterip를 생성합니다.
```
apiVersion: v1
kind: Service
metadata:
  creationTimestamp: null
  labels:
    app: wordpress
  name: wordpress
  namespace: wordpressns
spec:
  ports:
  - port: 80
    protocol: TCP
    targetPort: 80
  selector:
    app: wordpress
  type: ClusterIP
status:
  loadBalancer: {}
```
### 2.3 Create ingress
- wordpress의 ingress를 생성해줍니다.
```
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  namespace: wordpressns
  name: wordpress-go-ingress
  annotations:
    kubernetes.io/ingress.class: "nginx"
    ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
    - host: {host_dns_address}
      http:
        paths:
        - pathType: Prefix
          path: /
          backend:
            service:
              name: wordpress
              port:
                number: 80
```