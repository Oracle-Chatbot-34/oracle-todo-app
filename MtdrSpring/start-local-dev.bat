@echo off

:: Source environment variables
if exist .env.local.bat (
    call .env.local.bat
) else (
    echo Warning: .env.local.bat file not found!
)

:: Check if build is needed
if not exist backend\target\MyTodoList-0.0.1-SNAPSHOT.jar (
    echo Building the application...
    call local-build.bat
    if errorlevel 1 (
        echo Build failed! Exiting.
        exit /b 1
    )
)

:: Start backend in a separate window
start "Backend Server" cmd /c "java -jar -Dspring.profiles.active=local backend\target\MyTodoList-0.0.1-SNAPSHOT.jar"

echo Waiting for backend to initialize...
timeout /t 10

:: Start frontend development server
cd backend\src\main\frontend

:: Install dependencies if node_modules doesn't exist
if not exist node_modules (
    echo Installing frontend dependencies...
    call npm install
)

echo Starting frontend development server...
call npm start

:: Note: You'll need to manually close the backend window when done