@echo off
setlocal
cd /d "%~dp0"
if not defined JAVA_HOME set "JAVA_HOME=D:\Java\Java21"
if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo Set JAVA_HOME to a Java 21 JDK before building.
    exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"
call gradlew.bat --offline --no-daemon clean assemble sourceBundle --console=plain
exit /b %ERRORLEVEL%

