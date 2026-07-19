#!/bin/sh
#
# Gradle start up script for UN*X
# Requires: gradle/wrapper/gradle-wrapper.jar (run 'gradle wrapper' once to generate)
#

# Attempt to set APP_HOME
PRG="$0"
while [ -h "$PRG" ]; do
  ls=$(ls -ld "$PRG")
  link=$(expr "$ls" : '.*-> \(.*\)$')
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=$(dirname "$PRG")/"$link"
  fi
done
APP_HOME=$(dirname "$PRG")
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Determine the Java command to use
if [ -n "$JAVA_HOME" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
  if [ ! -x "$JAVACMD" ]; then
    die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
  fi
else
  JAVACMD="java"
  which java > /dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' found in PATH."
fi

die() {
  echo
  echo "$*"
  echo
  exit 1
}

# Check that gradle-wrapper.jar is present
if [ ! -f "$CLASSPATH" ]; then
  die "ERROR: gradle/wrapper/gradle-wrapper.jar not found.
Run 'gradle wrapper --gradle-version 8.4' once to generate it,
then commit it to the repository."
fi

exec "$JAVACMD" \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain "$@"
