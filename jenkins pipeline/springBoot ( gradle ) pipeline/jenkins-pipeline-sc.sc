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
                
                docker build -t test:latest .
                
                docker images | grep test
                '''
            }
        }
        
        
        stage('docker tag'){
            container('docker'){
                sh '''
                docker image tag test:latest jjsair0412/test:latest
                docker images | grep test
                '''
            }
        }
        
        stage('docker push'){
            container('docker'){
                
                withDockerRegistry([ credentialsId: "$docker_login_information", url: ""]) {
                    sh '''
                    docker push jjsair0412/test:latest
                    '''
                }
                
            }
        }
        
        stage('docker clean'){
            container('docker'){
                sh '''
                docker rmi jjsair0412/test
                docker rmi test
                docker images
                '''
                
            }
        }
    }
}