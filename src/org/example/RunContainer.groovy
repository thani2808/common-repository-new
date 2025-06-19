package org.example

class RunContainer implements Serializable {
    def steps

    RunContainer(steps) {
        this.steps = steps
    }

    void run(String containerName, String imageName, String hostPort, String dockerPort, String appType = 'springboot') {
        // Validate inputs
        if (!appType) {
            steps.error("❌ 'appType' is null or not set. Cannot continue.")
        }

        if (!containerName || !imageName || !hostPort || !dockerPort) {
            steps.error("❌ One or more required arguments are missing: containerName, imageName, hostPort, dockerPort")
        }

        def lowerAppType = appType.toLowerCase()
        containerName = containerName.toLowerCase()
        imageName = imageName.toLowerCase()

        steps.echo "🐳 Running container '${containerName}' as type '${lowerAppType}'"

        // Search for Dockerfile
        steps.echo "🔍 Searching for Dockerfile..."
        def dockerfilePath = steps.sh(
            script: "find . -name Dockerfile -print -quit",
            returnStdout: true
        ).trim()

        if (!dockerfilePath) {
            steps.error("❌ Dockerfile not found in repository")
        }

        def contextDir = dockerfilePath.replaceAll('/Dockerfile$', '').replace('\\', '/')
        steps.echo "📁 Dockerfile found: ${dockerfilePath}"
        steps.echo "📦 Docker build context: ${contextDir}"

        // Stop & remove existing container
        steps.sh "docker stop '${containerName}' || true"
        steps.sh "docker rm '${containerName}' || true"

        // Build Docker image
        steps.echo "🔧 Building Docker image: ${imageName}:latest"
        steps.sh "docker build -t '${imageName}:latest' '${contextDir}'"

        // Run container with network and port mapping
        def portMapping = "-p ${hostPort}:${dockerPort}"
        def runArgs = (lowerAppType == 'springboot') ? "--server.port=${dockerPort} --server.address=0.0.0.0" : ""

        steps.echo "🚀 Running container: ${containerName}"
        steps.sh """
            docker run -d --name '${containerName}' \
              --network spring-net \
              ${portMapping} \
              '${imageName}:latest' ${runArgs}
        """

        // Optional: Docker diagnostics
        steps.sh "docker ps -a"
        steps.sh "docker logs --tail 30 '${containerName}' || true"
    }
}
