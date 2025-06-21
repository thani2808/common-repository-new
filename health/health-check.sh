#!/bin/bash

PORT=$1
CONTAINER_NAME=$2
APP_TYPE=$3

# Determine health check endpoint
case "$APP_TYPE" in
  springboot) ENDPOINT="/actuator/health" ;;
  nodejs|nginx|php|python|ruby) ENDPOINT="/" ;;
  *) echo "⚠️ Unknown app type '${APP_TYPE}', defaulting to '/'"; ENDPOINT="/" ;;
esac

URL="http://localhost:${PORT}${ENDPOINT}"

echo "⏳ Health check for '${APP_TYPE}' on ${URL}"
sleep 15

for i in $(seq 1 10); do
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
