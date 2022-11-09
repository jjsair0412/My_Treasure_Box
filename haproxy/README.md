# introduce haproxy
haproxy는 LB ( L4 스위치 ) 역할을 vm이 대신 해줄 수 있게끔 해주는 서비스 입니다.

설정이 편리하고 이중화 구성도 가능해서 L4 또는 L7 스위치를 대신할 수 있습니다.

k8s와 같이 사용하기위해선 metallb와 연동해서 사용하거나 , ingress-nginx의 nodeport의 포트번호와 
80 443을 매칭시켜 사용합니다.