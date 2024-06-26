---
title: Linux Memory 관리의 시작 - free , cache , Swap
subtitle: Linux 커널의 Memory 할당 방식을 이해하여 잘 관리해 보자 !
tags: devops, Linux, Linux-kernel
domain: jjsair0412.hashnode.dev
cover: https://cdn.hashnode.com/res/hashnode/image/upload/v1713633130303/UHnFq_ROI.jpeg?auto=format
---

# Linux Memory 관리의 시작  - free , cache , Swap
- #### ***해당 문서는 책 'DevOps와 SE를 위한 Linux 커널 이야기' 를 읽고 정리한 내용 입니다.***

## OverView
서버 Application, Queue, 등 세상의 Application은 커널 기반의 Linux 시스템 위에서 작동하는 경우가 많다 생각합니다.

Linux마스터라는 자격증이 있을 정도로 Linux 자체를 다루는것은 중요합니다. 이는 단단한 시스템을 구축하고 사용자에게 하여금 좋은 경험을 전달 할 수 있습니다.

모든 Application은 실행(I/O 작업 등) 하기 위하여 메모리를 사용하게 됩니다. 그러나 모든 서버들이 Application 프로세스에서 요구하는 메모리를 감당할 순 없습니다.
순간적으로 사용자가 급증하여 메모리를 많이 필요하거나 코드 자체에서 메모리 누수가 발생할 가능성도 적지 않습니다.

따라서 Linux의 메모리를 잘 감시하는것은 필수적이며 발생되는 오류의 문제를 낮추고 서비스 신뢰도를 높힐 수 있습니다.

## 1. free 구경하기
Linux의 명령어중 전체적인 메모리 사용량을 빠르게 파악하는 ```free``` 명령어가 있습니다.

먼저 구경해 봅니다.

```bash
$ free -m
               total        used        free      shared  buff/cache   available
Mem:             949         125         602           2         220         683
Swap:              0           0           0
```

    top 명령어와 동일하게 많은 부분을 축약해서 빠르게 파악할 수 있고 , - 옵션으로 출력 단위또한 변경이 가능함.

- ```-b : byte 단위```
- ```-m : MB 단위```
- ```-g : GB 단위```

각 부분은 크게 2열로 나뉘어져 있습니다.

***- 1. Memory 정보***

