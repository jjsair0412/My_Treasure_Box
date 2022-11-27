import java.text.SimpleDateFormat

// 빌드 날짜 생성
def dateFormat = new SimpleDateFormat("yyyyMMdd")
def date = new Date()
day = dateFormat.format(date)


podTemplate(label: 'test',
	containers: [
        containerTemplate(name: 'gradle', image: 'gradle', ttyEnabled: true, command: 'cat'),
        containerTemplate(name: 'docker', image: 'docker:latest', ttyEnabled: true, command: 'cat')
    ],
    volumes: [
        hostPathVolume(mountPath: '/usr/bin/docker', hostPath: '/usr/bin/docker'),
        hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock')
    ]) {
    
    node('test') {
        
        stage('git clone'){
            println(day) // 빌드 날짜 출력 , 차후 이미지 태그로 사용 예정
            container('gradle'){
                sh '''
                git --version
                git clone https://github.com/jjsair0412/sample-code.git
                ls
                
                cd sample-code/sign_in_api
                ls
                
                '''
            }
        }
        
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
        
        stage('docker build'){
            container('docker'){
                sh '''
                docker --version
                
                
                cd sample-code/sign_in_api
                ls
                
                docker build -t jjsair0412/test:$BUILD_NUMBER .
                
                docker images | grep test
                '''
            }
        }
        
        
        stage('docker push'){
            container('docker'){
                
                withDockerRegistry([ credentialsId: "$docker_login_information", url: ""]) {
                    sh '''
                    docker push jjsair0412/test:$BUILD_NUMBER
                    '''
                }
                
            }
        }
        
        stage('docker clean'){
            container('docker'){
                sh '''
                docker rmi $(docker images | grep jjsair0412/test |  awk 'NR > 1 {print $3}') --force
                docker images
                '''
                
            }
        }
    }
}