#!/bin/bash

# OWASP ZAP Scan Script
# This script runs a quick scan against the specified target

# Set variables
TARGET_URL=${1:-"http://localhost:8080"}
OUTPUT_FILE="results.xml"

echo "Starting ZAP scan against $TARGET_URL"
echo "Results will be saved to $OUTPUT_FILE"

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "Docker is not installed. Please install Docker and try again."
    exit 1
fi

# Pull the ZAP Docker image if not already present
echo "Pulling ZAP Docker image..."
docker pull ghcr.io/zaproxy/zaproxy:stable

# Run the ZAP scan
echo "Running ZAP scan..."
docker run -v "$(pwd)":/zap/wrk/:rw -t ghcr.io/zaproxy/zaproxy:stable zap.sh -cmd -quickurl $TARGET_URL -quickout /zap/wrk/$OUTPUT_FILE -quickprogress

# Check if scan was successful
if [ $? -eq 0 ]; then
    echo "ZAP scan completed successfully!"
    echo "Results saved to $OUTPUT_FILE"
else
    echo "ZAP scan failed!"
    exit 1
fi

# Provide a summary of the results
echo "Scan Summary:"
if command -v xmllint &> /dev/null; then
    # Count alerts by risk level
    HIGH_COUNT=$(xmllint --xpath "count(//alertitem[riskcode='3'])" $OUTPUT_FILE 2>/dev/null || echo "0")
    MEDIUM_COUNT=$(xmllint --xpath "count(//alertitem[riskcode='2'])" $OUTPUT_FILE 2>/dev/null || echo "0")
    LOW_COUNT=$(xmllint --xpath "count(//alertitem[riskcode='1'])" $OUTPUT_FILE 2>/dev/null || echo "0")
    INFO_COUNT=$(xmllint --xpath "count(//alertitem[riskcode='0'])" $OUTPUT_FILE 2>/dev/null || echo "0")
    
    echo "- High Risk Issues: $HIGH_COUNT"
    echo "- Medium Risk Issues: $MEDIUM_COUNT"
    echo "- Low Risk Issues: $LOW_COUNT"
    echo "- Informational: $INFO_COUNT"
else
    echo "xmllint not found. Install libxml2-utils for detailed summary."
    echo "Full report available in $OUTPUT_FILE"
fi

echo "Scan completed. Review the report for details."