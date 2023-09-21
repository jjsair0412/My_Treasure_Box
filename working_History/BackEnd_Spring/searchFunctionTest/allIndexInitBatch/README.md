# spring batch server
elastic search에 RDB 데이터를 특정 주기에 넣어주는 배치 API
>해당 배치 API는 RDB의 모든 데이터를 특정 주기에 넣어줍니다.

## overview
대상 버전은 다음과 같습니다.
>elasticsearch 와 logstash 는 무료 버전인 7.10.0 을 사용합니다.
> 
>7.10.2 버전부터 elasticsearch 가 유료로 변경되었습니다.

|               |         |    |
|---------------|---------|----|
| name          | version | 비고 |
| ElasticSearch | 7.10.0  | -  |
| logstash      | 7.10.0  | -  |

## Repository RoadMap
- [SpringBatch의 동작과정 아키텍처 설명](./SpringBatch_사용방안.md)
- [ElasticSearch indexing 사용방안_SpringBatch 상세 코드 사용방안](./사용방안.md)