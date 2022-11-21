# springBoot code
## 1. code build information

1. gradle build
2. jdk 17
3. spring boot

### 1.1 pipeline process
1. git clone
2. gradle clean build
3. docker build
4. docker tag
5. docker push
6. docker rm -rf ( docker image clean )

## 2. jenkins 설치 환경 환경 및 주의 사항
k8s cluster에 jenkins helm 설치
- k8s cluster container runtime = docker
### 2.0 k8s에 배포된 jenkins가 작동하는 방식
k8s cluster에 배포된 jenkins는 agent를 Pod로 생성합니다.
( kubernetes plugin을 사용하여 k8s와 연동 . jenkins를 따로 설치하더라도 해당 plugin으로 k8s와 연동 가능 )

pod의 template ( pod yaml descriptor ) 은 pipeline에서 직접 설정해 주어도 되고 , 
jenkins 관리 -> 노드 관리 -> Configure Clouds 에서 설정해도 됩니다.

agent pod가 미리 정의해 둔 template에 맞게 생성되고 그 안에서 jenkins pipeline이 돌게 됩니다.

이때 만약 gradle로 코드를 빌드해야 한다면 ,, gradle container를 선택합니다..
그리고 docker 명령어를 수행해야 한다면 ,, docker container를 선택합니다..

```
...
	containers: [
        containerTemplate(name: 'gradle', image: 'gradle', ttyEnabled: true, command: 'cat'), # gradle container
        containerTemplate(name: 'docker', image: 'docker:latest', ttyEnabled: true, command: 'cat') # docker container 
    ]
...
```

아래와 같이 특정 image가 필요한 순간마다 agent pod의 container를 선택해서 pipeline을 수행합니다.

1. gradle build
```
...
         stage('gradle build'){
            
            container('gradle'){
                sh '''
                ls
                cd sample-code/sign_in_api
                ls
                
                gradle clean build
                '''
            }
        }
...
```

2. docker build
```
        stage('docker build'){
            container('docker'){
                sh '''
                docker --version
                
                
                cd sample-code/sign_in_api
                ls
                
                docker build -t test:latest .
                
                docker images | grep test
                '''
            }
        }
```

### 2.1 docker in docker
docker in docker 설정 필요
agent pod template에 아래와 같은 volume mount 설정 추가

추가시 주의할 점으로는 모든 worker에 docker가 설치 되어 있아야 함 
( jenkins agent pod가 뜨는 worker의 docker sock 파일을 mount하여 docker 컨테이너 내부에서 docker 사용하는 방법 ) 

```
...
    volumes: [
        hostPathVolume(mountPath: '/usr/bin/docker', hostPath: '/usr/bin/docker'),
        hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock')
    ]) {
...
```
### 2.2 docker login information
jenkins에 Credentials Parameter 추가하여 docker login정보 추가

Credential type으로 Username with password 설정
jenkins pipeline에서 아래와 같이 참조

```
...
        stage('docker push'){
            container('docker'){
                
                withDockerRegistry([ credentialsId: "$docker_login_information", url: ""]) {
                    sh '''
                    docker push jjsair0412/test:latest
                    '''
                }
                
            }
        }
...
```

    