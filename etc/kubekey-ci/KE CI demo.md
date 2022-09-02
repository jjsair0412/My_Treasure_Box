# KE CI demo
## 1. Prerequisites
- 해당 문서는 KE의 GUI를 통해 ci 파이프라인을 생성하는 과정에 대해 기술합니다.
- ci 대상 소스 코드 정보는 아래와 같습니다
	- https://gitlab.kuberix.co.kr/kimkh211/devops-maven-sample
	- maven
	- jdk:8
	- image registry : harbor.kuberix.co.kr
		- id : admin 
		- pwd : P@88w0rd
		- project : devops_maven_sample_1
- KE 설치 시 DevOps project를 enable 하였다고 전제 합니다.
- ci 대상이 될 workspace가 이미 생성되어 있다고 전제 합니다.
## 2.  Create targets
### 2.1 Create DevOps Project || moving ci target workspace
- 먼저 DevOps Project를 생성하기 위해 , cI 대상이 될 workspace로 이동합니다.
- 해당 문서에서는 kuberix workspace를 사용합니다.

![select_workspace][select_workspace]

[select_workspace]:./images/select_workspace

![select_workspace-1][select_workspace-1]

[select_workspace-1]:./images/select_workspace-1

### 2.2 Create DevOps Projects
- 좌측 탭의 DevOps Projects를 클릭하여  DevOps Projects들이 있는 곳으로 이동합니다.

![move_devops_prj-1][move_devops_prj-1]

[move_devops_prj-1]:./images/move_devops_prj-1.PNGPNG
- create 버튼을 클릭하여 DevOps Projects를 생성합니다.
	- Name과 cicd대상이 될 Cluster를 선택하는 것은 required 옵션입니다.
	- 해당 문서에서는 아래와 같이 사용합니다.
		- Name : ci-pipeline
		- Cluster Setting : host ( Host cluster )
  	- 옵션 작성이 완료됐다면 Ok버튼을 눌러 DevOps project를 생성합니다.

![create-devproj][create-devproj-1]

[create-devproj-1]:./images/create-devproj-1.PNG


![create-devproj-2][create-devproj-2]

[create-devproj-2]:./images/create-devproj-2.PNG

### 2.2 Setting DevOps Pipeline
- 직전에 만들어준 ci-pipeline 이름의 DevOps Projects로 이동합니다.

![move_ci-pipeline_prj-1][move_ci-pipeline_prj-1]

[move_ci-pipeline_prj-1]:./images/move_ci-pipeline_prj-1.PNG

-  좌측 탭의 pipelines로 이동한뒤 create 버튼을 클릭합니다.
-  Name만 작성한 뒤 Next 버튼을 클릭합니다. 해당 문서에서는 아래와 같은 Name을 사용합니다.
	-  Name : pipeline-ci-demo

![pipeline-setting][pipeline-setting]

[pipeline-setting]:./images/pipeline-setting.PNG

- pipeline 세부 설정을 진행 합니다.
	- ***Build Setting*** : Build Records 삭제 기한 및 최대 Build Records 개수를 지정합니다. 또한 동시 빌드 허용 여부를 설정합니다.
	- ***Build Parameters*** : pipeline에 등록할 변수를 지정합니다. 해당 문서에서는 private registry인   harbor를 사용했기에 변수를 아래와 같이 등록합니다.
		- Add 버튼을 클릭하여 등록하며 , type은 모두 string으로 통일합니다.
			- Name : REGISTRY  , 
			   value : harbor.kuberix.co.kr
			- Name : DOCKERHUB_NAMESPACE 
			  value : devops_maven_sample_1
			- Name : APP_NAME 
			   value : devops-maven-sample_1

![pipeline-setting-1][pipeline-setting-1]

[pipeline-setting-1]:./images/pipeline-setting-1.PNG

- 구성이 완료되었다면 ,  Create 버튼을 클릭하여 생성합니다.

![pipeline-setting-2][pipeline-setting-2]

[pipeline-setting-2]:./images/pipeline-setting-2.PNG

