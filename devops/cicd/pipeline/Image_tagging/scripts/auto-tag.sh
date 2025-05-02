#!/bin/bash
set -e

# 설정
DOCKER_REGISTRY="your-registry.com"
IMAGE_NAME="your-app"

# 현재 브랜치 가져오기
BRANCH_NAME=$(git rev-parse --abbrev-ref HEAD)

# 특정 브랜치에 따른 환경 설정
if [[ "$BRANCH_NAME" == "develop" ]]; then
  ENV="dev"
  # 최신 dev 태그 찾기
  LATEST_TAG=$(git tag -l "v*-dev" | sort -V | tail -n 1 || echo "v0.0.0-dev")
  LATEST_VERSION=${LATEST_TAG%-dev}
  
  # 버전 파싱 및 증가
  VERSION_PARTS=(${LATEST_VERSION//./ })
  NEW_PATCH=$((VERSION_PARTS[2] + 1))
  NEW_TAG="v${VERSION_PARTS[0]}.${VERSION_PARTS[1]}.${NEW_PATCH}-dev"

elif [[ "$BRANCH_NAME" =~ ^release/.* ]]; then
  ENV="stage"
  # 최신 dev 태그 찾기
  LATEST_TAG=$(git tag -l "v*-dev" | sort -V | tail -n 1 || echo "v0.0.0-dev")
  LATEST_VERSION=${LATEST_TAG%-dev}
  
  # 버전 파싱 및 마이너 버전 증가
  VERSION_PARTS=(${LATEST_VERSION//./ })
  NEW_MINOR=$((VERSION_PARTS[1] + 1))
  NEW_TAG="v${VERSION_PARTS[0]}.${NEW_MINOR}.0-stage"

elif [[ "$BRANCH_NAME" == "master" || "$BRANCH_NAME" == "main" ]]; then
  ENV="prod"
  # 최신 stage 태그 찾기
  LATEST_TAG=$(git tag -l "v*-stage" | sort -V | tail -n 1 || echo "v0.0.0-stage")
  LATEST_VERSION=${LATEST_TAG%-stage}
  
  # 메이저 릴리스 여부 확인
  read -p "메이저 버전을 증가시키겠습니까? (y/n): " MAJOR_RELEASE
  
  # 버전 파싱 및 증가
  VERSION_PARTS=(${LATEST_VERSION//./ })
  
  if [[ "$MAJOR_RELEASE" == "y" || "$MAJOR_RELEASE" == "Y" ]]; then
    NEW_MAJOR=$((VERSION_PARTS[0] + 1))
    NEW_TAG="v${NEW_MAJOR}.0.0-prod"
  else
    NEW_TAG="v${VERSION_PARTS[0]}.${VERSION_PARTS[1]}.${VERSION_PARTS[2]}-prod"
  fi

elif [[ "$BRANCH_NAME" =~ ^hotfix/.* ]]; then
  ENV="hotfix"
  # 최신 prod 태그 찾기
  LATEST_TAG=$(git tag -l "v*-prod" | sort -V | tail -n 1 || echo "v0.0.0-prod")
  LATEST_VERSION=${LATEST_TAG%-prod}
  
  # 버전 파싱 및 패치 버전 증가
  VERSION_PARTS=(${LATEST_VERSION//./ })
  NEW_PATCH=$((VERSION_PARTS[2] + 1))
  NEW_TAG="v${VERSION_PARTS[0]}.${VERSION_PARTS[1]}.${NEW_PATCH}-hotfix"

else
  echo "현재 브랜치 $BRANCH_NAME는 자동 태깅이 지원되지 않습니다."
  exit 0
fi

# 태그 및 환경 정보 출력
echo "브랜치: $BRANCH_NAME"
echo "환경: $ENV"
echo "새 태그: $NEW_TAG"

# 사용자 확인
read -p "이 태그로 계속 진행하시겠습니까? (y/n): " CONTINUE

if [[ "$CONTINUE" != "y" && "$CONTINUE" != "Y" ]]; then
  echo "작업이 취소되었습니다."
  exit 0
fi

# Docker 이미지 빌드 및 태깅
echo "Docker 이미지 빌드 중..."
docker build -t ${DOCKER_REGISTRY}/${IMAGE_NAME}:${NEW_TAG} .

# latest-{env} 태그 추가
docker tag ${DOCKER_REGISTRY}/${IMAGE_NAME}:${NEW_TAG} ${DOCKER_REGISTRY}/${IMAGE_NAME}:latest-${ENV}

###### 해당 부분은 Jenkins, Gitlab_ci 등 설정값에 따라 사용 안할 수 있음 ######
while true; do 
  read -p "Docker 이미지를 레지스트리에 푸시하시겠습니까? (y/n): " PUSH_IMAGE 

  if [[ "$PUSH_IMAGE" == "y" || "$PUSH_IMAGE" == "Y" || "$PUSH_IMAGE" == "yes" || "$PUSH_IMAGE" == "YES" ]]; then
      echo "이미지를 Registry 에 PUSH 합니다.."
      docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:${NEW_TAG}
      break
  elif [[ "$PUSH_IMAGE" == "n" || "$PUSH_IMAGE" == "N" || "$PUSH_IMAGE" == "no" || "$PUSH_IMAGE" == "NO" ]]; then
    echo "이미지 푸시를 건너뜁니다."
    break
  else
    echo "y 또는 n을 입력해 주세요."
  fi
done