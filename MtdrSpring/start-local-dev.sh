#!/bin/bash

# Source environment variables
if [ -f .env.local ]; then
    source .env.local
else
    echo "Warning: .env.local file not found!"
fi

# Check if build is needed
if [ ! -f backend/target/MyTodoList-0.0.1-SNAPSHOT.jar ]; then
    echo "Building the application..."
    ./local-build.sh
    if [ $? -ne 0 ]; then
        echo "Build failed! Exiting."
        exit 1
    fi
fi

# Start backend in the background
java -jar -Dspring.profiles.active=local backend/target/MyTodoList-0.0.1-SNAPSHOT.jar &
BACKEND_PID=$!

echo "Backend server started with PID: $BACKEND_PID"
echo "Waiting for backend to initialize..."
sleep 10

# Start frontend development server
cd backend/src/main/frontend

# Install dependencies if node_modules doesn't exist
if [ ! -d "node_modules" ]; then
    echo "Installing frontend dependencies..."
    npm install
fi

echo "Starting frontend development server..."
npm start

# When the frontend server is terminated, also kill the backend
kill $BACKEND_PID
