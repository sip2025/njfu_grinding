@echo off
chcp 65001 >nul 2>&1
cls
echo ========================================
echo NJFU Exam Helper - Desktop Version
echo ========================================
echo.

cd /d "%~dp0"

echo Checking environment...
echo.

java -version
if errorlevel 1 (
    echo.
    echo [ERROR] Java not found!
    echo Please install JDK from: https://adoptium.net/
    echo.
    pause
    exit /b 1
)

echo.
echo Starting application...
echo Please wait, this may take a minute on first run...
echo.

mvn javafx:run

if errorlevel 1 (
    echo.
    echo [ERROR] Failed to start application
    echo.
    echo Possible solutions:
    echo 1. Make sure you are in the desktop-app directory
    echo 2. Check if Maven is installed: mvn -version
    echo 3. Try running: mvn clean compile
    echo.
)

pause