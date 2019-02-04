pipeline {
  agent {
    kubernetes {
      label 'build-test-pod'
      defaultContainer 'jnlp'
      yaml '''
        apiVersion: v1
        kind: Pod
        spec:
          containers:
          - name: jnlp
            image: 'eclipsecbi/jenkins-jnlp-agent'
            args: ['\$(JENKINS_SECRET)', '\$(JENKINS_NAME)']
            volumeMounts:
            - mountPath: /home/jenkins/.ssh
              name: volume-known-hosts
          - name: plugin-build
            image: mickaelistria/wildwebdeveloper-build-test-dependencies@sha256:c9336c2b3ab06cc803e7465c2c1a3cea58bd09cbe5cbaf44f3630a77a9290e2f
            tty: true
            command: [ "uid_entrypoint", "cat" ]
          volumes:
          - configMap:
              name: known-hosts
            name: volume-known-hosts
    '''
    }
  }
  
  parameters {
    choice(name: 'TARGET_PLATFORM', choices: ['latest', 'oxygen', 'photon', 'r201809', 'r201812'], description: 'Which Target Platform should be used?')
  }

  options {
    buildDiscarder(logRotator(numToKeepStr:'15'))
  }

  // https://jenkins.io/doc/book/pipeline/syntax/#triggers
  triggers {
    pollSCM('H/5 * * * *')
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
        
        script {
          if (params.TARGET_PLATFORM == 'latest') {
            currentBuild.displayName = "#${BUILD_NUMBER}(4.11)"
          } else if (params.TARGET_PLATFORM == 'r201812') {
            currentBuild.displayName = "#${BUILD_NUMBER}(4.10)"
          } else if (params.TARGET_PLATFORM == 'r201809') {
            currentBuild.displayName = "#${BUILD_NUMBER}(4.9)"
          } else if (params.TARGET_PLATFORM == 'photon') {
            currentBuild.displayName = "#${BUILD_NUMBER}(4.8)"
          } else {
            currentBuild.displayName = "#${BUILD_NUMBER}(4.7)"
          }
        }

        dir('build') { deleteDir() }
        dir('.m2/repository/org/eclipse/xtext') { deleteDir() }
        dir('.m2/repository/org/eclipse/xtend') { deleteDir() }

        sh '''
          escaped() {
            echo $BRANCH_NAME | sed 's/\\//%252F/g'
          }
          
          escapedBranch=$(escaped)
          
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
            sed_inplace "s?<repository location=\\".*/job/\\([^/]*\\)/job/[^/]*/?<repository location=\\"$JENKINS_URL/job/\\1/job/$escapedBranch/?" $targetfile
          done
        '''
      }
    }
    
    stage('Gradle Build') {
      steps {
        container('jnlp') {
          sh "./1-gradle-build.sh"
          step([$class: 'JUnitResultArchiver', testResults: '**/build/test-results/test/*.xml'])
        }
      }
    }

    stage('Maven Build & Test') {
      parallel {
        stage('Maven Plugin Build') {
          steps {
            container('plugin-build') {
            dir('.m2/repository/org/eclipse/xtext') { deleteDir() }
            dir('.m2/repository/org/eclipse/xtend') { deleteDir() }
            configFileProvider([configFile(fileId: '7a78c736-d3f8-45e0-8e69-bf07c27b97ff', variable: 'MAVEN_SETTINGS')]) {
              sh "./2-maven-plugin-build.sh"
              step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/*.xml'])
            }
            }
          } // END steps
        } // END stage
        
        stage('Maven Tycho Build') {
          steps {
            container('plugin-build') {
              configFileProvider([configFile(fileId: '7a78c736-d3f8-45e0-8e69-bf07c27b97ff', variable: 'MAVEN_SETTINGS')]) {
                wrap([$class:'Xvnc', useXauthority: true]) {
                  sh "./3-maven-tycho-build.sh -s $MAVEN_SETTINGS --tp=${params.TARGET_PLATFORM}"
                }
                step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/*.xml'])
              }
            }
          }// END steps
        } // END stage
        
        stage('Gradle Longrunning Tests') {
          steps {
            container('plugin-build') {
            sh "./gradlew longrunningTest -PuseJenkinsSnapshots=true -PJENKINS_URL=$JENKINS_URL -PignoreTestFailures=true --continue"
            step([$class: 'JUnitResultArchiver', testResults: '**/build/test-results/longrunningTest/*.xml'])
            }
          }
        } // END stage
      } // END parallel
    } // END stage
  }

  post {
    success {
      archiveArtifacts artifacts: 'build/**'
    }
    failure {
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
        
        slackSend message: "${curResult}: <${env.BUILD_URL}|${env.JOB_NAME}#${env.BUILD_NUMBER}${envName}>", botUser: true, channel: 'xtext-builds', color: "${color}"
      }
    }
  }
}
