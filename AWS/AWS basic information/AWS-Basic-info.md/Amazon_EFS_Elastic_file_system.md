# Amazon_EFS_Elastic_file_system
## what is EFS ?
관리형 NFS이다 .
즉 , network file system이다.
- 내부적으로 NFS 프로토콜로 작동된다.

여러 EC2 인스턴스에 연결될 수 있고 , 연결될 여러 EC2 인스턴스들은 다른 AZ에 위치해도 된다.
- EFS의 접근 권한이나 포트를 열어주기 위해선 , Security Group을 설정하면 된다.
가용성이 높고 확장성도 높다.

그에 따라 비용도 비싸다.

**중요**
- Window가 아닌 Liunx 기반 AMI와 호환된다.

EFS는 미리 용량을 지정하지 않고 , 사용량에 따라 요금을 지불하는 형식이다.
- EFS에서 사용한 GB에 따라서 용량을 지불한다.

## Use case
웹 서버 , WordPress 등으로 사용할 수 있다.

## EFS 성능
1. EFS Scale
    - 용량을 미리 설정하지 않아도 , 자동확장된다.

## EFS의 다양한 mode
1. Performance mode
    - General mode ( default ) : 지연시간에 민감한 웹 서버에 사용
    - Max I/O : I/O 최대화를 위해 사용 , 지연 시간 , 병렬 처리 기능 향상됨 . 빅데이터등에 적합
2. Throughput mode
    - Bursting mode ( default ) : 사용 공간이 많을수록 버스팅 용량 및 처리량 늘어남 . 파일 시스템이 클수록 용량이 커지고 처리량도 늘어남 . 
    - Provisioned mode : 스토리지 크기에 상관없이 처리량 설정 가능 .


## EFS의 다양한 storage Classes
1. storage Tiers 설정
    - standrad 계층 : 액세스가 빈번한 파일 저장
    - EFS-IA 계층 : 파일 저장 비용이 낮음 . 그러나 EFS-IA 계층에 있는 파일을 검색하면 , 비용 발생 . 액세스가 빈번하지 않은 파일 저장 . 
    파일 액세스 기간 설정하여 , 설정된 기간만큼 해당 파일에 액세스하지 않는다면 EFS-IA로 이동됨

2. 가용성 및 내구성
    - standrad : EFS를 다중 AZ 설정 - 운영계에서 적합.
    - One Zone : 기본적으로 백업기능 활성화됨, EFS-IA 계층과 호환.
    비용을 큰 폭으로 아낄 수 있음. 하나의 AZ에서만 데이터가 저장됨 .
