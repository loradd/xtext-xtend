pipeline {
  agent any

  options {
    buildDiscarder(logRotator(numToKeepStr:'15'))
  }

  tools { 
    maven 'apache-maven-latest'
    jdk 'oracle-jdk8-latest'
  }
  
  // https://jenkins.io/doc/book/pipeline/syntax/#triggers
  triggers {
    pollSCM('H/5 * * * *')
  }
  
  stages {
    stage('Checkout') {
      steps {
        script {
          properties([
            [$class: 'GithubProjectProperty', displayName: '', projectUrlStr: 'https://github.com/eclipse/xtext-core/'],
            [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false],
            parameters([
              choice(choices: ['oxygen', 'photon', 'r201809', 'latest'], 
              description: 'Which Target Platform should be used?', 
              name: 'target_platform')
            ]),
            pipelineTriggers([githubPush()])
          ])
        }

        sh '''
          if [ -d ".git" ]; then
            git reset --hard
          fi
        '''
        
        checkout scm
        
        script {
          if ("latest" == params.target_platform) {
            currentBuild.displayName = "#${BUILD_NUMBER}(x)"
          } else if ("r201809" == params.target_platform) {
            currentBuild.displayName = "#${BUILD_NUMBER}(q)"
          } else if ("photon" == params.target_platform) {
            currentBuild.displayName = "#${BUILD_NUMBER}(p)"
          } else {
            currentBuild.displayName = "#${BUILD_NUMBER}(o)"
          }
        }

        dir('build') { deleteDir() }

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
            echo "Redirecting target platforms in $targetfile to $BRANCH_NAME"
            sed_inplace "s?<repository location=\\".*/job/\\([^/]*\\)/job/[^/]*/?<repository location=\\"$JENKINS_URL/job/\\1/job/$escapedBranch/?" $targetfile
          done
        '''
      }
    }

    stage('Gradle Build') {
      steps {
        sh '''
          ./gradlew clean cleanGenerateXtext build createLocalMavenRepo \
          -PuseJenkinsSnapshots=true \
          -PJENKINS_URL=$JENKINS_URL \
          -PcompileXtend=true \
          -PignoreTestFailures=true \
          --refresh-dependencies \
          --continue'''
        step([$class: 'JUnitResultArchiver', testResults: '**/build/test-results/test/*.xml'])
      }
    }
    
    stage('Build & Test') {
      parallel {
        stage('Maven Plugin Build') {
          steps {
            configFileProvider(
              [configFile(fileId: '7a78c736-d3f8-45e0-8e69-bf07c27b97ff', variable: 'MAVEN_SETTINGS')])
            {
              sh '''
                mvn \
                  -s $MAVEN_SETTINGS \
                  -f maven-pom.xml \
                  --batch-mode \
                  --update-snapshots \
                  -fae \
                  -PuseJenkinsSnapshots \
                  -DJENKINS_URL=$JENKINS_URL \
                  -DupstreamBranch=kth_issue1309_cbi \
                  -Dmaven.test.failure.ignore=true \
                  -Dmaven.repo.local=${WORKSPACE}/.m2/repository \
                  -DgradleMavenRepo=file:${WORKSPACE}/build/maven-repository/ \
                  -DtestSettingsXML=${WORKSPACE}/org.eclipse.xtend.maven.plugin/src/test/resources/settings.xml \
                  -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn \
                  clean deploy
              '''
            }
          }
        }
        
        stage('Maven Build') {
          steps {
            dir('.m2/repository/org/eclipse/xtext') { deleteDir() }
            dir('.m2/repository/org/eclipse/xtend') { deleteDir() }
    
            configFileProvider(
              [configFile(fileId: '7a78c736-d3f8-45e0-8e69-bf07c27b97ff', variable: 'MAVEN_SETTINGS')]) {
              sh '''
                if [ "latest" == "$target_platform" ] 
                then
                  export targetProfile="-Platest"
                elif [ "r201809" == "$target_platform" ] 
                then
                  export targetProfile="-Pr201809"
                elif [ "photon" == "$target_platform" ] 
                then
                  export targetProfile="-Pphoton"
                else
                  export targetProfile="-Poxygen"
                fi
                mvn \
                  -s $MAVEN_SETTINGS \
                  -f tycho-pom.xml \
                  --batch-mode \
                  -fae \
                  -DskipTests \
                  -Dmaven.test.failure.ignore=true \
                  -Dmaven.repo.local=.m2/repository \
                  -DJENKINS_URL=$JENKINS_URL \
                  -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn \
                  $targetProfile \
                  clean install
              '''
            }
    
            // step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/*.xml'])
    
          }
        }
    
        stage('Gradle Longrunning Tests') {
          steps {
            sh '''
              ./gradlew \
                longrunningTest \
                -PuseJenkinsSnapshots=true \
                -PJENKINS_URL=$JENKINS_URL \
                -PignoreTestFailures=true \
                --continue
            '''
            step([$class: 'JUnitResultArchiver', testResults: '**/build/test-results/longrunningTest/*.xml'])
          }
        }
      }
    }


    
  }

  post {
    success {
      archiveArtifacts artifacts: 'build/**'
    }
    failure {
      archiveArtifacts artifacts: '**/target/work/data/.metadata/.log, **/hs_err_pid*.log'
    }
  }
}