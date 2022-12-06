# Study information
해당 문서는 pod에 마운트되는 volume에 관한 내용을 담 고 있습니다.
## 1. 컨테이너와 볼륨
k8s에서 각 파드의 컨테이너들은 볼륨을 각각 마운트 할 수 있으며 , 같은 볼륨을 마운트시켜 파일을 공유 할 수 있습니다.
## 2. emptyDir
emptyDir type의 볼륨은 파드와 라이프사이클을 공유합니다.

따라서 파드가 제거되면 볼륨의 데이터도 같이 제거되게 됩니다.

파드 내부에서 컨테이너들끼리 파일을 공유할 때 사용하면 유용합니다.
### 2.1 usecase
아래 예제 파드는 html-generator 컨테이너가 index.html파일을 주기적으로 생성하여 /var/htdocs안에 파일을 생성합니다.

생성된 파일을 web-server 컨테이너가 다시 마운트하여 80번 포트로 서비스하는 파드 입니다.
```bash
apiVersion: v1
kind: Pod
metadata:
  name: test
spec:
  containers:
  - image: jjsair0412/test
    name: html-generator
    volumeMounts:
    - name: html
      mountPath: /var/htdocs
  - image: nginx:alpine
    name: web-server
    volumeMounts:
    - name: html
      mountPath: /usr/share/nginx/html
      readOnly: true
    ports:
    - containerPort: 80
      protocol: TCP
  volumes:
  - name: html
    emptyDir: {}
```
## 3. git 레포지토리를 파드에서 마운트 하기
이전엔 gitrepo라는 type의 volume을 사용했지만 , 지금 (2022.12.06)일자로 사용되지 않습니다.

따라서 initContainer에서 git 레포지토리를 클론하고 emptyDir 볼륨에 넣어놓고 해당 경로를 다른 컨테이너에서 마운트 하는 방식을
사용합니다.

https://kubernetes.io/docs/concepts/storage/volumes/#gitrepo

## 4. hostPath
hostPath type 볼륨은 노드 파일시스템의 특정 파일이나 디렉터리를 가르킵니다.
- 동일 노드에 실행중인 파드가 , 동일 경로인 hostPath를 가르킨다면 동일 파일이 출력됩니다.

또한 파드가 제거되더라도 마운트된 경로의 파일은 제거되지 않습니다.
이전 파드가 제거됐더라도 동일 파드가 이전 파드와 동일 노드에 스케줄링됐다면 파일을 볼 수 있습니다.

### 4.1 hostPath type 볼륨을 도입할 때 고려해야하는 이유
볼륨의 파일등의 콘텐츠는 특정 노드의 파일시스템에 저장되기 때문에 , 
파드가 다른 노드로 다시 스케줄링 되면 이전 데이터를 볼 수 없기에 도입시에는 신중해야 합니다.

여러 파드에 걸쳐 데이터를 유지하기 위해서 hostpath를 사용한다는것은 절대 하면 안돼는 행동입니다.

## 5. PersistentVolume
### 5.1 왜 PersistentVolume ? persistentvolumeclaim ?
k8s에서는 볼륨 마운트를 지정할 수 있는 기술이 엄청 많다.
예를들어 클라우드 제공업체에 따라 마운트 방법이 전부다 다르며 , nfs라면 네트워크 기반 지식이 있어야 nas와 연동이 가능하다.

애플리케이션을 배포하는 입장에서 , 인프라스트럭쳐를 다 이해하고 deployment등 애플리케이션을 배포한다는것은 
인프라스트럭처의 세부 사항에 대한 걱정을 없애고
클라우드 공급자가 IDC를 걸쳐 이식 가능한 애플리케이션을 만들고
애플리케이션과 개발자로서 실제 인프라스트럭처를 숨긴다는 k8s 기본 철학에 반합니다.

따라서 , pv와 pvc가 등장합니다.

