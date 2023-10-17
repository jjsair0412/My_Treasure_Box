# redis와 SpringBoot 연동방안
## Overview
Redis는 두종류로 나뉩니다.

1. 클러스터링 된 Redis
2. Single Node Redis

클러스터링 되어있는 Redis일경우와 , Single Node일 경우 Spring과 연동하는 방법이 달라집니다.

## 1. 의존성 추가
Gradle에 아래 의존성을 추가합니다.

```bash
// https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-data-redis
implementation 'org.springframework.boot:spring-boot-starter-data-redis:3.1.3'
```

## 2. application.properties 수정
Redis와 연동하기 위해 Application.properties를 설정합니다.

이때 Single Node Redis인지, Cluster Redis인지 나뉘게 됩니다.

### 2.1 단일 노드 Redis
- 아래와 같이 host 주소와 port를 입력해줍니다.
```application.properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### 2.2 Redis-cluster
- 클러스터링 된 Redis일 경우 아래와 같이 설정합니다.

#### 2.2.1 Kubernetes에 배포된 Redis일 경우
- [helm 설치방안](./install-redis-helm.md) 방식대로 설치된 redis-cluster 일 경우 , headless service 를 통해 각 Redis pod로 직접 연결해야 합니다.
```application.properties
spring.data.redis.cluster.nodes=\
  redis-cluster-0.redis-cluster-headless.redis.svc.cluster.local:6379,\
  redis-cluster-1.redis-cluster-headless.redis.svc.cluster.local:6379,\
  redis-cluster-2.redis-cluster-headless.redis.svc.cluster.local:6379,\
  redis-cluster-3.redis-cluster-headless.redis.svc.cluster.local:6379,\
  redis-cluster-4.redis-cluster-headless.redis.svc.cluster.local:6379,\
  redis-cluster-5.redis-cluster-headless.redis.svc.cluster.local:6379
```

#### 2.2.2 일반적으로 설치된 Redis일 경우
- [redis-info](./redis-info.md) 문서 방식대로 설치된 Redis cluster 일 경우, 모든 Redis에 대한 접근정보를 입력하여 직접 모든 노드와 연결해야 합니다.
```application.properties
spring.data.redis.cluster.nodes=\
  localhost:6379,\
  localhost:6380,\
  localhost:6381,\
  localhost:6382,\
  localhost:6383,\
  localhost:6384
```

## 3. Configuration 설정
Redis와 연동할 RedisConfig 객체를 만들어줍니다.
- application.properties에서 설정하면 필요 없는듯 ? 테스트필요
```java
@Configuration
@EnableRedisRepositories
@RequiredArgsConstructor
public class RedisGlobalConfig {

    private final RedisConnectionFactory redisConnectionFactory;

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }
}
```

## 4. Redis 사용
```RedisGlobalConfig``` 클래스의 ```redisTemplate()``` 메서드 반환값인 ```RedisTemplate``` 을 사용하여 Redis를 사용합니다.

해당 예시코드는 Spring Batch의 JobExecutionListener 인터페이스를 재정의한 클래스 입니다.
- 잡을 시작하기 전 키를 생성하고, 잡이 완료될경우에만 키를 삭제합니다.
### 4.1 키 생성
- Redis key 생성
```java
@Value("${redis.LockName}")
private String LockName;

private final RedisTemplate<String, Object> redisTemplate;

@Override
public void beforeJob(JobExecution jobExecution) {
    // key timeout시간 40분으로 설정. 사용되지않고 40분동안 있으면 자동제거됨
    redisTemplate.opsForValue().set(LockName,true, 40, TimeUnit.MINUTES); 
}
```
### 4.2 키 삭제
- Redis Key 삭제
```java
@Value("${redis.LockName}")
private String LockName;

private final RedisTemplate<String, Object> redisTemplate;

@Override
public void afterJob(JobExecution jobExecution) {
    if(jobExecution.getStatus() == BatchStatus.COMPLETED){
        redisTemplate.delete(partLockName);
    } else {
        throw new RuntimeException("Part Batch job failed");
    }
}
```