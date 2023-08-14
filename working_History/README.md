# OverView
해당 폴더는 kakao icloud를 통해 플랫폼 개발 과정중 발생한 기술적 이슈 사항들에 대한 고찰 및 해결 방안을 정리한 폴더 입니다.

## repository RoadMap
### S3 Presigned URL
object storage에 저장된 대용량 파일을 , 접근권한을 가진 사용자만 접근하여 업로드 및 다운로드가 가능해야 했습니다.

따라서 , AWS의 S3 Endpoint를 presigendURL로 생성하여 , 미리 권한을 부여하고 , 설정된 시간만큼만 접근이 가능하게끔 개발하였습니다.
- [PresgiendURL 관련 정보 (dependency등)에 대한 README](./BackEnd_Spring/awsS3PresignedURL/README.md)
- [PresgiendURL Code_aws일 경우](./BackEnd_Spring/awsS3PresignedURL/src/main/java/com/presignedurl/awss3presignedurl/logic/aws/)
- [PresgiendURL Code_openstack object storage swift service일 경우](./BackEnd_Spring/awsS3PresignedURL/src/main/java/com/presignedurl/awss3presignedurl/logic/openstackSwift/)

>카카오의 icloud에선 , presignedURL을 사용하여 개발하려고 하였으나 , icloud에서 제공하지 않고있는 제약사항이 있었음.
>
>icloud가 openstack 기반으로 되어있음을 깨달았고 , keystone에 swift (object storage service) 유저를 생성하여 개발하려 했으나 (openstack4j 라이브러리 사용), 보안상 이유로 실패
>
>***X-Auth-Token*** 값을 spring에서 keystone (kakao icloud keystone service) 에 요청하여 발급받은 후, FrontEnd에서 해당 토큰으로 object storage에 인증하는 방식으로 우회하여 개발하였음.

### 전체 api return json을 통일하는 방안 (에러 포함)
BackEnd SpringBoot에서 발생한 error 및 데이터 response를 공통 관리하고 , 규격을 통일하기 위해 controller의 response Domain을 통일하였으며 , Error 또한 ```@RestControllerAdvice``` 및 ```@ExceptionHandler```을 통해 에러 response를 통일하였습니다.

또한 custom Exception 처리에 , Exception Enum Class를 미리 정의하여 에러 코드 및 메세지를 통합 관리 하였습니다.

- [에러 공통처리 code 및 사용방안 README](./BackEnd_Spring/awsS3PresignedURL/src/main/java/com/presignedurl/awss3presignedurl/Error/)
- [정상 로직 수행 시 controller code 및 공통처리 사용방안 ](./BackEnd_Spring/awsS3PresignedURL/src/main/java/com/presignedurl/awss3presignedurl/commonResponse/)

### 영상 및 이미지 처리에 대한 고찰
s3 object storage에 영상 && 이미지를 저장해야하는 상황이며 , 해당 영상 또는 이미지의 해상도나 영상이라면 영상의 길이 등의 메타데이터를 따로 RDB에 저장해야 했습니다.

아래는 관련 방안에 대해 study한 결과가 모여있는 디렉터리 입니다.
- [best architecture](#best-architecture) 를 기반으로 콘텐츠를 어떻게 관리해야할지 study 하였으며 , 차후 실 운영단계에서는 아래와 같은 변경사항이 예상됩니다.
    - [code](./contentsManage/)

>**변경사항**
>
>1. backEnd -> nodejs 에서 spring Boot Application 변경
>
>2. lambda 사용 못하기 때문에 , 람다함수를 boot application으로 대체
> 
>3. presignedURL 사용 못하기 때문에 , Temp URL로 변경하거나 , x-auth-token을 통해서 관리

#### Best Architecture 
AWS 일 경우 , 

1. client가 업로드 요청하면 , Lambda or 특정 Backend 서버에 presigned URL 생성 요청
2. FE 측에서 callBack 요청으로 보냈기에 , response 받은 presigned URL로 contents S3에 저장
3. S3에 저장 트리거를 걸고있는 Lambda function이 동작하면서 , S3에 저장된 contents 메타데이터 추출
4. 추출 후 RDB에 메타데이터 저장

![Best Arch](./Images/bestArch.jpeg)

#### Lambda , presignedURL을 사용할 수 없을 경우

1. client가 세그먼트별로 영상을 쪼개서 Backend로 영상 저장
    - slo 방식으로 저장했기 때문에 , 각 세그먼트와 세그먼트 메니페스트 파일 두개를 s3에 저장
2. [ffmpeg-cli-wrapper 오픈소스](https://github.com/bramp/ffmpeg-cli-wrapper) 사용하여 , 해당 코드 올라가는 서버에 ffmpeg 설치 후 영상 저장하여 메타데이터 추출 후 RDB 저장
    - [테스트 코드 위치](./BackEnd_Spring/ffmpegTest/src/main/java/com/ffmpeg/ffmpegtest/service/ffmpegCli.java)

![worstArch](./Images/worstArch.jpeg)


### 영상 및 이미지 스트리밍과 빠르게 확인할수 있게끔 하는 CDN 관련 고찰
CDN을 통해 메인 페이지 이미지들을 빠르게 받아볼 수 있도록 해야 함

- TEST 결과
>1. S3 bucket 또는 Domain과 연결해서 , 연결된 곳의 캐시를 먹일 수 있음.
>2. 캐시 저장 유효기간을 정할 수 있는데 , 만약 실제 스토리지에 해당 데이터가 삭제되었더라도 , 유효기간이 지나지 않으면 캐시를 새로 받아오지 않기 때문에 반영되지 않음.
>3. origin path를 지정해서 , object storage에 특정 path에만 접근할 수 있게끔 설정할 수 있음. 
>   - 나머지 데이터들은 지킬 수 있다.
>4. https 접근 가능하도록 적용 가능