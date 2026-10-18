@echo off
setlocal
title loopclient mod build
cd /d "%~dp0"

echo --- loopclient mod build ---

rem 1) JDK: .tools next to project -> JAVA_HOME -> PATH
set "JAVA="
set "JDK=%~dp0..\.tools\jdk21\jdk-21.0.12.1+1"
if exist "%JDK%\bin\javac.exe" set "JAVA=%JDK%"
if not defined JAVA if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" set "JAVA=%JAVA_HOME%"
if not defined JAVA for %%I in (javac.exe) do if not "%%~$PATH:I"=="" set "JAVA=%%~dp$PATH:I.."
if not defined JAVA (
  echo JDK 21 not found. Install it, set JAVA_HOME, or put JDK into ..\.tools\jdk21
  pause
  exit /b 1
)
echo JDK: %JAVA%

rem 2) Gradle: .tools next to project -> PATH
set "GRADLE=%~dp0..\.tools\gradle-8.12\bin\gradle.bat"
if not exist "%GRADLE%" (
  for %%I in (gradle.bat) do if not "%%~$PATH:I"=="" set "GRADLE=%%~$PATH:I"
)
if not exist "%GRADLE%" (
  echo Gradle not found. Install Gradle 8.10+ or put it into ..\.tools\gradle-8.12
  pause
  exit /b 1
)

set "JAVA_HOME=%JAVA%"
set "GRADLE_USER_HOME=%~dp0..\.tools\gradle-home"
if not exist "%GRADLE_USER_HOME%" mkdir "%GRADLE_USER_HOME%"

echo.
echo First build downloads Minecraft 1.21.4, Yarn and Fabric API - a few minutes.
echo.
call "%GRADLE%" build --no-daemon --console=plain
echo.

if exist "build\libs\loopclient-1.0.0.jar" (
  echo OK: build\libs\loopclient-1.0.0.jar
  echo Copy it to %%APPDATA%%\.minecraft\mods
) else (
  echo BUILD FAILED - read the messages above.
  echo.
  echo If you saw "Failed to load native library" or "Failed to initialise native":
  echo   Gradle native DLL is blocked ^(antivirus / Windows^). Try:
  echo   1^) add this project folder to antivirus exclusions and run build.bat again;
  echo   2^) build in the cloud: push loopclient-mod to GitHub, the workflow
  echo      .github/workflows/build.yml builds the .jar into artifacts for free;
  echo   3^) build on another PC: gradle build in this folder.
)
echo.
pause