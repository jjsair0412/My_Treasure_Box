# certbot 사용 방안
## certbot ???
certbot을 통해 Let's Encrypt의 무료 TLS 인증서를 발급받아 사용할 수 있습니다.

발급된 TLS 인증서는 3개월 유효기간을 가지고 있지만 , cron을 통해서 자동 갱신되게끔 할 수 있습니다.

certbot의 사용 방안은 세가지로 나뉩니다.
1. **standalone** 
- 가상 웹서버를 가동하여 도메인소유주 확인
2. **webroot** 
- 자신의 웹서버가 제공하는 특정 파일로 도메인소유주 확인
3. **dns** 
- dns 레코드에 특정 값을 작성하여 도메인소유주 확인

## 1. certbot 설치
먼저 certbot을 설치합니다.

certbot은 홈페이지에서 snap을 통해 설치하는것을 권장하기 때문에 , snap을 통해 설치합니다.
```bash
$ sudo snap install certbot --classic
```

## 2. ssl 인증서 발급받기
### 2.1 standalone 방식으로 발급받기
