#!/bin/bash

echo "Starting Docker Compose services (Solr, Redis)... "
if ! docker compose up -d solr redis; then
    echo "Error starting Docker Compose services. Exiting."
    exit 1
fi

echo "Initializing Solr data..."
if ! docker compose run --rm solr-init; then
    echo "Error initializing Solr data. Exiting."
    exit 1
fi

# Function to kill background processes on exit
cleanup() {
    echo "Stopping background processes..."
    kill "$(jobs -p)" 2>/dev/null
    echo "Stopping Docker Compose services..."
    docker compose down
}
trap cleanup EXIT

echo "Starting Spring Boot API (http://localhost:8081)..."
# Optionally source a .env file in the repo root to load GOOGLE_API_KEY
if [ -f ".env" ]; then
    echo "Sourcing .env to load environment variables..."
    # Export variables from .env (handles simple KEY=VALUE lines)
    set -o allexport
    # shellcheck disable=SC1091
    source ".env"
    set +o allexport
fi

# Ensure GOOGLE_API_KEY is set before launching Spring Boot
if [ -z "${GOOGLE_API_KEY:-}" ]; then
    echo "ERROR: GOOGLE_API_KEY is not set in .env file."
    echo "Please set the environment variable before starting the application."
    echo "Example: export GOOGLE_API_KEY=your_api_key_here"
    exit 1
fi

# Start Spring Boot with the 'google' profile so application-google.yml is used
mvn spring-boot:run -Dspring-boot.run.profiles=google &
SPRING_BOOT_PID=$!
echo "Spring Boot API running with PID: $SPRING_BOOT_PID"

echo "Navigating to frontend directory and starting Angular Frontend (http://localhost:4200)..."
cd frontend || { echo "Error: 'frontend' directory not found. Exiting."; exit 1; }

echo "Installing npm dependencies (if not already installed)..."
npm install

echo "Starting Angular frontend..."
npm start &
FRONTEND_PID=$!
echo "Angular Frontend running with PID: $FRONTEND_PID"

FRONTEND_URL="http://localhost:4200"
echo "Attempting to open browser at $FRONTEND_URL"
uname_s=$(uname -s 2>/dev/null || echo "")
case "$uname_s" in
  Darwin)
    # macOS
    open "$FRONTEND_URL" || echo "Failed to open browser. Please open $FRONTEND_URL manually."
    ;;
  Linux)
    # Most Linux desktops provide xdg-open; fallback to gnome-open if available
    if command -v xdg-open >/dev/null 2>&1; then
      xdg-open "$FRONTEND_URL" || echo "Failed to open browser. Please open $FRONTEND_URL manually."
    elif command -v gnome-open >/dev/null 2>&1; then
      gnome-open "$FRONTEND_URL" || echo "Failed to open browser. Please open $FRONTEND_URL manually."
    else
      echo "No known browser opener (xdg-open/gnome-open) found. Please open $FRONTEND_URL manually."
    fi
    ;;
  CYGWIN*|MINGW*|MSYS*)
    # Git Bash / Cygwin / MSYS on Windows
    cmd.exe /c start "" "${FRONTEND_URL//&/^&}" || echo "Failed to open browser. Please open $FRONTEND_URL manually."
    ;;
  *)
    echo "Unsupported OS ($uname_s). Please open $FRONTEND_URL manually."
    ;;
esac

echo "All components are starting up. Press Ctrl+C to stop them."
wait
