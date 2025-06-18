package org.example

class RunContainer implements Serializable {
    def script

    RunContainer(script) {
        this.script = script
    }

    void run(String containerName, String imageName, String hostPort, String dockerPort, String appType = 'springboot') {
        containerName = containerName.toLowerCase()
        imageName = imageName.toLowerCase()

        script.echo "🐳 Running container '${containerName}' on port ${hostPort}"

        def portFlag = "-p ${hostPort}:${dockerPort}"
        def runArgs  = (appType == 'springboot') ? "--server.port=${dockerPort} --server.address=0.0.0.0" : ""

        // 🔍 Find Dockerfile in the repo
        script.echo "🔍 Searching for Dockerfile..."
        def dockerfilePath = script.sh(
            script: "find . -name Dockerfile -print -quit",
            returnStdout: true
        ).trim()

        if (!dockerfilePath) {
            script.error "❌ Dockerfile not found in repository"
        }

        def contextDir = dockerfilePath.replaceAll('/Dockerfile$', '').replace('\\', '/')
        script.echo "📁 Dockerfile found in: ${dockerfilePath}"
        script.echo "📦 Docker build context: ${contextDir}"

        // 🔧 Build the Docker image
        script.echo "🔧 Building Docker image: ${imageName}"
        script.sh "docker build -t '${imageName}:latest' \"${contextDir}\""

        // 🔁 Stop & remove any existing container
        script.sh "docker stop '${containerName}' || true"
        script.sh "docker rm '${containerName}' || true"

        // 🚀 Run the new container
        script.sh """
            docker run -d --name '${containerName}' \
              --network spring-net \
              ${portFlag} \
              '${imageName}:latest' ${runArgs}
        """

        // 📋 Optional: Show running containers and recent logs
        script.sh "docker ps -a"
        script.sh "docker logs --tail 30 '${containerName}' || true"
    }
}
