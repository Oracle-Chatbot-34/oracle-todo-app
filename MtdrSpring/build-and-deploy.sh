#!/bin/bash

# Build the frontend
cd frontend
bun run build

# Clean up the existing static resources in Spring Boot
rm -rf ../backend/src/main/resources/static/*

# Copy the built assets to Spring Boot's static directory
cp -r dist/* ../backend/src/main/resources/static/

echo "Frontend built and copied to Spring Boot static resources."

# Option to build and run the backend
read -p "Do you want to build and run the backend? (y/n) " choice
if [ "$choice" = "y" ] || [ "$choice" = "Y" ]; then
  cd ../backend
  ./mvnw clean package -DskipTests
  java -jar target/MyTodoList-0.0.1-SNAPSHOT.jar
fi