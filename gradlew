#!/bin/sh
#
# Gradle wrapper script
#
set -e
APP_HOME="$(cd "$(dirname "$0")" && pwd)"
exec "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@" 2>/dev/null || true

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
JAVA_EXE="$(command -v java)"
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

exec "$JAVA_EXE" $DEFAULT_JVM_OPTS \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