***- 2. [Swap 정보](#4-swap-메모리)***

이들을 각각 뜯어보도록 하겠습니다.
### Mem row
현재 시스템에 설치되어 있는 메모리의 정보
#### 1. total
시스템에 구성된 메모리의 총 양

#### 2. used
현재 시스템이 사용중인 메모리의 양

#### 3. free
시스템이 아직 사용하고 있지 않은 메모리의 양. 커널이 사용할 수 도 있고, 프로세스가 사용할 수 도 있음.

#### 4. shared 
프로세스 사이에 공유되고있는 메모리의 양

#### 5. buff/cache
버퍼 및 캐시 용도로 사용되고있는 메모리의 양. 프로세스가 사용하는것은 아니고 , I/O나 파일 읽기 등의 성능을 향상시키기 위해 커널이 사용중인 영역

#### 6. available
프로세스 또는 커널이 사용 가능한 메모리의 양

### Swap row
```Swap``` 메모리 정보

#### 1. total
```Swap``` 영역의 전체 용량

#### 2. used 
```Swap``` 영역이 실제로 사용중인 양

#### 3. free
```Swap``` 영역 중 사용하지 않은 영역의 양

이렇게 현재 사용중인 Memory 정보를 전체적으로 간략하게 확인이 가능합니다. 다른 영역에 대한 설명은 하단에서 진행하고 , 먼저 Linux 커널이 메모리를 어떻게 관리하는지 비밀이 숨어있는 ```buff/cache``` 영역에 대해 기술합니다.

### ```buff/cache``` 영역 ?!
![create_image](https://cdn.hashnode.com/res/hashnode/image/upload/v1713633130303/UHnFq_ROI.jpeg?auto=format)

커널은 기본적으로  **1. 블록 디바이스에서 데이터를 읽거나,**  **2. 사용자의 데이터를 디스크에 저장합니다.** 그러나 디스크는 다른 장치에 비해 느리기에, 성능을 높힐 필요성이 있었고, Linux는 이를 캐시 처리하기로 합니다.

***따라서 메모리의 일부를 디스크 요청에 대한 캐싱 영역으로 할당하여 사용합니다. 한번 읽어온 내용을 메모리에 캐싱처리해두고, 동일 요청에 대해 캐시를 반환하면서 성능을 높힙니다. 이때 사용되는 영역이 바로 ```buff/cache``` 영역입니다 !***

블록 디바이스에 대해 어떤것을 읽어와야하는지에 따라 캐시되는 영역이 다릅니다. 각각 파일 데이터, 파일 시스템 메타데이터 로 나뉘어지게 됩니다.

#### 1. 파일 데이터 읽어오기
만약 커널이 읽어야할 데이터가 파일 내용이라면 커널은 bio 구조체를 생성 합니다. 

그리고 해당 구조체는 블록 디바이스와 통신해서 데이터를 읽고 Page Cache에 캐싱 처리합니다.

#### 2. 파일 시스템 메타데이터 읽어오기
bio 구조체를 사용하지 않고, _get_blk() 와 같은 내부 함수를 사용합니다..

이때 가져온 내용을 Buffer Cache에 캐싱 처리합니다.

***결론적으로 Page Cache는 파일 내용을 저장하고 있는 캐시 , Buffer Cache 는 파일 시스템의 메타데이터를 담고 있는 블록을 저장하고 있는 캐시이며 , 각각 cached, buffers 영역으로 알 수 있습니다.***

이렇게 ```buff/cache``` 영역에 블록 디바이스에서 가져온 데이터를 캐시 처리하면서, Linux는 성능을 향상시키는것으로 확인됩니다. 서버 관리에 중추적인 역할을 하며, **중요한 영역입니다.**

그렇다면 ```buff/cache``` 영역이 왜 중요할까요 ?

### ```buff/cache``` 영역이 중요한 이유
Linux 서버에 Application 한대를 배포해 두었다고 생각해 봅니다.

서버 운영기간이 길지 않을경우엔.. 메모리는 가용 영역과 사용중인 영역 두곳으로 나누어져 운영될것입니다.

이후 시간이지날수록, Application은 블록 디바이스에 I/O 작업을 수행할것이고, 이는 그대로 메모리에 캐시된 영역(```buff/cache```) 영역이 늘어나게 됩니다.

**그러다 사용 영역이 점점 더 커져서,, 일정 임계치를 넘어서면 커널은 Cache된 영역을 Application이 사용할 수 있도록 메모리에 반환합니다. !**

이 과정을 반복하다보면 , 메모리에는 사용할 수 있는 가용 메모리가 없어지고 , 결국 **swap 메모리를 사용하게 되며 이는 성능저하를 발생시킵니다.**

이러한 문제가 발생하기에, 관리자는 ```buff/cache``` 값을 항상 감시해야하고 일정 수준을 넘어서지 않도록 평균치를 측정하여 운영해야 합니다.

## 2. 조금 더 명확하게 메모리 감시하기
free 명령어는 전반적인 상태를 한눈에 파악하기 좋습니다. 그러나 너무 축약시켯기에 실질적인 문제 원인을 트러블슈팅하기 어렵다는 단점이 있습니다.

Linux 프로세스는 자신과 관련된 정보들을 ```/proc/{PID}/*``` 디렉토리 안에 저장해 둡니다. 예를들어 PID 값이 1인 프로세스의 정보가 궁금하다면 ```/proc/1/*``` 경로를 확인하면 됩니다.

조금 뜬금없었지만.. 조금더 자세한 현황이 궁금할땐 ```/proc/meminfo``` 파일을 확인하면 됩니다.

```bash
# 수행 결과 주요부분만 복사 후 라벨링 하였습니다. 
# 실제적인 검색결과는 훨씬 더 길고 파악해야할 부분들이 많습니다.
$ cat /proc/meminfo 
MemTotal:         972344 kB
MemFree:          617212 kB
MemAvailable:     700244 kB
Buffers:            2168 kB
Cached:           198332 kB
SwapCached:            0 kB -- 1
Active:           115960 kB 
Inactive:         130372 kB 
Active(anon):        672 kB -- 2
Inactive(anon):    48040 kB -- 3
Active(file):     115288 kB -- 4
Inactive(file):    82332 kB -- 5
Unevictable:           0 kB
Mlocked:               0 kB
SwapTotal:             0 kB
SwapFree:              0 kB
Zswap:                 0 kB
Zswapped:              0 kB
Dirty:                 0 kB -- 6
...
```

1번부터 6번까지 라벨링해둔 주요 부분에 대해 설명합니다.

### 2.1 SwapCached
```swap``` 영역으로 빠진 메모리 영역 중 , 다시 메모리로 돌아온 영역을 뜻합니다..

시스템에 메모리가 부족하면 커널은 프로세스 주소 공간 중 ```swap``` 영역으로 이동 가능한 메모리 영역을 선택하여 ```swap``` 으로 옮기게 됩니다.

***이 과정에서 I/O 가 발생하기에 성능 저하가 발생합니다.***

**이후 메모리가 확보되면, swap으로 빠졋던 영역이 다시 메모리로 반환되는데, 그래도 커널은 swap 영역에서 해당 메모리 내용을 제거하지 않습니다.**
- 이는 다시 메모리부족이 일어날 것을 대비함에 있음. 다시 영역이 필요하면 재할당이 아닌 재활용하기 때문. (I/O를 줄이기 위해)


### 2.2 Active(anon)
Page Cache 영역을 제외한 메모리 영역. 주로 프로세스들이 사용하는 메모리 영역을 나타낼 때 사용 합니다.

### 2.3 Inactive(anon)
Active(anon) 과 같이 영역을 의미하는데, ***참조된지 오래되어 Swap 영역으로 이동될 수 있는 메모리 영역을 의미합니다.***

### 2.4 Active(file)
커널이 I/O 성능 향상을 위해 캐시로 사용하는 영역. buff/cache 영역이 여기에 속합니다.

### 2.5 Inactive(file)
커널이 캐시 목적으로 사용하고 있는 영역. ***참조된지 오래되서 Swap으로 이동될 수 있는 메모리 영역을 의미입니다.***

### 2.6 Dirty
캐시 목적으로 사용하는 영역 중 , ***쓰기 작업이 이루어져서 실제 블록 디바이스의 블록에 씌여져야 하는 영역을 의미합니다.***

커널은 블록 디바이스에 쓰기를 계속해서 수행하는것이 아니라, 모아놧다가 한번에 쓰기때문에 여기에 저장됩니다.

## 3. 커널이 메모리를 사용하는 방법 - slab 영역
***Linux의 커널또한 프로세스 입니다. (모든것은 프로세스로 ..?)***
- 커널 프로세스는 메모리로 I/O 작업성능을 최적화시키는 등의 작업을 수행합니다.

따라서 메모리를 할당받아 사용해야 하는데, 커널은 일반적인 방법으로 할당이 진행되지 않고, ```slab``` 이라는 영역을 통해 메모리를 할당받습니다.

```slab``` 에 대한 정보는 ```/proc/meminfo``` 에 담겨 있습니다.

```bash
$ cat /proc/meminfo 
...
Slab:              56772 kB -- 1
SReclaimable:      25892 kB -- 2
SUnreclaim:        30880 kB -- 3
...
```

라벨링된 각 부분의 대한 설명은 다음과 같습니다.

### 3.1 Slab
메모리 영역 중 커널이 직접 사용하고있는 영역

### 3.2 SReclaimable
Slab 영역 중 재사용이 가능한 영역. 캐시 용도로 사용되는 메모리들이 여기 포함됨.

### 3.3 SUnreclaim
Slab 영역 중 재사용될 수 없는 영역

### 3.4 ETC - slabtop command
```slab``` 영역에 대한 감시도 따로 명령어가 있습니다. ```slabtop``` 명령어를 사용ㅎ면 됩니다.

```bash
# 수행 결과
$ sudo slabtop -o
 Active / Total Objects (% used)    : 379927 / 382367 (99.4%)
 Active / Total Slabs (% used)      : 6810 / 6810 (100.0%)
 Active / Total Caches (% used)     : 117 / 171 (68.4%)
 Active / Total Size (% used)       : 50809.55K / 51725.90K (98.2%)
 Minimum / Average / Maximum Object : 0.01K / 0.13K / 10.12K

  OBJS ACTIVE  USE OBJ SIZE  SLABS OBJ/SLAB CACHE SIZE NAME                   
108800 108800 100%    0.02K    640      170      2560K avtab_node             
 72512  72512 100%    0.06K   1133       64      4532K vmap_area           
...
```

**slab 영역은 free 명령어 결과에서 used로 계산되기 때문에 프로세스들이 사용하는 영역을 모두 더했는데도 used와 맞지 않다면, slab 메모리에서 누수가 발생하는것일 확률이 있습니다.. 커널도 프로세스라 버그가 있을 수 있기 때문입니다.. . ..**

---
## 4. Swap 메모리 ?????
이제 Linux 커널이 메모리를 어떻게 효율적으로 할당하고 프로세스는 어떻게 할당받는지 대충은 감이 옵니다.

그런데 가장 생소한 메모리인 ```swap``` 이라는 메모리 단어가 계속 등장합니다.

메모리 정보를 가장 단축시켜서 보여주는 ```free``` 명령어 결과에도 나올정도면 중요한것으로 보입니다..

### OverView
Linux에는 Application이 배포됩니다. 이들은 사용 목적, 존재 이유 등 만들어져 배포된 것에는 정말 다양한 이유가 존재합니다. 

Application이 중단 되어도 괜찮을 수 도 있고, 심지어 한두시간정도 멈춰도 괜찮을 수 있습니다.

어떤 Application은 아닐것입니다. 

특정 Application이 중단된다면 서비스 전체가 중단되는 불상사가 일어날 수 도 있습니다.

이러한 불상사를 막기 위해 ,, Linux는 ```Swap``` 영역을 사용합니다 !

### 4.1 Swap 영역
```Swap``` 영역은 단순하게 **Linux 메모리를 비축해 두었다가 사용중 메모리가 부족할 때 사용하는 영역** 이라 볼 수 있습니다.

말그대로 **비상용** 영역입니다.

***물리 메모리가 아니고 메모리 처럼 사용하기 위해 만들어놓은 공간이기 때문에,, 메모리가 부족할때 사용은 하지만 메모리 접근과 처리속도에 있어선 물리 메모리보다 현저히 떨어집니다.***

따라서 ```Swap``` 영역을 사용한다는 것은 Application에 성능을 떨어트린다와 같은 맥락이 될 수 있습니다.
- 물론 모든 상황에서 절대로 ```Swap```을 못쓰게 막는것은 좋지 않은 선택입니다. (반복적이지 않은) 특정 시점에 서비스 유저가 몰리는것을 ```Swap``` 영역으로 방어할 수 있습니다.

### 4.2 Swap 영역 모니터링 하기
초기 글로 돌아가서, ```free``` 명령어 결과를 다시한번 확인해 봅니다.

```bash
$ free -m
               total        used        free      shared  buff/cache   available
Mem:             949         125         602           2         220         683
Swap:              0           0           0
```

2행의 ```Swap``` 영역에 대해 다시한번 풀어서 기술합니다.

#### 1. total
```Swap``` 영역의 전체 용량

#### 2. used 
```Swap``` 영역이 실제로 사용중인 양. 

지금은 0으로 ```Swap``` 메모리가 사용중이지는 않지만, 만약 올라간다면 반드시 원인을 파악해야 합니다.

***Swap을 사용했다는것 자체가 Application이 가용할 메모리와 Cache된 메모리가 필요 메모리보다 낮다는것을 의미하기 때문입니다.***

#### 3. free
```Swap``` 영역 중 사용하지 않은 영역의 양

이렇게 Swap 영역을 사용했다는것은 민감한 문제이고, 관리도 철저히 해야합니다. 그래야 단단한 서비스를 구축할 수 있습니다.

그렇다면 '누가 ```Swap```을 썻지?' 를 찾는것 또한 주요 관리 포인트가 될 수 있을것 입니다.

### 4.2 누가 Swap을 썻지 ? 
위에 말씀드렷던 것 처럼 모든 프로세스는 ```/proc/{PID}``` 폴더에 프로세스 자신의 관련 정보를 저장합니다.

프로세스가 사용하는 메모리 정보도 동일한데,, ```/proc/{PID}/smaps``` 파일에서 메모리 정보를 저장합니다.

실제 1번 프로세스가 사용하는 Swap을 감시해 보겠습니다.
- 일부분만 추출
```bash
$ cat smaps | more
...
55e67abb3000-55e67abb4000 r--p 00017000 ca:01 8618706                    /usr/lib/systemd/systemd
Size:                  4 kB
KernelPageSize:        4 kB
MMUPageSize:           4 kB
Rss:                   4 kB
Pss:                   2 kB
Pss_Dirty:             2 kB
Shared_Clean:          0 kB
Shared_Dirty:          4 kB
Private_Clean:         0 kB
Private_Dirty:         0 kB
Referenced:            4 kB
Anonymous:             4 kB
LazyFree:              0 kB
AnonHugePages:         0 kB
ShmemPmdMapped:        0 kB
FilePmdMapped:         0 kB
Shared_Hugetlb:        0 kB
Private_Hugetlb:       0 kB 
Swap:                  0 kB # 1번 프로세스가 사용인 Swap 메모리 크기 정보
SwapPss:               0 kB
...
```

이렇게 결과값에 프로세스가 사용중인 ```Swap``` 메모리정보가 들어가 있습니다.

만약 아래와 같은 결과값이 출력된다면,.,. ***Swap 메모리를 얼마나 사용하는지에 대한 계산은 다음과 같이 합니다.***
- **전체 20KB 중 7KB 를 Swap 메모리로 쓰고 있구나 !!!!**
```bash
$ cat smaps | more
...
55e67abb3000-55e67abb4000 r--p 00017000 ca:01 8618706                    /usr/lib/systemd/systemd
Size:                  20 kB
KernelPageSize:        4 kB
MMUPageSize:           4 kB
Rss:                   4 kB
Pss:                   2 kB
Pss_Dirty:             2 kB
Shared_Clean:          0 kB
Shared_Dirty:          4 kB
Private_Clean:         0 kB
Private_Dirty:         0 kB
Referenced:            4 kB
Anonymous:             4 kB
LazyFree:              0 kB
AnonHugePages:         0 kB
ShmemPmdMapped:        0 kB
FilePmdMapped:         0 kB
Shared_Hugetlb:        0 kB
Private_Hugetlb:       0 kB 
Swap:                  7 kB # 1번 프로세스가 사용인 Swap 메모리 크기 정보
SwapPss:               0 kB
...
```

허나 이 명령어는 프로세스의 메모리 영역별로 확인해아하기 때문에 한눈에 들어오기 쉽지 않습니다.

따라서 ```/proc/{PID}/status``` 파일을 사용하는것으로 프로세스의 ```Swap``` 메모리 참조를 감시하기에 좋습니다.

```bash
$ cat /proc/1/status
Name:	systemd
Umask:	0000
State:	S (sleeping)
Tgid:	1
Ngid:	0
Pid:	1
PPid:	0
TracerPid:	0
Uid:	0	0	0	0
Gid:	0	0	0	0
...
VmExe:	      44 kB
VmLib:	    9776 kB
VmPTE:	      92 kB
VmSwap:	       0 kB # 1번 프로세스의 Swap 메모리 사용량 !
```

---
## 5. Linux의 메모리 할당방법. 왜 메모리가 부족한가 ? - 메모리 재할당
### OverView
이렇게 ```Swap``` 메모리까지 학습하면서, 조금더 Linux 커널이 메모리를 관리하는 방법에 다가간것 같습니다.

그러나 메모리가 부족하다 라는 것이 어떤상황인지는 아직 감이 안잡힙니다.

이는 ***메모리 재할당 과정에 비밀이 있습니다.***

### 5.1 Buddy System
먼저 커널의 실제적 메모리할당 방법에 대해 이해해야 합니다.

Linux 커널은 Buddy System을 이용하서 프로세스에 메모리를 할당합니다.

***Buddy System은 메모리를 연속된 메모리 영역으로 관리합니다.***

Linux 커널은 메모리가 각각 ```4KB``` 로 구성되어 있습니다.
- 각각을 페이지라 하며, 가장 흔한 페이지 크기는 ```4KB``` 입니다.

***그래서 프로세스가 만약 4KB 를 원하면 , 페이지 1개(4KB) 만을 전달하고, 8KB를 원하면 페이지 2개를 전달합니다.***

이러한 Buddy System을 사용하기 때문에 프로세스 요청에 더 빠르게 응답이 가능합니다.

### 5.2 메모리 재할당 - 메모리가 부족한 원인
위처럼 커널은 프로세스에게 Buddy System을 통해 메모리를 할당하게 됩니다.

그렇다면 커널이 메모리를 ***재할당*** 하는 방법은 아래와 같습니다.

#### 1. 커널이 사용하는 캐시 메모리 재할당
이는 서비스 운영중 자연스럽게 발생하는 재할당 로직입니다.

커널은 메모리가 일을 안하는걸 못보는 성격 입니다. 따라서 가용영역에 남아있는 메모리들은 Cache용 메모리로 사용합니다.

허나 정작 실제 Application이 메모리를 할당받아 사용하려 했는데, 커널이 가용영역의 메모리를 Cache로 돌려둔 탓에 메모리가 부족할 수 있습니다.

**이때 커널은 Cache 영역의 메모리를 다시 가용 메모리 영역으로 돌리고 프로세스가 사용할 수 있게끔 재 할당 합니다.**

#### 2. Swap을 사용하는 재할당
이 부분이 성능 저하를 일으킵니다.

커널이 1번의 로직을 수행하고 Cache 영역의 메모리를 다 반환했음에도 프로세스의 메모리 요구사항을 충족시키지 못했다면, 이때 ```Swap``` 메모리 영역을 사용하게 됩니다.

커널은 프로세스 메모리 영역 중 Inactive(참조된지 오래되어 ```Swap``` 으로 사용 가능한 영역) 영역을 ```Swap``` 으로 옮기고, 해당 메모리 영역을 해제한다음 다른 프로세스에 할당합니다.

***이는 커널의 메모리 사용량이 증가할 뿐만 아니라, Swap 영역 자체의 I/O 속도가 실제 물리 메모리보다 느리기에 성능이 저하됩니다.***

## 6. 커널의 메모리 할당방법 커스텀하기
### OverView
지금까지의 정보를 읽다보면 커널은 메모리 영역을 ```Swap``` 으로 옮기려 하고, 캐시 재할당이 발생됬을 때 , 프로세스의 PageCache를 재할당하려는 경향이 있습니다.

이는 커널 파라미터로 값을 조절할 수 있습니다.

### 6.1 vm.swappiness
- 커널이 얼마나 공격적으로 메모리 영역을 ```Swap``` 영역으로 옮기는 가에 대한 파라미터
- Default : 60

  - 값이 커질수록 캐시를 비우지 않고 ```Swap``` 영역으로 옮기는 작업을 빠르게 진행
  - 값이 작아질수록 가능한 한 캐시를 비우는 작업을 진행

반영 명령어는 다음과 같습니다.
```bash
$ sysctl -w vm.swappiness=65
vm.swappiness = 65
```

이를 통해 커널이 메모리를 재할당 할 때 캐시 메모리를 재할당할 것인지 ```Swap``` 메모리를 재할당할 것인지 비율을 조절할 수 있음.

    무조건적인 캐시 해제가 좋은것만은 아님.

    캐시는 전반적인 응답속도를 높히는 장점이 있기에 
    
    상황에 따라 오히려 ```Swap``` 으로 내리는게 더 좋을수도 있음.

### 6.2 vm.vfs_cache_pressure
- 커널이 메모리를 재 할당 할 때 디렉터리나 inode 에 대한 캐시를 재할당 하려는 경향을 조절
- Default 100
  
  - 100 보다 크다 작다로 얼마나 많은 양을 재할당할 것인지 결정

반영 명령어는 다음과 같습니다.
```bash
$ sysctl -w vm.vfs_cache_pressure=100
vm.vfs_cache_pressure = 100
```

이를 통해 캐시 재할당을 빈번하게 혹은 적게 선택할 수 있음.

    이 값이 100 이상이 되면 미사용중이 아닌 캐시도 반환하려고 하기 때문에

    오히려 성능저하가 올 수 있음.

    충분한 테스트 이후 반영 필요

## 결론
알게된 Swap 영역을 감시하는것은 신뢰도있는 Application을 만드는것에 필수적이라 느낍니다.

만약 Application이 Swap 메모리 영역을 참조하는 비율이 선형적으로 증가한다면, 메모리 누수를 의심해봐야 할 것 입니다.

이런 경우 Application이 요청을 받고 끝나면 해당 메모리를 해제해야 하는데, 해제가 정상적으로 수행되지 않는것을 의미하기 때문입니다.

또한 반복적이지 않은 특정 시점에 Swap 메모리 영역의 사용량이 증가했다면, Swap 메모리로 갑자기 급증한 트래픽을 방어한것으로 볼 수 있을것 입니다. 이런경우가 이상적인것으로 보입니다.

메모리 사용량이 증가했다 해서, 무조건적인 수평 확장이 아니라 메모리의 누수를 점검할 수 있게 된것에 기쁩니다.