# IAM
## IAM ?
계정을 만들면 , root 권한이 자동으로 부여됨-   user별 관리 서비스 접근 권한 부여 서비스이다.
-   Global sservice이기에 리전에 영향을 받지 않는다.
    -   user끼리 group을 지을 수 있다.
    -   한 user는 여러 group에 존재할 수 있다.
    -   group이 없는 user도 가능하다. ( 추천되지 않음 )
	    - group이 없는 user에게는 inline 정책만을 부여할 수 있다.
    -   **group에는 user만 포함될 수 있다. group끼리 group을 지을 수 없다.**

## IAM : Permissions
IAM은 Json 형태의 documents로 정책을 생성하여 AWS의 서비스에 대한 권한을 부여할 수 있다.

프로그래밍이 아니기 때문에 , 프로그래머가 아니라고 하더라도 이해하기 쉽다.

AWS는 비용 문제나 보안 문제를 야기할 수 있기 때문에 , user별 최소 권한 원칙을 적용한다.
- 사용자가 꼭 필요한 기능이 아니면 열어두지 않는다.
## IAM Policies inheritance
IAM은 여러 그룹이 존재할 수 있고 , 여러 그룹에 속해있는 user가 있을 수 있다.

**각 그룹별로 IAM 정책을 부여하게 되는데 , 여러 그룹에 속해있는 user는 모든 그룹의 정책 중 최소 권한을 가진 그룹의 정책을 따르게 된다.**
- 만약 admin policy와 iam policy를 부여받앗다면 , user는 iam policy를 부여받게 된다.

## IAM Policies Structure
AWS에서는 아래와 같은 Json 구조를 자주 사용한다.
- 익숙해지자

IAM 정책의 대한 예시이다.
1. **Version** : 정책 언어 버전이다. 2012-10-17을 항상 포함한다.
2. **Id** : 정책을 식별하는 Id이다. (optional)
3. **Statement**: 필수 요소이다. 여러개가 올 수 있다.
	- **Sid** : 해당 Statement의 식별 코드이다 . (optional)
	- **Effect** : Statement가 특정 API에 접근을 허용 ( Allow ) 할 지 , 거부 ( Deny ) 할 지 에 대한 정보이다.
	- **Principal** : 해당 정책이 부여될 계정 정보이다. 구성은 account/user/role 순으로 들어간다.
						예시에선 AWS 계정의 루트 계정에 적용이 된다.
	- **Acction** : 허용되거나 거부되는 api 호출 목록이다.
	- **Resource** : 적용될 action의 resource 정보이다.
						예시에서는 mybucket이라는 s3
	

```json
{
	"Version": "2012-10-17",
	"Id": "S3-Account-Permissions",
	"Statement": [
		{
			"Sid": "1",
			"Effect": "Allow",
			"Principal":{
				"AWS": ["arn:aws:iam::12345789012:root"]
			},
			"Action": [
				"s3:GetObject",
				"s3:PutObject"
			],
			"Resource": ["arn:aws:s3:::mybucket/*"]
		}
	]
}
```

AWS의 AdministratorAccess IAM policy json을 확인해 보자.
- version은 2012-10-17이 들어가고 ,statement에 모든 권한을 허용 / 비허용 하고 , 모든 리소스에대해 모든 action을 허용하는 권한이다.
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "*",
            "Resource": "*"
        }
    ]
}
```
## IAM - Security
IAM service를 통해 생성된 user 보안 정책에는 두가지가 있다.
**1. Password Policy**
**2. Multi Factor Authentication - MFA**

### 1. Password Policy
IAM으로 user를 생성할 때 , 여러 조건을 걸어서 user가 Password를 설정할 때 , 강력한 보안의 Password를 설정하도록 강제할 수 있다.
- 최소 길이 이상의 password
- 특정 문자가 포함된 password
	- 대문자 포함
	- 숫자 포함
	- 특수문자 포함
	- 소문자 포함
- IAM 사용자들의 비밀번호 변경을 허용 또는 금지 가능
- 특정 기간 이후 비밀번호 변경 요구 가능
- user가 이전에 설정했던 비밀번호 재사용 금지 가능

### 2. Multi Factor Authentication - MFA
AWS에선 해당 메커니즘을 필수로 사용하는것을 권장한다.
- 관리자는 필수 구성
- iam user들에게도 필수적으로 구성하도록 권장

MFA = 비밀번호와 보안 장치를 같이 사용하여 로그인하는 방법
- 비밀번호와 MFA Token 값을 같이 사용하여 로그인함으로써 보안 능력을 향상시킬 수 있다.
- 비밀번호가 유출되더라도 MFA Token 값을 생성해줄 수 있는 물리장치가 없다면 로그인 불가

#### 2.1 MFA device options in AWS
AWS에선 아래 MFA device들을 지원한다.
1. ***Virtual MFA device***
	- **Google Authenticator** : phone only
	- **Authy** : multi device 기능 제공 , 하나의 장치에서 여러개 토큰 지원

2. ***Universal 2nd Factor (U2F) Security Key***
	- 물리 장치
	- Yubico사의 YubiKey

3. ***Hardware Key Fob MFA Device***
	- 물리 장치
	- Gemalto사의 key pod
	
4. ***Hardware Key Fob MFA Device for AWS GovCloud (US)***
	- 물리 장치
	- SurePassID사의 key pod