-  생성 결과 확인합니다.

## 3. pipeline 
### 3.1 create DevOps Pipeline
- 이전에 생성해두었던 pipeline으로 이동합니다.

![pipeline-setting-3][pipeline-setting-3]

[pipeline-setting-3]:./images/pipeline-setting-3.PNG

- pipeline을 생성합니다.
- 이전에 만들어준 pipeline을 클릭하여 접속하고 , Edit Pipeline을 클릭하여 gui로 파이프라인을 구성합니다.

![go-pipe][go-pipe]

[go-pipe]:./images/go-pipe.PNG

- 배포 대상 application은 maven 파일이기 때문에 template을 maven으로 선택하여 만들어지는 template sample 을 바탕으로 구성하거나 , custom을 통해서 처음부터 pipeline을 구성합니다.
- 해당 문서에서는 작성 예시를 위해 Custom pipeline으로 진행합니다.
- next를 클릭하고 바로 Create 합니다.

![go-pipe-1][go-pipe-1]

[go-pipe-1]:./images/go-pipe-1.PNG

### 3.2 DevOps Pipeline detail setting 
- 먼저 사용 방안에 대해 기술합니다.
- 기본적으로 파이프라인을 생성할 때 . + 버튼을 클릭하여 stage를 추가할 수 있습니다.

![go-pipe-2][go-pipe-2]

[go-pipe-2]:./images/go-pipe-2.PNG

- 스테이지 내부에서는 Name 옵션을 통해 stage의 이름을 지정하고 , Agent타입 또한 4가지를 선택할 수 있습니다.
	- Any
	- node
	- kubernetes
	- none

![go-pipe-3][go-pipe-3]

[go-pipe-3]:./images/go-pipe-3.PNG

- conditions 옵션을 통해 해당 stage 조건을 선택합니다. ( option 이기 때문에 비워놓아도 문제가 없습니다. )

![go-pipe-4][go-pipe-4]

[go-pipe-4]:./images/go-pipe-4.PNG

- task에서 해당 stage가 무슨 작업을 할지 지정합니다.

![go-pipe-5][go-pipe-5]

[go-pipe-5]:./images/go-pipe-5.PNG

- stage들의 하단에 Add Parallel Stage 버튼을 클릭하면 , 병렬로 수행하는 stage를 구성할 수 있습니다.

![go-pipe-6][go-pipe-6]

[go-pipe-6]:./images/go-pipe-6.PNG


### 3.3 configuration pipeline
#### 3.3.1 pipeline workflow
- 위에 설명을 바탕으로 파이프라인을 구성합니다.
- 생성할 실제 파이프라인의 프로세스는 다음과 같습니다.
1. git clone 
2. build
3. harbor push

#### 3.3.2 configuration pipeline
***1. git clone***
- 첫번째 stage에서는 gitlab에 올라가있는 소스 코드를 clone 합니다.
- stage name을 Checkout SCM 으로 변경한 뒤 , step을 git으로 설정 합니다.
- 그 후 url과 branch 정보를 아래와 같이 구성합니다.
	- Url : http://gitlab.kuberix.co.kr/kimkh211/devops-maven-sample.git
	- Branch : master

![go-pipe-7][go-pipe-7]

[go-pipe-7]:./images/go-pipe-7.PNG

- OK 버튼 클릭하여 stage 생성합니다.

***2. build***
- + 버튼을 클릭하여 stage를 생성합니다.
- name은 Build로 지정하고 , step은 container로 지정합니다.
- maven 으로 구성되어진 코드이기 때문에 , maven container를 사용합니다. container name은 maven으로 작성한뒤  OK버튼을 클릭합니다.

![go-pipe-8][go-pipe-8]

[go-pipe-8]:./images/go-pipe-8.PNG

- maven container가 생성된것을 확인할 수 있습니다.

![go-pipe-9][go-pipe-9]

[go-pipe-9]:./images/go-pipe-9.PNG

