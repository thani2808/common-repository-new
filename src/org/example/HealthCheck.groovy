package org.example

class HealthCheck implements Serializable {
    def steps

    HealthCheck(steps) {
        this.steps = steps
    }

    void check(String port, String containerName, String appType = 'springboot') {
        steps.echo "⏳ Starting health check for '${appType}' app on port ${port}"

        // Ensure the script is executable
        steps.sh 'chmod +x health/health-check.sh'

        // Call the script using the correct relative path
        steps.sh "./health/health-check.sh ${port} ${containerName} ${appType}"
    }
}