클러스터 관리자는 IDC의 nas나 클라우드 제공업체의 볼륨에 마운트 시켜 놓고 미리 pv를 만듭니다.
애플리케이션을 개발하고 배포하는 입장에선 k8s api 서버에 내 애플리케이션은 용량을 얼마나 할당해 줘 라는것만 요청 ( pvc 생성 )
하여 위의 문제점을 해결합니다.

결론적으로 pv , pvc를 사용하면 개발자는 인프라계층에서 어떤 스토리지 기술이 도입되었는가를 알 필요가 없습니다.

### 5.2 namespace 범위
PersistentVolume은 특정 namespace에 속하지 않지만 ( cluster level resource )
PersistentVolumeclaim은 특정 namespace에 속합니다.

### 5.3 pv와 pvc가 바운드되는 조건
PersistentVolume은 PersistentVolumeclaim이 요청한 용량을 수용할 수 있을 만큼 충분히 커야 합니다.
volume accessmode는 claim에서 요청한 접근 모드를 포함해야 합니다.

### 5.4 accessmode의 종류
1. ReadWriteOnce ( RWO )
- 단일 노드만 읽기 / 쓰기용으로 볼륨을 마운트 할 수 있습니다. 
2. ReadOnlyMany ( ROX )
- 다수 노드가 읽기 용으로 볼륨을 마운트 할 수 있습니다.
3. ReadWriteMany ( RWX )
- 다수 노드가 읽기 / 쓰기용으로 볼륨을 마운트 할 수 있습니다.

얘네는 파드 수가 아니라 , 어떤 볼륨을 동시에 사용할 수 있는 워커 노드의 수와 관련이 있습니다.
- 만약 RWX라면 여러 워커 노드가 해당 볼륨을 동시에 읽기/쓰기로 마운트할 수 있다는것을 의미합니다.

### 5.5 PersistentVolume 재 사용
**Retain 정책인 경우**
마운트된 상태의 pvc와 파드를 제거한 뒤 ,
다시 사용했엇던 pv를 재 마운트 시킨다면 pvc가 Pending상태로 남습니다.
그리고 pv는 Released 상태를 나타내게 됩니다.

그러한 이유는 이미 volume을 사용했기에 데이터를 가지고 있으므로 cluster 관리자가 볼륨을 완전히 비워내기 전까지 새로운 pvc에 마운트되지 않습니다.

그러한 이유는 다음과 같습니다.
```
클러스터관리자가 볼륨을 비우지 않았다면
동일한 PersistentVolume을 사용하는 새 파드는
다른 namespace에서 pvc와 파드가 생성됐다고 할 지라도
volume에 있는 파일을 읽을 수 있기 때문에 마운트되지 않습니다.
```
#### 5.5.1 재 사용하기 위해선 ? persistetnVolumeClaimPolicy !
persistetnVolumeClaimPolicy 정책을 Retain이 아닌 Recycle이나 Delete를 주면 됩니다.

1. Recycle일 경우
  pvc와 파드가 제거되면 볼륨 내부 콘텐츠를 삭제하여 재 사용할 수 있게끔 만듭니다.
  여러번 다른 파드나 pvc에서 재 사용이 가능합니다.
  - 지원이 중단될 예정입니다. storageclass로 동적 프로비저닝하는것을 추천

  https://kubernetes.io/docs/concepts/storage/persistent-volumes/#recycle

2. Delete일 경우
  기반 스토리지를 삭제합니다.
  storageclass의 기반 정책


이러한 claimpolicy는 pv가 생성된 이후 변경이 가능합니다.
### 5.6 accessMode 다중 지정
PersistentVolume에서 accessMode를 지정할 수 있는데 , 여러개를 지정할 수 있습니다.

ex) pv.yaml
```bash
apiVersion: v1
kind: PersistentVolume
metadata:
  name: pv-hostpath
spec:
  capacity:
    storage: 2Gi 
  volumeMode: Filesystem
  accessModes:
    - ReadWriteOnce 
    - ReadWriteMany
    - ReadOnlyMany
  storageClassName: manual 
  persistentVolumeReclaimPolicy: Retain # pvc가 해제되더라도 pv가 남도록 Retain 부여
  hostPath: # 볼륨 플러그인
    path: /
```