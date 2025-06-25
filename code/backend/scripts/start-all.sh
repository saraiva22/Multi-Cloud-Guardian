#!/bin/bash

set -e

echo ""
echo "[1/5] Building the JVM JAR using Gradle..."
./gradlew :multicloud-guardian:host:extractUberJar

echo ""
echo "[3/5] Building Docker images manually..."
docker build -t multi-cloud-guardian-jvm -f ./multicloud-guardian/host/tests/Dockerfile-jvm .
docker build -t multi-cloud-guardian-postgres-test -f ./multicloud-guardian/host/tests/Dockerfile-postgres-test .
docker build -t multi-cloud-guardian-nginx -f ./multicloud-guardian/host/tests/Dockerfile-nginx .
docker build -t multi-cloud-guardian-ubuntu -f ./multicloud-guardian/host/tests/Dockerfile-ubuntu .

echo ""
echo "[4/5] Starting all services using docker-compose..."
docker compose -f ./multicloud-guardian/host/docker-compose.yml up --force-recreate -d