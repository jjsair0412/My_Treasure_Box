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

## 2. sample code
```yaml
apiVersion: core.oam.dev/v1beta1
kind: Application
metadata:
  name: training-serving
  namespace: ml
spec:
  components:
  # Train the model
  - name: demo-training
    type: model-training
    properties:
      image: fogdong/train-color:v1
      framework: tensorflow
      storage: # storageclass 정보 입력 가능 ? 테스트 필요
        - name: "my-pvc"
          mountPath: "/model"
  
  # Start the model serving
  - name: demo-serving
    type: model-serving
    # The model serving will start after model training is complete
    dependsOn:
      - demo-training
    properties:
      # The protocol used to start the model serving can be left blank. By default, seldon's own protocol is used.
      protocol: tensorflow
      predictors:
        - name: model
          # The number of replicas for the model serving
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
      exposeType: NodePort # NodePort로 접근정보 open
      env:
        - name: URL
          # The address of the model serving
          value: http://ambassador.vela-system.svc.cluster.local/seldon/default/demo-serving/v1/models/my-model:predict
      ports:
        # Test service port
        - port: 3333
          expose: true
```