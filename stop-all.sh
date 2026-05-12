#!/bin/bash

echo "Stopping Docker Compose services..."
docker compose down

echo "Stopping Spring Boot API (port 8081)..."
PID_SPRING_BOOT=$(lsof -t -i :8081)
if [ -n "$PID_SPRING_BOOT" ]; then
    kill -9 "$PID_SPRING_BOOT"
    echo "Killed Spring Boot process $PID_SPRING_BOOT"
else
    echo "Spring Boot API not found running on port 8081."
fi

echo "Stopping Angular Frontend (port 4200)..."
PID_FRONTEND=$(lsof -t -i :4200)
if [ -n "$PID_FRONTEND" ]; then
    kill -9 "$PID_FRONTEND"
    echo "Killed Angular Frontend process $PID_FRONTEND"
else
    echo "Angular Frontend not found running on port 4200."
fi

echo "All components stopped."
