# CRD 개발 - golang
해당 폴더에서는 실제 cr , crd를 yaml파일로 정의한 뒤 , ( etcd에 등록 ) 

golang용 operator를 커스터마이징 하여 해당 cr를 통해서 k8s object를 컨트롤하는 실습 입니다.

## INDEX
- [CRD 생성](#1-crd-생성)
- [CR 생성](#2-cr-생성)
- [kubernetes custom controlelr 생성](#3-custom-controller-생성)

## 0. prerequisite
참고 문서
- [CRD 생성관련 공식문서](https://kubernetes.io/docs/tasks/extend-kubernetes/custom-resources/custom-resource-definitions/)
- [by devjh의 블로그](https://frozenpond.tistory.com/111)
- [operator SDK 개발 방안 공식문서](https://sdk.operatorframework.io/docs/building-operators/golang/tutorial/)
- [operator 사용 방안](https://sphong0417.tistory.com/3)
- [CRD , CR yaml manifest 참고 문서](https://www.techtarget.com/searchitoperations/tip/Learn-to-use-Kubernetes-CRDs-in-this-tutorial-example)

실제 코드 위치
- [CR , CRD yaml manifest]()
- [golang code]()

## 0.1 개발 환경
- macOS m1
- IDE Tool : vscode
- Java : java 17.0.5 2022-10-18 LTS
- operator version : [operator sdk](https://github.com/operator-framework/operator-sdk)
- kubernetes version : v1.25.2

## 0.2 개발 목표
helloworld kind를 가지는 cr을 개발합니다.

기능은 다음과 같습니다.
helloworld kind를 가진 cr을 생성 후 , message를 변경한다면 

1. nginx image를 가진 deployment 배포
2. 해당 deployment를 expose하는 서비스 생성
- service type은 NodePort 사용
3. log로 "hello world i'm jinseong" 출력

의 순서로 배포 작동하게끔하는 cr을 개발합니다.

또한 **replicas 개수가 변경되면 , 파드 개수가 늘어나거나 줄어들게끔 개발**합니다.

해당 CRD는 **namespace에 종속적**이게끔 개발합니다.

kubectl로 상태를 확인할 때 , **"hw" 라는 단축키로 kube-api server에 요청을 보낼 수 있게 끔** 개발합니다.

crd의 **fild 값은 message , appId , replicas , language , image** 를 받습니다.
- 이때 language는 enum 변수를 통해서 java, go , python만 가능하게끔 한정합니다.
- replicas의 최소는 1개 , 최대는 3개로 한정 합니다.

**appId , replicas , message,image는 필수 옵션**으로 한정합니다.

## 0.3 개발 과정
1. CRD yaml파일로 etcd에 정의
2. 정의된 CRD를 kind로 하는 CR yaml로 etcd에 정의
3. CR로 k8s object controller 하기 위해 operator-sdk custom하여 개발
- control 해야 할 object 목록
    - deployment
    - service

## 1. CRD 생성
CRD를 아래와 같이 생성합니다.

각 필드의 대한 설명은 주석으로 대체 합니다.

필드에 들어갈 수 있는 값들은 , 아래 공식 문서를 참고하여 필요한것들을 찾아서 작성합니다.
- [CRD 유효성 스키마 검사 방안](https://kubernetes.io/docs/tasks/extend-kubernetes/custom-resources/custom-resource-definitions/#specifying-a-structural-schema)
- [OpenAPI v3.0 validation schema](https://kubernetes.io/docs/tasks/extend-kubernetes/custom-resources/custom-resource-definitions/#validation)

```yaml
# apiextensions.k8s.io/v1을 지정하여 open API v3.0을 통해 구조를 검사하도록 정의
# structural schema 문서 읽어보기
apiVersion: apiextensions.k8s.io/v1
# kind는 CustomResourceDefinition
kind: CustomResourceDefinition
metadata:
  # name must match the spec fields below, and be in the form: <plural>.<group>
  name: helloworlds.jjsair0412.example.com
spec:
  # group name to use for REST API: /apis/<group>/<version>
  group: jjsair0412.example.com
  # list of versions supported by this CustomResourceDefinition
  versions:
    # version 명시
    - name: v1
      # Each version can be enabled/disabled by Served flag.
      served: true
      # One and only one version must be marked as the storage version.
      storage: true
      schema:
        # openAPIV3Schema는 required option 
        openAPIV3Schema:
          # k8s object type
          type: object
          # spec 필드 또한 필수 옵션.
          required: ["spec"]
          properties:
            spec:
              type: object
              required: ["appId", "replicas", "message","image"] # appId , replicas , message, image는 필수 옵션
              properties:
                message: # go struct 및 cr에 message를 입력하게끔 정의.
                  type: string # message의 data type 지정
                image: #go struct 및 cr에 image를 입력하게끔 정의.
                  type: string # image의 data type 지정
                appId: # go struct 및 cr에 appId를 입력하게끔 정의.
                  type: string # appId data type 지정
                replicas: #  go struct 및 cr에 replicas를 입력하게끔 정의.
                  type: integer # replicas의 data type 지정
                  minimum: 1 # 최소 1개~ 최대 3개
                  maximum: 3
                language: # go struct 및 cr에 language를 입력하게끔 정의.
                  type: string # language data type 지정
                  enum: # enum을 통해 데이터 미리 정해놓고, 셋중 하나가 아니라면 에러터지게끔 생성
                  - java
                  - go
                  - python
  # either Namespaced or Cluster , namespace에 종속적이게끔 생성
  scope: Namespaced
  names:
    # plural name to be used in the URL: /apis/<group>/<version>/<plural>
    plural: helloworlds
    # kubectl CLI 출력 결과 출력될 이름
    # singular name to be used as an alias on the CLI and for display
    singular: helloworld
    # CR의 Kind가 될 이름
    # kind is normally the CamelCased singular type. Your resource manifests use this.
    kind: helloworld
    # hw로 kubectl 명령어 날릴 수 있도록 생성
    # shortNames allow shorter string to match your resource on the CLI
    shortNames:
    - hw
```

apply하여 etcd에 CRD를 등록 합니다.

```bash
$ kubectl apply -f mycrd.yaml
customresourcedefinition.apiextensions.k8s.io/helloworld.jjsair0412.example.com created
```

get crd 명령어로 생성한 crd의 Name과 생성 시간을 확인할 수 있습니다.

```bash
$ kubectl get crd
NAME                                 CREATED AT
helloworlds.jjsair0412.example.com   2022-12-21T04:18:45Z
...
```

kubectl explain 명령어를 통해 crd의 대한 정보를 확인할 수 있습니다.

```bash
$ kubectl explain helloworlds
```

***이제 helloworld라는 kind를 가진 CR을 생성할 수 있습니다.***

## 2. CR 생성
CR을 아래와 같이 생성합니다.

각 필드의 대한 설명은 주석으로 대체 합니다.

```yaml
# CRD yaml에서 정의한 group 명/CRD yaml에서 정의한 version
# spec.group/spec.versions.name
apiVersion: "jjsair0412.example.com/v1"
# CRD yaml에서 정의한 kind 명
# spec.names.kind
kind: helloworld
metadata:
  name: hello-sample
# spec 은 필수 옵션
spec:
  # appId , replicas , message, image는 필수 옵션
  appId: "hello-world"
  replicas: 3
  message: "hi"
  image: nginx
  language: java
```

만약, 필수 옵션인 appId, replicas , message , image중 하나를 지정하지 않고 CR을 생성하면 다음과 같은 에러가 발생합니다.

1. 필수옵션 누락의 경우

```yaml
apiVersion: "jjsair0412.example.com/v1"
kind: helloworld
metadata:
  name: hello-sample
spec:
  # appId: "hello-world"
  replicas: 3
  message: "hi"
  image: nginx
  language: java
```

appId 누락되어 아래와 같은 에러 발생
```bash
The helloworld "hello-sample" is invalid: spec.appId: Required value
```

2. replicas 개수제한 안맞을 경우
replicas의 개수를 1~3개로 제한했기에 , 3개를 넘어가거나 1개보다 작으면 아래와 같은 에러가 발생합니다.

```yaml
apiVersion: "jjsair0412.example.com/v1"
kind: helloworld
metadata:
  name: hello-sample
spec:
  appId: "hello-world"
  replicas: 5
  message: "hi"
  image: nginx
  language: java
```

replicas가 5개라 아래와 같은 에러 발생

```bash
The helloworld "hello-sample" is invalid: spec.replicas: Invalid value: 5: spec.replicas in body should be less than or equal to 3
```

3. enum 에 값이 포함되지 않을경우
language 필드의 enum값에 지정한 값이 아닌 다른 값을 넣엇을 경우 , 에러가 발생합니다.

```yaml
apiVersion: "jjsair0412.example.com/v1"
kind: helloworld
metadata:
  name: hello-sample
spec:
  appId: "hello-world"
  replicas: 3
  message: "hi"
  image: nginx
  language: h
```

java , go , python중 한가지만 사용 가능
```bash
The helloworld "hello-sample" is invalid: spec.language: Unsupported value: "h": supported values: "java", "go", "python"
```

또한 기본적인 metadata를 변경시켜서 namespace별로 CR을 격리하거나 , 이름을 변경할 수 있습니다.

```yaml
apiVersion: "jjsair0412.example.com/v1"
kind: helloworld
metadata:
  name: hello-sample
  namespace: test
# required option
spec:
  # required option
  appId: "hello-world"
  replicas: 3
  message: "hi"
  image: nginx
  language: java
```

test namespace에 hello-sample CR이 생성되는것을 확인할 수 있습니다.

```bash
$ kubectl get hw -n test
NAME           AGE
hello-sample   6s
```

그러나 해당 CR을 control하여 kubernetes resource를 감시하다가 상태를 변경시킬 수 있는 custom controller가 없기때문에 , 동작하지 않습니다.

따라서 custom controller를 생성해야 합니다.

## 3. Custom controller 생성
이제 만들어진 CR이 k8s 기본 리소스인 Pod , Deployment , service 등과 연계해서 동작할 수 있도록 custom controller를 생성해야 합니다.

해당 문서에서는 java의 spring을 통해서 custom controller를 생성합니다.

아래 작성된 URL을 확인해보면 , 다양한 언어에서 client 라이브러리를 지원하는것을 확인할 수 있습니다.
- [공식적으로 지원되는 쿠버네티스 클라이언트 라이브러리](https://kubernetes.io/ko/docs/reference/using-api/client-libraries/#%EA%B3%B5%EC%8B%9D%EC%A0%81%EC%9C%BC%EB%A1%9C-%EC%A7%80%EC%9B%90%EB%90%98%EB%8A%94-%EC%BF%A0%EB%B2%84%EB%84%A4%ED%8B%B0%EC%8A%A4-%ED%81%B4%EB%9D%BC%EC%9D%B4%EC%96%B8%ED%8A%B8-%EB%9D%BC%EC%9D%B4%EB%B8%8C%EB%9F%AC%EB%A6%AC)

Java kubernetes client 공식문서는 git에 있습니다.
- [Java Kubernetes client docs](https://github.com/kubernetes-client/java/wiki)

### 3.1 CRD model 생성하기
아래 문서를 보고 , 미리 생성해둔 CRD와 CR yaml manifest를 java용 model로 변환작업을 수행해야 합니다.

생성한 CRD , CR이 MVC 패턴의 model이라고 생각하면 됩니다.
먼저 아래 참조한 가이드를 읽어봅시다.

- [관련 docs : CustomResourceDefinition에서 Java 코드 생성 가이드](https://github.com/kubernetes-client/java/blob/master/docs/generate-model-from-third-party-resources.md#example-commands-for-local-crd-manifests)

해당 문서처럼 docker 명령어를 통해 model을 생성할 수 도 있지만 , 아래 인터페이스들을 implement하여 직접 커스터마이징도 가능합니다.
- [KubernetesListObject interface](https://github.com/kubernetes-client/java/blob/master/kubernetes/src/main/java/io/kubernetes/client/common/KubernetesListObject.java)
- [KubernetesObject intreface](https://github.com/kubernetes-client/java/blob/master/kubernetes/src/main/java/io/kubernetes/client/common/KubernetesObject.java)

#### 3.1.1 image pull
작업중인 로컬에서 k8s client image를 pull 합니다.

```bash
$ docker pull ghcr.io/kubernetes-client/java/crd-model-gen:v1.0.6
```

#### 3.1.2 model 생성
받은 docker image로 이전에 생성해둔 CRD yaml을 java model로 변환해 줍니다.

원격지 ( 통신이 되어야 함 ) 에 있는 CRD yaml 파일을 model로 변환시킬 수도 있고 , 로컬에 있는 CRD yaml 파일을 model로 변환시킬 수 있습니다.
- [원격지 방법 from docs]()
- [로컬 방법 from docs]()

기본적으로 model 생성기 컨테이너는 로컬 도커 데몬에 [kind kubernetes](https://refactorfirst.com/kind-kubernetes-cluster) 클러스터를 자동으로 프로비저닝하고 CRD를 클러스터에 적용하는 방식으로 작동합니다. 
따라서 호스트에는 docker 가 실행중 ( deamon ) 이여야만 합니다.

아래 옵션 설명을 잘 읽고 , 자신의 환경에 맞게끔 option을 넣어 생성합니다.
```bash
-u: <CRD's download URL or file path, use it multiple times to read multiple CRDs>
-n: <the target CRD group name, which is in the reverse order of ".spec.group">
-p: <output package name for the generated java classes>
-o: <output path of the generated project>
```

**주의 !**
- -n 옵션에 들어가는 CRD group name은 역순으로 넣어야 합니다. 
  -  만약 CRD group이 jjs.group.com 이라면 , -n에 들어가는 옵션은 com.group.jjs

가이드문서를 작성하는 환경에선 , local에 yaml manifest가 있기에 로컬 방법으로 합니다.
아래 명령어로 model을 생성시켜줍니다.
- LOCAL_MANIFEST_FILE : 로컬의 CRD 파일의 경로를 작성합니다.

***docs의 가이드 방법***
```bash
# Downloading 
mkdir -p /tmp/crds && cd /tmp/crds

# 실습용 CRD 받아오기. 가이드에선 이미 있기 때문에 하지않는다.
wget https://gist.githubusercontent.com/yue9944882/266fee8e95c2f15a93778263633e72ed/raw/be12c13379eeed13d2532cb65da61fffb19ee3e7/crontab-crd.yaml

# Local generation
LOCAL_MANIFEST_FILE=/tmp/crds/crontab-crd.yaml
mkdir -p /tmp/java && cd /tmp/java
docker run \
  --rm \
  -v "$LOCAL_MANIFEST_FILE":"$LOCAL_MANIFEST_FILE" \
  -v /var/run/docker.sock:/var/run/docker.sock \ # 로컬에 설치된 docker sock 파일 참조
  -v "$(pwd)":"$(pwd)" \
  -ti \
  --network host \
  ghcr.io/kubernetes-client/java/crd-model-gen:v1.0.6 \
  /generate.sh \
  -u $LOCAL_MANIFEST_FILE \
  -n com.example.stable \
  -p com.example.stable \
  -o "$(pwd)"
```

***실 수행한 명령어***
스크립트를 하나 생성해서 실행했습니다.
```bash
$ cat create-model.sh
#!/usr/bin/env bash

LOCAL_MANIFEST_FILE=~/tmp/jjscrds/mycrd.yaml

mkdir -p /tmp/java && cd /tmp/java

docker run \
  --rm \
  -v "$LOCAL_MANIFEST_FILE":"$LOCAL_MANIFEST_FILE" \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v "$(pwd)":"$(pwd)" \
  -ti \
  --network host \
  ghcr.io/kubernetes-client/java/crd-model-gen:v1.0.6 \
  /generate.sh \
  -u $LOCAL_MANIFEST_FILE \
  -n com.example.jjsair0412 \
  -p com.example.customcontrollercode \
  -o "$(pwd)"
```
평균 10~30분 정도 시간이 소요됩니다.

model 생성을 성공한다면 아래와 같은 결과값을 볼 수 있습니다.

만약 자꾸 생성이 실패한다면 , 스크립트를 실행하는 경로를 HOME 으로 이동하여 수행하면 됩니다.
```bash
$ cd ~
$ bash create-model.sh
```


```bash
...
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 20.660 s
[INFO] Finished at: 2022-12-22T13:32:11Z
[INFO] ------------------------------------------------------------------------
---Done.
---Done.
```

생성해 두었던 /tmp/java 디렉터리에 자바 프로젝트가 생성된 것을 확인 할 수 있으며 , 아래 경로에서 model java file들을 확인할 수 있습니다.

```bash
$ cd ~/tmp/java/src/main/java/com/example/customcontrollercode/models

$ tree
.
├── V1Helloworld.java
├── V1HelloworldList.java
└── V1HelloworldSpec.java
```



### 3.2 Kubernetes Custom Controller 생성
model이 생성되었다면 , 해당 model을 통해 실제 코드를 작성합니다.

해당 문서는 controller를 생성하기 위해서 spring boot를 사용했습니다.

각 컴포넌트들의 재사용을 용이하게 하기 위해서 컴포넌트들을 Bean 객체로 등록하여 사용합니다.

**참고 문서**
- [CoreV1Api Java Examples](https://www.programcreek.com/java-api-examples/?api=io.kubernetes.client.openapi.apis.CoreV1Api)
- [crd 생성하기 참고 문서](https://refactorfirst.com/java-spring-boot-kubernetes-controller)
- [Kubernetes Clients manifest example](https://github.com/kubernetes-client/java/tree/master/examples)
- [AppsV1API RestAPI 및 각 메서드 별 설명](https://github.com/kubernetes-client/java/blob/master/kubernetes/docs/AppsV1Api.md)

[**실제 코드**](https://github.com/jjsair0412/kubernetes_info/tree/main/kubernetes%20resource/CRD%20%EA%B0%9C%EB%B0%9C%20%EA%B4%80%EB%A0%A8%20-%20cr%20%2C%20crd%20%2C%20k8s%20object%20%2C%20operator/crd%20%EA%B0%9C%EB%B0%9C%20-%20kubernetes/custom-controller-code)


#### 3.2.0 작업 전 환경설정
먼저 K8S API server의 접근을 enable 시켜야합니다.

kube proxy 명령어를 통해서 open시킬 수 있지만, 차후 인증-인가 작업을 통해 유저를 생성하여 해당 유저로 API Server로 통신하게끔 작업 할 예정입니다.
- [k8s apiserver와 통신하기 관련 블로그](https://coffeewhale.com/apiserver)
- [apiserver 통신 관련 공식문서](https://kubernetes.io/ko/docs/tasks/administer-cluster/access-cluster-api/)

#### 3.2.1 구성 요소
controller에는 필수 요소가 4가지 들어가야 합니다.

- reconciler 
    - reconciler는 CRD instance가 변경되는 요청을 처리하기 위해 사용합니다.
    변경 사항으로 무언가를 할 수 있도록 CRD 인스턴스를 생성, 업데이트 또는 삭제할 때 호출됩니다.
- Shared Index Informer
   - controller는 새로운 CRD instancer가 CRUD작업을 감시하며 Kubernetes Cluster API와 지속적으로 연결상태를 유지 할 필요가 없기 때문에 해당 구성요소는 cache와 비슷하게 동작합니다.
- APIClient
   - Kubernetes Cluster의 API server와 통신하기 위해서 사용됩니다.
- CRD Model
   - model class들을 쉽게 생성하고 이용하기 위해 필요합니다. 전에 도커로 만들어둔 파일입니다.

각 구성 요소의 실제 코드는 다음과 같습니다.

**1. reconciler**
```java
    // Resources 객체를 reconcier에서 사용하기에 Bean 객체로 등록
    @Bean
    Resources resources(){
        return new Resources();
    }

    // AppsV1Api가 k8s api server에 request를 보내어 k8s resource 관리 수행
    @Bean
    Reconciler reconciler(SharedIndexInformer<V1Helloworld> shareIndexInformer,
            AppsV1Api appsV1Api) {
        return request -> {
            String key = request.getNamespace() + "/" + request.getName();
            
            V1Helloworld resourceInstance = shareIndexInformer
                    .getIndexer()
                    .getByKey(key);

            
            if (resourceInstance != null) {
                V1Deployment v1Deployment = resources().createDeployment(resourceInstance);
                System.out.println("Creating resource deployment...");

                try {
                    appsV1Api.createNamespacedDeployment(
                            request.getNamespace(),
                            v1Deployment,
                            "true",
                            null,
                            "",
                            "");
                } catch (ApiException e) {
                    createErrorCode("createNamespacedDeployment",e);
                    System.out.println("Creating resource failed");
                    if (e.getCode() == 409) { // 생성되어있다면 409 에러 발생 . 생성되어있는데 다시한번 resourceInstance가 들어왔다는건 update
                        
                        System.out.println("Updating resource...");
                        try {
                            appsV1Api.replaceNamespacedDeployment(
                                    request.getName(),
                                    request.getNamespace(),
                                    v1Deployment,
                                    null,
                                    null,
                                    "",
                                    "");
                            
                        } catch (ApiException ex) {
                            createErrorCode("replaceNamespacedDeployment",ex);
                            throw new RuntimeException(ex);
                        }
                    } else {
                        throw new RuntimeException(e);
                    }
                    
                }
                return new Result(false);
            }else{
                System.out.println("delete deployment resource..."); // delete는 k8s에서 메타데이터로 리소스들을 관리하기 때문에 , crd로 생성시켜준 이름과 resource 이름이 동일하다면 만들지 않아도 k8s resource가 삭제 된다.
            }
            return new Result(false);

        };
    }
```


**2. Shared Index Informer**

Shared Index Informer는 SharedInformerFactory를 사용하고 , SharedInformerFactory는 ApiClient를 파라미터로 받습니다.

따라서 SharedInformerFactory를 먼저 Bean 객체로만들어 둡니다.

```java
    @Bean
    SharedInformerFactory sharedInformerFactory(ApiClient apiClient){
        return new SharedInformerFactory(apiClient);
    }


    @Bean
    SharedIndexInformer<V1Helloworld> sharedIndexInformer(SharedInformerFactory sharedInformerFactory,
            ApiClient apiClient) {
        System.out.println("hello im sharedIndexInformer method");
        GenericKubernetesApi<V1Helloworld, V1HelloworldList> api = new GenericKubernetesApi<>(V1Helloworld.class,
                V1HelloworldList.class,
                "jjsair0412.example.com", // CRD Group
                "v1", // CRD version
                "helloworlds", // CRD Plural name
                apiClient);
        return sharedInformerFactory.sharedIndexInformerFor(api, V1Helloworld.class, 0);
    }
```

**3. APIClient**
```java
    @Bean
    ApiClient myApiClient() throws IOException {
      ApiClient apiClient = new ApiClient();
      System.out.println("basepath is + "+ apiClient.getBasePath());
      return apiClient.setHttpClient(
          apiClient.getHttpClient().newBuilder().readTimeout(Duration.ZERO).build());
    }
```

**4. K8S Cluster와 통신하는 AppsV1Api**
```java
    // AppsV1Api로 kube api server와 통신 . v1api bean required Bean.
    @Bean
    AppsV1Api appsV1Api(ApiClient apiClient) {
        apiClient.setBasePath("http://127.0.0.1:" + 8001); 
        // apiClient.setDebugging(true); // kube api server와 통신시 debugging option 설정
        System.out.println("final basepath is + "+ apiClient.getBasePath());
        return new AppsV1Api(apiClient);
    }
```

**5. resource 설정 부분**
api server에 던질 resource는 메서드의 getter.setter로 정의할 수 있습니다.

```java
    public V1Deployment createDeployment(V1Helloworld resourceInstance) {
        V1Deployment deploymentSet = new V1Deployment();
        V1DeploymentSpec deploymentSpec = new V1DeploymentSpec();
        String applanguageInfo = resourceInstance.getSpec().getLanguage().toString();

        deploymentSpec.template(podTemplate(resourceInstance, applanguageInfo)); // pod template 들어감
        deploymentSpec.replicas(resourceInstance.getSpec().getReplicas());

        deploymentSet.setMetadata( 
                new V1ObjectMeta() 
                        .name(resourceInstance.getMetadata().getName())
                        .labels( // deployment lable 지정
                                Map.of(
                                        "app", applanguageInfo,
                                        "message", resourceInstance.getSpec().getMessage()
                                    )
                                )
                    );

        deploymentSpec.selector(new V1LabelSelector() // deployment  pod selector 지정
                        .matchLabels(
                        Map.of(
                                "app", applanguageInfo,
                                "message", resourceInstance.getSpec().getMessage()
                            )
                        )
                        
                    );

        deploymentSet.setSpec(deploymentSpec);
        
        System.out.println("deployment spec init com");
        return deploymentSet;
    }

    // deployment pod template 생성
    private static V1PodTemplateSpec podTemplate(V1Helloworld resourceInstance, String applanguageInfo) {
        System.out.println("hello im V1PodTemplate");
        V1ObjectMeta podMeta = new V1ObjectMeta();
        V1PodTemplateSpec podTemplateSpec = new V1PodTemplateSpec();

        List<V1Container> podContainers = new ArrayList<V1Container>();
        

        podContainers.add(0,createContainers(resourceInstance));

        podMeta.labels( // pod label 지정 ( required )
                Map.of(
                        "app", applanguageInfo,
                        "message", resourceInstance.getSpec().getMessage()
                    )
                );

        podMeta.setName("hello" + "-" + UUID.randomUUID()); // pod name 지정 ( required )

        podTemplateSpec.setMetadata(podMeta);
        podTemplateSpec.spec(new V1PodSpec().containers(podContainers)); 


        return podTemplateSpec;
    }

        // pod container 정보 생성
    private static V1Container createContainers(V1Helloworld resourceInstance){
        V1Container container = new V1Container();
        container.setImage(resourceInstance.getSpec().getImage());
        container.setName(resourceInstance.getSpec().getAppId());
        
        return container;
    }
```