@rem
@rem Copyright 2015 the original author or authors.
@rem
@if "%DEBUG%" == "" @echo off
@setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve Gradle Wrapper
set WRAPPER_JAR="%APP_HOME%\gradle\wrapper\gradle-wrapper.jar"

if exist %WRAPPER_JAR% (
    java -jar %WRAPPER_JAR% %*
) else (
    echo Gradle wrapper jar not found. Please build with installed Gradle or Android Studio.
)
