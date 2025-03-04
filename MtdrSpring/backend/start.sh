#!/bin/bash
# start-dashmaster.sh

# Get the directory of the current script
SCRIPT_DIR=$(dirname "$(realpath "$0")")

# Create secure directory if it doesn't exist
mkdir -p "$SCRIPT_DIR/.dashmaster"

# Check if .env file exists, if not create from example
if [ ! -f "$SCRIPT_DIR/.dashmaster/.env" ]; then
    if [ -f "$SCRIPT_DIR/.env" ]; then
        cp "$SCRIPT_DIR/.env" "$SCRIPT_DIR/.dashmaster/.env"
        chmod 600 "$SCRIPT_DIR/.dashmaster/.env"
        echo "Created .env file in $SCRIPT_DIR/.dashmaster/ from local .env"
    else
        echo "Error: No .env file found. Please create one in $SCRIPT_DIR/.dashmaster/ or copy from .env.example"
        exit 1
    fi
fi

# Load environment variables
set -a
source "$SCRIPT_DIR/.dashmaster/.env"
set +a

# Print status
echo "Starting DashMaster application with environment from $SCRIPT_DIR/.dashmaster/.env"
echo "Telegram Bot: ${TELEGRAM_BOT_NAME}"
echo "Oracle Wallet: ${WALLET_LOCATION}"

# Start the application
"$SCRIPT_DIR/mvnw" spring-boot:run