- maven build를 진행해야 하기에 , maven container 하단의 + 버튼을 클릭합니다.
- shell step을 선택하고 , 아래와 같은 스크립트 명령어를 작성한 뒤 OK 버튼을 클릭하여 stage 구성을 완료 합니다.

```
script  mvn clean package -Dmaven.test.skip=true
```

![go-pipe-10][go-pipe-10]

[go-pipe-10]:./images/go-pipe-10.PNG

***3. harbor push***
- build 한 docker image를 private registry 인 harbor에 push하는 stage를 작성합니다.
- 더하기 버튼을 클릭하여 stage를 생성한 뒤 , stage name을 push로 수정합니다.
- maven container에서 스크립트를 수행해야 하기 때문에 , 동일하게 step을 container로 지정후 maven 으로 name을 구성합니다.

![go-pipe-11][go-pipe-11]

[go-pipe-11]:./images/go-pipe-11.PNG

- maven container step 하단 + 버튼을 클릭 후 shell step을 추가합니다.
- 아래와 같은 명령어를 작성한 뒤 ok 버튼으로 구성합니다.

```
script  mvn clean package -Dmaven.test.skip=true
```

![go-pipe-12][go-pipe-12]

[go-pipe-12]:./images/go-pipe-12.PNG

- harbor에 접근하기 위해선 harbor 계정 정보가 필요하기에 , Add nesting steps 버튼을 클릭하여 withCredentials를 선택합니다.

- 그 후 Credentials를 선택해야 하는데 , 미리 만들어 두지 않았기 때문에 create credential 버튼을 클릭하여 생성합니다.

![go-pipe-13][go-pipe-13]

[go-pipe-13]:./images/go-pipe-13.PNG

-  credential을 아래와 같은 정보로 구성합니다.
	- Name : dockerhub-id
	- type : Username and password
	- Username : kuberix
	- Password/Token : P@88w0rd
- 구성 완료 후 ok 버튼을 통해  credential을 생성합니다.

![go-pipe-14][go-pipe-14]

[go-pipe-14]:./images/go-pipe-14.PNG

- 방금 구성한 credential을 선택하고 , 아래와 같은 정보로 구성한 뒤 ok 버튼을 통해 step을 생성합니다.
	- Credential Name : dockerhub-id
	- Password Variable : DOCKER_PASSWORD
	- Username Variable : DOCKER_USERNAME

![go-pipe-15][go-pipe-15]

[go-pipe-15]:./images/go-pipe-15.PNG

- harbor login 스크립트를 작성합니다.
- withCredentials 하단의 Add nesting steps 버튼을 클릭하여 sh step을 생성하고 , 아래와 같은 스크립트를 작성한 뒤 ok버튼으로 구성을 완료합니다. 

```
echo "$DOCKER_PASSWORD" | docker login $REGISTRY -u "$DOCKER_USERNAME" --password-stdin
```

![go-pipe-16][go-pipe-16]

[go-pipe-16]:./images/go-pipe-16.PNG

- harbor push 스크립트를 작성합니다.
- Add step 버튼을 클릭하여 shell step을 선택한 후 , 아래와 같은 스크립트 명령을 구성한 뒤 ok로 step을 생성합니다.

```
script   docker push $REGISTRY/$DOCKERHUB_NAMESPACE/$APP_NAME:SNAPSHOT-$BUILD_NUMBER
```

![go-pipe-17][go-pipe-17]

[go-pipe-17]:./images/go-pipe-17.PNG

- 최종적으로 구성된 파이프라인은 다음 사진과 같습니다.
- OK 버튼을 클릭해 파이프라인을 완성합니다.

![go-pipe-18][go-pipe-18]

[go-pipe-18]:./images/go-pipe-18.PNG

### 3.4 run pipeline
- 생성한 pipeline을 클릭하여 들어간 후 구성을 확인합니다.

![go-pipe-19][go-pipe-19]

[go-pipe-19]:./images/go-pipe-19.PNG

- run 버튼을 클릭하여 pipeline을 실행하는데, parameter 구성을 확인한 후 ok 버튼으로 pipeline을 실행합니다.
