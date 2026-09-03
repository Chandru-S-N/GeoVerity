@ECHO OFF
SETLOCAL ENABLEDELAYEDEXPANSION

SET "MAVEN_VERSION=3.9.6"
SET "MAVEN_DIR=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%"
SET "MAVEN_EXE=%MAVEN_DIR%\apache-maven-%MAVEN_VERSION%\bin\mvn.cmd"

IF NOT EXIST "%MAVEN_EXE%" (
    ECHO [GeoVerity] Setting up Maven %MAVEN_VERSION%...
    IF NOT EXIST "%MAVEN_DIR%" MKDIR "%MAVEN_DIR%"
    SET "ZIP_FILE=%MAVEN_DIR%\apache-maven-%MAVEN_VERSION%-bin.zip"
    
    IF NOT EXIST "!ZIP_FILE!" (
        ECHO [GeoVerity] Downloading Maven from Apache mirror...
        powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object Net.WebClient).DownloadFile('https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip', '!ZIP_FILE!')"
    )
    
    ECHO [GeoVerity] Extracting Maven...
    powershell -Command "Expand-Archive -Path '!ZIP_FILE!' -DestinationPath '%MAVEN_DIR%' -Force"
)

IF EXIST "%MAVEN_EXE%" (
    CALL "%MAVEN_EXE%" %*
) ELSE (
    ECHO [Error] Failed to locate Maven at %MAVEN_EXE%
    EXIT /B 1
)
