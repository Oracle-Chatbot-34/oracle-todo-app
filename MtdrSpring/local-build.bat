@echo off

:: Source environment variables
if exist .env.local.bat (
    call .env.local.bat
) else (
    echo Warning: .env.local.bat file not found!
)

:: Check for required environment variables
set missing_vars=false
if "%DB_USER%"=="" (
    echo Error: DB_USER is not set. Please configure your .env.local.bat file.
    set missing_vars=true
)
if "%DB_PASSWORD%"=="" (
    echo Error: DB_PASSWORD is not set. Please configure your .env.local.bat file.
    set missing_vars=true
)
if "%UI_USERNAME%"=="" (
    echo Error: UI_USERNAME is not set. Please configure your .env.local.bat file.
    set missing_vars=true
)
if "%UI_PASSWORD%"=="" (
    echo Error: UI_PASSWORD is not set. Please configure your .env.local.bat file.
    set missing_vars=true
)
if "%JWT_SECRET%"=="" (
    echo Error: JWT_SECRET is not set. Please configure your .env.local.bat file.
    set missing_vars=true
)

if "%missing_vars%"=="true" (
    echo Missing required environment variables. Exiting.
    exit /b 1
)

:: Build the backend
echo Building Spring Boot application...
cd backend
call mvnw clean package spring-boot:repackage -DskipTests
cd ..

echo Build completed successfully!