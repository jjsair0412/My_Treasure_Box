# certbot 사용 방안
## certbot ???
certbot을 통해 Let's Encrypt의 무료 TLS 인증서를 발급받아 사용할 수 있습니다.

발급된 TLS 인증서는 3개월 유효기간을 가지고 있지만 , cron을 통해서 자동 갱신되게끔 할 수 있습니다.

## 1. certbot 설치
certbot은 홈페이지에서 snap을 통해 설치하는것을 권장하기 때문에 , snap을 통해 설치합니다.
```bash
$ sudo snap install certbot --classic
```

