#!/bin/sh
#
# X Studio Gradle wrapper launcher.
#
# gradle/wrapper/gradle-wrapper.jar is NOT included in this archive: the jar that shipped before was a
# hand-built stand-in, not Gradle's official wrapper, so it was removed instead of being trusted.
# Generate the official wrapper once (needs Gradle 8.9 or newer installed) and commit the result:
#
#     gradle wrapper --gradle-version 8.9 --distribution-type bin
#
# Codemagic does this automatically (see codemagic.yaml). Running the command above also replaces
# this script with Gradle's own gradlew.
APP_HOME=$( cd "$(dirname "$0")" > /dev/null && pwd -P ) || exit
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$JAR" ]; then
    echo "ERROR: $JAR is missing." >&2
    echo "Run:  gradle wrapper --gradle-version 8.9 --distribution-type bin" >&2
    exit 1
fi

if [ -n "$JAVA_HOME" ] ; then JAVACMD="$JAVA_HOME/bin/java"; else JAVACMD=java; fi
exec "$JAVACMD" -Xmx64m -Xms64m "-Dorg.gradle.appname=gradlew" -classpath "$JAR" \
    org.gradle.wrapper.GradleWrapperMain "$@"
