# EBS_EC2_Instance_storage
## EBS ?
EC2 인스턴스에 연결되어 사용되는 스토리지

EC2 인스턴스를 초기 생성할 때 설정하여 만들어 줄 수 있고 , 따로 생성해서 붙일 수 도 있다.

따로 생성하여 붙일땐 EBS를 Attach 한 이후 EC2 인스턴스로 접속하여 설정해줘야하는 부분이 있다. 아래 공식 문서 참고하여 작업하면 된다.
- https://docs.aws.amazon.com/ko_kr/AWSEC2/latest/UserGuide/ebs-using-volumes.html

EBS는 EBS 자신이 위치한 AZ안에 있는 인스턴스에만 연결할 수 있다.

## EBS Snapshot
EBS Snapshot은 EBS 볼륨의 특정 시점에 대한 백업 이다.

EBS를 ECS 인스턴스에 분리시킬 필요는 없지만 , 분리하는것이 권장된다.

EBS Snspshot을 다른 AZ나 리전에 복사하여 사용할 수 도 있다.
- 해당 기능을 사용해서 EBS 볼륨을 다른 AZ로 전송할 수 있다.

### EBS Snaphsot 기능들
1. EBS Snapshot Archive
    - 최대 75%까지 저렴한 archive tier로 EBS Snapshot을 옮길 수 있다.
    - 옮기는데에는 24시간에서 최대 72시간 소요
2. EBS Snapshot 휴지통 ( Recycle Bin for EBS Snapshot )
    - EBS 스냅샷을 영구 삭제하지 않고 , 휴지통에 넣어놓는다. 
      복구가 가능하다.
    - 보관기간은 1일에서 1년사이로 지정 가능
3. Fast Snapshot Restore (FSR)
    - 스냅샷을 완전 초기화하여 첫 사용에서의 지연 시간을 없애는 기능.
    - 스냅샷 용량이 아주 크거나 EBS 또는 EC2 인스턴스를 빠르게 초기화 해야 할 때 유용하다.
    - 비용이 비싸니까 주의
