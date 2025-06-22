package org.example

class RunContainer implements Serializable {
    def steps

    RunContainer(steps) {
        this.steps = steps
    }

    void run(String containerName, String imageName, String hostPort, String dockerPort, String appType = 'springboot') {
        if (!containerName || !imageName || !hostPort || !dockerPort || !appType)
            steps.error("❌ Missing required parameters.")

        def contextDir = steps.sh(
            script: "find . -name Dockerfile -print -quit",
            returnStdout: true
        ).trim()?.replaceAll('/Dockerfile$', '')

        if (!contextDir) steps.error("❌ Dockerfile not found.")

        // Stop and remove any existing container
        steps.sh "docker stop '${containerName}' || true"
        steps.sh "docker rm '${containerName}' || true"

        // Build the Docker image
        steps.sh "docker build -t '${imageName}:latest' '${contextDir}'"

        // Run the container based on app type
        if (appType.toLowerCase() == 'nginx') {
            steps.sh """
                docker run -d --name '${containerName}' \
                  --network spring-net \
                  -p ${hostPort}:80 \
                  '${imageName}:latest'
            """
        } else if (appType.toLowerCase() == 'springboot') {
            steps.sh """
                docker run -d --name '${containerName}' \
                  --network spring-net \
                  -p ${hostPort}:8080 \
                  '${imageName}:latest' \
                  --server.port=${dockerPort} --server.address=0.0.0.0
            """
        } else {
            steps.error("❌ Unsupported appType '${appType}'. Supported: springboot, nginx")
        }
    }
}
