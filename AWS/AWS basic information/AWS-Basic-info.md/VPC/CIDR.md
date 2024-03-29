# CIDR
가변적인 서브넷마스크를 이용하여 IP 대역을 지정한 뒤 IP 주소를 할당하는 방법.

구성 요소는 두 가지로, ***IP*** , ***Subnet Mask*** 로 나뉘어집니다.
## CIDR 이전 클래스 기반 IPv4 체계
IPv4는 A, B, C, D, E 클래스로 나뉘어져 있으며, 각 클래스는 주소의 범위와 용도에 따라 구분됩니다. 
|클래스 명|주소 범위|서브넷 마스크|특징|용도|
|--|--|--|--|--|
|클래스 A|```1.0.0.0``` ~ ```126.0.0.0```|```255.0.0.0``` 또는 ```/8```|첫 번째 옥텟(8비트)이 네트워크를, 나머지 세 옥텟이 호스트를 나타냅니다.|매우 큰 네트워크에 사용됩니다.|
|클래스 B|```128.0.0.0``` ~ ```191.255.0.0```|```255.255.0.0``` 또는 ```/16```|첫 두 옥텟이 네트워크를, 나머지 두 옥텟이 호스트를 나타냅니다.|중간 크기의 네트워크에 사용됩니다.|
|클래스 C|```192.0.0.0``` ~ ```223.255.255.0```|```255.255.255.0``` 또는 ```/24```|첫 세 옥텟이 네트워크를, 마지막 옥텟이 호스트를 나타냅니다.|작은 네트워크에 사용됩니다.|
|클래스 D|```224.0.0.0``` ~ ```239.255.255.255```|-|멀티캐스트 주소로 사용됩니다.|멀티캐스트 그룹에 패킷을 전송하는 데 사용됩니다.|
|클래스 E|```240.0.0.0``` ~ ```255.255.255.255```|-|실험적인 목적으로 예약되어 있습니다.|일반적으로 사용되지 않습니다.|

> IPv4 주소가 부족해짐에 따라 , CIDR를 사용하게 됨.

## CIDR의 IP 및 Subnet Mask

IPv4 체계에서 , 1.2.3.4/32 IP는 아래 표의 2진수값을 10진수로 표현한것 입니다.
- 따라서 , 첫번째 IP는 ```0.0.0.0 - 00000000.00000000.00000000.00000000``` 이 되고,
  할당 가능한 마지막 IP는 ```255.255.255.255 - 11111111.11111111.11111111.11111111``` 이 됩니다.

|10진수|2진수|비고|
|--|--|--|
|1.2.3.4|00000001.00000010.00000011.00000100|-|

이때 Subnet Mask가 적용되어 해당 2진수 IP 대역에서 **할당 가능한 IP들의 범위를 나타낼 수 있습니다.**

Subnet Mask는 ```/2, /32``` 이렇게 작성하게 되며, **2진수의 맨 앞 (맨 왼쪽) 부터 1을 몇개나 채우는지 를 나타냅니다.**
- 예를들어 ```/2``` 라면 , **1은 맨 왼쪽부터 두개 채워지고,**
- 예를들어 ```/32``` 라면 , **1은 맨 왼쪽부터 32개 채워집니다.**

>프리픽스들을 2진수와 10진수로 변환하면 아래 표처럼 계산할 수 있습니다.

|2진수|10진수|프리픽스|
|--|--|--|
|```11000000.00000000.00000000.00000000```|```192.0.0.0```|```/2```|
|```11111000.00000000.00000000.00000000```|```248.0.0.0```|```/5```|
|```11111111.11111111.10000000.00000000```|```255.255.128.0```|```/17```|

따라서 , CIDR는 프리픽스를 기반으로 IP 대역을 표현할 수 있으며, 가질 수 있는 대역을 계산하는 방법은 다음과 같습니다.
>서브넷 마스크의 첫번째 주소와, 맨 마지막 주소는 각각 네트워크 주소 , 브로드 케스트 주소라 할당받을 수 없습니다.

### 가질 수 있는 대역 계산 방안

계산 순서는 ,
1. 프리픽스를 2진수로 변경
2. 네트워크 주소 구하기
    - 네트워크 주소는 , 주어진 IP와 서브넷 마스크를 모두 2진수로 변경한 다음 , 둘을 비트별로 AND 연산을 수행합니다.
        - ***AND 연산 : 둘다 1일때만 1***
