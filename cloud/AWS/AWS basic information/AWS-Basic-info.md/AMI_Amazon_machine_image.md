# AMI_Amazon_machine_image
## What is AMI ? (Amazon_machine_image)
EC2 인스턴스의 기반이 된다.

사용자 custom EC2 인스턴스를 의미하며 , AWS에서 미리 만들어준 AMI를 사용하거나 , 사용자가 임의로 AMI를 생성하여 만들 수 도 있다.

사용자 각각의 소프트웨어 구성에 대해서 운영체제를 정의 및 설정하여 빠르게 부팅시킬수 있다.

이는 , 사용자가 EC2 인스턴스에 필요한 소프트웨어를 사전에 페키징하여 AMI로 만들어 둘 수 있기에 가능하다.

AMI를 원하는 리전에 복사해놓거나 하여 글로벌하게 사용할 수 있다.

## mutli type AMI
1. public AMI
    - AWS가 미리 만들어둔 AMI
2. user custom AMI
    - 사용자 custom AMI
3. AWS Marketplace AMI
    - AWS Marketplace에서 AMI를 사고 팔 수 있는 공간

## AMI Process
EC2 인스턴스에서 AMI는 다음 프로세스로 실행된다.
1. EC2 인스턴스를 시작한 후 이를 사용자 custom으로 변경한다.
2. EC2 인스턴스를 중지하여 데이터 무결성을 확보한다.
3. AMI를 구축한다.
    - 이때 EBS Snapshot또한 생성된다.
4. 생성된 AMI로 다른 인스턴스를 생성할 수 있게 된다.

해당 프로세스를 통해 만들어진 AMI로 다른 AZ의 인스턴스를 생성하면 EC2 인스턴스의 사본을 생성할 수 있다.

## AMI Demo UseCase
다음의 user date script를 갖는 EC2 인스턴스를 생성하자.

```bash
#!/bin/bash
# install httpd (Linux 2 version)
yum update -y
yum install -y httpd
systemctl start httpd
systemctl enable httpd
```

그럼 해당 인스턴스는 httpd를 설치하고 , enable시키는 작업을 수행할 것이다.

이 인스턴스를 우클릭하여 create image 버튼으로 AMI를 생성한 뒤 , 
방금 만든 AMI로 EC2 인스턴스를 생성하자..

AMI로 생성한 EC2 인스턴스의 user data script는 다음과 같다.

```bash
#!/bin/bash
echo "<h1>Hello World from $(hostname -f)</h1>" > /var/www/html/index.html
```

EC2 인스턴스가 running하는데 시간이 굉장히 짧아진것을 확인할 수 있으며 , 생성한 AMI에서 httpd를 설치 및 enable을 수행하기 때문에 index.html 파일을 생성하는 user data script를 통해서 , 페이지가 출력되는것을 확인할 수 있다.