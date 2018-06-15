node('buildOnDocker') {

	checkout([ 
	    $class: 'GitSCM', 
	    branches: [
	        [name: '*/${gitlabBranch}']
	        ], 
	        doGenerateSubmoduleConfigurations: false, 
	        extensions: [], 
	        submoduleCfg: [], 
	        userRemoteConfigs: [
	            [
	                credentialsId: '4306e8ea-70bb-4966-9d0b-941f132e552f', 
	                url: 'ssh://git@git/infraestrutura/jenkins.git'
	            ]
	        ]
	  ])

	stage('sonar-validate') {
        def sonarqubeScannerHome = tool name: 'Sonar';
            withCredentials([[
                $class: 'StringBinding', 
                credentialsId: 'SONARTOKEN', 
                variable: 'SONARTOKEN'
            ]]) 
                {
                    sh "${sonarqubeScannerHome}/bin/sonar-scanner \
                    -Dsonar.projectKey=jenkins:${gitlabBranch} \
                    -Dsonar.projectName=jenkins:${gitlabBranch} \
                    -Dsonar.projectVersion=`git rev-parse --short HEAD` \
                    -Dsonar.login=${env.SONARTOKEN} \
                    -Dsonar.host.url=http://sonar.com.br \
                    -Dsonar.sources=./"
	            }
    }

    stage('Quality Gate Check'){
    sh '''
      PROJECT_STATUS=$(curl sonar.com.br/api/qualitygates/project_status?projectKey=jenkins:${gitlabBranch}|jq .projectStatus.status)
      if [ ${PROJECT_STATUS} = \\"ERROR\\" ]; then
        echo "Project didn't pass on Sonar quality check"
        #exit 1
      fi
    '''
  }
  stage('email') {
    emailext attachLog: true, body: 'Check console output at $BUILD_URL to view the results.', subject: '$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS!', to: '$EMAIL_INFRA_DIGITAL'
  }
}
