# log 제거 script
tomcat의 catalina.out 로그 파일이 특정 일자가 지나가면 제거되는 script 입니다.

crontab에 등록하여 사용합니다.

10일이 지나간 log는 삭제시키는 스크립트 입니다.

## 반영
매월 매일 24:00에 수행되게끔 crontab에 등록합니다. 
```bash
$crontab -e
```

vi 에디터로 cron 작성 후 저장
```bash
0 0 * * * /data/tomcat/delete_sc.sh
```

반영 결과를 확인합니다.
```bash
$crontab -l
0 0 * * * /data/tomcat/delete_sc.sh
```

