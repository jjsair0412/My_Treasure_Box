# Redis vs Memcached
## 참고문서
- [Amazon_redis_vs_memcached](https://aws.amazon.com/ko/elasticache/redis-vs-memcached/)
- [Redis 성능관리 ebook preview](https://preview.hanbit.co.kr/2647/sample_ebook.pdf)
- [[Cache] Redis vs Memcached blog 1](https://chrisjune-13837.medium.com/redis-vs-memcached-10e796ddd717)
- [[Cache] Redis vs Memcached blog 2](https://americanopeople.tistory.com/148)

Redis와 Memcached는 모두 인 메모리 데이터 저장소로, 정적 데이터들을 캐시로 저장해두어 Application에서 빠르게 데이터를 가져올 수 있는 데이터 저장소 입니다.

## Overview
둘다 사용성에 있어서 간편하며 고성능을 가지고 있습니다.

그러나 캐시 메모리로 사용할 때 둘의 차이점을 정확하게 파악하고 사용해야 합니다.

Memcached는 단순성을 위해 설계되었으며 Redis는 다양한 데이터 구조를 지원하기에, 광범위한 사용 사례에 효과적인 풍부한 기능을 제공합니다.

## 차이점
### 1. 데이터 구조:
- Redis는 다양한 데이터 구조를 지원합니다. 이에는 String, Hash, List, Set, 정렬된 셋 등이 포함되며, 이를 통해 더 복잡한 애플리케이션을 구현할 수 있습니다.
- Memcached는 주로 간단한 키-값 저장소로 사용됩니다. 따라서 복잡한 데이터 구조를 필요로 하는 애플리케이션에는 적합하지 않을 수 있습니다.

### 2. 지속성:
- Redis는 디스크에 데이터를 지속적으로 저장할 수 있는 옵션을 제공합니다. 이를 통해 **시스템 재시작 후에도 데이터를 복구할 수 있습니다.**
- Memcached는 순수한 캐시 솔루션으로, **지속성을 제공하지 않습니다. 즉, 프로세스가 종료되면 모든 데이터가 사라집니다.**

### 3. 복제 및 확장성:
- Redis는 마스터-슬레이브 복제, 자동 파티셔닝, 클러스터링 등 다양한 고가용성 및 확장성 기능을 지원합니다.
- Memcached는 간단한 확장성은 제공하지만, Redis만큼의 고급 복제 및 클러스터링 기능은 없습니다.

### 4. 저장 공간
- Redis는 한 키에 저장할 수 있는 Value의 범위가 512MB
- Memcached는 한 키에 저장할 수 있는 Value의 범위가 1MB

### 5. 스레드
- Redis는 싱글 스레드만 지원하기 떄문에, 1번에 1개의 명령만 처리합니다.
- Memcahced는 멀티 스레드를 지원하기 때문에, Keys(저장된 모든키를 보여주는 명령어)나 flushall(모든 데이터 삭제)등의 명령어를 사용할 때, Redis에 비해 속도가 월등히 빠릅니다.

### 6. 안정성
- Redis는 트래픽이 몰리는 경우, 응답속도가 불안정하다 합니다.
- Memcached는 트래픽이 몰려도 Redis에 비해 응답 속도가 안정적입니다.
    - [관련문서](https://preview.hanbit.co.kr/2647/sample_ebook.pdf)

### 7. 기타 기능:
- Redis는 Lua 스크립팅, 트랜잭션, 다양한 종류의 타임아웃, pub-sub 모델 등 추가 기능을 제공합니다.
- Memcached는 Redis와 같은 다양한 추가기능을 제공하지 않고, 더 단순하고 기본적인 사용 사례에 초점을 맞춥니다.

## 결론
결론적으로 Memcached가 Redis보다 단순한 처리를 하기에 속도가 더 빠르고 안정적이지만, 순수한 캐시 솔루션으로 메모리에 저장된 값이 프로세스가 종료되면 없어진다거나, 다양한 데이터셋을 지원하지 않는다는 단점을 가집니다.

Redis는 Memcached보다 더 다양한 데이터셋과 기능들을 제공하며, 캐시에 저장된 데이터를 지속적으로 저장할 수 있는 방안이 있어 유지성이 뛰어나고 확장성 또한 더 좋습니다. 그러나 속도나 안정성면에서 Memcached보다 조금 떨어지는 부분이 없잖아 있습니다.

오직 캐시 기능과 속도, 메모리 지속성이 필요하지 않다면 ? -> Memcached
다양한 부가기능(트랜잭선, pus-sub 모델 등)과 캐시, 메모리에 저장된 값을 유지시킬 필요가 있다면 ? -> Redis