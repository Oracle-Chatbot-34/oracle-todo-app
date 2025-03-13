#!/bin/bash

# Exit on any error
set -e

# Set Docker registry from environment or default
DOCKER_REGISTRY=${DOCKER_REGISTRY:-"your-oci-region.ocir.io/your-tenancy-namespace/dashmaster"}

# Set the image name and version
IMAGE_NAME="dashmaster"
IMAGE_VERSION="latest"
FULL_IMAGE_NAME="${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_VERSION}"

# Navigate to the backend directory
cd MtdrSpring/backend

# Build frontend first
echo "Building frontend assets..."
cd ../frontend
npm install
npm run build
mkdir -p ../backend/src/main/resources/static
cp -r dist/* ../backend/src/main/resources/static/
cd ../backend

# Build backend with Maven
echo "Building backend..."
./mvnw clean package -DskipTests

# Build Docker image
echo "Building Docker image: ${FULL_IMAGE_NAME}"
docker build -t "${FULL_IMAGE_NAME}" .

# Push to registry
echo "Pushing image to registry..."
docker push "${FULL_IMAGE_NAME}"

echo "Image build and push completed"