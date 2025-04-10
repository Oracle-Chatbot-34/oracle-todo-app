#!/bin/bash
# Development startup script for DashMaster

# Set the working directory to the script location
SCRIPT_DIR=$(dirname "$(realpath "$0")")
cd "$SCRIPT_DIR"

# Check if .env file exists in backend directory
if [ ! -f "$SCRIPT_DIR/backend/.env" ]; then
    echo "Error: .env file not found in backend directory."
    exit 1
fi
# Check if .env file exists in frontend directory
if [ ! -f "$SCRIPT_DIR/frontend/.env" ]; then
    echo "Error: .env file not found in frontend directory."
    exit 1
fi

# Load environment variables
set -a
source .env
set +a

# Print status
echo "Starting DashMaster application in development mode"

# Function to detect available terminal emulator
get_terminal() {
    if command -v gnome-terminal &> /dev/null; then
        echo "gnome-terminal"
    elif command -v xterm &> /dev/null; then
        echo "xterm"
    elif command -v kitty &> /dev/null; then
        echo "kitty"
    else
        echo "Error: No supported terminal emulator found (gnome-terminal, xterm, or kitty)." >&2
        exit 1
    fi
}

TERMINAL=$(get_terminal)

# Start backend in a new terminal
echo "Starting backend in new terminal..."
case $TERMINAL in
    "gnome-terminal")
        gnome-terminal --title="DashMaster Backend" -- bash -c "cd \"$SCRIPT_DIR/backend\" && ./mvnw spring-boot:run; read -p 'Press Enter to close...'"
        ;;
    "xterm")
        xterm -T "DashMaster Backend" -e "cd \"$SCRIPT_DIR/backend\" && ./mvnw spring-boot:run; read -p 'Press Enter to close...'" &
        ;;
    "kitty")
        kitty --title "DashMaster Backend" bash -c "cd \"$SCRIPT_DIR/backend\" && ./mvnw spring-boot:run; read -p 'Press Enter to close...'" &
        ;;
esac

# Wait for backend to initialize
echo "Waiting for backend to initialize..."
sleep 10

# Start frontend in a new terminal
echo "Starting frontend in new terminal..."
case $TERMINAL in
    "gnome-terminal")
        gnome-terminal --title="DashMaster Frontend" -- bash -c "cd \"$SCRIPT_DIR/frontend\" && bun install && bun dev; read -p 'Press Enter to close...'"
        ;;
    "xterm")
        xterm -T "DashMaster Frontend" -e "cd \"$SCRIPT_DIR/frontend\" && bun install && bun dev; read -p 'Press Enter to close...'" &
        ;;
    "kitty")
        kitty --title "DashMaster Frontend" bash -c "cd \"$SCRIPT_DIR/frontend\" && bun install && bun dev; read -p 'Press Enter to close...'" &
        ;;
esac

echo "Development servers are running in separate terminals. Close the terminals to stop the servers."