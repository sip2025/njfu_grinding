@echo off
chcp 65001 >nul 2>&1
REM NJFU Exam Helper - Windows Build Script

echo ========================================
echo NJFU Exam Helper Desktop Build Script
echo ========================================
echo.

REM Check Java version
echo [1/4] Checking Java environment...
java -version 2>nul
if errorlevel 1 (
    echo ERROR: Java not found, please install JDK 17 or higher
    echo Please download from: https://adoptium.net/
    pause
    exit /b 1
)
echo.

REM Check Maven
echo [2/4] Checking Maven environment...
mvn -version 2>nul
if errorlevel 1 (
    echo ERROR: Maven not found, please install Maven 3.6 or higher
    echo Please download from: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)
echo.

REM Clean and compile
echo [3/4] Compiling project...
call mvn clean compile
if errorlevel 1 (
    echo ERROR: Compilation failed
    pause
    exit /b 1
)
echo.

REM Package
echo [4/4] Packaging application...
call mvn package
if errorlevel 1 (
    echo ERROR: Packaging failed
    pause
    exit /b 1
)
echo.

echo ========================================
echo Build completed successfully!
echo JAR location: target\njfu-grinding-desktop-1.0.0.jar
echo ========================================
echo.
echo To run the application:
echo   Option 1: java -jar target\njfu-grinding-desktop-1.0.0.jar
echo   Option 2: mvn javafx:run
echo.
pause