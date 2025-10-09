# DockerFile Best Pretice
원본 글 : https://medium.com/@devlinktips/dockerfiles-10-things-seniors-do-that-juniors-dont-68b3a4f40c3e

DockerFile 작성 시 CheckList

## 1. 캐시 정책 준수
Docker Build 시 Layer들은 캐시처리가 되며 빠른 속도를 보장

코드 수정 시 캐시를 깨트리지 않는 작성법으로 빌드시간을 단축시켜야 함.

### 잘못된 CASE
- 코드 한줄만 수정해도 모든 레이어가 다시 빌드됨.
```bash
FROM node:20
COPY . .
RUN npm install
RUN npm run build
CMD ["npm", "start"]
```
### 올바른 CASE
- ```npm install``` 은 의존성 패키지 변경이 있을 경우에만 실행됨.
- 코드 수정만으론 마지막 ```npm run build``` 만 다시 돌아감.
```bash
FROM node:20
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build
CMD ["npm", "start"]
```

## 2. Multi Stage Build 필수
컴파일러, 임시파일 등 모든것을 가지고있는 무거운 컨테이너는 금물

Runtime에 필요한 파일만 포함해야 함.
```bash
FROM golang:1.22 as builder
WORKDIR /src
COPY . .
RUN go build -o app

FROM gcr.io/distroless/base
COPY --from=builder /src/app /app
CMD ["/app"]
```

## 3. dockerignore 활용하기
.git, 로그파일, node_module 등 필요없는 파일은 제외

```bash
node_modules
.git
*.log
```

## 4. ARG vs ENV의 구분
둘은 확연히 다른 명령어임.

||||
|--|--|--|
|구분|시점|유지여부|
|ARG|빌드 타임|빌드후 사라짐|
|ENV|런타임|컨테이너 내에 유지됨|

```bash
ARG NODE_ENV=production
ENV NODE_ENV=$NODE_ENV
```

이 둘을 구분해서 사용해야 함. 안그러면 보안 위협이 있을 수 있거나 캐시가 깨질 수 있음.

## 5. DockerFile HEALTHCHECK 활용
Docker 특성 상, 컨테이너 내부 Application 프로세스가 죽어도 Running 상태를 표시함.

따라서 HEALTHCHECK는 필수.

**만약 K8s 환경이라면, Probe의 HEALTHCHECK와 중복되며, Probe만 사용됨. -> 트래픽 라우팅 제어**

ECS, 컨테이너 단일 환경일 경우 꼭 사용하자.

```bash
...
HEALTHCHECK CMD curl -f http://localhost:3000 || exit 1
...
```

## 6. 의존성 버전 고려
```apt-get install nodejs``` 이런식으로 작성할 경우, 업스트림 업데이트 시 빌드 실패할 수 있음.

따라서 아래처럼 특정 버전을 고정시켜서, 빌드실패 가능성을 낮춤.

```bash
RUN apt-get update && apt-get install -y nodejs=20.10.0-1nodesource1
```

## 7. root 권한 부여 X
Docker 컨테이너는 기본적으로 root 권한이 있음.

이는 권한탈취 등의 보안사고가 일어날 확률이 굉장히 높아서, 아래처럼 유저를 꼭 추가하여 막아야 함.

```bash
RUN adduser app && chown -R app /app
USER app
```

## 8. Build Secrets 사용
이미지 내부에 키 등의 보안 중요 사항을 넣으면 안됨.

API 키 등..

BuildKit secret mount를 사용하면, 빌드이후 생성되는 이미지에 키를 포함하지 않음
```bash
# enable BuildKit: DOCKER_BUILDKIT=1 docker build .
RUN --mount=type=secret,id=npmrc \
    npm install
```

## 9. Layer 정리하여 이미지 크기 최소화
DockerFile의 각 RUN 명령어는 새로운 Layer를 생성함.

캐시를 지우지 않을경우, 이미지가 계속 커질 수 있음.

### 나쁜 예
```bash
RUN apt-get update && apt-get install -y build-essential
```

### 좋은 예
```bash
RUN apt-get update && apt-get install -y build-essential \
    && rm -rf /var/lib/apt/lists/*
```

