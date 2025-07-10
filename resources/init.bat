@echo off
SETLOCAL

echo ===============================
echo 🔧 Initializing Environment...
echo ===============================

:: Set environment variables
SET APP_NAME=my-app
SET ENV=dev

:: Print environment variables
echo Application Name: %APP_NAME%
echo Environment: %ENV%

:: Check for required tools
where git >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo ❌ Git is not installed or not in PATH.
    EXIT /B 1
)

where java >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo ❌ Java is not installed or not in PATH.
    EXIT /B 1
)

:: Run project-specific setup tasks
echo ✅ Environment initialized successfully!

ENDLOCAL
