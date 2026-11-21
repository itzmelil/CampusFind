@echo off
setlocal
pushd "%~dp0" || exit /b 1

where java >nul 2>&1
if errorlevel 1 goto java_missing

if not exist "build\desktop" mkdir "build\desktop"
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -d "build\desktop" "src\main\java\campusfind\CampusFind.java" "src\main\java\campusfind\CampusFindDesktop.java"
if errorlevel 1 goto failed

java -cp "build\desktop" campusfind.CampusFindDesktop
if errorlevel 1 goto failed
popd
exit /b 0

:java_missing
echo Install the Java 21 JDK and reopen PowerShell or File Explorer.

:failed
echo CampusFind could not start. Check the message above.
pause
popd
exit /b 1
