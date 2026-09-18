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
