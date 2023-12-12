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
        // 직전 Tag 값을 가지고있는 전역변수 선언
        def previousTAG = '0'
        
        // 실제 소스코드 clone
        stage('git clone'){
            container('gradle'){
                sh '''
                git --version
                git clone https://github.com/jjsair0412/sample-code.git
                
                cd sample-code/sign_in_api
                '''
            }
        }
        
        
        // docker build
        stage('docker build'){
            container('docker'){
                sh '''
                docker --version
                
                
                cd sample-code/sign_in_api
                ls -al
                
                chmod +x gradlew
                
                docker build -t jjsair0412/test:${BUILD_NUMBER} .
                
                docker images | grep test
                '''
            }
        }
        
        // image push
        stage('docker push'){
            container('docker'){
                
                withDockerRegistry([ credentialsId: "$docker_login_information", url: ""]) {
                    sh '''
                    docker push jjsair0412/test:${BUILD_NUMBER}
                    '''
                }
                
            }
        }
        
        // helm chart의 image tag 최신으로 수정
        stage('helm chart version modify'){
            
            container('gradle'){
                withCredentials([gitUsernamePassword(credentialsId: 'git_access_token', gitToolName: 'git-tool')]) {
                    
                    sh '''
                    git clone https://github.com/jjsair0412/sample-code-cd.git
                    cd sample-code-cd
                        
                    
                    git config --global user.name "jjsair0412"
                    git config --global user.email "jjsair0412@naver.com"
                    git checkout -B main
                    '''
                    
                    // 이전 build number 전역변수에 값 할당
                    script {
                        previousTAG = sh(script: "cd sample-code-cd && sed -n 's/.*repository: jsair0412\\/test:\\(.*\\)/\\1/p' ./helm/app/values.yaml", returnStdout: true).trim()
                    }          
                    
                    
                    sh """
                        cd sample-code-cd
                        
                        sed -i 's|repository: jsair0412/test:$previousTAG|repository: jsair0412/test:${env.BUILD_NUMBER}|g' ./helm/app/values.yaml
                    """
                    
                    // 변동사항 push
                    sh '''
                    cd sample-code-cd
                    
                    cat ./helm/app/values.yaml
                    
                    # Git commit and push
                    git add .
                    git commit -m "Update image tag"
                    git push origin main
                    '''
                }
            }
        }
        
        
        // jjsair0412/test image 모두 제거하여 clean
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