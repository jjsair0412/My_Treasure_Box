# ML Algorithm 기반 방법론
모델 tunning 및 parameter 조정은 , 머신러닝 모델의 성능을 최적화하고 Class Imbalance 데이터에 대응하기 위한 중요 전략 중 하나임.

방법은 다양하지만, 알맞는 모델 알고리즘 선택, 가중치 조정, 임계값 조정, 적절한 Metric 사용, Hyper-parameter Tunning, Validataion, Ensenble 등 많은 방법이 있음.

## 1. Model Algorithm 선택
클래스 불균형 문제에 민감한 모델 알고리즘을 선택한다.

예를들어 Binary Classification에서는 SVM , Random Forest , Gradient Boosting 과 같은 모델이 좋은 성능을 보이기에 이들을 선택하는것이 좋음. 

## 2. Class Weight 조정
모델의 Loss Function에 class weight를 부여하여, 소수 클래스에 더 높은 가중치 (weight) 를 할당하는것,

모델은 소수클래스를 더 주요하게 생각하고 학습을 진행함.

## 3. Threshold 조정
모델 예측 Threshold 값, 즉 임계값을 조정함으로써 정밀도(precision)와 재현율(recall) 사이의 균형을 조절하여 클래스 불균형 문제를 다룸.

## 4. 적절한 Metric 사용
Accruacy가 일반적으로 소수 클래스에서 부적절하기 때문에, 다른 Metric을 기반으로 모델을 최적화 한다.

## 5. Hyper-parameter Tunning
모델의 Hyper Parameter를 조정하여 최적 모델을 찾는다.

## 6. Validation
Cross Validation을 통해 모델 일반화 능력을 평가하고, 클래스 불균형 문제에 대해서도 성능 신뢰성을 확인할 필요가 있음.

## 7. Ensemble 활용
다양한 모델을 경합하는 앙상블 기법을 사용하여 클래스 불균형 문제를 다룬다.
