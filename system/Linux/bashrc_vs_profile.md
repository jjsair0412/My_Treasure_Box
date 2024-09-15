# bashrc_vs_profile.md
기본적으로 ubuntu와 같은 linux 운영체제에 연결할 때 , ssh나 id password를 입력하고 접근합니다.
```bash
$ ssh -i ~ 

$ id : paswd :
```

이때 로그인한 default user (vagrant ,root ,,) 들은 , /etc/profile 설정을 가지고 로그인 됩니다.

따라서 root 유저를 제외한 모든 유저에 환경변수를 적용하기 위해선 , export 명령어를 /etc/profile에 추가한 후 . source로 적용시켜야 합니다.
```bash
$ vi /etc/profile
...
export MYVAR="myvalue"
```

그러나 root user는 , 접속할때 /root/.bashrc 설정을 가지고 로그인되기 때문에 . root유저에 환경변수를 적용하기 위해선 위 파일에 export를 넣어주어야 합니다.
```bash
$ vi /root/.bashrc
...
export MYVAR="h"
```

## 1. 테스트 및 결과 확인
/etc/profile에는 위와 같이 MYVAR에 myvalue를 넣어주었습니다.

또한 /root/.bashrc 에는 MYVAR에 hello를 넣어주었습니다.

```bash
vagrant@master:~$ echo $MYVAR
myvalue
vagrant@master:~$ su jinseong
Password:
jinseong@master:/home/vagrant$ echo $MYVAR
myvalue
```
jinseong user와 vagrant user 모두 MYVAR에 myvalue 값이 출력되는것을 확인할 수 있습니다.

```bash
vagrant@master:~$ sudo su
root@master:/home/vagrant# echo $MYVAR
hello
```

root user에선 MYVAR이 hello로 출력되는것을 확인할 수 있습니다.