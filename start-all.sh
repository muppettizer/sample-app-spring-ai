#!/bin/bash

echo "Starting Docker Compose services (Solr, Redis)..."
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
mvn spring-boot:run &
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

echo "All components are starting up. Press Ctrl+C to stop them."
wait
