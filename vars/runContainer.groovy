def call(String containerName, String imageName, String port, String appType = 'springboot') {
    containerName = containerName.toLowerCase()
    imageName = imageName.toLowerCase()

    def portFlag = appType == 'springboot' ? "-p ${port}:${port}" : "-p ${port}:80"
    def runArgs  = appType == 'springboot' ? "--server.port=${port} --server.address=0.0.0.0" : ""

    sh """
        docker stop "${containerName}" || true
        docker rm "${containerName}" || true

        docker run -d --name "${containerName}" --network spring-net ${portFlag} "${imageName}" ${runArgs}

        docker ps -a
        docker logs --tail 30 "${containerName}" || true
    """
}
