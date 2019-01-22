if [ -z "$JENKINS_URL" ]; then
  # if not set in environment use default
  JENKINS_URL=https://ci.eclipse.org/xtext/
fi

# Use TARGET_PLATFORM from environment and 'oxygen' as default.
# Overridable by 'tp' command-line arg
if [ -z "$TARGET_PLATFORM" ]; then
  TARGET_PLATFORM=oxygen
fi

# args parsing inspired by https://gist.github.com/jehiah/855086
function usage()
{
  echo "Perform Maven build for xtext-eclipse"
  echo ""
  echo "./1-maven-build.sh"
  echo -e "\t-h --help"
  echo -e "\t--tp=$TARGET_PLATFORM (valid values: oxygen,photon,r201809,r201812,latest)"
  echo ""
}

while [ "$1" != "" ]; do
  PARAM=`echo $1 | awk -F= '{print $1}'`
  VALUE=`echo $1 | awk -F= '{print $2}'`
  case $PARAM in
    -h | --help)
      usage
      exit
      ;;
    --tp)
      TARGET_PLATFORM=$VALUE
      ;;
    *)
      echo "ERROR: unknown parameter \"$PARAM\""
      usage
      exit 1
      ;;
  esac
  shift
done

echo "Using target platform '$TARGET_PLATFORM'"
mvn \
  -f tycho-pom.xml \
  --batch-mode \
  --update-snapshots \
  -fae \
  -Dmaven.repo.local=.m2/repository \
  -Dmaven.test.failure.ignore=true \
  -DJENKINS_URL=$JENKINS_URL \
  -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn \
  -P$TARGET_PLATFORM \
  $@
