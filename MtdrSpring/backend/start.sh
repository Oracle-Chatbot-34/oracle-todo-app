#!/bin/bash
# start-dashmaster.sh

# Get the directory of the current script
SCRIPT_DIR=$(dirname "$(realpath "$0")")

# Check if .env file exists in the root directory
if [ ! -f "$SCRIPT_DIR/.env" ]; then
    echo "Error: No .env file found in $SCRIPT_DIR. Please create one or copy from .env.example"
    exit 1
fi

# Load environment variables
set -a
source "$SCRIPT_DIR/.env"
set +a

# Print status
echo "Starting DashMaster application with environment from $SCRIPT_DIR/.env"
echo "Telegram Bot: ${TELEGRAM_BOT_NAME}"
echo "Oracle Wallet: ${WALLET_LOCATION}"

# Start the application
"$SCRIPT_DIR/mvnw" spring-boot:run
