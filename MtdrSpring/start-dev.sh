#!/bin/bash
# Development startup script for DashMaster

# Set the working directory to the script location
SCRIPT_DIR=$(dirname "$(realpath "$0")")
cd "$SCRIPT_DIR"

# Check environment files
for dir in backend frontend; do
    if [ ! -f "$SCRIPT_DIR/$dir/.env" ]; then
        echo "Error: .env file not found in $dir directory."
        exit 1
    fi
done

# Load environment variables (explained below)
set -a
source .env
set +a

echo "Starting DashMaster application in development mode"

# Enhanced terminal detection
get_terminal() {
    terminals=(
        "gnome-terminal:gnome-terminal"
        "xterm-256color:xterm"
        "xterm:xterm"
        "kitty:kitty"
        "konsole:konsole"
        "xfce4-terminal:xfce4-terminal"
    )
    
    for term in "${terminals[@]}"; do
        cmd="${term#*:}"
        if command -v "$cmd" &> /dev/null; then
            echo "${term%:*}"
            return
        fi
    done
    
    echo "Error: No supported terminal found (tried: ${terminals[*]})." >&2
    exit 1
}

TERMINAL=$(get_terminal)

# Start backend
echo "Starting backend in ${TERMINAL}..."
case $TERMINAL in
    "gnome-terminal"|"xterm-256color"|"xterm")
        $TERMINAL --title="DashMaster Backend" -- bash -c "cd \"$SCRIPT_DIR/backend\" && ./mvnw spring-boot:run; read -p 'Press Enter to close...'"
        ;;
    "kitty"|"konsole"|"xfce4-terminal")
        $TERMINAL --title "DashMaster Backend" -e bash -c "cd \"$SCRIPT_DIR/backend\" && ./mvnw spring-boot:run; read -p 'Press Enter to close...'" &
        ;;
esac

# Wait for backend initialization
sleep 10

# Start frontend
echo "Starting frontend in ${TERMINAL}..."
case $TERMINAL in
    "gnome-terminal"|"xterm-256color"|"xterm")
        $TERMINAL --title="DashMaster Frontend" -- bash -c "cd \"$SCRIPT_DIR/frontend\" && bun install && bun dev; read -p 'Press Enter to close...'"
        ;;
    "kitty"|"konsole"|"xfce4-terminal")
        $TERMINAL --title "DashMaster Frontend" -e bash -c "cd \"$SCRIPT_DIR/frontend\" && bun install && bun dev; read -p 'Press Enter to close...'" &
        ;;
esac

echo "Servers running in ${TERMINAL} terminals. Close terminals to stop."