3. 브로드 케스트 주소 구하기.
    - 서브넷 마스크와 네트워크 주소를 모두 0비트를 1비트로, 1비트를 0비트로 바꿈 (보수 연산(NOT))
    - 보수 연산된 두 값을 OR 연산 수행한 값이 브로드케스트 주소
        - ***OR 연산 : 대응되는 비트 중에서 하나라도 1이면 1을 반환함.***
4. ***할당 가능 범위 : 네트워크 주소 다음 IP (```0.0.0.1```) 부터 , 해당 프리픽스의 브로드케스트 주소 바로 앞 주소 까지***

아래 예의 모든 IP 주소는 ```1.0.0.0 입니다.```

- ```/2``` 라면
    - 프리픽스 및 IP 2진수 변경
        - /2 의 2진수 : ```11000000.00000000.00000000.00000000```
        - 1.0.0.0 의 2진수 : ```00000001.00000000.00000000.00000000```
    - 네트워크 주소 구하기
        - ```11000000.00000000.00000000.00000000``` 과 ```00000001.00000000.00000000.00000000``` AND 연산 수행
        - 네트워크 주소 :   ```00000000.00000000.00000000.00000000```
    - 브로드 케스트 주소 구하기
        - 보수 연산된 네트워크 주소 : ```00000000.00000000.00000000.00000000``` 보수 연산 : ```11111111.11111111.11111111.11111111```
        - 보수 연산된 Prefix : ```11000000.00000000.00000000.00000000``` 보수 연산 : ```00111111.11111111.11111111.11111111```
        - 보수 연산된 네트워크 주소. Prefix를 OR 연산 수행
            -  브로드 케스트 주소 :```11111111.11111111.11111111.11111111```
    - 결과 도출
        - 따라서 ```0.0.0.1``` ~ , ```63.255.255.254``` 까지가 subnet mask ```/2```의 할당 가능한 IP 대역입니다.
-  ```/5``` 라면,
    - 네트워크 주소는 동일 .
    - ```/5``` 를 2진수로 변경하면 ```11111000.00000000.00000000.00000000```
    - 0비트를 1비트로, 1비트를 0비트로 바꾸어 ```00000111.11111111.11111111.11111111```, 브로드케스트 주소 : ```7.255.255.255```
    - ***할당 가능 범위 : ```0.0.0.1``` ~ ```7.255.255.254```***


>아래 표처럼 계산할 수 있습니다.

|CIDR|IP|Prefix|2진수 Prefix|10진수 Prefix|네트워크 주소|브로드케스트 주소|가질 수 있는 IP 대역|
|--|--|--|--|--|--|--|--|
|```0.0.0.0/0```|```0.0.0.0```|```/0```|```00000000.00000000.00000000.00000000```|```0.0.0.0```|```0.0.0.0```|```255.255.255.255```|네트워크 IP(```0.0.0.0```) 및 브로드케스트 주소를 제외한 모든 IP를 포함합니다.|
|```192.37.168.3/2```|```192.37.168.3```|```/2```|```11000000.00000000.00000000.00000000```|```192.0.0.0```|```192.0.0.0```|```63.255.255.255```|```192.0.0.1 ~ 63.255.255.254```|
|```1.0.0.3/5```|```1.0.0.3```|```/5```|```11111000.00000000.00000000.00000000```|```248.0.0.0```|```0.0.0.0```|```7.255.255.255```|```0.0.0.1``` ~ ```7.255.255.254```|
|```172.10.0.0/8```|```172.10.0.0```|```/8```|```11111111.00000000.00000000.00000000```|```255.0.0.0```|```172.0.0.0```|```172.255.255.255```|```172.0.0.1``` ~ ```172.255.255.254```|
|```192.37.168.3/16```|```192.37.168.3```|```/16```|```11111111.11111111.00000000.00000000```|```255.255.0.0```|```192.37.0.0```|```192.37.255.255```|```192.37.0.1``` ~ ```0.0.255.254```|
|```172.13.2.0/24```|```172.13.2.0```|```/24```|```11111111.11111111.11111111.00000000```|```255.255.255.0```|```172.13.2.0```|```0.0.0.255```|```172.0.0.1``` ~ ```172.13.2.254```|
|```10.10.2.5/32```|```10.10.2.5```|```/32```|```11111111.11111111.11111111.11111111```|```255.255.255.255```|```10.10.2.5```|```255.255.255.255```|```10.10.2.5``` ~ ```10.10.2.5```|