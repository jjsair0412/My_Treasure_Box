# OverView
해당 폴더는 kakao icloud를 통해 플랫폼 개발 과정중 발생한 기술적 이슈 사항들에 대한 해결 방안을 정리한 폴더 입니다.

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

