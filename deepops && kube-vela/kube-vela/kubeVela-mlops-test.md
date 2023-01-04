# kubeVela-install
해당 문서는 deepops k8s cluster 위에 kubevela를 배포한 후 , 

## Prerequirement

**구축 환경**
| os | 사양 | k8s version | deepops version | container runtime | role | ip addr | vela-core version |
|--|--|--|--|--|--|--|--|
| ubuntu 20.04 | 2core 4GB | v1.21.6 | 22.01 | containerd://1.4.9 | control plane | 10.0.0.2 | vela-core-1.6.5 |
| ubuntu 20.04 | 2core 4GB | v1.21.6 | 22.01 | containerd://1.4.9 | worker | 10.0.0.3 |

kube-vela의 model-serving addon을 배포하기 위해서는 Kubernetes 버전이 1.22 이하여야만 합니다.

**따라서 deepops는 22.01 , 21.09 버전을 설치해야만 합니다.**

참고 문서는 다음을 따릅니다.
- https://kubevela.io/blog/2022/03/02/kubevela-with-machine-learning

셈플 코드는 다음 kubevela sample yaml code를 참고합니다.
- https://github.com/kubevela/samples/tree/master/11.Machine_Learning_Demo

## 이론 - KubeVela AI
KubeVela의 AI addon은 두 가지로 나뉩니다.
1. model-serving
2. model-training

model-training addon은 KubeFlow의 KubeFlow's training-operator를 기반으로 하고 , TensorFlow, PyTorch 및 MXNet과 같은 다양한 프레임워크에서 분산 model-training을 지원합니다.

model-serving addon은 model을 쉽게 사용할 수 있는 Seldon Core를 기반으로 하며 트래픽 분산 및 A/B 테스트와 같은 고급 기능도 지원합니다.

KubeVela의 두 가지 AI addon을 통해서 model-training 및 serving 작업을 단순화 할 수 있습니다.
동시에 model-training 및 model-serving 프로세스를 KubeVela의 자체 워크플로, 멀티 클러스터 및 기타 기능과 결합하여 프로덕션 수준의 서비스를 완성할 수 있습니다.

- Note: You can find all source code and YAML files in [KubeVela Samples](https://github.com/kubevela/samples/tree/master/11.Machine_Learning_Demo). If you want to use the model pretrained in this example, style-model.yaml and color-model.yaml in the folder will do that and copy the model into the PVC.

## 1. addon 설치
model-training과 model-serving addon을 enalbe 합니다.
- vela cli 명령어를 통해 addon을 설치할 수 도 있고 , velaUX의 addon에서 enable 버튼으로 설치할 수 도 있습니다.

```bash
$ vela addon enable model-training
$ vela addon enable model-serving
```

model-training은 두가지 component type으로 이루어져 있습니다.
1. model-training
2. jupyter-notebook

model-serving은 한가지 component type으로 이루어져 있습니다.
1. model-serving

## 1.1 **만약 LB가 없다면** - helm chart로 model-training && model-serving 설치
model-serving이 배포되면서 ambassador service가 배포되는데 , default type이 LoadBalanacer type입니다.

만약 LB가 없다면 velaUX의 model-serving application type이 UnHealthy 로 나타나게 되기에 , addon으로 설치할 경우 직접 service를 edit해서 NodePort로 바꾸어 주거나 , 

```bash
$ kubectl edit svc ambassador -n vela-system
...
sepc:
...
  type: LoadBalancer -> NodePort로 변경
...
```

KubeVela helm deploy 방안 문서를 참고하여 , helm chart deploy를 해야 합니다.


## 2. 설치 결과 확인
각 addon별 사용 가능한 속성들을 vela show 명령어로 확인할 수 있습니다.
```bash
$ vela show model-training -n vela-system
$ vela show jupyter-notebook -n vela-system
$ vela show model-serving -n vela-system
```

kubectl 명령어로 파드가 running인지 확인합니다.
```
$ kubectl get all -n vela-system
```

velaUX에서 application이 running 상태인지 확인합니다.


## 3. Model Training
테스트를 위해 회색 이미지를 컬러 이미지로 바꿔주는 TensorFlow framework을 사용해서 model training을 진행합니다.

테스트에 활용된 color tensorflow git 주소는 다음과 같습니다.
- https://github.com/emilwallner/Coloring-greyscale-images


먼저 model-training과 model-serving을 진행할 namespace를 생성합니다.

```bash
$ kubectl create ns ml
```

model-serving 및 model-training에서 사용 가능한 변수들의 설명이나 type , required 정보를 확인하여 아래 template에 대입합니다.
```bash
$ vela show model-training -n vela-system
$ vela show jupyter-notebook -n vela-system
$ vela show model-serving -n vela-system
```

**model-training 진행합니다.**

storage 부분에 사용할 storageclass를 명시해 주어야 합니다.
- default sc가 있다면 default로 등록됩니다.


```yaml
apiVersion: core.oam.dev/v1beta1
kind: Application
metadata:
  name: training-serving # application name
  namespace: ml # namespace
spec:
  components:
  # model training 정보 입력 ! training 시작
  - name: demo-training
    type: model-training
    properties:
      image: fogdong/train-color:v1
      # tensorflow framwork 사용
      # "tensorflow" or "pytorch" or "mpi" or "xgboost" or "mxnet" 사용 가능
      framework: tensorflow
      # Declare storage to persist models. Here, the default storage class in the cluster will be used to create the PVC
      storage: 
        - name: "my-pvc"
          mountPath: "/model"
  
  # Start the model serving
  - name: demo-serving
    type: model-serving
    # The model serving will start after model training is complete
    # demo-training model-training이 끝나야 해당 demo-serving 시작
    dependsOn:
      - demo-training
    properties:
      # The protocol used to start the model serving can be left blank. By default, seldon's own protocol is used.
      # tanserflow protocol 사용. 
      # "seldon" or "tensorflow" or "v2" 사용 가능
      protocol: tensorflow
      predictors:
        - name: model
          # The number of replicas for the model serving
          # model serving replicas 개수
          replicas: 1
          graph:
            # model name
            name: my-model
            # model frame
            implementation: tensorflow
            # Model address, the previous step will save the trained model to the pvc of my-pvc, so specify the address of the model through pvc://my-pvc
            modelUri: pvc://my-pvc

  # test model serving
  - name: demo-rest-serving
    type: webservice
    # The test service will start after model training is complete
    dependsOn:
      - demo-serving
    properties:
      image: fogdong/color-serving:v1
      # Use LoadBalancer to expose external addresses for easy to access
      exposeType: NodePort # NodePort로 접근정보 open . LoadBalancer type 사용가능
      env:
        - name: URL
          # The address of the model serving
          # model serving value 정보 기입 , URL 정보로 model serving으로 접근 ( k8s ambassador service 통해서 감 )
          # servicename.namespacename.svc.cluster.local
          value: http://ambassador.vela-system.svc.cluster.local/seldon/default/demo-serving/v1/models/my-model:predict
      ports:
        # Test service port
        - port: 3333
          expose: true
```