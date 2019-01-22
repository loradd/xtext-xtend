pipeline {
  agent any

  options {
    buildDiscarder(logRotator(numToKeepStr:'15'))
  }

  tools { 
    maven 'M3'
  }
  
  parameters {
    choice(name: 'TARGET_PLATFORM', choices: ['oxygen', 'photon', 'r201809', 'r201812'], description: 'Which Target Platform should be used?')
  }
  
  
  stages {
    stage('Checkout') {
      steps {
        sh '''
          if [ -d ".git" ]; then
            git reset --hard
          fi
        '''
        checkout scm
      }
      if ("latest" == params.TARGET_PLATFORM) {
        currentBuild.displayName = "#${BUILD_NUMBER}(x)"
      } else if ("r201812" == params.TARGET_PLATFORM) {
        currentBuild.displayName = "#${BUILD_NUMBER}(r)"
      } else if ("r201809" == params.TARGET_PLATFORM) {
        currentBuild.displayName = "#${BUILD_NUMBER}(q)"
      } else if ("photon" == params.TARGET_PLATFORM) {
        currentBuild.displayName = "#${BUILD_NUMBER}(p)"
      } else {
        currentBuild.displayName = "#${BUILD_NUMBER}(o)"
      }

      sh '''
        sed_inplace() {
          if [[ "$OSTYPE" == "darwin"* ]]; then
            sed -i '' "$@"
          else
            sed -i "$@" 
          fi  
        }
        
        targetfiles="$(find releng -type f -iname '*.target')"
        for targetfile in $targetfiles
        do
          echo "Redirecting target platforms in $targetfile to $JENKINS_URL"
          sed_inplace "s?<repository location=\\".*/job/\\([^/]*\\)/job/\\([^/]*\\)/?<repository location=\\"$JENKINS_URL/job/\\1/job/\\2/?" $targetfile
        done
      '''
    }

    stage('Maven Build & Test') {
      parallel {
        stage('Gradle Build') {
          steps {
            sh "./1-gradle-build.sh"
            step([$class: 'JUnitResultArchiver', testResults: '**/build/test-results/test/*.xml'])
          }
        }
    
        stage('Maven Plugin Build') {
          steps {
            dir('.m2/repository/org/eclipse/xtext') { deleteDir() }
            dir('.m2/repository/org/eclipse/xtend') { deleteDir() }
            sh "./2-maven-plugin-build.sh"
            step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/*.xml'])
          }
        }
    
        stage('Maven Tycho Build') {
          steps {
            wrap([$class:'Xvnc', useXauthority: true]) {
              sh "./3-maven-tycho-build.sh --tp=${params.TARGET_PLATFORM}"
            }
            step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/*.xml'])
          }
        }
      }
    }
  }

  post {
    success {
      archiveArtifacts artifacts: 'org.eclipse.xtend.ide.swtbot.tests/screenshots/**, build/**, **/target/work/data/.metadata/.log, **/hs_err_pid*.log'
    }
    changed {
      script {
        def envName = ''
        if (env.JENKINS_URL.contains('ci.eclipse.org/xtext')) {
          envName = ' (JIPP)'
        } else if (env.JENKINS_URL.contains('jenkins.eclipse.org/xtext')) {
          envName = ' (CBI)'
        } else if (env.JENKINS_URL.contains('typefox.io')) {
          envName = ' (TF)'
        }
        
        def curResult = currentBuild.currentResult
        def color = '#00FF00'
        if (curResult == 'SUCCESS' && currentBuild.previousBuild != null) {
          curResult = 'FIXED'
        } else if (curResult == 'UNSTABLE') {
          color = '#FFFF00'
        } else if (curResult == 'FAILURE') {
          color = '#FF0000'
        }
        
        slackSend message: "${curResult}: <${env.BUILD_URL}|${env.JOB_NAME}#${env.BUILD_NUMBER}${envName}>", baseUrl: 'https://itemis.slack.com/services/hooks/jenkins-ci/', botUser: true, channel: 'xtext-builds', color: '#00FFFF', token: '1vbkhv8Hwlp3ausuFGj1BdJb'
      }
    }
  }
}
