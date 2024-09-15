# Terraform Data Resource
## Overview
AWS와 같은 특정 Provider를 Terraform으로 관리할 경우, Terraform은 data source를 제공하는데 해당 데이터는 ***동적 정보를 제공합니다..***

이러한 동적 정보를 Terraform에서 쉽게 출력할 수 있는데, 해당 방법에 대해 기술하였습니다.
- [관련 공식문서](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ip_ranges)

## 1. 사용 사례
AWS는 API를 사용하여 구조화된 형식으로 많은 데이터를 노출시킬 수 있습니다.

이러한 정보들은 변경될수 있는 정보, 즉 동적 정보를 의미합니다.

여기에는 AMI 목록이나 AWS에서 사용중인 모든 IP 주소 등이 포합됩니다.

따라서 이를 잘 이용하면, Terraform에서 지역 기반 트래픽 필터링 등의 기능을 수행할 수 있습니다.
- 예를들어 ***특정 리전 목록의 IP 주소만 허용/비허용 하고 싶다*** 가 됩니다.
- security group을 Terraform으로 생성하여 해당 요구사항을 처리합니다.


```json
data "aws_ip_ranges" "european_ec2" {
  regions  = ["eu-west-1", "eu-central-1"] // 데이터 소스의 input, eu-weast-1 , eu-central-1 region 설정, 해당 리전에 할당된 IP 범위만 조회
  services = ["ec2"] // ec2 서비스에 할당된 IP 범위만 가져오도록 지정
}

resource "aws_security_group" "from_europe" {
  name = "from_europe"

  ingress {
    from_port        = "443"
    to_port          = "443"
    protocol         = "tcp"
    cidr_blocks      = data.aws_ip_ranges.european_ec2.cidr_blocks // 선택한 지역의 ipv4 주소의 443 포트를 허용
    ipv6_cidr_blocks = data.aws_ip_ranges.european_ec2.ipv6_cidr_blocks // 선택한 지역의 ipv6 주소의 443 포트를 허용
  }

  tags = {
    CreateDate = data.aws_ip_ranges.european_ec2.create_date // IP 범위 데이터가 마지막으로 업데이트된 날짜를 태그로 지정
    SyncToken  = data.aws_ip_ranges.european_ec2.sync_token // IP 범위 데이터 버전을 나타내는 동기화 토큰을 태그로 지정
  }
}
```