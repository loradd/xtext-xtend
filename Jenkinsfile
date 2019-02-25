node {
	properties([
		[$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', numToKeepStr: '15']],
		disableConcurrentBuilds(),
		parameters([
			choice(choices: 'oxygen\nphoton\nr201809\nr201812\nlatest', description: 'Which Target Platform should be used?', name: 'target_platform')
		])
		, pipelineTriggers([upstream(threshold: 'SUCCESS', upstreamProjects: 'xtext-eclipse/' + URLEncoder.encode("$BRANCH_NAME", "UTF-8"))])
	])
	
	stage('Checkout') {
		sh '''
			if [ -d ".git" ]; then
				git reset --hard
			fi
		'''

		checkout scm
		if ("latest" == params.target_platform) {
			currentBuild.displayName = "#${BUILD_NUMBER}(x)"
		} else if ("r201812" == params.target_platform) {
			currentBuild.displayName = "#${BUILD_NUMBER}(r)"
		} else if ("r201809" == params.target_platform) {
			currentBuild.displayName = "#${BUILD_NUMBER}(q)"
		} else if ("photon" == params.target_platform) {
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
	
	
	
	def mvnHome = tool 'M3'
	env.M2_HOME = "${mvnHome}"
	dir('.m2/repository/org/eclipse/xtext') { deleteDir() }
	dir('.m2/repository/org/eclipse/xtend') { deleteDir() }
	
	
	stage('Maven Tycho Build') {
		def targetProfile = "-Poxygen"
		if ("latest" == params.target_platform) {
			targetProfile = "-Platest"
		} else if ("r201812" == params.target_platform) {
			targetProfile = "-Pr201812"
		} else if ("r201809" == params.target_platform) {
			targetProfile = "-Pr201809"
		} else if ("photon" == params.target_platform) {
			targetProfile = "-Pphoton"
		} else if ("oxygen" == params.target_platform) {
			targetProfile = "-Poxygen"
		}
		wrap([$class:'Xvnc', useXauthority: true]) {
			sh "${mvnHome}/bin/mvn -f tycho-pom.xml --batch-mode -fae -Dmaven.test.failure.ignore=true -DJENKINS_URL=$JENKINS_URL -Dmaven.repo.local=${WORKSPACE}/.m2/repository -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn ${targetProfile} clean install"
		}
		step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/*.xml'])
	}
	
	archive 'org.eclipse.xtend.ide.swtbot.tests/screenshots/**, build/**, **/target/work/data/.metadata/.log, **/hs_err_pid*.log'
}
