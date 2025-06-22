#!/bin/bash

PORT=$1
CONTAINER_NAME=$2
APP_TYPE=$3

ENDPOINT="/"
[[ "$APP_TYPE" == "springboot" ]] && ENDPOINT="/actuator/health"

IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' $CONTAINER_NAME)
URL="http://${IP}:${PORT}${ENDPOINT}"

echo "⏳ Checking ${APP_TYPE} app on ${URL}"
sleep 10

for i in {1..10}; do
  CODE=$(curl -s -o /dev/null -w "%{http_code}" "$URL")
  echo "Attempt $i: HTTP $CODE"
  if [[ "$CODE" == "200" || "$CODE" == "403" || "$CODE" == "302" ]]; then
    echo "✅ Health check passed"
    exit 0
  fi
  sleep 3
done

echo "❌ Health check failed for $CONTAINER_NAME ($APP_TYPE)"
docker logs "$CONTAINER_NAME" || true
exit 1
