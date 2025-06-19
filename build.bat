@echo off
REM UniReq Build Script for Windows
REM Builds the Burp Suite extension JAR file using Maven

echo 🚀 Building UniReq - HTTP Request Deduplicator Extension
echo ==============================================

REM Check if Maven is installed
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo ❌ Error: Maven is not installed or not in PATH
    echo Please install Maven 3.6+ and try again
    echo Visit: https://maven.apache.org/install.html
    pause
    exit /b 1
)

REM Check if Java is installed
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo ❌ Error: Java is not installed or not in PATH
    echo Please install Java 11+ and try again
    pause
    exit /b 1
)

echo ✅ Java detected

REM Display Maven version
for /f "tokens=*" %%i in ('mvn -version ^| findstr "Apache Maven"') do echo ✅ %%i

echo.
echo 🔧 Cleaning previous builds...
call mvn clean -q
if %errorlevel% neq 0 (
    echo ❌ Maven clean failed
    pause
    exit /b 1
)

echo 📦 Compiling and packaging extension...
call mvn compile package -q
if %errorlevel% neq 0 (
    echo ❌ Maven build failed
    pause
    exit /b 1
)

REM Check if build was successful
if exist "target\unireq-deduplicator-1.0.0.jar" (
    echo.
    echo 🎉 Build completed successfully!
    echo 📍 Extension JAR location: target\unireq-deduplicator-1.0.0.jar
    echo.
    echo 📋 Next steps:
    echo 1. Open Burp Suite
    echo 2. Go to Extensions → Installed
    echo 3. Click 'Add' and select the JAR file
    echo 4. Look for the 'UniReq' tab in Burp's interface
    echo.
    for %%I in (target\unireq-deduplicator-1.0.0.jar) do echo 📊 File size: %%~zI bytes
) else (
    echo ❌ Build failed - JAR file not found
    pause
    exit /b 1
)

pause 