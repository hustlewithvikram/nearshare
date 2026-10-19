@echo off
setlocal
set GRADLE_VERSION=8.13
set BASE=%USERPROFILE%\.gradle\nearshare-gradle
set GRADLE_HOME=%BASE%\gradle-%GRADLE_VERSION%
set GRADLE_BIN=%GRADLE_HOME%\bin\gradle.bat
if not exist "%GRADLE_BIN%" (
  if not exist "%BASE%" mkdir "%BASE%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%BASE%\gradle-%GRADLE_VERSION%-bin.zip'"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%BASE%\gradle-%GRADLE_VERSION%-bin.zip' '%BASE%'"
)
call "%GRADLE_BIN%" %*
endlocal
