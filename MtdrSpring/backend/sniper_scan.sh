#!/bin/bash
# Create and run the Sniper container
docker run -it --rm docker.io/blackarchlinux/blackarch:latest bash -c "
# Upgrade system
pacman -Syu --noconfirm

# Install sn1per from official repository
pacman -Sy sn1per --noconfirm

# Run the scan 
# Replace example.org with your actual domain
sn1per -t www.example.org
"