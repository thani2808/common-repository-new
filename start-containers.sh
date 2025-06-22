#!/bin/bash
set -ex

# Load repo list
REPO_LIST_FILE="common-repository-new/common-repo-list.js"

# Create Docker network if not exists
docker network create spring-net || true

# Loop through each repo in the list
repos=$(jq -c '.[]' "$REPO_LIST_FILE")
for repo in $repos; do
    repo_name=$(echo "$repo" | jq -r '.["repo-name"]')
    image_name=$(echo "$repo" | jq -r '.["dockerhub_username"]')"/"$repo_name
    host_port=$(echo "$repo" | jq -r '.["host_port"]')
    app_type=$(echo "$repo" | jq -r '.["app-type"]')

    container_name="${repo_name}-container"

    # Remove any old container
    docker rm -f "$container_name" || true

    # Special case: Eureka Server must run first
    if [[ "$repo_name" == *eureka* ]]; then
        docker run -d --rm \
            --name "$container_name" \
            --network spring-net \
            -p 8761:8761 \
            "$image_name"
        echo "🌀 Started Eureka server: $container_name"
        sleep 10
    else
        # Start other microservices
        docker run -d --rm \
            --name "$container_name" \
            --network spring-net \
            -p "$host_port:$host_port" \
            -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/ \
            "$image_name"
        echo "🚀 Started service: $container_name on port $host_port"

        # Basic health check
        echo "🔍 Health checking http://localhost:$host_port/"
        for i in $(seq 1 20); do
            code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$host_port/)
            echo "Attempt $i => HTTP $code"
            if [[ "$code" == "200" ]]; then
                echo "✅ $repo_name is healthy"
                break
            fi
            sleep 3
        done
    fi
done
