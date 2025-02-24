#!/bin/bash

# Source environment variables if not already set
source .env.local

# Start the application with the local profile
cd backend
java -jar -Dspring.profiles.active=local target/MyTodoList-0.0.1-SNAPSHOT.jar
