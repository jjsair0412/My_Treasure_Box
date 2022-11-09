# haproxy 기본 설치 방안
## Prerequisites
해당 문서는 haproxy를 설정하는 방안에 대해 기술합니다.

LB 구성은 다음과 같습니다.
haproxy로 80번 요청이 들어오게 되면 , k8s 클러스터 ( 모든 노드 ) 중 한개의 노드의 30001번 포트로 포워딩되는 구성
haproxy로 443번 요청이 들어오게 되면 , k8s 클러스터 ( 모든 노드 ) 중 한개의 노드의 30002번 포트로 포워딩되는 구성

roundrobin 형식으로 로드벨런싱 하게끔 설정하며 , tcp mode로 구성합니다.

만약 다른 포트를 열 필요가 있다면 , cfg파일을 수정하여 열어주어야 합니다.

## 1. install haproxy & config setting
### 1.1 install haproxy 
haproxy를 설치합니다.
```
sudo yum install haproxy

sudo apt-get install haproxy
```

### 1.2 setting haproxy.cfg
haproxy의 config파일을 수정합니다.
```
sudo cp /etc/haproxy/haproxy.cfg /etc/haproxy/haproxy.cfg.old # 기존 cfg파일 old로 변경

sudo vi /etc/haproxy/haproxy.cfg

#  cfg 파일 최 하단에 아래와 같이 구성합니다.
# 30001는 80포트에 포워딩된 nodeport port번호 입니다.
# 30002는 443포트에 포워딩된 nodeport port번호 입니다.
...

listen tcp-80
bind *:80
balance roundrobin
mode tcp
log global
option tcplog
server master1 10.xxx.xxx.xxx:30001 check 
server worker1 10.xxx.xxx.xxx:30001 check


listen tcp-443
bind *:443
balance roundrobin
mode tcp
log global
option tcplog
server master1 10.xxx.xxx.xxx:30002 check 
server worker1 10.xxx.xxx.xxx:30002 check
```

### 1.3 haproxy 실행
haproxy를 start 합니다.
```
systemctl enable haproxy # enable 

systemctl start haproxy # start
```
