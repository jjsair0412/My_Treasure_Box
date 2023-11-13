# VPC Flow Logs
## Overview
VPC Flow Log를 사용하면, AWS 인터페이스로 들어오는 IP 트래픽을 감시하고, 해당 트래픽을 통해 네트워크 문제를 해결할 수 있습니다.

VPC 수준이나, 서브넷, Elastic Network 수준에서 로그를  포착할 수 있습니다.

또한 해당 로그를 Amazon S3, CloudWatch Logs, Kinesis Data Firehose에 전송할 수 있습니다.

## 사용방안
### 1. 트러블슈팅
만약 EC2 Instance에 Inbound 트래픽은 Accept인데, Outbound만 Reject라면, NACL에서 막혔다는것을 알 수 있습니다.
- 반대의 상황또한 동일
- NACL은 Stateless라서 Inbound와 Outbound의 허용, 거부를 따로 줄 수 있음.
- 보안그룹은 Stateful이라서 한번 들어왔으면 무조건 나갈 수 있고, 한번 나갓다면 무조건 들어올 수 있음.


### 2. CloudWatch와 연동
VPC Flow로그들을 CloudWatch Logs에 넣어놓고 가장 많이 접근한 Top 10 IP Addr을 꺼내서 확인하는 등의 모니터링을 수행할 수 있습니다.

또한 Cloudwatch에 특정 메트릭에대한 필터를 걸어서, (SSH 프로토콜 등) CloudWatch Alarm을 트리거하여 Amazon SNS를 통해 특정 프로토콜이 수행되면 경보를 수행하도록 구성할 수 있습니다.

또한 Flow Logs를 S3 Bucket에 적제하여 Amazon Athena를 사용하여 SQL로 된 VPC Flow Logs를 분석할 수 도 있습니다.

