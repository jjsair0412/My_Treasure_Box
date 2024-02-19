# 데이터 접근 방법론 (Resampling)
## Over Sampling
### 1. SMOTE (Synthetic Minority Over-sampling)
소수 클래스 데이터 포인트들을 이용해서 새로운 데이터 를 생성하여 데이터셋을 균형화 하는 방법

생성되는 데이터 포인트는 소수 클래스 데이터 포인트 한개를 특정짓고 특정된 소수 데이터 포인트의 주변 소수 데이터 포인트를 랜덤하게 선택함. 이후 그들 사이에서 소수 클래스 데이터포인트를 생성함.

다양한 데이터 생성이 가능하고 모델의 일반화능력을 키울 수 있지만, 데이터 중복이 발생될 수 있다는 단점을 가짐.

- [관련 알고리즘 구현 문서](https://kjhov195.github.io/2019-12-27-SMOTE/)

## Under Sampling

## Combined Sampling