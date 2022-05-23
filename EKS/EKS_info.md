# EKS info
- EKS 정보 & 설치 방안
## EKS 상태도
![EKS_info_1][EKS_info_1]

[EKS_info_1]:./images/EKS_info_1.PNG

_**EKS는 aws에서 쿠버네티스의 master node ( control plane )**_ 이다.
사진처럼 외부에서 EKS를 조작하는 시스템 ( ubuntu , linux 등 .. ) 에서 kubectl 명령어를 작성하면 ,

EKS로 전달하게되고 , EKS는 만들어준 worker node에 스케줄링 시켜주는 등의 명령어를 수행한다.

## EKS 구성 방법

1.  **AWS 리전중 한가지를 선택한다.**
-   예제에서는 서울 리전을 선택한다.

2.  **kubectl 명령어를 실행할 인스턴스를 한개 생성한다.**
-   생성한 뒤 다운로드할 목록이 몇 가지 있다

-- AWS CLI 관리툴인 aws
-- EKS설치/운영 툴인 eksctl
-- k8s 관리툴인 kubectl

요 **세가지를 설치**해야 한다.

### 외부 시스템에서 aws , eksctl , kubectl 설치

1.  aws 설치방법

[Linux에서 AWS CLI 버전 2 설치, 업데이트 및 제거](https://docs.aws.amazon.com/ko_kr/cli/latest/userguide/install-cliv2-linux.html)

```bash
$ sudo apt-get install -y unzip
# 얘는 zip파일로 압축되어있기 때문에 unzip부터 설치

$ curl "<https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip>" -o "awscliv2.zip"
$ unzip awscliv2.zip # 압축풀기
$ sudo ./aws/install

# 설치 상태 확인
$ aws --version
```

----------

1.  eksctl 설치 방법

[eksctl 명령줄 유틸리티](https://docs.aws.amazon.com/ko_kr/eks/latest/userguide/eksctl.html)

```bash
$ curl --silent --location "<https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$>(uname -s)_amd64.tar.gz" | tar xz -C /tmp
$ sudo mv /tmp/eksctl /usr/local/bin
$ eksctl version # 설치상태 확인
```

----------

1.  kubectl 설치 방법

[kubectl 설치](https://docs.aws.amazon.com/ko_kr/eks/latest/userguide/install-kubectl.html)

```bash
$ curl -o kubectl <https://amazon-eks.s3.us-west-2.amazonaws.com/1.19.6/2021-01-05/bin/linux/amd64/kubectl>
$ chmod +x ./kubectl
$ mkdir -p $HOME/bin && cp ./kubectl $HOME/bin/kubectl && export PATH=$PATH:$HOME/bin
$ echo 'export PATH=$PATH:$HOME/bin' >> ~/.bashrc
$ kubectl version --short --client # 설치상태 확인
```

설치가 완료됐다면 kubectl 명령어를 날릴 시스템 구성은 끝난거다.

이제 EKS를 구성한다.

----------

1.  AWS IAM 생성
![IAM][IAM]

[IAM]:./images/IAM.PNG

위처럼 AWS IAM 서비스를 선택한다.

![IAM-2][IAM-2]

[IAM-2]:./images/IAM-2.PNG

사용자 탭을 누른 후 , 사용자 추가를 클릭한다.

![IAM-3][IAM-3]

[IAM-3]:./images/IAM-3.PNG
사용자 이름과 자격 증명 유형을 선택한다.

![IAM-4][IAM-4]

[IAM-4]:./images/IAM-4.PNG
권한을 선택해주는데 , 상황에 따라 다르겠지만 실습용 으로는 AdministratorAccess 권한을 설정해줬다.

이후 태그를 설정하고 , 사용자를 생성한다.

### 중요 !!

사용자가 생성되면 , 액세스ID와 액세스 키가 발급 되는데, 이거 잊어버리거나

git에 올리면 절대 안됀다.

따로 csv 파일로 다운로드해서 안전한곳에 보관해두자.

----------

1.  Bastion Host(ubuntu)에서 aws 관리할수 있도록 aws 계정(eks-mng-user) 등록

아까 만들어준 시스템 ( ubuntu ) 에서 aws를 관리할 수 있도록 계정을 등록한다.

```bash
$ aws configure
  AWS Access Key ID [None]: # 액세스 ID 입력
  AWS Secret Access Key [None]: # secret key 입력
  Default region name [None]: ap-northeast-2 # 인스턴스가 위치한 리전 이름 입력. 지금은 서울
  Default output format [None]: json # 그냥 엔터눌러도 되는데, 누르면 확인할 때 unkown 에러뜸. json 써주자

# 연결이 잘 됐는지 확인
$ aws sts get-caller-identity
{
    "UserId": ".,..VY4",
    "Account": "....47",
    "Arn": "arn:aws:iam::953938112747:user/eks-mng-user"
}

# 요런식으로 출력되면 연결 성공.
```

----------

1.  EKS 구성

[Amazon EKS 시작하기 - eksctl](https://docs.aws.amazon.com/ko_kr/eks/latest/userguide/getting-started-eksctl.html)

※ EKS는 시간당 0.01$가 나간다. **실습 끝나면 무조건 꺼주자** -> EKS에서 **설정한 노드개수만큼 인스턴스가 생성**되는데 , -> 마치 쿠버네티스의 deployment처럼 **auto scalling 갯수를 계속 맞춰주기 때문에** -> 각 **worker node를 중지시키더라도 다시 생성**되게 된다. -> 그니까 **중지됐다고 과금이 멈추는것이 아니기때문에 , EKS를 다 실습했다면 아예 EKS를 삭제시켜주어야 한다.,**

eksctl 명령어를 실행해서 EKS 클러스터를 생성해준다.

```bash
$ eksctl create cluster \\
    --name k8s-demo \\ # EKS 이름
    --region ap-northeast-2 \\ # 리전 이름
    --with-oidc \\ 
    --ssh-access \\
    --ssh-public-key aws-login-key \\ # key 이름
    --nodes 3 \\ # 노드 개수
    --node-type t3.medium \\
    --node-volume-size=20 \\ # volume size 설정
    --managed
```

20분정도 걸린다. 다되면 리전에서 워커노드들이 설정한 개수만큼 생성된 것을 볼 수 있다.

----------

설치 후 , kubectl 명령어를 입력해서 아래처럼 노드들이 출력되는걸 확인하면 끝

```bash
ubuntu@ip-172-31-42-161:~$ kubectl get nodes
NAME                                                STATUS   ROLES    AGE   VERSION
ip-192-168-47-140.ap-northeast-2.compute.internal   Ready    <none>   26m   v1.21.5-eks-9017834
ip-192-168-8-141.ap-northeast-2.compute.internal    Ready    <none>   26m   v1.21.5-eks-9017834
ip-192-168-82-207.ap-northeast-2.compute.internal   Ready    <none>   26m   v1.21.5-eks-9017834
```

----------

## EKS 삭제하는 명령어
- eks 환경을 제거한다.
```bash
$ eksctl delete cluster --name k8s-demo
```

----------

## 참고

그리고 지금 kubectl 명령어를 작성하는 관리장비는 , eks와 worker node들이 묶여잇는 부분의 바깥이다.

외부접속을 하고있는것이다.

따라서 파드를 생성했다고 하더라도 ,해당 관리장비에서는 ingress를 만들어주기 전까지 파드에 접근할 수 없다.

파드에 접근하고싶다면 , EKS 인스턴스중 하나로 들어가서 접근해야 한다. 들어가는 방법은 ,
aws gui에서 eks 의 pulic ip를 확인하고 ip주소와 key를 넣어주면서 들어가면 된다.