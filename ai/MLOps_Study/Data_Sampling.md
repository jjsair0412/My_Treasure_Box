# Data Sampling
## Data Sampling이란 ?
큰 데이터 집합에서 작은 부분집합을 추출하는 프로세스,

데이터의 일부를 조사하고 전체 데이터 집합에 대한 결론을 도출하는데 활용함.

전체 데이터 셋에 대한 통찰력을 얻거나 계산 및 저장공간을 줄이는데 도움이 됨.

## Data Sampling 의 방법들
### 1. Random Sampling
데이터셋에서 무작위로 샘플을 선택하는 방법. 각 데이터 포인트가 선택될 확률이 동일하기 때문에, 편향이 적게 되는 데이터 샘플을 얻을 수 있음.

그러나 잘못된(불균형) 데이터셋에서 Random Sampling 기법을 적용하면, 잘못된 데이터들이 그대로 데이터샘플로 넘어오기떄문에 단점이 명확함.

### 2. Stratifed Sampling
데이터를 계층적으로 분류한 이후, 각 계층에서 샘플을 추출하는 방안. 각 계층의 특성을 고려하여 샘플을 얻기 위해 사용함.
- ex) 남성과 여성의 성별에 따라 샘플을 추출할 때 사용

### 3. Cluster Sampling
데이터를 여러 그룹 또는 여러 Cluster로 나누고, 몇 개의 Cluster를 무작위로 선택한 이후, 선택된 Cluster 내의 모든 데이터를 포함하는 방법.
- Cluster로 나눈 뒤, 각 Cluster별로 Stratifed Sampling 기법을 도입하여 추출하는 경우도 있음.

데이터가 고루 분포되지 않은 경우에 유용하며, 데이터가 Cluster로 그룹화 될 때 사용함.

### 4. Weight Sampling
데이터 포인트에 가중치를 할당하여, 가중치 기반으로 샘플을 추출하는 방법.

가중치가 높은 데이터는 더 중요하기에, 중요한 데이터는 더 자주 선택될 가능성이 높음.

불균형 데이터 분포를 가질경우 활용됨.

### 5. Importance Sampling
확률분포에 기반한 샘플링 기법. 베이지안 추론, 몬테카를로 시뮬레이션 또는 결합 확률분포 기법 등의 방법으로 샘플링 수행

## Data Sampling 고려 사항
### 1. 편항 & 오차관리
특정 데이터 세트에 대해 과소 또는 과대표현이 일어나지 않도록 샘플링을 선정해야됨. 편향되거나 오차가 너무크면 안됨.

### 2. Data Sampling 샘플 크기 및 신뢰 수준
Data Sampling에서 샘플 크기와 신뢰 수준은 중요한 결정 사항임.

통게적 방법을 사용하여 샘플 크기 및 신뢰 수준을 결정해야함.