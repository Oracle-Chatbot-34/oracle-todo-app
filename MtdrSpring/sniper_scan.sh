#!/bin/bash

# Sn1per Security Scan Script
# This script runs a Sn1per scan against the specified target

# Set target URL (default to localhost if not provided)
TARGET_URL=${1:-"http://localhost:8080"}
echo "Starting Sn1per scan against $TARGET_URL"

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "Docker is not installed. Please install Docker and try again."
    exit 1
fi

# Pull the BlackArch Docker image
echo "Pulling BlackArch Docker image..."
docker pull docker.io/blackarchlinux/blackarch:latest

# Run Sn1per
echo "Running Sn1per scan..."
docker run -it --rm docker.io/blackarchlinux/blackarch:latest bash -c "
    echo 'Upgrading system...'
    pacman -Syu --noconfirm
    
    echo 'Installing sn1per...'
    pacman -Sy sn1per --noconfirm
    
    echo 'Running sn1per scan against $TARGET_URL...'
    sn1per -t $TARGET_URL
"

echo "Sn1per scan complete."