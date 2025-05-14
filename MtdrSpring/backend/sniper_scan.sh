#!/bin/bash

# Sn1per Security Scan Script
# This script runs a Sn1per scan against the specified target

# Set target URL (default to localhost if not provided)
TARGET_URL=${1:-"http://localhost:8080"}
CONTAINER_NAME="sniper_container"

echo "Starting Sn1per scan against $TARGET_URL"

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "Docker is not installed. Please install Docker and try again."
    exit 1
fi

# Create or start the container
if ! docker container inspect $CONTAINER_NAME &> /dev/null; then
    echo "Creating persistent Sn1per container..."
    docker run -it --name $CONTAINER_NAME docker.io/blackarchlinux/blackarch:latest bash -c "
        echo 'Upgrading system...'
        pacman -Syu --noconfirm
        
        echo 'Installing sn1per...'
        pacman -Sy sn1per --noconfirm
    "
fi

# Run Sn1per in the container
echo "Running Sn1per scan against $TARGET_URL..."
docker start $CONTAINER_NAME
docker exec -it $CONTAINER_NAME bash -c "sn1per -t $TARGET_URL"

# Extract and display results summary
echo
echo "Sn1per scan complete. Extracting results summary..."
docker exec $CONTAINER_NAME bash -c "ls -la /usr/share/sniper/loot/ | grep localhost"
docker exec $CONTAINER_NAME bash -c "cat /usr/share/sniper/loot/workspace/localhost/findings.txt 2>/dev/null || echo 'No findings summary available'"

echo
echo "Security scan completed. Check the detailed reports for more information."