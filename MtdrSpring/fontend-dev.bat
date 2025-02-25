@echo off

:: Source environment variables
if exist .env.local.bat (
    call .env.local.bat
) else (
    echo Warning: .env.local.bat file not found!
)

:: Start the React development server
cd backend\src\main\frontend

:: Install dependencies if node_modules doesn't exist
if not exist node_modules (
    echo Installing frontend dependencies...
    call npm install
)

:: Start the development server
echo Starting React development server...
call npm start