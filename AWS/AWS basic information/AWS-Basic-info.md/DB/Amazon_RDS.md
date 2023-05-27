# Amazon_RDS ?
what is Amazon RDS ?
## overview
- 관계형 db를 의미한다. ( Relational Database Service )
    - SQL을 쿼리로 가지는 관계형 DB.

## RDS의 이점
- 관리형 서비스
    - DB 프로비저닝 , OS 패치가 자동화
    - 지속적으로 백업 생성
    - DB성능 데시보드에서 모니터링 가능
    - 읽기전용 replicas를 사용해 Read 성능 향상
    - 다중 AZ 설정해서 재해복구 향상
    - 업그레이드 가능
    - 수직확장 , 수평확장 가능
    - storage는 EBS에 저장됨
        - storage또한 오토스케일링 할 수 있음
        - 임계값을 정해서 해당 조건에 만족하면 오토스케일링됨
- **그러나 , RDS에 SSH 엑세스할 수 없음**

## RDS 종류
- RDS에서 생성이 허용되는 DB는 다음과 같다.
1. PostgreSQL
2. MySql 
3. MariaDB 
4. Oracle 
5. Microsoft SQL Server 
6. Aurora (AWS DB)

## RDS Read Replicas
- 비동기식 복재되어 생성됨
    - MasterDB에 변경사항 생겨도 반영 x
- 최대 15개까지 확장 가능
    - AZ , Region을 걸쳐서 생성 가능
        - 같은 Region에 존재하면 Free , 다른 Region에 존재하면 비용발생
- 읽기만 가능해서 , select문만 수행 가능
- 만들어진 Read Replicas를 DB로 승격시켜서 사용할 수 있음
    - 승격된애는 자체적인 생명주기를 가짐

## RDS Multi AZ (재해복구용 Replicas)
- 동기식 복제되어 생성됨
    - Standby , Master 형태로 복제
    - MasterDB에 변경사항 생기면 , 동기식이라 Standby에도 변경
    - Standby DB는 독립된 DNS 이름 가짐
- **Read Replicas도 Multi AZ가 될 수 있다**

## RDS Custom
- 원래 RDS는 , RDS가 설치된 OS에 ssh 접근할 수 없는데 ,  **Oracle이나 MSSQL은 가능하다.**
- ssh 접근해여 가능
    - 관리권한 전체를 가짐 
    - RDS가 깔린 OS 도 접근가능


## Aurora
- AWS 기술 
    - 오토스케일링 기능 enable하면 , 오토스케일링됨
    - PG와 MySQL에 호환되게 만듬
        - PG나 MySql에 연결하면 작동함
- Storage
    - 자동확장됨. 10GB부터 시작하는데 , 128 TB까지 데이터가 자동 확장됨
- Read Replica
    - 15개 가짐
    - 복제속도 빠름
- 장애조치
    - 장애조치가 즉각적이고 클라우드 네이티브라 HA
    - 30초 이상 MasterDB가 작동안하면 , 장애조치됨
- 데이터 하나가 망가지면 자기혼자 자가복제 가능
- Multi Az처럼 작동
    - MasterDB , ReadOnly DB인 StandbyDB 존재
    - StandbyDB들은 ReadOnly로 작동하며 , 복제됨
- Writer Endpoint , Reader Endpoint 두가지를 제공. 
    - 단일진입점을 제공함으로써 , 많은 ReadonlyDB의 로드벨런싱 제공

### Aurora - custom endpoint
- 특정 Replica DB들의 리소스가 더 크다면 , custom endpoint를 생성해서 그쪽에서 읽기를 수행할 수 있음.

### Aurora - Multi Master
- writer node를 여러개 두어서 , 즉각적인 장애조치가 가능하게끔 구성할수 있음


# Backup && Restore
## RDS Backup
- 자동 Backup
    - 매일 일정 기간에 DB전체 백업
        - 5분마다 트랜잭션 로그를 백업함
        - **가장 최신 백업이 5분전이라는 의미**
    - 1일에서 35일까지 백업 저장기간을 지정할 수 있음
- 수동 Backup
    - 사용자가 수동으로 사용해야함
    - 사용자가 원하는만큼 snapshot을 저장해 둘 수 있음

## Aurora Backup
- 자동 Backup 
    - 1~35일 기간으로 저장할 수 있으며 , **비활성화 불가능**
    - 정해진 시간 내의 어느 시점으로도 복구 가능
- 수동 Backup
    - 사용자가 수동으로 사용해야함
    - 정해진 시간 내의 어느 시점으로도 복구 가능

## Restore
- RDS 및 Aurora 백업 && 스냅샷
- S3부터 MySQL RDS를 Restore
- S3부터 Mysql cluster를 Restore
