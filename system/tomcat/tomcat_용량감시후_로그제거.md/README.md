# delete_volume_log
tomcat 로그가 쌓이는 디렉터리의 용량을 감시하다가 . 

해당 디렉터리의 사용량이 50%가 넘어가면 로그를 삭제하는 스크립트 입니다.

## 작동 예
로그 쌓이는 폴더 용량이 50% 이상 사용하면 로그를 삭제합니다.

삭제대상 로그는 수정된지 이틀이 지난 파일들이 삭제됩니다.

crontab에 등록해서 사용합니다.

## 반영
매월 매일 24:10에 수행되게끔 crontab에 등록합니다. 
```bash
$crontab -e
```

vi 에디터로 cron 작성 후 저장
```bash
10 0 * * * /data/tomcat/delete_volume_sc.sh
```

반영 결과를 확인합니다.
```bash
$crontab -l
10 0 * * * /data/tomcat/delete_volume_sc.sh
```
