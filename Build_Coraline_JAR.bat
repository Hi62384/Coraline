@echo off
setlocal EnableExtensions DisableDelayedExpansion
title Coraline - One-Click Mod JAR Builder

rem Place this file beside gradlew.bat, build.gradle and gradle.properties.
rem Double-click it after applying any code changes or ProjectBridge patches.
pushd "%~dp0" 2>nul
if errorlevel 1 (
    echo [ERROR] Could not open the folder containing this script.
    goto :FAILED_NO_FOLDER
)

echo ==============================================================
echo                  CORALINE - BUILD UPDATED JAR
echo ==============================================================
echo.
echo Project: %CD%
echo.

if not exist "gradlew.bat" (
    echo [ERROR] gradlew.bat was not found.
    echo Put this builder in the TOP LEVEL of your Coraline project,
    echo in the same folder as gradlew.bat and build.gradle.
    goto :FAILED
)
if not exist "build.gradle" (
    echo [ERROR] build.gradle was not found in this folder.
    goto :FAILED
)
if not exist "gradle.properties" (
    echo [ERROR] gradle.properties was not found in this folder.
    goto :FAILED
)

rem Prefer an installed Java 17 JDK for this Minecraft Forge 1.20.1 mod.
call :FIND_JAVA_17
if defined CHOSEN_JDK (
    set "JAVA_HOME=%CHOSEN_JDK%"
    set "PATH=%CHOSEN_JDK%\bin;%PATH%"
    echo [OK] Using Java 17 JDK: %CHOSEN_JDK%
) else (
    echo [INFO] Java 17 not found in typical folders; using configured Java.
)
where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java was not found.
    echo Install Java 17 JDK from https://adoptium.net/temurin/releases/?version=17
    goto :FAILED
)

rem This repository's Gradle processResources step calls the Python optimizer.
python -c "import sys; sys.exit(0 if sys.version_info.major == 3 else 1)" >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Python 3 was not found using the 'python' command.
    echo This project needs Python 3 to optimize and package its runtime assets.
    echo Install it from https://www.python.org/downloads/
    echo Then reopen this builder.
    goto :FAILED
)

echo.
echo [INFO] Checking Java and Python:
java -version 2>&1
python --version 2>&1

echo.
echo [1/3] Building the Forge mod. This can take a while on first run.
echo       The project handles resource optimization and JAR reobfuscation.
echo       Please leave this window open until it finishes.
echo.
call "gradlew.bat" --console=plain --no-daemon build
if errorlevel 1 (
    echo.
    echo [ERROR] The Gradle build failed. No updated JAR was packaged.
    echo Read the build errors above, fix them, and run this program again.
    goto :FAILED
)

echo.
echo [2/3] Locating the finished mod JAR...
set "MOD_ID=coraline"
set "MOD_VERSION="
for /f "usebackq tokens=1,* delims==" %%A in ("gradle.properties") do (
    if /i "%%A"=="mod_id" set "MOD_ID=%%B"
    if /i "%%A"=="mod_version" set "MOD_VERSION=%%B"
)
if not defined MOD_VERSION (
    echo [ERROR] Could not read mod_version from gradle.properties.
    goto :FAILED
)
set "SOURCE_JAR=build\libs\%MOD_ID%-%MOD_VERSION%.jar"
if not exist "%SOURCE_JAR%" (
    echo [ERROR] Gradle reported success but the expected JAR is missing:
    echo         %SOURCE_JAR%
    echo Check build\libs to see what Gradle produced.
    goto :FAILED
)

rem Use a constant output name so each run replaces the previous ready-to-use JAR.
rem Keep the extra output in build/ so Git ignores it automatically.
echo [3/3] Preparing your ready-to-install JAR...
if not exist "build\ready-to-install" mkdir "build\ready-to-install"
if errorlevel 1 (
    echo [ERROR] Could not create the output folder.
    goto :FAILED
)
copy /y "%SOURCE_JAR%" "build\ready-to-install\coraline-latest.jar" >nul
if errorlevel 1 (
    echo [ERROR] Could not copy the finished JAR.
    goto :FAILED
)

for %%F in ("build\ready-to-install\coraline-latest.jar") do (
    if %%~zF LSS 1024 (
        echo [ERROR] The resulting JAR is unexpectedly small.
        goto :FAILED
    )
)

echo.
echo ==============================================================
echo  SUCCESS! Updated Coraline %MOD_VERSION% JAR is ready.
echo ==============================================================
echo.
echo Your JAR:
echo   %CD%\build\ready-to-install\coraline-latest.jar
echo.
echo Copy coraline-latest.jar into your Minecraft Forge 1.20.1 mods folder.
echo Remove older Coraline JARs from that mods folder first.
echo.
echo Opening your ready-to-install folder now...
start "" "%CD%\build\ready-to-install"
popd
echo.
pause
exit /b 0

:FIND_JAVA_17
set "CHOSEN_JDK="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" (
    "%JAVA_HOME%\bin\java.exe" -version 2>&1 | findstr /c:"version \"17" >nul
    if not errorlevel 1 (
        set "CHOSEN_JDK=%JAVA_HOME%"
        exit /b 0
    )
)
for /d %%J in (
    "%ProgramFiles%\Eclipse Adoptium\jdk-17*"
    "%ProgramFiles%\Java\jdk-17*"
    "%ProgramFiles%\Microsoft\jdk-17*"
    "%ProgramFiles%\Amazon Corretto\jdk17*"
    "%ProgramFiles%\Zulu\zulu-17*"
    "%ProgramFiles%\BellSoft\LibericaJDK-17*"
) do (
    if exist "%%~fJ\bin\java.exe" (
        set "CHOSEN_JDK=%%~fJ"
        exit /b 0
    )
)
exit /b 0

:FAILED
popd
:FAILED_NO_FOLDER
echo.
echo Build did not complete. Nothing was installed or modified in Minecraft.
echo.
pause
exit /b 1
