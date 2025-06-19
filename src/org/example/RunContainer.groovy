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

        steps.sh "docker stop '${containerName}' || true"
        steps.sh "docker rm '${containerName}' || true"
        steps.sh "docker build -t '${imageName}:latest' '${contextDir}'"

        def runArgs = appType.toLowerCase() == 'springboot' ? "--server.port=${dockerPort} --server.address=0.0.0.0" : ""
        steps.sh """
            docker run -d --name '${containerName}' \
              --network spring-net \
              -p ${hostPort}:${dockerPort} \
              '${imageName}:latest' ${runArgs}
        """
    }
}
