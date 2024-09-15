# Dockerfile Multi Stage 방안
Docker build시 하나의 Dockerfile 내부에서 stage를 여러개로 분리하여 이미지 크기를 줄이고, 빌드속도를 높히며, 빌드와 배포단계를 분리시켜 보안상의 이점을 가질 수 있는 방법
- [참고 문서](https://malwareanalysis.tistory.com/417)

## 1. 사용 사례
### 1.1 빌드와 배포 분리
빌드 단계와 배포단계를 분리시켜서 , 최종 배포용 container에 소스코드 및 빌드 도구등을 포함시키지 않아도 되기 때문에 이미지파일의 크기나 보안상의 이점을 가질 수 있음.

### 1.2 이미지파일의 크기 줄이기
소스코드나 정적파일등을 가지지 않고, 언어별 build가 진행된 뒤에 발생하는 배포용 파일(jar 등) 만 들고 배포가 가능하기떄문에, docker image파일 자체의 크기를 줄일 수 있음.

### 1.3 빌드속도 향상
Multi Stage방식을 사용하면, Dockerfile 작업을 병렬로 실행하여 빌드 속도를 향상시킬 수 있음.
- 조건 : 병렬로 실행하고자 하는 작업이 연관관계가 없어야 함

## 2. dockerbuild란 ?
MultiStage 기능은 Dockerbuild의 옵션기능이기에, Docker build가 무엇인인지 이해하는것이 필요합니다.

Docker build는 Docker 이미지를 생성하는 일련의 과정입니다.

Docker Image는 Dockerfile에 작성된 정의들(RUN, COPY, ADD ...) 을 따라서 순차적으로 실행되고, 각 단계에서 새로운 이미지레이어를 생성합니다. 그리고, 각 이미지 레이어들을 캐시에 저장하게 되는데, 이후 동일명령어로 다시 이미지를 빌드할 때, 변경되지 않은 레이어는 캐시에 저장된것을 사용함으로써 빌드속도를 단축합니다.

Dockerfile의 모든 명령어가 수행된 이후에, 새로운 이미지가 생성됩니다.

## 3. Multi Stage 사용 방법
Dockerfile 내부에서 여러개의 BaseImage를 지정하여 docker build를 수행합니다. BaseImage 는 Dockerfile에서 FROM 명령어에 설정된 이미지를 의미합니다.

FROM 키워드를 기준으로 작업공간이 분리되게 되는데, 분리된 작업공간을 Stage라 하며, Stage가 2개 이상일 경우를 Multi Stage라 합니다.

### 3.1 Multi Stage의 예시
#### 3.1.1 Multi Stage가 적용 안된상태의 Dockerfile
먼저, Multi Stage가 반영되지 않은 Dockerfile을 확인해 봅니다.
- 예시에 사용된 파일들은 자바 Application에 대한 Dockerfile 입니다.

아래 예시를 보면,  ```maven:3.6.3-jdk-11-slim``` Base Image 내부에서 단순하게 모든 소스코드와 정적 파일들을 다 포함해서 컨테이너 이미지를 생성하게 됩니다. 따라서 이미지자체의 크기도 클 뿐만 아니라, 소스코드 파일이 크기때문에 빌드 속도또한 느리게 진행됩니다.
```Dockerfile
# JDK와 Maven을 포함한 Base Image
FROM maven:3.6.3-jdk-11-slim
WORKDIR /app

# 소스 코드와 POM 파일 복사
COPY src /app/src
COPY pom.xml /app

# Maven을 이용해 애플리케이션 빌드
RUN mvn -f /app/pom.xml clean package

# 애플리케이션 실행
ENTRYPOINT ["java","-jar","/app/target/app.jar"]
```

#### 3.1.2 Multi Stage가 적용된 상태의 Dockerfile
같은 Java Application을 maven으로 빌드-배포하는 Dockerfile입니다. 그런데, 이 예시는 Multi Stage가 적용되어 있습니다.

각 stage는 ```FROM```을 기준으로 분리되며, ```FROM``` 명령어에는 ```AS``` 명령어를 추가하여 별칭을 지정할 수 있습니다.

stage에서 ```AS``` 로 추가한 별칭을 통해 다른 stage를 ```--from``` 명령어로 참조할 수 있기 때문에, 아래 Dockerfile처럼 build stage, deploy stage를 분리할 수 있습니다.

또한 가장 중요한것이, Multi Stage를 적용하게 되면, **가장 마지막에 실행된 stage 작업이, 도커 이미지로 생성** 되기 때문에, 아래 예시를 보면 2 stage에서 1 stage에서 build 과정을 통해 생성된 jar 파일만 갖고있기에 Docker 이미지파일 크기도 작을뿐더러 쓸모없는 파일(소스코드, 정적파일 등..) 을 들고있지 않아 보안상에 이점도 가질 수 있습니다.
```Dockerfile
# 1 stage : Maven을 사용하여 Java 애플리케이션 빌드
# Base Image : maven:3.6.3-jdk-11-slim
# AS (별칭) : build
FROM maven:3.6.3-jdk-11-slim AS build
WORKDIR /app

# 소스 코드와 POM 파일 복사
COPY src /app/src
COPY pom.xml /app

# Maven을 이용한 패키지 빌드
RUN mvn -f /app/pom.xml clean package

# 2 stage : JRE를 사용하여 런타임 이미지 생성
# Base Image : openjdk:11-jre-slim
FROM openjdk:11-jre-slim
WORKDIR /app

# 1단계에서 빌드한 JAR 파일을 현재 이미지로 복사
# --from=build 로 1 stage를 참조하여 1 stage에 생성된 app.jar 파일을 복사해서 2 stage에 가져옴
COPY --from=build /app/target/*.jar /app/app.jar

# 애플리케이션 실행
ENTRYPOINT ["java","-jar","app.jar"]
```