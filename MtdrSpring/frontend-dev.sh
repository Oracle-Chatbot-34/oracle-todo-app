#!/bin/bash

# Source environment variables
source .env.local

# Start the React development server
cd backend/src/main/frontend

# Install dependencies if node_modules doesn't exist
if [ ! -d "node_modules" ]; then
    echo "Installing frontend dependencies..."
    npm install
fi

# Start the development server
echo "Starting React development server..."
npm start
