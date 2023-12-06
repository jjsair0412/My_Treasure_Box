# AWS Architecture
해당 Repository는 ```배워서 바로 쓰는 14가지 AWS 구축 패턴``` 책을 기반으로 study하며 작성한 문서를 저장해둔 Repository 입니다.

## Repository RoadMap
### 1. Enterprise WEB
AWS의 Object Storage인 (S3) Service와 CDN (CloudFront) 서비스를 활용하여 정적 콘텐츠의 응답시간을 줄이고 웹 서버에 접근하는 빈도수를 낮춤으로써 클라우드 비용을 아끼고, RDS를 사용하여 AZ별 DB를 Standby, Prod로 나눈 아키텍처 입니다.
- [Enterprise_Link](./Enterprise_WEB/)

### 2. 특정일 부하가 높은 서버 : Intranet
회사 인트라넷 과 같은 특정 업무시간이나 특정 일자에 웹서버 접근 빈도수가 높은 시스템의 아키텍처.

ELB -> Auto Scaling Group , Amazon Aurora , ElasticCache 를 사용하여 데이터 트랜잭션에 걸리는 시간을 줄이고, 특정 임계치를 지나면 EC2가 스케일 아웃 되는 아키텍처

- [Intranet_Link](./특정일_부하가높은_서버_%20intranet/)

### 3. 높은 가용성 달성하기
AWS AZ에 장애가 생기더라도, 서비스가 정상 작동하도록 하는 높은 가용성이 보장된 아키텍처.

고비용 아키텍처이기 때문에, 필요에 있어 사용해야함.

- [HA_Arch](./높은_가용성_달성하기/)

### 4. 온프레미스 환경의 데이터를 AWS에 백업
클라우드로 전환하기 위해 , 혹은 타 클라우드에서 AWS로 전환하기 위해 AWS의 S3 및 글레이셔에 데이터를 주기적으로 백업해두는 아키텍처.

- [Backup](./온프레미스_환경의_데이터_AWS백업/)