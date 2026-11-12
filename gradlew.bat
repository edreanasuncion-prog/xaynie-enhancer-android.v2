@rem X Studio Gradle wrapper launcher (see gradlew for why the jar is not bundled)
@if "%DEBUG%"=="" @echo off
set APP_HOME=%~dp0
set JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
if not exist "%JAR%" (
  echo ERROR: %JAR% is missing. 1>&2
  echo Run: gradle wrapper --gradle-version 8.9 --distribution-type bin 1>&2
  exit /b 1
)
if defined JAVA_HOME (set JAVACMD="%JAVA_HOME%\bin\java.exe") else (set JAVACMD=java.exe)
%JAVACMD% -Xmx64m -Xms64m -classpath "%JAR%" org.gradle.wrapper.GradleWrapperMain %*
