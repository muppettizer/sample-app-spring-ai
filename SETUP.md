# Overview

Spring Boot app with Spring AI

# Setup

```bash
# Install or Update SDKMan if not already installed
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Or Update SDKMan if already installed
sdk selfupdate
sdk version

# SDKMan list available versions
sdk list java
sdk list maven
sdk list springboot

# Check current versions
sdk current

# Install required versions
sdk install java 21.0.8-amzn
sdk install maven 3.9.11
sdk install springboot 3.5.13

# Set default versions
sdk default java 21.0.8-amzn
sdk default maven 3.9.9
sdk default springboot 3.5.13

# Verify installations
java -version
mvn -version
spring --version

# Or create .sdkmanrc file in project root with:
# Also SDKMan can auto switch using sdkman_auto_env: true in ~/.sdkman/etc/config
echo "java=21.0.8-amzn" >> .sdkmanrc
echo "maven=3.9.11" >> .sdkmanrc
echo "springboot=3.5.13" >> .sdkmanrc
sdk env

```

# Spring Initializr

https://start.spring.io/

```bash
# List available options
spring help init --list
spring init --list

# List dependencies
#- spring-ai-anthropic
#- spring-ai-azure-openai
#- spring-ai-bedrock
#- spring-ai-bedrock-converse
#- spring-ai-google-genai
#- spring-ai-google-genai-embedding

# Create a new Spring Boot project
export APP_NAME="sample-app-spring-ai"
spring init \
  --dependencies=web,actuator,devtools,lombok,docker-compose,testcontainers,validation,spring-ai-google-genai \
  --build=maven \
  --java-version=21 \
  --boot-version=3.5.13 \
  --groupId=com.sample.app \
  --artifactId=${APP_NAME} \
  --name=${APP_NAME} \
  --force \
  ./

# sdk env to switch versions if needed
sdk env --install

# Open in IDE
idea .

# Install project dependencies
mvn clean install

# Run the application
mvn spring-boot:run

# Run tests
mvn test

# Build the application
mvn clean package

# Run with Docker Compose
docker-compose up --build

# Stop Docker Compose
docker-compose down

# Run Solr container and initialize it
docker compose up -d solr && docker compose run --rm solr-init

# Test
curl -s "http://localhost:8983/solr/security-screener/select?q=*:*&rows=0&facet=true&facet.field=assetClass&facet.field=country&facet.field=currency&facet.mincount=1&wt=json"
```


# Frontend

```sh
npm install -g @angular/cli
ng version

# Create a new Angular project
ng new frontend
cd frontend

# Run it once
ng serve

# Dependencies
npm install ag-grid-community@34 ag-grid-enterprise@34 ag-grid-angular

# Compoennt
ng generate component grid
```
