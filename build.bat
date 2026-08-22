@echo off
setlocal enabledelayedexpansion
title UAGC build
cd /d "%~dp0"

echo.
echo  UAGC build
echo.

rem gradle needs java on PATH, the build itself pins java 21 through a toolchain
where java >nul 2>&1
if errorlevel 1 goto :no_java

rem clear old jars so build\libs never holds two versions at once
if exist "build\libs" del /q "build\libs\UAGC-*.jar" >nul 2>&1

rem pass clean as the first argument to force a full rebuild
set "TASKS=build"
if /i "%~1"=="clean" set "TASKS=clean build"

call "%~dp0gradlew.bat" %TASKS% --console=plain
if errorlevel 1 goto :build_failed

echo.
set "BUILT="
for /f "delims=" %%f in ('dir /b /o-d "build\libs\UAGC-*.jar" 2^>nul') do call :report "%%f"
if not defined BUILT goto :no_jar

echo.
echo  drop it in a plugins folder, or just run ..\TestServer\start.bat
echo  which copies the newest jar from here on every launch.
echo.
pause
exit /b 0

:report
if defined BUILT exit /b 0
set "BUILT=%~1"
echo  [ok] build\libs\%~1
for %%s in ("build\libs\%~1") do echo       %%~zs bytes
exit /b 0

:no_java
echo  [x] java is not on PATH.
echo      install a java 21 jdk, open a new window, run this again.
echo.
pause
exit /b 1

:build_failed
echo.
echo  [x] the build failed, the output above says why.
echo.
pause
exit /b 1

:no_jar
echo  [x] the build reported success but no jar landed in build\libs.
echo.
pause
exit /b 1
