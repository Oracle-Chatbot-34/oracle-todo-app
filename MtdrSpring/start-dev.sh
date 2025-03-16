#!/bin/bash
# Development startup script for DashMaster

# Set the working directory to the script location
SCRIPT_DIR=$(dirname "$(realpath "$0")")
cd "$SCRIPT_DIR"

# Check if .env file exists
if [ ! -f ".env" ]; then
  echo "Error: No .env file found. Creating from example..."
  cp .env.example .env
  echo "Please edit the .env file with your configuration values."
  exit 1
fi

# Load environment variables
set -a
source .env
set +a

# Print status
echo "Starting DashMaster application in development mode"

# Start backend in the background
echo "Starting backend..."
cd "$SCRIPT_DIR/backend"
./mvnw spring-boot:run &
BACKEND_PID=$!

# Wait for backend to initialize
echo "Waiting for backend to initialize..."
sleep 10

# Start frontend in the background
echo "Starting frontend..."
cd "$SCRIPT_DIR/frontend"
bun install
bun dev &
FRONTEND_PID=$!

# Function to handle script termination
cleanup() {
  echo "Shutting down services..."
  kill $FRONTEND_PID
  kill $BACKEND_PID
  echo "Development servers stopped"
  exit 0
}

# Set trap for SIGINT (Ctrl+C)
trap cleanup SIGINT

# Keep the script running
echo "Development servers are running. Press Ctrl+C to stop."
wait