package org.example

class HealthCheck implements Serializable {
    def steps

    HealthCheck(steps) {
        this.steps = steps
    }

    void check(String port, String containerName, String appType = 'springboot') {
        def endpoint = appType == 'springboot' ? "/actuator/health" : "/"

        steps.sh """
            echo "⏳ Starting health check..."
            sleep 15
            for i in \$(seq 1 20); do
                CODE=\$(curl -s -o /dev/null -w '%{http_code}' http://localhost:${port}${endpoint} || echo "000")
                echo "Attempt \$i: HTTP \$CODE"
                if [ "\$CODE" = "200" ]; then
                    echo '✅ Healthy'
                    exit 0
                fi
                sleep 3
            done
            echo '❌ Health check failed'
            docker logs '${containerName}' || true
            exit 1
        """
    }
}
