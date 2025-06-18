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

        // ✅ Build Docker image before running (no pushd/popd needed)
        script.echo "🔧 Building Docker image: ${imageName}"
        script.sh "docker build -t '${imageName}:latest' ."

        // 🔁 Clean up existing container (if any)
        script.sh "docker stop '${containerName}' || true"
        script.sh "docker rm '${containerName}' || true"

        // 🐳 Run the container
        script.sh """
            docker run -d --name '${containerName}' \
              --network spring-net \
              ${portFlag} \
              '${imageName}:latest' ${runArgs}
        """

        // Optional logging
        script.sh "docker ps -a"
        script.sh "docker logs --tail 30 '${containerName}' || true"
    }
}
