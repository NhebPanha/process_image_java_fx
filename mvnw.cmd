@echo off
setlocal
set "MAVEN_CMD=%USERPROFILE%\.maven\apache-maven-3.9.9\bin\mvn.cmd"
if exist "%MAVEN_CMD%" (
    "%MAVEN_CMD%" %*
) else (
    echo Maven not found at %MAVEN_CMD%. Please ensure Maven is installed.
    exit /b 1
)
