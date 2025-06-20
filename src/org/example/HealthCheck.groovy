package org.example

class HealthCheck implements Serializable {
    def steps

    HealthCheck(steps) {
        this.steps = steps
    }

    void check(String port, String containerName, String appType = 'springboot') {
        def actualPort = port
        def endpoint = getHealthEndpoint(appType)

        // Override port for known fixed port apps like nginx
        if (appType?.toLowerCase() == 'nginx') {
            actualPort = '80'
        }

        def url = "http://localhost:${actualPort}${endpoint}"
        steps.echo "⏳ Starting health check for '${appType}' on ${url}"

        steps.sh "sleep 15"

        steps.sh """
            for i in \$(seq 1 10); do
              CODE=\$(curl -s -o /dev/null -w '%{http_code}' ${url} || echo 000)
              echo "Attempt \$i: HTTP \$CODE"
              if [[ "\$CODE" == "200" || "\$CODE" == "403" || "\$CODE" == "302" ]]; then
                echo "✅ Health check passed with code \$CODE"
                exit 0
              fi
              sleep 3
            done

            echo "❌ Health check failed for ${containerName} (${appType})"
            docker logs ${containerName} || echo 'No logs found.'
            exit 1
        """
    }

    private String getHealthEndpoint(String appType) {
        switch (appType?.toLowerCase()) {
            case 'springboot':
                return "/actuator/health"
            case 'nodejs':
            case 'nginx':
            case 'php':
            case 'python':
            case 'ruby':
                return "/"
            default:
                steps.echo "⚠️ Unknown app type '${appType}', defaulting to root endpoint"
                return "/"
        }
    }
}
