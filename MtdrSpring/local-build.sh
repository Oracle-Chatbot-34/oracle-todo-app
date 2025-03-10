#!/bin/bash

# Source environment variables
if [ -f .env.local ]; then
    source .env.local
else
    echo "Warning: .env.local file not found!"
fi

# Check for required environment variables
missing_vars=false
for var in DB_USER DB_PASSWORD UI_USERNAME UI_PASSWORD JWT_SECRET; do
    if [ -z "${!var}" ]; then
        echo "Error: $var is not set. Please configure your .env.local file."
        missing_vars=true
    fi
done

if [ "$missing_vars" = true ]; then
    echo "Missing required environment variables. Exiting."
    exit 1
fi

# Build the backend
echo "Building Spring Boot application..."
cd backend
./mvnw clean package spring-boot:repackage -DskipTests
cd ..

echo "Build completed successfully!"
