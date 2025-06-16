def call(String port, String containerName, String appType = 'springboot') {
    def endpoint = appType == 'springboot' ? "/actuator/health" : "/"

    sh """
        echo "⏳ Sleeping before health check..."
        sleep 15

        for i in \$(seq 1 20); do
            CODE=\$(curl -s -o /dev/null -w '%{http_code}' http://localhost:${port}${endpoint} || echo "000")
            echo "Attempt \$i: HTTP \$CODE"
            if [ "\$CODE" = "200" ]; then
                echo '✅ Health check passed'
                exit 0
            fi
            sleep 3
        done

        echo '❌ Health check failed after 20 attempts'
        echo '🧾 Showing logs for debugging:'
        docker logs "${containerName}" || true
        exit 1
    """
}
