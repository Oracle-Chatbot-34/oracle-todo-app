@echo off

:: Source environment variables
if exist .env.local.bat (
    call .env.local.bat
) else (
    echo Warning: .env.local.bat file not found!
)

:: Start the application with the local profile
cd backend
java -jar -Dspring.profiles.active=local target\MyTodoList-0.0.1-SNAPSHOT.jar