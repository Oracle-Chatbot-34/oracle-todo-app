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
  # Check for macOS first
  if [[ "$OSTYPE" == "darwin"* ]]; then
    if [[ "$TERM_PROGRAM" == "Apple_Terminal" ]]; then
      echo "Apple_Terminal"
      return
    elif [[ "$TERM_PROGRAM" == "iTerm.app" ]]; then
      echo "iTerm"
      return
    fi
  fi

  # Linux terminal detection
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
  
  echo "Error: No supported terminal found. Tried: ${terminals[*]}, Apple_Terminal, iTerm." >&2
  exit 1
}

TERMINAL=$(get_terminal)
echo "Detected terminal: $TERMINAL"

# Start backend
echo "Starting backend in ${TERMINAL}..."
case $TERMINAL in
  "Apple_Terminal")
    osascript -e "tell application \"Terminal\" to do script \"cd \\\"$SCRIPT_DIR/backend\\\" && ./mvnw spring-boot:run; read -p 'Press Enter to close...'\"" 
    ;;
  "iTerm")
    osascript -e "tell application \"iTerm\" to create window with default profile command \"cd \\\"$SCRIPT_DIR/backend\\\" && ./mvnw spring-boot:run; read -p 'Press Enter to close...'\"" 
    ;;
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
  "Apple_Terminal")
    osascript -e "tell application \"Terminal\" to do script \"cd \\\"$SCRIPT_DIR/frontend\\\" && bun install && bun dev; read -p 'Press Enter to close...'\"" 
    ;;
  "iTerm")
    osascript -e "tell application \"iTerm\" to create window with default profile command \"cd \\\"$SCRIPT_DIR/frontend\\\" && bun install && bun dev; read -p 'Press Enter to close...'\"" 
    ;;
  "gnome-terminal"|"xterm-256color"|"xterm")
    $TERMINAL --title="DashMaster Frontend" -- bash -c "cd \"$SCRIPT_DIR/frontend\" && bun install && bun dev; read -p 'Press Enter to close...'"
    ;;
  "kitty"|"konsole"|"xfce4-terminal")
    $TERMINAL --title "DashMaster Frontend" -e bash -c "cd \"$SCRIPT_DIR/frontend\" && bun install && bun dev; read -p 'Press Enter to close...'" &
    ;;
esac

echo "Servers running in ${TERMINAL} terminals. Close terminals to stop."