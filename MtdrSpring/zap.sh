#!/bin/bash
# Security Testing Script using OWASP ZAP
help () {
    echo -e "${YELLOW}Usage: $0 [URL]${WHITE}"
    echo -e "${YELLOW}URL: The target URL to scan. Defaults to $DEV_URL if not provided.${WHITE}"
    echo -e "${YELLOW}Example: $0 http://localhost:5173/${WHITE}"
}

# URLS
DEV_URL="http://localhost:5173/"
PROD_URL="https://example.com/"  # Change to the production deployment URL

# Arguments
if [ "$#" -gt 1 ]; then
    help
fi

TARGET_URL="${1:-$DEV_URL}"

#Color variables
WHITE='\033[0m'
RED='\033[0;31m'
YELLOW='\033[0;33m'

# Setup environment
OUTPUT_DIR="./zap_reports"
mkdir -p "$OUTPUT_DIR"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
OUTPUT_FILE="$OUTPUT_DIR/zap_report_$TIMESTAMP.xml"
HTML_REPORT="$OUTPUT_DIR/zap_report_$TIMESTAMP.html"

if [[ "$TARGET_URL" == "$PROD_URL" ]]; then
    echo -e "{$RED}RUNNING IN PRODUCTION MODE. {$YELLOW}This will run the test directly into the production deployment of the project, proceed with caution{$WHITE}"
    echo -e "Continue? (y/N): "
    read -r CONTINUE
    if [[ "$CONTINUE" != "y" && "$CONTINUE" != "Y" ]]; then
        echo "Exiting..."
        exit 1
    fi
    echo "Running in production mode..."
else
    echo "Running in development mode..."
fi

# Check url connectvity
if ! curl -s --head "$TARGET_URL" | grep "200 OK" > /dev/null; then
    echo -e "${RED}Error: Unable to connect to $TARGET_URL. Please check the URL is correct and the project is running."
    exit 1
fi

# Check requirements
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Docker is not installed. Please install Docker to run this script.${WHITE}"
    exit 1
fi

# Install OWASP ZAP container
if docker pull ghcr.io/zaproxy/zaproxy:stable && docker pull zaproxy/zap-stable; then
    echo -e "${YELLOW}OWASP ZAP container pulled successfully.${WHITE}"
else
    echo -e "${RED}Failed to pull OWASP ZAP container. Please check your Docker installation.${WHITE}"
    exit 1
fi

# Run OWASP ZAP in Docker
echo -e "${YELLOW}Running OWASP ZAP against $TARGET_URL...${WHITE}"
if ! docker run -v "$PWD:/zap/wrk/:rw" -t zaproxy/zap-stable zap.sh -cmd \
    -quickurl "$TARGET_URL" \
    -quickout ./result.xml \
    -quickprogress; then
    echo -e "${RED}Failed to run OWASP ZAP. Please check your Docker installation and permissions.${WHITE}"
    exit 1
fi

mv result.xml "$OUTPUT_FILE"
mv report.html "$HTML_REPORT"

echo -e "${YELLOW}OWASP ZAP scan completed. Reports saved to $OUTPUT_DIR.${WHITE}"