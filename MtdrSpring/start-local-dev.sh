#!/bin/bash

# Source environment variables
source .env.local

# Build the application
./local-build.sh

# Start backend in the background
java -jar -Dspring.profiles.active=local backend/target/MyTodoList-0.0.1-SNAPSHOT.jar &
BACKEND_PID=$!

echo "Backend server started with PID: $BACKEND_PID"
echo "Waiting for backend to initialize..."
sleep 10

# Start frontend development server
cd backend/src/main/frontend
npm install
npm start

# When the frontend server is terminated, also kill the backend
kill $BACKEND_PID
